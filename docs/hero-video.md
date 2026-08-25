# FARMCONNECTPRICES — HERO VIDEO & FALLBACK ARCHITECTURE

## 1. Executive Summary

The **FarmConnectPrices** landing page hero section supports optional video background rendering. To maintain optimal performance, minimal Git repository size, and rapid initial page loads for mobile users on low-bandwidth networks, the monolithic 105.78 MB MP4 background video is **intentionally excluded** from version control and local static public assets.

The hero experience defaults to a **first-class CSS/3D ambient gradient background** that renders instantly with zero layout shifts, zero console error noise, and zero performance penalty.

---

## 2. Fallback & Video Strategy Matrix

| Condition | Visual Strategy | Component Behavior |
| :--- | :--- | :--- |
| **Video Disabled / Missing** | Ambient Multi-Layer CSS Gradient | Background renders radial emerald glow (`from-emerald-950/50 via-slate-950 to-slate-950`). Video element omitted. |
| **`VITE_HERO_VIDEO_URL` Provided** | Video Auto-play with Fade-In | HTML5 `<video>` attempts load. On success, transitions smoothly to 80% opacity. |
| **Video Load Error (404/Network)** | Instant Graceful Fallback | `onError` callback sets `videoError = true`, smoothly hiding video element and showing CSS fallback. |
| **`prefers-reduced-motion`** | Static Ambient CSS Gradient | Video playback disabled to respect accessibility settings. |
| **Mobile Viewports (< 768px)** | First-Class Ambient CSS Gradient | CSS gradient prevents 105 MB cellular data download while preserving high-contrast text typography. |

---

## 3. Recommended Future Production Video Delivery

When re-introducing video streaming for future production releases:

1. **Asset Compression:** Compress video to modern H.264 / WebM formats targeting **5 MB – 8 MB total file size** (720p/1080p, 24fps, high compression, audio track removed).
2. **CDN / Cloud Object Storage:** Host the video file on a dedicated Cloud CDN (e.g. AWS CloudFront, Cloudflare, or Firebase Storage) rather than bundling inside frontend static assets.
3. **Configuration Injection:** Specify the CDN asset URL in `.env`:
   ```env
   VITE_HERO_VIDEO_URL=https://cdn.farmconnectprices.com/videos/hero-background-720p.mp4
   ```
4. **Poster Image:** Provide a lightweight WebP/JPEG poster frame (`/images/hero-poster.webp`, ~50 kB) for immediate rendering during video buffer periods.
