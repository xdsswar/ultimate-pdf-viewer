package com.sun.internals.print;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

import java.nio.IntBuffer;

/**
 * Small image helpers for the print path.
 *
 * @author XDSSWAR
 */
public final class ImageFx {

    private ImageFx() {
    }

    /**
     * Converts an image to grayscale (for black-and-white printing) using the
     * standard luma weights. Reads and writes pixels in bulk as packed
     * {@code int} ARGB, which is far faster than per-pixel {@code getColor}.
     *
     * @param src the source image (must be readable via a pixel reader)
     * @return a new grayscale image of the same size; the source is unchanged
     */
    public static Image toGrayscale(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        // Guard the pixel-array size: w*h must fit a positive int (a huge page at
        // high print DPI could otherwise overflow to a negative length and crash).
        // Such an extreme page falls back to color rather than failing the job.
        if (w <= 0 || h <= 0 || src.getPixelReader() == null || (long) w * h > Integer.MAX_VALUE) {
            return src;
        }
        int[] buf = new int[w * h];
        WritablePixelFormat<IntBuffer> fmt = PixelFormat.getIntArgbInstance();
        src.getPixelReader().getPixels(0, 0, w, h, fmt, buf, 0, w);
        for (int i = 0; i < buf.length; i++) {
            int argb = buf[i];
            int a = argb >>> 24;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int y = (int) (0.299 * r + 0.587 * g + 0.114 * b + 0.5);
            if (y > 255) {
                y = 255;
            }
            buf[i] = (a << 24) | (y << 16) | (y << 8) | y;
        }
        WritableImage out = new WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, fmt, buf, 0, w);
        return out;
    }
}
