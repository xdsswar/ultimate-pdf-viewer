package xss.it.nfx.pdfium.text;

import javafx.geometry.Rectangle2D;

/**
 * A single character on a page together with its bounding box.
 *
 * <p>The {@link #bounds() bounds} are expressed in PDF points (1 point = 1/72
 * inch) with a <strong>top-left origin</strong> — i.e. {@code y} grows downward,
 * matching JavaFX scene coordinates. Multiply by your render scale to map a glyph
 * box onto a rendered page image.</p>
 *
 * @param index    the zero-based character index within the page's text
 * @param unicode  the Unicode code point of the character
 * @param bounds   the glyph bounding box in points, top-left origin
 * @author XDSSWAR
 */
public record PdfTextChar(int index, int unicode, Rectangle2D bounds) {

    /**
     * The character as a Java string (handles supplementary code points).
     *
     * @return the character text
     */
    public String text() {
        return new String(Character.toChars(unicode));
    }
}
