package xss.it.nfx.pdfium;

import javafx.scene.image.Image;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.nfx.pdfium.text.PdfTextChar;

import java.util.List;

/**
 * A single page of a {@link PdfDocument}: its geometry, rendering, and text.
 *
 * <p>Sizes and text-box coordinates are in PDF points (1/72 inch). Rendering
 * produces a JavaFX {@link Image} with no AWT/Swing involvement. A page shares
 * its parent document's native resources, so it becomes unusable once the
 * document is {@link PdfDocument#close() closed}.</p>
 *
 * @author XDSSWAR
 */
public interface PdfPage {

    /**
     * The zero-based index of this page within its document.
     *
     * @return the page index
     */
    int getIndex();

    /**
     * The page width in PDF points.
     *
     * @return the width in points
     */
    double getWidth();

    /**
     * The page height in PDF points.
     *
     * @return the height in points
     */
    double getHeight();

    /**
     * Renders this page to a JavaFX image.
     *
     * @param scale           points-to-pixels scale factor (e.g. {@code 2.0}
     *                        renders at 144 DPI); the resulting image is
     *                        {@code round(width*scale) x round(height*scale)} px
     * @param rotationDegrees a multiple of 90 (0/90/180/270), clockwise
     * @return the rendered image with a white background
     */
    Image render(double scale, int rotationDegrees);

    /**
     * Extracts all text on the page in reading order.
     *
     * @return the page text (may be empty)
     */
    String getText();

    /**
     * Returns every character on the page with its bounding box, for building
     * text selection or hit-testing.
     *
     * @return the page characters in reading order
     */
    List<PdfTextChar> getChars();

    /**
     * Searches this page for the given text.
     *
     * @param query the text to find (case-insensitive)
     * @return the matches on this page, in reading order
     */
    List<PdfSearchResult> search(String query);
}
