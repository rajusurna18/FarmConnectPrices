package com.farmlink.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DiagnosticImagesDto {

    @NotNull(message = "Original image metadata is required")
    @Valid
    private ImageMetadataDto original;

    @NotNull(message = "Diagnostic image derivative metadata is required")
    @Valid
    private ImageMetadataDto diagnostic;

    @NotNull(message = "Display image derivative metadata is required")
    @Valid
    private ImageMetadataDto display;

    public DiagnosticImagesDto() {
    }

    public DiagnosticImagesDto(ImageMetadataDto original, ImageMetadataDto diagnostic, ImageMetadataDto display) {
        this.original = original;
        this.diagnostic = diagnostic;
        this.display = display;
    }

    public ImageMetadataDto getOriginal() {
        return original;
    }

    public void setOriginal(ImageMetadataDto original) {
        this.original = original;
    }

    public ImageMetadataDto getDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(ImageMetadataDto diagnostic) {
        this.diagnostic = diagnostic;
    }

    public ImageMetadataDto getDisplay() {
        return display;
    }

    public void setDisplay(ImageMetadataDto display) {
        this.display = display;
    }

    public static class ImageMetadataDto {
        @NotBlank(message = "storagePath is required")
        private String storagePath;

        @NotBlank(message = "fileName is required")
        private String fileName;

        @NotBlank(message = "mimeType is required")
        private String mimeType;

        @NotNull(message = "sizeBytes is required")
        @Positive(message = "sizeBytes must be positive")
        private Long sizeBytes;

        private Integer width;
        private Integer height;

        public ImageMetadataDto() {
        }

        public ImageMetadataDto(String storagePath, String fileName, String mimeType, Long sizeBytes, Integer width, Integer height) {
            this.storagePath = storagePath;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.width = width;
            this.height = height;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }
    }
}

