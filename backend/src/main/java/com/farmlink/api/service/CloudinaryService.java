package com.farmlink.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.farmlink.api.config.CloudinaryConfig;
import com.farmlink.api.dto.CloudinaryUploadResult;
import com.farmlink.api.exception.InvalidImageSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dedicated service for all image-storage operations using Cloudinary.
 * Replaces Firebase Cloud Storage for image files across FarmLink AI.
 */
@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);

    public static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    public static final int HEADER_MAGIC_BYTES_LENGTH = 16;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> APPROVED_FOLDERS = Set.of(
            "crops",
            "products",
            "profiles",
            "diagnostics",
            "general"
    );

    private final CloudinaryConfig cloudinaryConfig;
    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryService(
            CloudinaryConfig cloudinaryConfig,
            @Autowired(required = false) Cloudinary cloudinary
    ) {
        this.cloudinaryConfig = cloudinaryConfig;
        this.cloudinary = cloudinary;
    }

    /**
     * Checks if the Cloudinary client is configured and available.
     */
    public boolean isConfigured() {
        return cloudinaryConfig != null && cloudinaryConfig.isConfigured() && cloudinary != null;
    }

    /**
     * Upload an image from a Spring MultipartFile.
     */
    public CloudinaryUploadResult uploadImage(MultipartFile file, String folderCategory, String authenticatedUid) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be null or empty.");
        }
        return uploadImage(file.getBytes(), file.getOriginalFilename(), file.getContentType(), folderCategory, authenticatedUid);
    }

    /**
     * Upload an image from raw bytes with full validation.
     */
    public CloudinaryUploadResult uploadImage(
            byte[] imageBytes,
            String originalFileName,
            String declaredContentType,
            String folderCategory,
            String authenticatedUid
    ) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured. Image upload unavailable.");
        }
        if (authenticatedUid == null || authenticatedUid.isBlank()) {
            throw new IllegalArgumentException("Authenticated user ID is required for image upload.");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image content must not be empty.");
        }

        // 1. Size Validation
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image size (" + imageBytes.length + " bytes) exceeds maximum permitted limit (" + MAX_IMAGE_SIZE_BYTES + " bytes).");
        }

        // 2. MIME & Extension Validation
        String normalizedContentType = declaredContentType != null ? declaredContentType.toLowerCase(Locale.ROOT).trim() : "image/jpeg";
        if (!ALLOWED_MIME_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("Unsupported image Content-Type: " + declaredContentType + ". Allowed: " + ALLOWED_MIME_TYPES);
        }

        // 3. Server-Side Magic Byte Signature Verification
        verifyMagicBytes(imageBytes, normalizedContentType, originalFileName);

        // 4. Resolve folder path: farmlink/<folderCategory>/<authenticatedUid>
        String sanitizedCategory = sanitizeFolderCategory(folderCategory);
        String targetFolder = "farmlink/" + sanitizedCategory + "/" + authenticatedUid;
        String publicId = targetFolder + "/" + UUID.randomUUID().toString();

        // 5. Cloudinary Upload Params
        Map<?, ?> params = ObjectUtils.asMap(
                "public_id", publicId,
                "folder", targetFolder,
                "overwrite", true,
                "resource_type", "image"
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(imageBytes, params);

            if (result == null || result.get("public_id") == null || result.get("secure_url") == null) {
                throw new IOException("Cloudinary returned invalid or empty upload result for publicId " + publicId);
            }

            String uploadedPublicId = (String) result.get("public_id");
            String secureUrl = (String) result.get("secure_url");
            String format = (String) result.get("format");
            Number bytes = (Number) result.get("bytes");
            Number width = (Number) result.get("width");
            Number height = (Number) result.get("height");
            String createdAt = (String) result.get("created_at");

            logger.info("Successfully uploaded image to Cloudinary: publicId={}, folder={}", uploadedPublicId, targetFolder);

            return new CloudinaryUploadResult(
                    uploadedPublicId,
                    secureUrl,
                    format != null ? format : "",
                    bytes != null ? bytes.longValue() : imageBytes.length,
                    width != null ? width.intValue() : 0,
                    height != null ? height.intValue() : 0,
                    targetFolder,
                    createdAt != null ? createdAt : ""
            );
        } catch (Exception e) {
            logger.error("Cloudinary upload failed for publicId {}: {}", publicId, e.getMessage());
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Authorized image deletion.
     * Enforces ownership validation against the authenticated UID.
     */
    public boolean deleteImage(String publicId, String authenticatedUid) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured. Image deletion unavailable.");
        }
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("publicId must not be null or blank.");
        }
        if (authenticatedUid == null || authenticatedUid.isBlank()) {
            throw new IllegalArgumentException("Authenticated user ID is required for image deletion.");
        }

        // Ownership enforcement: publicId must contain the authenticatedUid
        validateOwnership(publicId, authenticatedUid);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> destroyResult = (Map<String, Object>) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = destroyResult != null ? (String) destroyResult.get("result") : null;
            logger.info("Cloudinary deletion for publicId {}: status={}", publicId, resultStatus);
            if ("ok".equalsIgnoreCase(resultStatus)) {
                return true;
            } else if ("not found".equalsIgnoreCase(resultStatus)) {
                return false;
            } else {
                throw new IOException("Cloudinary destroy returned unexpected status: " + resultStatus);
            }
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Cloudinary deletion failed for publicId {}: {}", publicId, e.getMessage());
            throw new IOException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Inspects Cloudinary asset metadata.
     */
    public AssetInfo getAssetInfo(String publicId) {
        if (!isConfigured() || publicId == null || publicId.isBlank()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            if (resource == null) {
                return null;
            }
            String pId = (String) resource.get("public_id");
            String secureUrl = (String) resource.get("secure_url");
            String format = (String) resource.get("format");
            Number bytes = (Number) resource.get("bytes");
            Number width = (Number) resource.get("width");
            Number height = (Number) resource.get("height");
            String mime = format != null ? "image/" + format.toLowerCase(Locale.ROOT) : "image/jpeg";

            return new AssetInfo(
                    pId,
                    secureUrl,
                    format,
                    bytes != null ? bytes.longValue() : 0L,
                    width != null ? width.intValue() : 0,
                    height != null ? height.intValue() : 0,
                    mime
            );
        } catch (Exception e) {
            logger.warn("Could not retrieve Cloudinary resource for publicId {}: {}", publicId, e.getMessage());
            return null;
        }
    }

    private void validateOwnership(String publicId, String authenticatedUid) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("publicId must not be blank.");
        }
        String clean = publicId.trim();
        // Prevent path traversal and malformed inputs
        if (clean.contains("..") || clean.contains("\\") || clean.startsWith("/")) {
            throw new AccessDeniedException("Invalid publicId format.");
        }

        String[] parts = clean.split("/");
        // Format 1: farmlink/<category>/<ownerUid>/<assetId>
        if (parts.length >= 4 && "farmlink".equalsIgnoreCase(parts[0])) {
            String ownerSegment = parts[2];
            if (ownerSegment.equals(authenticatedUid)) {
                return;
            }
        }
        // Format 2: users/<ownerUid>/diagnostics/...
        if (parts.length >= 3 && "users".equalsIgnoreCase(parts[0])) {
            String ownerSegment = parts[1];
            if (ownerSegment.equals(authenticatedUid)) {
                return;
            }
        }

        logger.warn("Security Alert: User {} attempted to delete unowned asset {}", authenticatedUid, clean);
        throw new AccessDeniedException("Ownership violation: User " + authenticatedUid + " does not own asset " + clean);
    }

    private String sanitizeFolderCategory(String category) {
        if (category == null || category.isBlank()) {
            return "general";
        }
        String clean = category.toLowerCase(Locale.ROOT).trim();
        if (APPROVED_FOLDERS.contains(clean)) {
            return clean;
        }
        return "general";
    }

    /**
     * Bounded server-side magic byte signature verification for JPEG, PNG, WebP, and GIF.
     */
    public void verifyMagicBytes(byte[] bytes, String contentType, String fileName) {
        if (bytes.length < 12) {
            throw new InvalidImageSignatureException("Image file size is too small for signature verification: " + fileName);
        }

        boolean valid = switch (contentType) {
            case "image/jpeg" ->
                    bytes.length >= 3 &&
                            (bytes[0] & 0xFF) == 0xFF &&
                            (bytes[1] & 0xFF) == 0xD8 &&
                            (bytes[2] & 0xFF) == 0xFF;
            case "image/png" ->
                    bytes.length >= 8 &&
                            (bytes[0] & 0xFF) == 0x89 &&
                            bytes[1] == 0x50 &&
                            bytes[2] == 0x4E &&
                            bytes[3] == 0x47 &&
                            bytes[4] == 0x0D &&
                            bytes[5] == 0x0A &&
                            bytes[6] == 0x1A &&
                            bytes[7] == 0x0A;
            case "image/webp" ->
                    bytes.length >= 12 &&
                            bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                            bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            case "image/gif" ->
                    bytes.length >= 6 &&
                            bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' &&
                            bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            default -> false;
        };

        if (!valid) {
            throw new InvalidImageSignatureException(
                    "Image signature verification failed for file " + fileName + ": header bytes do not match declared Content-Type '" + contentType + "'."
            );
        }
    }

    public record AssetInfo(
            String publicId,
            String secureUrl,
            String format,
            long sizeBytes,
            int width,
            int height,
            String contentType
    ) {}
}
