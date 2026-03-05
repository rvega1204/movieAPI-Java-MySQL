package com.rvg.movieapi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * File system implementation of {@link FileService}.
 * <p>
 * Handles storing uploaded files to a local directory and retrieving
 * them as input streams. The target directory is created automatically
 * if it does not exist at the time of upload.
 */
@Service
public class FileServiceImpl implements FileService {

    /**
     * Uploads a file to the specified directory path.
     * <p>
     * Creates the target directory if it does not exist, then copies
     * the file content using {@link Files#copy}. Uses the original
     * file name as the stored file name.
     *
     * @param path the target directory path
     * @param file the multipart file to store
     * @return the original file name used to store the file
     * @throws IOException if the file cannot be read or written
     */
    @Override
    public String uploadFile(String path, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String filePath = path + File.separator + fileName;
        File f = new File(path);

        if (!f.exists()) {
            f.mkdirs();
        }

        Files.copy(file.getInputStream(), Paths.get(filePath));
        return fileName;
    }

    /**
     * Returns an {@link InputStream} for reading the specified file.
     * <p>
     * Constructs the full path by joining the base directory and the
     * file name, then opens a {@link FileInputStream} to the file.
     *
     * @param path     the base directory path
     * @param fileName the name of the file to retrieve
     * @return an open {@link InputStream} for the file
     * @throws FileNotFoundException if no file exists at the constructed path
     */
    @Override
    public InputStream getResourceFile(String path, String fileName) throws FileNotFoundException {
        String fullPath = path + File.separator + fileName;
        return new FileInputStream(fullPath);
    }
}
