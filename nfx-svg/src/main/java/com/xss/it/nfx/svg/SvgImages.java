package com.xss.it.nfx.svg;

import com.xss.it.nfx.svg.ffm.SvgNative;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * Internal helper that turns native Skia output into JavaFX images with no
 * AWT/Swing in the path. Skia writes BGRA (premultiplied) pixels into a native
 * {@link MemorySegment} that is copied into a JavaFX-managed
 * {@link WritableImage}; the native memory is freed immediately.
 *
 * @author XDSSWAR
 */
public final class SvgImages {

    private SvgImages() {
    }

    /**
     * Renders a document into a JavaFX image of exactly {@code wpx x hpx} pixels.
     *
     * @param doc    the native document
     * @param wpx    target width in pixels
     * @param hpx    target height in pixels
     * @param argbBg background fill as 0xAARRGGBB (0 = transparent)
     * @return the rendered image
     */
    public static Image render(SvgNative doc, int wpx, int hpx, int argbBg) {
        int w = Math.max(1, wpx);
        int h = Math.max(1, hpx);

        // Render into a short-lived native buffer, then copy the pixels into a
        // JavaFX-managed WritableImage and free the native memory immediately.
        // (A PixelBuffer over an auto-Arena segment can be freed by the GC while
        // the JavaFX render thread is still uploading it -> null-texture crash
        // under fast zoom. Copying avoids that lifetime hazard.)
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate((long) w * h * 4L);
            doc.render(w, h, argbBg, buffer);

            ByteBuffer pixels = buffer.asByteBuffer();
            WritableImage image = new WritableImage(w, h);
            image.getPixelWriter().setPixels(0, 0, w, h,
                    PixelFormat.getByteBgraPreInstance(), pixels, w * 4);
            return image;
        }
    }

    /**
     * Converts a JavaFX color to a 0xAARRGGBB integer (transparent if
     * {@code null}).
     *
     * @param color the color, or {@code null} for transparent
     * @return the packed ARGB value
     */
    public static int argb(Color color) {
        if (color == null) {
            return 0x00000000;
        }
        int a = (int) Math.round(color.getOpacity() * 255.0);
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
