package com.sun.internals;

import com.sun.internals.document.Searchable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.render.PdfRenderer;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.ultimate.pdf.viewer.text.SearchResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Viewer-side document that adapts the {@code nfx-pdfium} engine to the internal
 * {@link Searchable} contract: it adds per-page rotation/viewport metadata and a
 * thumbnail cache, and renders pages to JavaFX images via {@link PdfRenderer}.
 *
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class PdfDocumentImpl implements Searchable {

    /** PDF base resolution (points per inch). */
    public static final float DPI = 72f;

    /** The underlying engine document. */
    private final PdfDocument document;

    /** Original bytes, retained for {@link #save(File)}. */
    private final byte[] source;

    /** Source file name if opened from a file, otherwise {@code null}. */
    private final String fileName;

    /** Per-page metadata (rotation + viewport); drives the page/thumbnail list. */
    private final ObservableList<PageData> pagesList = FXCollections.observableArrayList();

    /** Thumbnail image cache, keyed by page index. */
    private final Map<Integer, Image> thumbCache = new HashMap<>();

    public PdfDocumentImpl(File file) throws IOException {
        this(Files.readAllBytes(file.toPath()), file.getName(), null);
    }

    public PdfDocumentImpl(InputStream stream) throws IOException {
        this(stream.readAllBytes(), null, null);
    }

    public PdfDocumentImpl(byte[] bytes) {
        this(bytes, null, null);
    }

    /**
     * Opens a document from bytes, retaining a source file name and using the
     * given password (may be {@code null} for unencrypted documents).
     *
     * @param bytes    the PDF bytes
     * @param fileName the source file name, or {@code null}
     * @param password the password, or {@code null}
     */
    public PdfDocumentImpl(byte[] bytes, String fileName, String password) {
        this.source = bytes;
        this.fileName = fileName;
        this.document = PdfDocument.open(bytes, password);
        updatePagesList();
    }

    @Override
    public PdfDocument getPdfDocument() {
        return document;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getFileSize() {
        return source.length;
    }

    @Override
    public boolean isFastWebView() {
        // A linearized ("Fast Web View") PDF places a /Linearized dictionary as
        // the first object, right after the %PDF header. Scan only the start of
        // the file so this stays cheap.
        int limit = Math.min(source.length, 2048);
        String head = new String(source, 0, limit, StandardCharsets.ISO_8859_1);
        return head.contains("/Linearized");
    }

    /* ----------------------------------------------------------- page model */

    @Override
    public ObservableList<PageData> getPagesList() {
        return pagesList;
    }

    @Override
    public void setPageRotation(int pageNumber, double rotationAngle) {
        PageData pageData = pagesList.get(pageNumber);
        pagesList.set(pageNumber, new PageData(pageData.getPageNumber(), rotationAngle, pageData.getViewport()));
        thumbCache.remove(pageNumber);
    }

    @Override
    public PageData getPageRotation(int pageNumber) {
        return pagesList.get(pageNumber);
    }

    @Override
    public void setViewport(int pageNumber, Rectangle2D viewport) {
        PageData pageData = pagesList.get(pageNumber);
        pagesList.set(pageNumber, new PageData(pageData.getPageNumber(), pageData.getRotationAngle(), viewport));
    }

    /* ------------------------------------------------------------ rendering */

    @Override
    public Image renderPage(int pageNumber, float scale, double rotationAngle, boolean useCache) {
        if (useCache) {
            Image cached = thumbCache.get(pageNumber);
            if (cached != null) {
                return cached;
            }
        }
        PdfPage page = document.getPage(pageNumber);
        Image image = PdfRenderer.render(page, scale, (int) Math.round(rotationAngle));
        if (useCache) {
            thumbCache.put(pageNumber, image);
        }
        return image;
    }

    @Override
    public int getNumberOfPages() {
        return document.getPageCount();
    }

    @Override
    public boolean isLandscape(int pageNumber) {
        PdfPage page = document.getPage(pageNumber);
        double rotationAngle = pagesList.get(pageNumber).getRotationAngle();
        if (rotationAngle % 180 == 0.0) {
            return page.getHeight() < page.getWidth();
        }
        return page.getWidth() < page.getHeight();
    }

    /* ---------------------------------------------------------------- search */

    @Override
    public List<SearchResult> getSearchResults(String searchText) {
        List<SearchResult> results = new ArrayList<>();
        if (searchText == null || searchText.isBlank()) {
            return results;
        }
        for (PdfSearchResult hit : document.search(searchText)) {
            Rectangle2D marker = hit.bounds();
            results.add(new SearchResult(searchText, hit.snippet(), hit.pageIndex(), marker));
        }
        return results;
    }

    /* --------------------------------------------------------------- lifecycle */

    @Override
    public void save(File file) throws IOException {
        Files.write(file.toPath(), source);
    }

    @Override
    public void close() {
        thumbCache.clear();
        document.close();
    }

    private void updatePagesList() {
        List<PageData> pageKeys = new ArrayList<>();
        int numberOfPages = document.getPageCount();
        for (int i = 0; i < numberOfPages; i++) {
            pageKeys.add(new PageData(i, 0.0));
        }
        pagesList.setAll(pageKeys);
    }
}
