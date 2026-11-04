package com.example.Oboe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// Lưu file vào thư mục local thay cho AWS S3.
@Service
public class FileStorageService {

    @Value("${app.file-storage.dir:uploads}")
    private String storageDir;

    @Value("${app.file-storage.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String cleanFolder = folder.endsWith("/") ? folder.substring(0, folder.length() - 1) : folder;
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path targetDir = Paths.get(storageDir, cleanFolder).toAbsolutePath();
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(fileName);
        file.transferTo(targetFile.toFile());

        return baseUrl + "/" + cleanFolder + "/" + fileName;
    }
}
