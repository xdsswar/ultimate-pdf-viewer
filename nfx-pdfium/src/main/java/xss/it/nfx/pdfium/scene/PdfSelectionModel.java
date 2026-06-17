package xss.it.nfx.pdfium.scene;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import xss.it.nfx.pdfium.PdfDocument;

/**
 * A document-level text selection shared across page views, so a selection can
 * span multiple pages (anchor on one page, focus on another). A selection is an
 * ordered range from {@code (startPage, startChar)} to {@code (endPage, endChar)}
 * where char indices match {@link xss.it.nfx.pdfium.PdfPage#getChars()}.
 *
 * <p>Page views observe {@link #revisionProperty()} to repaint when the
 * selection changes, and ask {@link #rangeOnPage(int, int)} which characters of
 * their page are selected.</p>
 *
 * @author XDSSWAR
 */
public final class PdfSelectionModel {

    /** Sentinel meaning "to the end of the page". */
    private static final int TO_END = Integer.MAX_VALUE;

    private int pageCount;

    private int anchorPage = -1;
    private int anchorChar = -1;
    private int focusPage = -1;
    private int focusChar = -1;

    private final ReadOnlyIntegerWrapper revision = new ReadOnlyIntegerWrapper(this, "revision", 0);

    /**
     * Sets the number of pages in the document (used by {@link #selectAll()}).
     *
     * @param pageCount the page count
     */
    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    /** A counter bumped on every change; observe it to repaint. */
    public ReadOnlyIntegerProperty revisionProperty() {
        return revision.getReadOnlyProperty();
    }

    /** Whether there is no active selection. */
    public boolean isEmpty() {
        return anchorPage < 0 || focusPage < 0;
    }

    /** Clears the selection. */
    public void clear() {
        anchorPage = anchorChar = focusPage = focusChar = -1;
        bump();
    }

    /**
     * Begins a selection at a caret position (anchor == focus).
     *
     * @param page the page index
     * @param ch   the character index
     */
    public void begin(int page, int ch) {
        anchorPage = focusPage = page;
        anchorChar = focusChar = ch;
        bump();
    }

    /**
     * Extends the selection to a position, keeping the anchor (drag / shift-click).
     *
     * @param page the page index
     * @param ch   the character index
     */
    public void extendTo(int page, int ch) {
        if (isEmpty()) {
            begin(page, ch);
            return;
        }
        focusPage = page;
        focusChar = ch;
        bump();
    }

    /** Selects all text in the whole document. */
    public void selectAll() {
        anchorPage = 0;
        anchorChar = 0;
        focusPage = Math.max(0, pageCount - 1);
        focusChar = TO_END;
        bump();
    }

    /**
     * The inclusive selected character range on a page, or {@code null} if none.
     *
     * @param page          the page index
     * @param pageCharCount the number of characters on that page
     * @return {@code [fromChar, toChar]} inclusive, or {@code null}
     */
    /**
     * Whether the given page lies within the current selection's page span (so it
     * may contain selected characters). Cheap — does not need the page's character
     * count, so callers can avoid loading a page's text unless it is actually
     * selected.
     *
     * @param page the page index
     * @return {@code true} if the selection covers any part of that page
     */
    public boolean coversPage(int page) {
        return !isEmpty() && page >= startPage() && page <= endPage();
    }

    public int[] rangeOnPage(int page, int pageCharCount) {
        if (isEmpty() || pageCharCount <= 0) {
            return null;
        }
        int sp = startPage();
        int ep = endPage();
        if (page < sp || page > ep) {
            return null;
        }
        int from = (page == sp) ? startChar() : 0;
        int to = (page == ep) ? Math.min(endChar(), pageCharCount - 1) : pageCharCount - 1;
        from = Math.max(0, Math.min(from, pageCharCount - 1));
        if (from > to) {
            return null;
        }
        return new int[]{from, to};
    }

    /**
     * Extracts the selected text across all spanned pages.
     *
     * @param document the document the selection refers to
     * @return the selected text (pages separated by newlines)
     */
    public String selectedText(PdfDocument document) {
        if (isEmpty() || document == null) {
            return "";
        }
        int sp = startPage();
        int ep = endPage();
        int sc = startChar();
        int ec = endChar();
        StringBuilder sb = new StringBuilder();
        for (int p = sp; p <= ep && p < document.getPageCount(); p++) {
            String t = document.getPage(p).getText();
            int len = t.length();
            if (len > 0) {
                int from = (p == sp) ? sc : 0;
                int to = (p == ep) ? ec : len - 1;
                from = Math.max(0, Math.min(from, len - 1));
                to = Math.max(0, Math.min(to, len - 1));
                if (from <= to) {
                    sb.append(t, from, to + 1);
                }
            }
            if (p < ep) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /* ----------------------------------------------------- ordered endpoints */

    private boolean anchorFirst() {
        return anchorPage < focusPage || (anchorPage == focusPage && anchorChar <= focusChar);
    }

    private int startPage() {
        return anchorFirst() ? anchorPage : focusPage;
    }

    private int startChar() {
        return anchorFirst() ? anchorChar : focusChar;
    }

    private int endPage() {
        return anchorFirst() ? focusPage : anchorPage;
    }

    private int endChar() {
        return anchorFirst() ? focusChar : anchorChar;
    }

    private void bump() {
        revision.set(revision.get() + 1);
    }
}
