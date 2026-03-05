package com.rvg.movieapi.controllers;

import com.rvg.movieapi.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

/**
 * REST controller for file upload and retrieval operations.
 * <p>
 * Exposes two endpoints under {@code /api/v1/file}:
 * <ul>
 *   <li>{@code POST /upload} — uploads a file to the configured poster directory</li>
 *   <li>{@code GET /{fileName}} — serves a file by name as a binary stream</li>
 * </ul>
 * The base storage path is injected from {@code project.poster}.
 */
@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Value("${project.poster}")
    private String path;

    /**
     * Uploads a file to the poster directory.
     * <p>
     * Delegates storage to {@link FileService#uploadFile(String, MultipartFile)}.
     * Returns 400 if the uploaded file is empty.
     *
     * @param file the multipart file to upload
     * @return 200 OK with the stored file name, or 400 if the file is empty
     * @throws Exception if the file cannot be stored
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) throws Exception {
        String fileName = fileService.uploadFile(path, file);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        return ResponseEntity.ok("File uploaded: " + fileName);
    }

    /**
     * Serves a stored file as a binary stream.
     * <p>
     * Detects the content type from the file name using {@link URLConnection#guessContentTypeFromName}.
     * Falls back to {@code application/octet-stream} if the type cannot be determined.
     * <p>
     * Returns 400 if the file name contains path traversal sequences ({@code ..}),
     * and 404 if the file does not exist on disk.
     *
     * @param fileName the name of the file to serve (supports extensions via {@code :.+})
     * @param response the HTTP response used to stream the file content
     * @throws IOException if reading the file or writing the response fails
     */
    @GetMapping("/{fileName:.+}")
    public void serveFile(@PathVariable String fileName,
                          HttpServletResponse response) throws IOException {

        if (fileName.contains("..")) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid file name");
            return;
        }

        try (InputStream inputStream = fileService.getResourceFile(path, fileName);
             OutputStream outputStream = response.getOutputStream()) {

            if (inputStream == null) {
                response.sendError(HttpStatus.NOT_FOUND.value(), "File not found");
                return;
            }

            String contentType = URLConnection.guessContentTypeFromName(fileName);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            response.setContentType(contentType);

            StreamUtils.copy(inputStream, outputStream);
            outputStream.flush();
        }
    }
}
