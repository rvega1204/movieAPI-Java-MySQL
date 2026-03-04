package com.rvg.movieapi.controllers;

import com.rvg.movieapi.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Value("${project.poster}")
    private String path;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) throws Exception {
        String fileName = fileService.uploadFile(path, file);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        return ResponseEntity.ok("File uploaded: " + fileName);
    }

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
