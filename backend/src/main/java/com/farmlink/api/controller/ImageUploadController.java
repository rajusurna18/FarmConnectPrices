package com.farmlink.api.controller;

import com.farmlink.api.dto.CloudinaryUploadResult;
import com.farmlink.api.exception.InvalidImageSignatureException;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Dedicated REST controller for image upload and asset management via Cloudinary.
 * All operations require valid Firebase Authentication.
 */
@RestController
@RequestMapping("/api/v1/images")
public class ImageUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadController.class);

    private final CloudinaryService cloudinaryService;

    public ImageUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Upload an image to Cloudinary.
     * Supports crops, products, profiles, diagnostics, and general assets.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication
    ) {
        String uid = extractUid(authentication);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication is required to upload images."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File parameter is required and must not be empty."));
        }

        try {
            CloudinaryUploadResult result = cloudinaryService.uploadImage(file, folder, uid);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (InvalidImageSignatureException e) {
            logger.warn("Image signature validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid image upload argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Cloudinary service state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            logger.error("I/O error during image upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during image upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unexpected error during image upload."));
        }
    }

    /**
     * Delete an owned image asset from Cloudinary.
     * Enforces ownership validation against the authenticated caller.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteImage(
            @RequestParam("publicId") String publicId,
            Authentication authentication
    ) {
        String uid = extractUid(authentication);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication is required to delete images."));
        }

        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "publicId parameter is required."));
        }

        try {
            boolean deleted = cloudinaryService.deleteImage(publicId, uid);
            if (deleted) {
                return ResponseEntity.ok(Map.of("status", "DELETED", "publicId", publicId));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Asset not found: " + publicId));
            }
        } catch (AccessDeniedException e) {
            logger.warn("Access denied deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete image {}: {}", publicId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete image: " + e.getMessage()));
        }
    }

    private String extractUid(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof FirebaseAuthenticationToken token) {
            return token.getUid();
        }
        String name = authentication.getName();
        return (name != null && !name.isBlank()) ? name : null;
    }
}
