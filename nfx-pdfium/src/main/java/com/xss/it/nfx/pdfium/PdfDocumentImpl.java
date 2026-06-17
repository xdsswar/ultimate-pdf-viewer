package com.xss.it.nfx.pdfium;

import com.xss.it.nfx.pdfium.ffm.Pdfium;
import com.xss.it.nfx.pdfium.ffm.PdfiumDocument;
import com.xss.it.nfx.pdfium.ffm.PdfiumException;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfException;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.PdfPasswordCallback;
import xss.it.nfx.pdfium.text.PdfSearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link PdfDocument} implementation backed by a native PDFium handle.
 * Internal — consumers only ever see the {@link PdfDocument} interface.
 *
 * @author XDSSWAR
 */
public final class PdfDocumentImpl implements PdfDocument {

    /** The native document. */
    private final PdfiumDocument document;

    /** Lazily created page wrappers, indexed by page number. */
    private final PdfPageImpl[] pages;

    private boolean closed;

    /**
     * Opens a document from bytes with a fixed password.
     *
     * @param bytes    the PDF bytes
     * @param password the password, or {@code null}
     */
    public PdfDocumentImpl(byte[] bytes, String password) {
        this.document = openOrThrow(bytes, password);
        this.pages = new PdfPageImpl[document.pageCount()];
    }

    /**
     * Opens a document from bytes, prompting via the callback if it is encrypted.
     *
     * @param bytes    the PDF bytes
     * @param callback supplies passwords on demand (may be {@code null})
     */
    public PdfDocumentImpl(byte[] bytes, PdfPasswordCallback callback) {
        this.document = openWithCallback(bytes, callback);
        this.pages = new PdfPageImpl[document.pageCount()];
    }

    /** Opens with a single password, mapping native failures to {@link PdfException}. */
    private static PdfiumDocument openOrThrow(byte[] bytes, String password) {
        try {
            return Pdfium.open(bytes, password);
        } catch (PdfiumException e) {
            throw new PdfException(e.getMessage(), e);
        }
    }

    /**
     * Opens with retry: tries no password first, then asks the callback for each
     * attempt while PDFium reports a password is required.
     */
    private static PdfiumDocument openWithCallback(byte[] bytes, PdfPasswordCallback callback) {
        PdfiumDocument doc = Pdfium.tryOpen(bytes, null);
        if (doc != null) {
            return doc;
        }
        if (callback != null && Pdfium.lastError() == Pdfium.ERR_PASSWORD) {
            for (int attempt = 0; ; attempt++) {
                String password = callback.getPassword(attempt);
                if (password == null) {
                    break; // user cancelled
                }
                doc = Pdfium.tryOpen(bytes, password);
                if (doc != null) {
                    return doc;
                }
                if (Pdfium.lastError() != Pdfium.ERR_PASSWORD) {
                    break; // not a password problem any more — stop retrying
                }
            }
        }
        throw new PdfException(Pdfium.lastError() == Pdfium.ERR_PASSWORD
                ? "Password required or incorrect"
                : "Failed to open document (corrupt or unsupported)");
    }

    /** Native handle, for internal helpers (renderer, page). */
    PdfiumDocument native_() {
        return document;
    }

    @Override
    public int getPageCount() {
        return document.pageCount();
    }

    @Override
    public PdfPage getPage(int index) {
        if (index < 0 || index >= pages.length) {
            throw new IndexOutOfBoundsException("Page " + index + " of " + pages.length);
        }
        PdfPageImpl page = pages[index];
        if (page == null) {
            page = new PdfPageImpl(this, index);
            pages[index] = page;
        }
        return page;
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            sb.append(getPage(i).getText());
            if (i < count - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public List<PdfSearchResult> search(String query) {
        List<PdfSearchResult> all = new ArrayList<>();
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            all.addAll(getPage(i).search(query));
        }
        return all;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        document.close();
    }
}
