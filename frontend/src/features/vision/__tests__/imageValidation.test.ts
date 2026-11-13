import { describe, it, expect } from 'vitest';
import {
  sniffImageSignature,
  isSvgOrXml,
  validateDimensions,
  validateImageFile,
} from '../utils/imageValidation';

describe('Vision AI — Image Validation Utilities', () => {
  describe('sniffImageSignature', () => {
    it('detects authentic JPEG signature (FF D8 FF)', () => {
      const buffer = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46]).buffer;
      expect(sniffImageSignature(buffer)).toBe('image/jpeg');
    });

    it('detects authentic PNG signature (89 50 4E 47 0D 0A 1A 0A)', () => {
      const buffer = new Uint8Array([
        0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
      ]).buffer;
      expect(sniffImageSignature(buffer)).toBe('image/png');
    });

    it('detects authentic WebP signature (RIFF ... WEBP)', () => {
      const buffer = new Uint8Array([
        0x52, 0x49, 0x46, 0x46, // "RIFF"
        0x20, 0x00, 0x00, 0x00, // length
        0x57, 0x45, 0x42, 0x50, // "WEBP"
        0x56, 0x50, 0x38, 0x20, // "VP8 "
      ]).buffer;
      expect(sniffImageSignature(buffer)).toBe('image/webp');
    });

    it('returns null for arbitrary binary or text buffers', () => {
      const textBuffer = new TextEncoder().encode('Hello, I am a plain text file pretending to be an image.').buffer;
      expect(sniffImageSignature(textBuffer)).toBeNull();
    });

    it('returns null for short buffers (< 4 bytes)', () => {
      const shortBuffer = new Uint8Array([0xff, 0xd8]).buffer;
      expect(sniffImageSignature(shortBuffer)).toBeNull();
    });

    it('returns null and rejects SVG buffers', () => {
      const svgBuffer = new TextEncoder().encode('<svg xmlns="http://www.w3.org/2000/svg"><circle/></svg>').buffer;
      expect(sniffImageSignature(svgBuffer)).toBeNull();
    });
  });

  describe('isSvgOrXml', () => {
    it('identifies <?xml declarations', () => {
      const bytes = new TextEncoder().encode('<?xml version="1.0" encoding="UTF-8"?><svg></svg>');
      expect(isSvgOrXml(bytes)).toBe(true);
    });

    it('identifies <svg tags', () => {
      const bytes = new TextEncoder().encode('<svg viewBox="0 0 100 100"></svg>');
      expect(isSvgOrXml(bytes)).toBe(true);
    });

    it('identifies <!DOCTYPE html', () => {
      const bytes = new TextEncoder().encode('<!DOCTYPE html><html><body>malicious</body></html>');
      expect(isSvgOrXml(bytes)).toBe(true);
    });

    it('returns false for authentic binary image bytes', () => {
      const jpegBytes = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);
      expect(isSvgOrXml(jpegBytes)).toBe(false);
    });
  });

  describe('validateDimensions', () => {
    it('approves standard agricultural image dimensions (1920x1080, 2048x1536)', () => {
      expect(validateDimensions({ width: 1920, height: 1080 }).isValid).toBe(true);
      expect(validateDimensions({ width: 2048, height: 1536 }).isValid).toBe(true);
      expect(validateDimensions({ width: 800, height: 600 }).isValid).toBe(true);
    });

    it('rejects zero or negative dimensions', () => {
      const zero = validateDimensions({ width: 0, height: 600 });
      expect(zero.isValid).toBe(false);
      expect(zero.error).toContain('invalid zero or negative dimensions');

      const negative = validateDimensions({ width: 800, height: -10 });
      expect(negative.isValid).toBe(false);
    });

    it('rejects excessive single-axis dimension (> 6000px)', () => {
      const res = validateDimensions({ width: 7000, height: 1000 });
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('exceed the maximum allowable axis limit');
    });

    it('rejects excessive total pixel count (> 25 MP decompression bomb defense)', () => {
      // 5500 x 5000 = 27,500,000 pixels (> 25,000,000)
      const res = validateDimensions({ width: 5500, height: 5000 });
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('exceeds the maximum safe limit');
    });

    it('rejects extreme, distorted aspect ratios', () => {
      // Aspect ratio = 3000 / 50 = 60 (> 20)
      const res = validateDimensions({ width: 3000, height: 50 });
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('extreme, unsupported aspect ratio');
    });
  });

  describe('validateImageFile', () => {
    it('rejects empty file (0 bytes)', async () => {
      const emptyFile = new File([], 'empty.jpg', { type: 'image/jpeg' });
      const res = await validateImageFile(emptyFile);

      expect(res.isValid).toBe(false);
      expect(res.error).toContain('empty (0 bytes)');
    });

    it('rejects oversized files exceeding 10 MB limit', async () => {
      // Create a mock file with size reported as 11 MB
      const oversizedFile = new File(['x'], 'huge.jpg', { type: 'image/jpeg' });
      Object.defineProperty(oversizedFile, 'size', { value: 11 * 1024 * 1024 });

      const res = await validateImageFile(oversizedFile);
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('exceeds maximum allowed size');
    });

    it('does NOT reject valid small images (< 100 bytes)', async () => {
      // Tiny 40-byte PNG header buffer
      const tinyBytes = new Uint8Array([
        0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
        0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4,
        0x89, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e,
      ]);
      const tinyFile = new File([tinyBytes], 'tiny.png', { type: 'image/png' });

      const res = await validateImageFile(tinyFile);
      // Valid PNG header should be identified as image/png
      expect(res.detectedFormat).toBe('image/png');
      expect(res.isValid).toBe(true);
    });

    it('rejects text files masquerading as JPEG images by extension', async () => {
      const fakeFile = new File(['This is malicious plain text'], 'trojan.jpg', {
        type: 'image/jpeg',
      });

      const res = await validateImageFile(fakeFile);
      expect(res.isValid).toBe(false);
      expect(res.detectedFormat).toBeNull();
      expect(res.error).toContain('Unsupported or invalid image format');
    });

    it('explicitly rejects SVG files disguised as .jpg or .png', async () => {
      const svgFile = new File(['<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>'], 'leaf.jpg', {
        type: 'image/jpeg',
      });

      const res = await validateImageFile(svgFile);
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('SVG images are not supported');
    });

    it('validates authentic JPEG file binary', async () => {
      const jpegBytes = new Uint8Array([
        0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01,
      ]);
      const validJpeg = new File([jpegBytes], 'tomato_leaf.jpeg', { type: 'image/jpeg' });

      const res = await validateImageFile(validJpeg);
      expect(res.isValid).toBe(true);
      expect(res.detectedFormat).toBe('image/jpeg');
    });

    it('validates authentic WebP file binary', async () => {
      const webpBytes = new Uint8Array([
        0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50,
      ]);
      const validWebp = new File([webpBytes], 'crop_field.webp', { type: 'image/webp' });

      const res = await validateImageFile(validWebp);
      expect(res.isValid).toBe(true);
      expect(res.detectedFormat).toBe('image/webp');
    });
  });
});

