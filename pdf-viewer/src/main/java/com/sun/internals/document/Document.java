package com.sun.internals.document;

import com.sun.internals.PageData;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import xss.it.nfx.pdfium.PdfDocument;

import java.io.File;
import java.io.IOException;

/**
 * Internal document abstraction that wraps the {@code nfx-pdfium} engine and
 * adds viewer-specific state (per-page rotation/viewport, thumbnail caching).
 * Produces JavaFX images directly (no AWT).
 *
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public interface Document {

    /**
     * The underlying public document, exposed through {@code Viewer.getDocument()}.
     *
     * @return the nfx-pdfium document
     */
    PdfDocument getPdfDocument();

    /**
     * The source file name (including extension) if the document was opened from
     * a file, otherwise {@code null} (e.g. opened from a stream or raw bytes).
     *
     * @return the file name, or {@code null} if unknown
     */
    String getFileName();

    /**
     * The size of the original document in bytes.
     *
     * @return the byte count of the source document
     */
    long getFileSize();

    /**
     * Whether the document is linearized ("Fast Web View"), i.e. optimized for
     * incremental loading over a network.
     *
     * @return {@code true} if the document is linearized
     */
    boolean isFastWebView();

    /**
     * List of pages with additional properties. Data source for the page list.
     */
    ObservableList<PageData> getPagesList();

    /**
     * Sets rotation for the given page.
     */
    void setPageRotation(int pageNumber, double rotationAngle);

    /**
     * Gets rotation data for the given page.
     */
    PageData getPageRotation(int pageNumber);

    /**
     * Sets the viewport for the given page.
     */
    void setViewport(int pageNumber, Rectangle2D viewport);

    /**
     * Renders the page at the given scale and rotation into a JavaFX image.
     *
     * @param pageNumber    the page number (zero-based)
     * @param scale         points-to-pixels scale factor
     * @param rotationAngle the user rotation angle in degrees
     * @param useCache      cache the rendered image (used for thumbnails)
     * @return the rendered JavaFX image
     */
    Image renderPage(int pageNumber, float scale, double rotationAngle, boolean useCache);

    /**
     * Returns the total number of pages inside the document.
     */
    int getNumberOfPages();

    /**
     * Determines if the given page should be shown in landscape orientation.
     */
    boolean isLandscape(int pageNumber);

    /**
     * Writes the (unmodified) document to the given file.
     */
    void save(File file) throws IOException;

    /**
     * Closes the document and releases native resources.
     */
    void close() throws IOException;
}
