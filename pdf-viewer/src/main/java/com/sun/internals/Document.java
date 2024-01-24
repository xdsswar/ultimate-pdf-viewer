package com.sun.internals;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import java.io.IOException;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public interface Document {

    /**
     * List of pages with additional properties. Data source for thumbnail list
     */
    ObservableList<PageData> getPagesList();

    /**
     * Sets rotation for given page
     */
    void setPageRotation(int pageNumber, double rotationAngle);

    /**
     * Gets rotation for given page
     */
    PageData getPageRotation(int pageNumber);

    /**
     * Sets viewport for given page
     */
    void setViewport(int pageNumber, Rectangle2D viewport);

    /**
     * Renders the page specified by the given number at the given scale.
     *
     * @param pageNumber   the page number
     * @param scale        the scale
     * @param rotationAngle the rotation angle
     * @param useCache     cache the page (used for thumbnails)
     * @return the generated fx image
     */
    Image renderPage(int pageNumber, float scale, double rotationAngle, boolean useCache) throws IOException;

    /**
     * Returns the total number of pages inside the document.
     *
     * @return the total number of pages
     */
    int getNumberOfPages();

    /**
     * Determines if the given page has a landscape orientation.
     *
     * @param pageNumber the page
     * @return true if the page has to be shown in landscape mode
     */
    boolean isLandscape(int pageNumber);

    /**
     * Closes the document.
     */
    void close() throws IOException;
}
