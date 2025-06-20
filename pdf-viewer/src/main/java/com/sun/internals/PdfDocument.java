package com.sun.internals;

import com.sun.internals.document.Searchable;
import com.sun.internals.render.Render;
import com.sun.internals.text.TextStripper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.ImageType;
import xss.it.ultimate.pdf.viewer.text.SearchResult;

import java.awt.image.BufferedImage;
import java.awt.print.Pageable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class PdfDocument implements Searchable {
    /**
     * Scale
     */
    public  static final float DPI = 72f;

    /**
     * Represents the underlying PDDocument associated with this class.
     */
    private final PDDocument document;

    /**
     * A cache that maps PageData objects to their corresponding fxImages.
     * Used to store rendered page images for quick retrieval.
     */
    private final Map<PageData, Image> fxImageCache= new HashMap<>();

    /**
     * An observable list of PageData objects representing the pages in the document.
     */
    private final ObservableList<PageData> pagesList=FXCollections.observableArrayList();

    /**
     * Represents the image type.
     */
    private ImageType imageType = ImageType.ARGB;

    private final Render render;

    /**
     * Constructs a PdfDocument object by loading a PDF file from the given File.
     *
     * @param file The File object representing the PDF file to load.
     * @throws IOException If there is an error loading the PDF file.
     */
    public PdfDocument(File file) throws IOException {
        this.document = Loader.loadPDF(file);
        this.render = new Render(document);
        updatePagesList();
    }

    /**
     * Constructs a PdfDocument object by loading a PDF from the given InputStream.
     *
     * @param stream The InputStream from which to load the PDF.
     * @throws IOException If there is an error reading the PDF from the InputStream.
     */
    public PdfDocument(InputStream stream) throws IOException {
        this.document = Loader.loadPDF(stream.readAllBytes());
        this.render = new Render(document);
        updatePagesList();
    }


    /**
     * Gets an observable list of PageData objects representing the pages in the document.
     *
     * @return An observable list of PageData objects.
     */
    @Override
    public ObservableList<PageData> getPagesList() {
        return pagesList;
    }

    /**
     * Sets the rotation angle for a specific page.
     *
     * @param pageNumber    The page number to set the rotation for.
     * @param rotationAngle The rotation angle to set.
     */
    @Override
    public void setPageRotation(int pageNumber, double rotationAngle) {
        PageData pageData = pagesList.get(pageNumber);
        pagesList.set(pageNumber, new PageData(pageData.getPageNumber(), rotationAngle, pageData.getViewport()));
    }

    /**
     * Retrieves the rotation angle for a specific page.
     *
     * @param pageNumber The page number to retrieve the rotation angle for.
     * @return The rotation angle of the page.
     */
    @Override
    public PageData getPageRotation(int pageNumber) {
        return pagesList.get(pageNumber);
    }

    /**
     * Sets the viewport for a specific page.
     *
     * @param pageNumber The page number to set the viewport for.
     * @param viewport   The viewport to set.
     */
    @Override
    public void setViewport(int pageNumber, Rectangle2D viewport) {
        PageData pageData = pagesList.get(pageNumber);
        pagesList.set(pageNumber, new PageData(pageData.getPageNumber(), pageData.getRotationAngle(), viewport));
    }

    /**
     * Renders a specific page as a BufferedImage with the given scale and rotation.
     *
     * @param pageNumber     The page number to render.
     * @param scale          The scale at which to render the page.
     * @param rotationAngle  The rotation angle for rendering.
     * @param useCache       Whether to use a cache for rendering.
     * @return A Fx Image representing the rendered page.
     */
    @Override
    public BufferedImage renderPage(int pageNumber, float scale, double rotationAngle, boolean useCache) throws IOException {
        document.getPage(pageNumber).setRotation((int) rotationAngle);
        // PDFRenderer renderer = new PDFRenderer(document);
        //return renderer.renderImage(pageNumber, scale);
        return  render.render(pageNumber, scale);
    }

    /**
     * Gets the total number of pages in the document.
     *
     * @return The total number of pages.
     */
    @Override
    public int getNumberOfPages() {
        return document.getNumberOfPages();
    }

    /**
     * Checks if a specific page is in landscape orientation.
     *
     * @param pageNumber The page number to check.
     * @return True if the page is in landscape orientation, false otherwise.
     */
    @Override
    public boolean isLandscape(int pageNumber) {
        PDPage page = document.getPage(pageNumber);
        double rotationAngle = pagesList.get(pageNumber).getRotationAngle();
        PDRectangle cropBox = page.getCropBox();
        if (rotationAngle % 180 == 0.0) {
            return cropBox.getHeight() < cropBox.getWidth();
        } else {
            return cropBox.getWidth() < cropBox.getHeight();
        }
    }

    /**
     * Closes the document and releases associated resources.
     */
    @Override
    public void close() throws IOException {
        document.close();
    }

    /**
     * Updates the pages list based on the number of pages in the document.
     * This method populates the pagesList with PageData objects representing each page.
     */
    private void updatePagesList() {
        pagesList.clear();
        int numberOfPages = document.getNumberOfPages();
        List<PageData> pageKeys = new ArrayList<>();
        for (int i = 0; i < numberOfPages; i++) {
            PageData pageKey = new PageData(i, 0.0);
            pageKeys.add(pageKey);
        }
        pagesList.setAll(pageKeys);
    }

    /**
     * Gets the PDDocument associated with this instance of TextStripper.
     *
     * @return The PDDocument associated with this TextStripper.
     */
    @Override
    public PDDocument getDocument() {
        return document;
    }

    /**
     * Retrieves a list of search results for the specified search text within the PDF document.
     *
     * @param searchText The text to search for within the PDF document.
     * @return A list of SearchResult objects representing search results.
     * @throws IOException If there is an error during the search operation.
     */
    @Override
    public List<SearchResult> getSearchResults(String searchText) throws IOException {
        final TextStripper stripper = new TextStripper(this, searchText);
        return stripper.getSearchResults();
    }

    /**
     * Gets a Pageable object representing paginated content.
     *
     * @return A Pageable object for paginated content.
     */
    @Override
    public Pageable getPageable() {
        return new PDFPageable(this.document);
    }


    /**
     * Gets the image type.
     * @return the image type.
     */
    @Override
    public ImageType getImageType() {
        return imageType;
    }

    /**
     * Sets the image type.
     * @param imageType the image type to set.
     */
    @Override
    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

}
