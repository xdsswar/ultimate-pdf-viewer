package com.xss.it.nfx.pdfium;

import com.xss.it.nfx.pdfium.ffm.PdfiumDocument;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * Internal helper that turns native PDFium output into JavaFX images with no
 * AWT/Swing in the path. PDFium writes BGRA pixels into a native
 * {@link MemorySegment} that is shared zero-copy with a {@link PixelBuffer}.
 *
 * @author XDSSWAR
 */
public final class PdfImages {

    private PdfImages() {
    }

    /**
     * Renders a page into a JavaFX image of exactly {@code wpx x hpx} pixels.
     *
     * @param doc      the native document
     * @param index    the zero-based page index
     * @param wpx      target width in pixels
     * @param hpx      target height in pixels
     * @param rotation 0/1/2/3 quarter turns clockwise
     * @param argbBg   background fill as 0xAARRGGBB
     * @return the rendered image
     */
    public static Image render(PdfiumDocument doc, int index, int wpx, int hpx,
                               int rotation, int argbBg) {
        int w = Math.max(1, wpx);
        int h = Math.max(1, hpx);

        // Render into a short-lived native buffer, then copy the pixels into a
        // JavaFX-managed WritableImage and free the native memory immediately.
        // (A PixelBuffer over an auto-Arena segment can be freed by the GC while
        // the JavaFX render thread is still uploading it -> null-texture crash
        // under fast scroll/virtualization. Copying avoids that lifetime hazard.)
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate((long) w * h * 4L);
            doc.render(index, w, h, rotation, argbBg, buffer);

            ByteBuffer pixels = buffer.asByteBuffer();
            WritableImage image = new WritableImage(w, h);
            image.getPixelWriter().setPixels(0, 0, w, h,
                    PixelFormat.getByteBgraPreInstance(), pixels, w * 4);
            return image;
        }
    }

    /**
     * Renders a page wrapper into a JavaFX image. Lives here (same package as the
     * impls) so it can reach their package-private native handle.
     *
     * @param page     the page to render
     * @param wpx      target width in pixels
     * @param hpx      target height in pixels
     * @param rotation 0/1/2/3 quarter turns clockwise
     * @param argbBg   background fill as 0xAARRGGBB
     * @return the rendered image
     */
    public static Image renderPage(PdfPageImpl page, int wpx, int hpx, int rotation, int argbBg) {
        return render(page.owner().native_(), page.getIndex(), wpx, hpx, rotation, argbBg);
    }

    /**
     * Converts a rotation in degrees to PDFium's 0/1/2/3 quarter-turn units.
     *
     * @param degrees rotation in degrees (any multiple of 90, may be negative)
     * @return 0, 1, 2 or 3
     */
    public static int quarterTurns(double degrees) {
        int q = (int) Math.round(degrees / 90.0);
        return ((q % 4) + 4) % 4;
    }

    /**
     * Converts a JavaFX color to a 0xAARRGGBB integer (white if {@code null}).
     *
     * @param color the color, or {@code null} for opaque white
     * @return the packed ARGB value
     */
    public static int argb(Color color) {
        if (color == null) {
            return 0xFFFFFFFF;
        }
        int a = (int) Math.round(color.getOpacity() * 255.0);
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
