package com.sun.internals;

import javafx.geometry.Rectangle2D;

import java.util.Objects;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class PageData {
    /**
     * Represents the page number within a document.
     */
    private final int pageNumber;

    /**
     * Represents the rotation angle of the page.
     */
    private final double rotationAngle;

    /**
     * Represents the rectangular viewport of the page.
     */
    private Rectangle2D viewport=null;


    /**
     * Constructs a PageData object with specified properties.
     *
     * @param pageNumber    The page number within a document.
     * @param rotationAngle The rotation angle of the page.
     * @param viewport      The rectangular viewport of the page.
     */
    public PageData(int pageNumber, double rotationAngle, Rectangle2D viewport) {
        this.pageNumber = pageNumber;
        this.rotationAngle = rotationAngle;
        this.viewport = viewport;
    }

    /**
     * Constructs a PageData object with specified properties.
     *
     * @param pageNumber    The page number within a document.
     * @param rotationAngle The rotation angle of the page.
     */
    public PageData(int pageNumber, double rotationAngle) {
        this.pageNumber = pageNumber;
        this.rotationAngle = rotationAngle;
    }


    /**
     * Retrieves the page number of this PageData object.
     *
     * @return The page number.
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Retrieves the rotation angle of this PageData object.
     *
     * @return The rotation angle.
     */
    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Retrieves the viewport of this PageData object.
     *
     * @return The viewport.
     */
    public Rectangle2D getViewport() {
        return viewport;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PageData pageData = (PageData) object;
        return pageNumber == pageData.pageNumber
                && Double.compare(rotationAngle, pageData.rotationAngle) == 0
                && Objects.equals(viewport, pageData.viewport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, rotationAngle, viewport);
    }
}
