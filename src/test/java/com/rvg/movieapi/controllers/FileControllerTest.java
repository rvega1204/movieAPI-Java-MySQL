package com.rvg.movieapi.controllers;

import com.rvg.movieapi.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private FileController fileController;

    private static final String TEST_PATH = "/uploads/posters";

    void setUp() {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
    }

    // -------------------------------------------------------------------------
    // uploadFile()
    // -------------------------------------------------------------------------

    @Test
    void uploadFile_whenFileIsValid_returns200WithFileName() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "content".getBytes());
        when(fileService.uploadFile(TEST_PATH, file)).thenReturn("poster.jpg");

        ResponseEntity<String> result = fileController.uploadFile(file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).contains("poster.jpg");
    }

    @Test
    void uploadFile_whenFileIsEmpty_returns400() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);
        when(fileService.uploadFile(TEST_PATH, emptyFile)).thenReturn("empty.jpg");

        ResponseEntity<String> result = fileController.uploadFile(emptyFile);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isEqualTo("File is empty");
    }

    @Test
    void uploadFile_delegatesToFileService() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "content".getBytes());
        when(fileService.uploadFile(TEST_PATH, file)).thenReturn("poster.jpg");

        fileController.uploadFile(file);

        verify(fileService).uploadFile(TEST_PATH, file);
    }

    // -------------------------------------------------------------------------
    // serveFile()
    // -------------------------------------------------------------------------

    @Test
    void serveFile_whenFileNameContainsPathTraversal_sends400() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);

        fileController.serveFile("../secret.txt", response);

        verify(response).sendError(HttpStatus.BAD_REQUEST.value(), "Invalid file name");
        verifyNoInteractions(fileService);
    }

    @Test
    void serveFile_whenFileNotFound_sends404() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        when(fileService.getResourceFile(TEST_PATH, "missing.jpg")).thenReturn(null);
        when(response.getOutputStream()).thenReturn(
                new jakarta.servlet.ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(jakarta.servlet.WriteListener l) {
                    }

                    @Override
                    public void write(int b) {
                    }
                });

        fileController.serveFile("missing.jpg", response);

        verify(response).sendError(HttpStatus.NOT_FOUND.value(), "File not found");
    }

    @Test
    void serveFile_whenFileExists_setsContentTypeAndStreamsContent() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

        when(fileService.getResourceFile(TEST_PATH, "poster.jpg")).thenReturn(inputStream);

        // Capture what gets written to the response output stream
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        jakarta.servlet.ServletOutputStream servletOutputStream = new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener l) {
            }

            @Override
            public void write(int b) {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                outputStream.write(b, off, len);
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        fileController.serveFile("poster.jpg", response);

        verify(response).setContentType("image/jpeg");
        assertThat(outputStream.toByteArray()).isEqualTo(content);
    }

    @Test
    void serveFile_whenContentTypeUnknown_usesOctetStream() throws Exception {
        ReflectionTestUtils.setField(fileController, "path", TEST_PATH);
        ByteArrayInputStream inputStream = new ByteArrayInputStream("data".getBytes());

        when(fileService.getResourceFile(TEST_PATH, "file.bin")).thenReturn(inputStream);

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        jakarta.servlet.ServletOutputStream servletOutputStream = new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener l) {
            }

            @Override
            public void write(int b) {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                outputStream.write(b, off, len);
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        fileController.serveFile("file.bin", response);

        verify(response).setContentType("application/octet-stream");
    }
}