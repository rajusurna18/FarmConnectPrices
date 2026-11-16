package com.farmlink.api.dto;

/**
 * Encapsulates the authoritative upload result returned by Cloudinary.
 * Used for persisting image references in Firestore documents.
 */
public record CloudinaryUploadResult(
        String publicId,
        String secureUrl,
        String format,
        long sizeBytes,
        int width,
        int height,
        String folder,
        String createdAt
) {}
