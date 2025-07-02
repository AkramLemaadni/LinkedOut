package ma.linkedout.linkedout.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Store a file in the file system and return the file path
     * 
     * @param file The file to store
     * @param subdirectory The subdirectory to store the file in (e.g., "cv", "profile", "company")
     * @return The relative path to the stored file
     * @throws IOException If an error occurs during file storage
     */
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, subdirectory);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                logger.info("Created directory: {}", uploadPath);
            } catch (IOException e) {
                logger.error("Failed to create upload directory: {}", uploadPath, e);
                throw new IOException("Failed to create upload directory: " + uploadPath);
            }
        }
        // Check if directory is writable
        if (!Files.isWritable(uploadPath)) {
            logger.error("Upload directory is not writable: {}", uploadPath);
            throw new IOException("Upload directory is not writable: " + uploadPath);
        }
        
        // Generate a unique filename to prevent overwrites
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + fileExtension;
        
        // Save the file
        try (InputStream inputStream = file.getInputStream()) {
            Path filePath = uploadPath.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file: {}", filePath);
            
            // Return the relative path
            return subdirectory + "/" + filename;
        } catch (IOException e) {
            logger.error("Failed to store file", e);
            throw new IOException("Failed to store file: " + e.getMessage());
        }
    }
    
    /**
     * Delete a file from the file system
     * 
     * @param filePath The relative path to the file
     * @return true if the file was deleted, false otherwise
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * Get the absolute path to a file
     * 
     * @param filePath The relative path to the file
     * @return The absolute path to the file
     */
    public Path getFilePath(String filePath) {
        return Paths.get(uploadDir, filePath);
    }
} 