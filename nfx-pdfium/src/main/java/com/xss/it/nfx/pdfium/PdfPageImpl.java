package com.xss.it.nfx.pdfium;

import com.xss.it.nfx.pdfium.ffm.PageSize;
import com.xss.it.nfx.pdfium.ffm.PdfiumTextPage;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.render.PdfRenderer;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.nfx.pdfium.text.PdfTextChar;

import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link PdfPage} implementation. Holds its owning document and index,
 * caches the page's intrinsic size, and converts PDFium's bottom-left point
 * coordinates into the top-left point coordinates used across the public API.
 *
 * @author XDSSWAR
 */
public final class PdfPageImpl implements PdfPage {

    private final PdfDocumentImpl owner;
    private final int index;
    private final double width;
    private final double height;

    PdfPageImpl(PdfDocumentImpl owner, int index) {
        this.owner = owner;
        this.index = index;
        PageSize size = owner.native_().size(index);
        this.width = size.width();
        this.height = size.height();
    }

    /** The owning document (internal accessor for the renderer). */
    PdfDocumentImpl owner() {
        return owner;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public Image render(double scale, int rotationDegrees) {
        return PdfRenderer.render(this, scale, rotationDegrees);
    }

    @Override
    public String getText() {
        try (PdfiumTextPage text = owner.native_().loadText(index)) {
            return text.text();
        }
    }

    @Override
    public List<PdfTextChar> getChars() {
        List<PdfTextChar> chars = new ArrayList<>();
        try (PdfiumTextPage text = owner.native_().loadText(index)) {
            int count = text.charCount();
            for (int i = 0; i < count; i++) {
                PdfiumTextPage.CharBox box = text.charBox(i);
                int cp = codePointAt(text.text(i, 1));
                chars.add(new PdfTextChar(i, cp, toTopLeft(box)));
            }
        }
        return chars;
    }

    @Override
    public List<PdfSearchResult> search(String query) {
        List<PdfSearchResult> results = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return results;
        }
        try (PdfiumTextPage text = owner.native_().loadText(index)) {
            for (PdfiumTextPage.Match match : text.find(query, false, false)) {
                List<Rectangle2D> quads = quadsFor(text, match.start(), match.count());
                String snippet = text.text(match.start(), match.count());
                results.add(new PdfSearchResult(index, match.start(), match.count(), snippet, quads));
            }
        }
        return results;
    }

    /* ------------------------------------------------------------ helpers */

    /** Converts a PDFium bottom-left char box to a top-left {@link Rectangle2D}. */
    private Rectangle2D toTopLeft(PdfiumTextPage.CharBox box) {
        double x = box.left();
        double y = height - box.top();
        double w = Math.max(0, box.right() - box.left());
        double h = Math.max(0, box.top() - box.bottom());
        return new Rectangle2D(x, y, w, h);
    }

    /**
     * Builds highlight quads for a run of characters, merging glyphs that share a
     * visual line into one rectangle so multi-line matches produce one rect per
     * line. Empty (glyph-less) boxes such as spaces are skipped.
     */
    private List<Rectangle2D> quadsFor(PdfiumTextPage text, int start, int count) {
        List<Rectangle2D> quads = new ArrayList<>();
        Rectangle2D run = null;
        int end = start + count;
        for (int i = start; i < end; i++) {
            Rectangle2D box = toTopLeft(text.charBox(i));
            if (box.getWidth() <= 0 && box.getHeight() <= 0) {
                continue;
            }
            if (run == null) {
                run = box;
            } else if (sameLine(run, box)) {
                run = union(run, box);
            } else {
                quads.add(run);
                run = box;
            }
        }
        if (run != null) {
            quads.add(run);
        }
        return quads;
    }

    /** Two boxes are on the same line when their vertical extents overlap >50%. */
    private static boolean sameLine(Rectangle2D a, Rectangle2D b) {
        double top = Math.max(a.getMinY(), b.getMinY());
        double bottom = Math.min(a.getMaxY(), b.getMaxY());
        double overlap = bottom - top;
        double minHeight = Math.min(a.getHeight(), b.getHeight());
        return minHeight > 0 && overlap > 0.5 * minHeight;
    }

    /** Bounding union of two rectangles. */
    private static Rectangle2D union(Rectangle2D a, Rectangle2D b) {
        double minX = Math.min(a.getMinX(), b.getMinX());
        double minY = Math.min(a.getMinY(), b.getMinY());
        double maxX = Math.max(a.getMaxX(), b.getMaxX());
        double maxY = Math.max(a.getMaxY(), b.getMaxY());
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    /** First code point of a string, or 0 if empty. */
    private static int codePointAt(String s) {
        return (s == null || s.isEmpty()) ? 0 : s.codePointAt(0);
    }
}
