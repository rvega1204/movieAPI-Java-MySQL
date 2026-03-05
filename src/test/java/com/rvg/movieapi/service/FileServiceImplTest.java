package com.rvg.movieapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceImplTest {

    private final FileServiceImpl fileService = new FileServiceImpl();

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // uploadFile()
    // -------------------------------------------------------------------------

    @Test
    void uploadFile_storesFileInTargetDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "image-content".getBytes());

        fileService.uploadFile(tempDir.toString(), file);

        assertThat(tempDir.resolve("poster.jpg")).exists();
    }

    @Test
    void uploadFile_returnsOriginalFileName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "image-content".getBytes());

        String result = fileService.uploadFile(tempDir.toString(), file);

        assertThat(result).isEqualTo("poster.jpg");
    }

    @Test
    void uploadFile_storesCorrectFileContent() throws Exception {
        byte[] content = "image-content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", content);

        fileService.uploadFile(tempDir.toString(), file);

        byte[] stored = Files.readAllBytes(tempDir.resolve("poster.jpg"));
        assertThat(stored).isEqualTo(content);
    }

    @Test
    void uploadFile_createsDirectoryIfNotExists() throws Exception {
        Path nonExistentDir = tempDir.resolve("new-folder");
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "content".getBytes());

        fileService.uploadFile(nonExistentDir.toString(), file);

        assertThat(nonExistentDir).exists().isDirectory();
        assertThat(nonExistentDir.resolve("poster.jpg")).exists();
    }

    // -------------------------------------------------------------------------
    // getResourceFile()
    // -------------------------------------------------------------------------

    @Test
    void getResourceFile_whenFileExists_returnsInputStream() throws Exception {
        Path file = tempDir.resolve("poster.jpg");
        Files.write(file, "image-content".getBytes());

        InputStream result = fileService.getResourceFile(tempDir.toString(), "poster.jpg");

        assertThat(result).isNotNull();
        result.close();
    }

    @Test
    void getResourceFile_whenFileExists_streamContainsCorrectContent() throws Exception {
        byte[] content = "image-content".getBytes();
        Files.write(tempDir.resolve("poster.jpg"), content);

        InputStream result = fileService.getResourceFile(tempDir.toString(), "poster.jpg");

        assertThat(result.readAllBytes()).isEqualTo(content);
        result.close();
    }

    @Test
    void getResourceFile_whenFileNotFound_throwsFileNotFoundException() {
        assertThatThrownBy(() -> fileService.getResourceFile(tempDir.toString(), "missing.jpg"))
                .isInstanceOf(FileNotFoundException.class);
    }
}