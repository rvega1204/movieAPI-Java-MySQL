package com.rvg.movieapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface FileService {

    String uploadFile(String path, MultipartFile file) throws Exception;

    InputStream getResourceFile(String path, String fileName) throws FileNotFoundException;
}
