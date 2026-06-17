package xss.it.nfx.pdfium.text;

import javafx.geometry.Rectangle2D;

import java.util.List;

/**
 * One occurrence of a search term within a document.
 *
 * <p>A match is a contiguous run of characters on a single page. Its on-page
 * geometry is given as one or more {@link #quads() quads} (one rectangle per
 * visual line the match spans), in PDF points with a top-left origin — ready to
 * be scaled onto a rendered page for highlighting.</p>
 *
 * @param pageIndex  the zero-based page the match is on
 * @param charStart  the zero-based starting character index within the page
 * @param charCount  the number of characters in the match
 * @param snippet    the matched text
 * @param quads      the highlight rectangles in points (top-left origin)
 * @author XDSSWAR
 */
public record PdfSearchResult(int pageIndex, int charStart, int charCount,
                              String snippet, List<Rectangle2D> quads) {

    /**
     * The overall bounding box of the match (union of its quads), or
     * {@link Rectangle2D#EMPTY} if it has none.
     *
     * @return the bounding rectangle in points, top-left origin
     */
    public Rectangle2D bounds() {
        if (quads == null || quads.isEmpty()) {
            return Rectangle2D.EMPTY;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Rectangle2D q : quads) {
            minX = Math.min(minX, q.getMinX());
            minY = Math.min(minY, q.getMinY());
            maxX = Math.max(maxX, q.getMaxX());
            maxY = Math.max(maxY, q.getMaxY());
        }
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}
