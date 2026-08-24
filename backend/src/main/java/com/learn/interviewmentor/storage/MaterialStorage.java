package com.learn.interviewmentor.storage;

import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores study material files on disk.
 *
 * Same rules as {@link ScreenshotStorage}, and for the same reasons - we
 * generate the filename, we sniff the real bytes, the file lands inside one
 * directory and nowhere else - with a wider allow-list, because notes arrive as
 * PDFs and slide decks rather than photos.
 *
 * Two things are worth spelling out:
 *
 *  - **The uploader is an admin, and that changes nothing.** A trusted role is
 *    not a reason to skip validation: an admin account can be phished, and a
 *    malicious file served back to hundreds of students is a worse outcome than
 *    one bad screenshot.
 *  - **HTML and SVG are refused.** Both can carry script. Served from our own
 *    origin to a logged-in student, one would be stored XSS with a token sitting
 *    in localStorage next to it. Everything here goes out as an attachment with
 *    nosniff as well; refusing the type is the belt to that braces.
 */
@Component
public class MaterialStorage {

    /** Extension by magic-number signature. The Content-Type header is a claim, not evidence. */
    private static final Map<String, String> BY_SIGNATURE = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/zip", ".zip"
    );

    /**
     * Office formats and plain text have no usable magic number of their own -
     * .docx and .pptx are ZIP containers, and a .txt is just bytes - so for
     * these the extension is all there is to go on. They are accepted because a
     * mentor's notes realistically arrive as one of them, and none of them is
     * interpreted by the browser when served as an attachment with nosniff.
     */
    private static final Set<String> BY_EXTENSION = Set.of(
            ".txt", ".md", ".csv", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx"
    );

    /** Big enough for a slide deck, small enough not to fill the disk. */
    private static final long MAX_BYTES = 25L * 1024 * 1024;

    private final Path root;

    public MaterialStorage(@Value("${app.materials.dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    /** Saves the upload and returns the generated filename. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attach a file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("The file must be under 25 MB");
        }

        String extension = resolveExtension(file);

        // Our own name. Nothing the client sent goes into the path - a crafted
        // name like "../../application.properties" would otherwise write
        // outside this directory.
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(filename).normalize();

        // Belt and braces: even a name we generated must land inside root.
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid file");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Could not save study material to " + root, e);
        }
        return filename;
    }

    public Path pathOf(String filename) {
        Path p = root.resolve(filename).normalize();
        if (!p.startsWith(root)) {
            throw new BadRequestException("Invalid file");
        }
        return p;
    }

    /**
     * What we will serve this file back as.
     *
     * Never the type the client claimed. For the sniffable formats it is the one
     * the bytes prove; for the Office formats it is derived from the extension
     * we allowed.
     */
    public String contentTypeOf(MultipartFile file) {
        String sniffed = detectType(file);
        if (BY_SIGNATURE.containsKey(sniffed)) {
            return sniffed;
        }
        return switch (extensionOf(originalName(file))) {
            case ".txt", ".md" -> "text/plain";
            case ".csv" -> "text/csv";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    /**
     * A display name safe to put in a Content-Disposition header.
     *
     * Strips any directory part and anything that could break out of the quoted
     * filename in the header itself.
     */
    public String safeDisplayName(MultipartFile file) {
        String name = originalName(file);
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\"\\\\]", "").trim();
        if (name.isEmpty()) {
            name = "study-material";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private String resolveExtension(MultipartFile file) {
        String sniffed = detectType(file);
        String fromBytes = BY_SIGNATURE.get(sniffed);

        String claimed = extensionOf(originalName(file));

        // A .docx really is a ZIP, so a ZIP signature with an Office extension
        // keeps the Office extension - otherwise every deck would download as
        // a .zip the student then has to work out what to do with.
        if ("application/zip".equals(sniffed) && BY_EXTENSION.contains(claimed)) {
            return claimed;
        }
        if (fromBytes != null) {
            return fromBytes;
        }
        if (BY_EXTENSION.contains(claimed)) {
            return claimed;
        }
        throw new BadRequestException(
                "Upload a PDF, image, ZIP, or an Office or text document. "
                        + "HTML and SVG are not allowed because they can carry scripts.");
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null ? "" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    /** Sniffs the leading bytes. Callers can set any header they like. */
    private String detectType(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            // readNBytes, not read: read() may return fewer bytes than asked for
            // even when more are coming, which would misjudge a valid file.
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return "unknown";
        }
        if (read < 4) {
            return "unknown";
        }

        if (head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') {
            return "application/pdf";
        }
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "image/png";
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "image/webp";
        }
        // "PK\3\4" - a real ZIP, and also every .docx / .pptx / .xlsx.
        if (head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) {
            return "application/zip";
        }
        return "unknown";
    }
}
