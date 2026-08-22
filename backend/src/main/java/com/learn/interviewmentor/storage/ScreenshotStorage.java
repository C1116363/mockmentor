package com.learn.interviewmentor.storage;

import com.learn.interviewmentor.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores payment screenshots on disk.
 *
 * File upload is the single easiest place to open a hole in a web app, so this
 * class is deliberately strict:
 *
 *  - **We generate the filename.** The name the browser sends is never used.
 *    A crafted name like "../../application.properties" would otherwise let a
 *    caller write outside the upload directory.
 *  - **Allow-list of image types**, checked against the real bytes, not the
 *    Content-Type header the client claimed.
 *  - **SVG is rejected** even though it is an image. SVG can contain script,
 *    so serving one back to an admin would be stored XSS.
 *  - **Size cap**, so nobody fills the disk.
 *
 * Files live outside the repo and are served only through an endpoint that
 * checks who is asking - a screenshot of somebody's bank app is private.
 */
@Component
public class ScreenshotStorage {

    /** Extension by magic-number signature. Header lies; bytes don't. */
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final Path root;

    public ScreenshotStorage(@Value("${app.uploads.dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    /** Saves the upload and returns the generated filename. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attach a screenshot of the payment");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Screenshot must be under 5 MB");
        }

        String detected = detectType(file);
        String extension = ALLOWED.get(detected);
        if (extension == null) {
            throw new BadRequestException("Upload a JPG, PNG or WebP image");
        }

        // Our own name. Nothing from the client goes into the path.
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = root.resolve(filename).normalize();

        // Belt and braces: even a generated name must land inside root.
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid file");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the screenshot", e);
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

    /** The content type we detected, which is what we store and serve back. */
    public String contentTypeOf(MultipartFile file) {
        return detectType(file);
    }

    /**
     * Sniffs the leading bytes. A caller can set any Content-Type they like, so
     * trusting the header would mean trusting the attacker.
     */
    private String detectType(MultipartFile file) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 12) {
                return "unknown";
            }
        } catch (IOException e) {
            return "unknown";
        }

        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "image/png";
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "image/webp";
        }
        return "unknown";
    }
}
