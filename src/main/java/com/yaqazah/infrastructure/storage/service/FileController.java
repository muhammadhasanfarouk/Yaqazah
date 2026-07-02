//package com.yaqazah.infrastructure.storage.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/files")
//@RequiredArgsConstructor
//public class FileController {
//
//    private final FileService fileService;
//
//    /**
//     * Endpoint to upload a standard MultipartFile (e.g., from a web form)
//     * POST /api/files/upload
//     */
//
//    @PostMapping("/upload")
//    public ResponseEntity<String> upload(
//            @RequestParam("image") MultipartFile image) {
//
//        String url = fileService.uploadImage(image, "driver-distracted-timestamp.jpg");
//
//        return ResponseEntity.ok(url);
//    }
//
////    @PostMapping("/upload")
////    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
////        if (file.isEmpty()) {
////            return ResponseEntity.badRequest().body("File is empty");
////        }
////
////        try {
////            String fileUrl = fileService.uploadFile(file);
////            return ResponseEntity.ok(fileUrl);
////        } catch (RuntimeException e) {
////            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
////        }
////    }
//
//    /**
//     * Endpoint to upload a Base64 image (Used for automated screenshot capturing)
//     * POST /api/files/upload/base64
//     * * Expects a JSON body like:
//     * {
//     * "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABA...",
//     * "fileName": "driver-distracted-timestamp.jpg"
//     * }
//     */
//
//    @DeleteMapping("/{fileName}")
//    public ResponseEntity<Void> deleteImage(@PathVariable String fileName) {
//        fileService.deleteImage(fileName);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PostMapping("/upload/base64")
//    public ResponseEntity<String> uploadBase64(@RequestBody Map<String, String> payload) {
//        String base64String = payload.get("image");
//        String fileName = payload.get("fileName");
//
//        if (base64String == null || base64String.isEmpty() || fileName == null || fileName.isEmpty()) {
//            return ResponseEntity.badRequest().body("Image data and fileName are required");
//        }
//
//        try {
//            String fileUrl = fileService.uploadBase64(base64String, fileName);
//            return ResponseEntity.ok(fileUrl);
//        } catch (RuntimeException e) {
//            return ResponseEntity.internalServerError().body("Failed to upload screenshot: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Endpoint to delete a file by its Azure Blob name
//     * DELETE /api/files/{fileName}
//     */
////    @DeleteMapping("/{fileName}")
////    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
////        try {
////            fileService.deleteFile(fileName);
////            return ResponseEntity.ok("File deleted successfully");
////        } catch (RuntimeException e) {
////            return ResponseEntity.internalServerError().body("Failed to delete file: " + e.getMessage());
////        }
////    }
//}