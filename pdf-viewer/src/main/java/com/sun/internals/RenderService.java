package com.sun.internals;

import com.sun.internals.document.Document;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import xss.it.ultimate.pdf.viewer.PdfViewer;

import java.io.IOException;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class RenderService extends Service<Image> {
    /**
     * Represents the PdfViewer associated with this class.
     */
    private final PdfViewer pdfViewer;

    /**
     * A boolean flag indicating whether thumbnail rendering is enabled.
     */
    private final boolean thumbnailRenderer;

    /**
     * A FloatProperty for controlling the scale of the viewer's content.
     */
    private final FloatProperty scale;

    /**
     * A DoubleProperty for controlling the rotation angle of the viewer's content.
     */
    private final DoubleProperty rotation;

    /**
     * An IntegerProperty representing the current page number being displayed in the viewer.
     */
    private final IntegerProperty page;


    /**
     * Constructs a RenderService with the specified PdfViewerSkin, thumbnail rendering flag,
     * and executor for managing rendering tasks.
     *
     * @param pdfViewer       The PdfViewer associated with this rendering service.
     * @param thumbnailRenderer A boolean flag indicating whether thumbnail rendering is enabled.
     */
    public RenderService(PdfViewer pdfViewer, boolean thumbnailRenderer) {
        super();
        setExecutor(pdfViewer.getExecutor());
        this.pdfViewer = pdfViewer;
        this.thumbnailRenderer = thumbnailRenderer;
        scale = new SimpleFloatProperty(1f);
        rotation = new SimpleDoubleProperty(0.0);
        page = new SimpleIntegerProperty(-1);
        InvalidationListener restartListener = o -> restart();
        page.addListener(restartListener);
        scale.addListener(restartListener);
        rotation.addListener(restartListener);
    }

    /**
     * Gets the current scale value of the viewer's content.
     *
     * @return The current scale value.
     */
    public float getScale() {
        return scale.get();
    }

    /**
     * Returns the FloatProperty representing the scale of the viewer's content.
     *
     * @return The scale property.
     */
    public FloatProperty scaleProperty() {
        return scale;
    }

    /**
     * Sets the scale of the viewer's content to the specified value.
     *
     * @param scale The new scale value to set.
     */
    public void setScale(float scale) {
        this.scale.set(scale);
    }

    /**
     * Gets the current rotation angle of the viewer's content.
     *
     * @return The current rotation angle.
     */
    public double getRotation() {
        return rotation.get();
    }

    /**
     * Returns the DoubleProperty representing the rotation angle of the viewer's content.
     *
     * @return The rotation property.
     */
    public DoubleProperty rotationProperty() {
        return rotation;
    }

    /**
     * Sets the rotation angle of the viewer's content to the specified value.
     *
     * @param rotation The new rotation angle to set.
     */
    public void setRotation(double rotation) {
        this.rotation.set(rotation);
    }

    /**
     * Gets the current page number being displayed in the viewer.
     *
     * @return The current page number.
     */
    public int getPage() {
        return page.get();
    }

    /**
     * Returns the IntegerProperty representing the current page number in the viewer.
     *
     * @return The page property.
     */
    public IntegerProperty pageProperty() {
        return page;
    }

    /**
     * Sets the current page number to the specified value in the viewer.
     *
     * @param page The new page number to set.
     */
    public void setPage(int page) {
        this.page.set(page);
    }

    /**
     * Creates a new rendering task for rendering an image.
     *
     * @return A Task<Image> instance representing the rendering task.
     */
    @Override
    protected Task<Image> createTask() {
        return new RenderTask(thumbnailRenderer, getPage(), getScale(), getRotation());
    }


    private final class RenderTask extends Task<Image>{
        /**
         * A boolean flag indicating whether this rendering task is for a thumbnail.
         */
        private final boolean thumbnail;

        /**
         * The page number associated with this rendering task.
         */
        private final int page;

        /**
         * The scale at which the rendering should be performed.
         */
        private final float scale;

        /**
         * The rotation angle for rendering the content.
         */
        private final double rotation;

        /**
         * Constructs a RenderTask with the specified rendering parameters.
         *
         * @param thumbnail A boolean flag indicating whether this task is for rendering a thumbnail.
         * @param page      The page number to render.
         * @param scale     The scale at which to render the content.
         * @param rotation  The rotation angle for rendering the content.
         */
        public RenderTask(boolean thumbnail, int page, float scale, double rotation) {
            super();
            this.thumbnail = thumbnail;
            this.page = page;
            this.scale = scale;
            this.rotation = rotation;
        }

        /**
         * Background task that renders a specific page of the PDF document to an image.
         *
         * @return An Image object representing the rendered page, or null if the page is out of range or rendering fails.
         * @throws Exception If there is an error during the rendering process.
         */
        @Override
        protected Image call() throws Exception {
            Document document = pdfViewer.getDocument();
            int numberOfPages = document != null ? document.getNumberOfPages() : 0;
            return (page >= 0 && page < numberOfPages)
                    ? renderPDFPage(
                    page,
                    scale,
                    rotation,
                    pdfViewer.isCacheThumbnails() && thumbnail
            ) : null;
        }

        /**
         * Renders a PDF page to an image with the specified parameters.
         *
         * @param pageNumber The page number to render.
         * @param scale      The scale at which to render the page.
         * @param rotation   The rotation angle for rendering.
         * @param useCache   Whether to use a cache for rendering.
         * @return An Image object representing the rendered page, or null if rendering fails or is canceled.
         * @throws IOException If there is an error rendering the PDF page.
         */
        private synchronized Image renderPDFPage(int pageNumber, float scale, double rotation, boolean useCache) throws IOException {
            Document document = pdfViewer.getDocument();
            return document != null ? document.renderPage(pageNumber,scale,rotation,useCache) : null;
        }

    }
}
