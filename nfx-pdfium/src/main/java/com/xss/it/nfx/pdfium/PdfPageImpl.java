package com.xss.it.nfx.pdfium;

import com.xss.it.nfx.pdfium.ffm.PageSize;
import com.xss.it.nfx.pdfium.ffm.PdfiumTextPage;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.render.PdfRenderer;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.nfx.pdfium.text.PdfTextChar;
import xss.it.nfx.pdfium.text.SearchOptions;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
        return search(query, SearchOptions.DEFAULT);
    }

    @Override
    public List<PdfSearchResult> search(String query, SearchOptions options) {
        List<PdfSearchResult> results = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return results;
        }
        SearchOptions opts = options != null ? options : SearchOptions.DEFAULT;
        try (PdfiumTextPage text = owner.native_().loadText(index)) {
            findMatches(text, query, opts, results);
        }
        return results;
    }

    /* ------------------------------------------------------------ search */

    /** Matches dropped when stripping diacritics (Unicode combining marks). */
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{Mn}+");

    /**
     * Finds every occurrence of {@code query} on the page under the given options
     * and appends a {@link PdfSearchResult} for each.
     *
     * <p>Matching is done in Java rather than via PDFium's native find so that
     * case-, whole-word- and diacritics-insensitivity can be combined freely
     * (PDFium has no diacritics flag). The page text is "folded" character by
     * character (optionally lower-cased and stripped of diacritics) into a string
     * whose every position maps back to the original page character index, so a
     * substring hit can be turned straight back into precise highlight quads.</p>
     */
    private void findMatches(PdfiumTextPage text, String query, SearchOptions opts,
                             List<PdfSearchResult> out) {
        String foldedQuery = fold(query, opts);
        if (foldedQuery.isEmpty()) {
            return;
        }
        int count = text.charCount();
        StringBuilder folded = new StringBuilder(count);
        // folded position -> original page character index it originated from.
        int[] map = new int[count + 16];
        int mapLen = 0;
        for (int i = 0; i < count; i++) {
            String f = fold(text.text(i, 1), opts);
            for (int k = 0; k < f.length(); k++) {
                if (mapLen == map.length) {
                    int[] grown = new int[map.length * 2];
                    System.arraycopy(map, 0, grown, 0, mapLen);
                    map = grown;
                }
                folded.append(f.charAt(k));
                map[mapLen++] = i;
            }
        }

        String hay = folded.toString();
        int qLen = foldedQuery.length();
        int from = 0;
        int hit;
        while ((hit = hay.indexOf(foldedQuery, from)) >= 0) {
            int endExclusive = hit + qLen;
            if (!opts.matchWholeWords() || isWholeWord(hay, hit, endExclusive)) {
                int startChar = map[hit];
                int endChar = map[endExclusive - 1];
                int matchCount = endChar - startChar + 1;
                List<Rectangle2D> quads = quadsFor(text, startChar, matchCount);
                String snippet = text.text(startChar, matchCount);
                out.add(new PdfSearchResult(index, startChar, matchCount, snippet, quads));
            }
            from = hit + 1; // allow overlapping matches, mirroring PDFium
        }
    }

    /**
     * Folds a string for matching: optionally strips diacritics (NFD decompose,
     * drop combining marks) and lower-cases, per the options.
     */
    private static String fold(String s, SearchOptions opts) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String r = s;
        if (!opts.matchDiacritics()) {
            r = COMBINING_MARKS.matcher(Normalizer.normalize(r, Normalizer.Form.NFD)).replaceAll("");
        }
        if (!opts.matchCase()) {
            r = r.toLowerCase(Locale.ROOT);
        }
        return r;
    }

    /** Whether the half-open range {@code [start,end)} sits on word boundaries. */
    private static boolean isWholeWord(String s, int start, int end) {
        boolean leftOk = start == 0 || !Character.isLetterOrDigit(s.charAt(start - 1));
        boolean rightOk = end == s.length() || !Character.isLetterOrDigit(s.charAt(end));
        return leftOk && rightOk;
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
