package xss.it.nfx.pdfium.render;

import com.xss.it.nfx.pdfium.PdfImages;
import com.xss.it.nfx.pdfium.PdfPageImpl;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfException;
import xss.it.nfx.pdfium.PdfPage;

/**
 * Low-level, stateless renderer that rasterizes PDF pages to JavaFX images.
 *
 * <p>Use this when you want pixels directly; for an interactive, reactive node
 * prefer {@link xss.it.nfx.pdfium.scene.PdfPageView}. Rendering is zero-copy
 * (PDFium writes BGRA straight into a buffer shared with the image) and uses no
 * AWT/Swing.</p>
 *
 * @author XDSSWAR
 */
public final class PdfRenderer {

    private PdfRenderer() {
    }

    /**
     * Renders a page with a white background.
     *
     * @param page            the page to render
     * @param scale           points-to-pixels scale factor
     * @param rotationDegrees a multiple of 90, clockwise
     * @return the rendered image
     */
    public static Image render(PdfPage page, double scale, int rotationDegrees) {
        return render(page, scale, rotationDegrees, Color.WHITE);
    }

    /**
     * Renders a page with the given background color.
     *
     * @param page            the page to render
     * @param scale           points-to-pixels scale factor (image size is
     *                        {@code round(width*scale) x round(height*scale)} px)
     * @param rotationDegrees a multiple of 90, clockwise
     * @param background      the background fill (use a translucent color for
     *                        transparency); {@code null} means opaque white
     * @return the rendered image
     */
    public static Image render(PdfPage page, double scale, int rotationDegrees, Color background) {
        if (!(page instanceof PdfPageImpl impl)) {
            throw new PdfException("Unsupported PdfPage implementation: " + page);
        }
        int rotation = PdfImages.quarterTurns(rotationDegrees);
        int wPts = (int) Math.round(page.getWidth() * scale);
        int hPts = (int) Math.round(page.getHeight() * scale);
        // Quarter turns swap the bitmap's width and height.
        boolean swap = (rotation & 1) == 1;
        int wpx = swap ? hPts : wPts;
        int hpx = swap ? wPts : hPts;
        return PdfImages.renderPage(impl, wpx, hpx, rotation, PdfImages.argb(background));
    }

    /**
     * Convenience overload that renders a page of a document by index.
     *
     * @param document        the document
     * @param pageIndex       the zero-based page index
     * @param scale           points-to-pixels scale factor
     * @param rotationDegrees a multiple of 90, clockwise
     * @return the rendered image
     */
    public static Image render(PdfDocument document, int pageIndex, double scale, int rotationDegrees) {
        return render(document.getPage(pageIndex), scale, rotationDegrees);
    }
}
