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
import java.util.Set;
import java.util.UUID;

/**
 * Stores the CV a candidate attaches to a booking.
 *
 * Same rules as the other two stores - we generate the filename, we check the
 * real bytes, the file lands inside one directory - with a deliberately narrow
 * allow-list. Its own class rather than reusing MaterialStorage because a CV is
 * not study material: that one accepts ZIPs, images and spreadsheets, none of
 * which is a CV, and widening a store's allow-list to fit a second caller is how
 * an upload endpoint quietly becomes a general-purpose file drop.
 *
 * <h2>A CV is personal data</h2>
 * These carry a phone number, an address, sometimes a date of birth. Two things
 * follow, and both are enforced elsewhere but worth knowing here:
 *
 * <ul>
 *   <li>The file is served only through an endpoint that checks who is asking -
 *       the owner, the mentor actually assigned to that interview, and admins.
 *       Never from a static directory.</li>
 *   <li>The stored name is a UUID. The candidate's own filename is usually their
 *       full name, so a predictable path would leak who has applied to what
 *       before any access check ran.</li>
 * </ul>
 */
@Component
public class CvStorage {

    /**
     * PDF, DOC, DOCX. Nothing else is a CV.
     *
     * No images: a photographed CV is unreadable at interview time and cannot be
     * searched. No ZIPs, no HTML - the second one can carry script, and serving
     * it back to a mentor from our own origin would be stored XSS.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    /** A CV that does not fit in this is not a CV. */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public CvStorage(@Value("${app.cv.dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    /** Saves the upload and returns the generated filename. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attach a CV, or leave it out");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Your CV must be under 5 MB");
        }

        String extension = resolveExtension(file);

        // Our own name. The candidate's filename is usually their full name, so
        // it must not become a guessable path.
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(filename).normalize();

        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid file");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Could not save the CV to " + root, e);
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

    /** Deletes a replaced CV. A booking keeps one, not a pile of old ones. */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(pathOf(filename));
        } catch (IOException e) {
            // Not worth failing the replacement over: the new CV is already
            // saved and the row points at it. An orphan on disk is a tidiness
            // problem, not a correctness one.
            throw new StorageException("Could not delete the old CV", e);
        }
    }

    public String contentTypeOf(MultipartFile file) {
        return switch (extensionOf(originalName(file))) {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    /** A display name safe to put in a Content-Disposition header. */
    public String safeDisplayName(MultipartFile file) {
        String name = originalName(file);
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\"\\\\]", "").trim();
        if (name.isEmpty()) {
            name = "cv" + extensionOf(originalName(file));
        }
        return name.length() > 160 ? name.substring(0, 160) : name;
    }

    /**
     * The extension, checked against the bytes where the bytes say anything.
     *
     * A PDF has a magic number and is checked properly. .doc and .docx do not
     * have one we can rely on - .docx is a ZIP container and .doc is an OLE
     * compound file - so for those the extension is what there is. Both are
     * served as an attachment with nosniff, so neither is interpreted by the
     * browser regardless.
     */
    private String resolveExtension(MultipartFile file) {
        String claimed = extensionOf(originalName(file));

        if (!ALLOWED_EXTENSIONS.contains(claimed)) {
            throw new BadRequestException(
                    "Upload your CV as a PDF or Word document. PDF is best - it looks the same "
                            + "on the interviewer's machine as it does on yours.");
        }

        if (".pdf".equals(claimed) && !looksLikePdf(file)) {
            throw new BadRequestException(
                    "That file is named .pdf but is not a PDF. Export it again from your editor.");
        }
        return claimed;
    }

    private boolean looksLikePdf(MultipartFile file) {
        byte[] head = new byte[4];
        try (InputStream in = file.getInputStream()) {
            // readNBytes, not read: read() may return fewer bytes than asked for
            // even when more are coming, which would reject a valid file.
            if (in.readNBytes(head, 0, head.length) < head.length) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null ? "" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }
}
