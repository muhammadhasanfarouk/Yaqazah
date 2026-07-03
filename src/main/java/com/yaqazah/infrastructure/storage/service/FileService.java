package com.yaqazah.infrastructure.storage.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileService {

    private final BlobServiceClient blobServiceClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    /**
     * Upload Base64 image (Used for automated screenshot capturing)
     */


    public String uploadImage(MultipartFile file, String fileName) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty.");
            }

            if (!"image/jpeg".equals(file.getContentType())) {
                throw new IllegalArgumentException("Only JPEG images are allowed.");
            }

            String originalName = Objects.requireNonNull(file.getOriginalFilename());
            String extension = originalName.substring(originalName.lastIndexOf('.'));

            // Append the extension if the caller didn't include one
            if (!fileName.endsWith(extension)) {
                fileName += extension;
            }

            BlobContainerClient containerClient =
                    blobServiceClient.getBlobContainerClient(containerName);

            BlobClient blobClient = containerClient.getBlobClient(fileName);

            blobClient.upload(file.getInputStream(), file.getSize(), true);

            blobClient.setHttpHeaders(
                    new BlobHttpHeaders().setContentType(file.getContentType())
            );

            return blobClient.getBlobUrl();

        } catch (IOException e) {
            throw new RuntimeException("Azure upload failed", e);
        }
    }

    public String uploadBase64(String base64String, String fileName) {
        try {
            String cleanBase64 = base64String.contains(",")
                    ? base64String.split(",")[1]
                    : base64String;

            byte[] bytes = Base64.getDecoder().decode(cleanBase64);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            // Upload byte array stream
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                blobClient.upload(inputStream, bytes.length, true);
            }

            // Set content type explicitly to image/jpeg so browsers view instead of download
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType("image/jpeg");
            blobClient.setHttpHeaders(headers);

            return blobClient.getBlobUrl();

        } catch (IOException e) {
            throw new RuntimeException("Azure upload failed", e);
        }
    }
}