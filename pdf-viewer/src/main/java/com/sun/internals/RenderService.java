package com.sun.internals;

import com.sun.internals.document.Document;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

/**
 * JavaFX service that renders a PDF page to a crisp JavaFX {@link Image} via the
 * native PDFium engine. Re-renders whenever the page, scale or rotation changes.
 *
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class RenderService extends Service<Image> {

    private final AbstractViewer abstractViewer;
    private final boolean thumbnailRenderer;

    private final FloatProperty scale;
    private final DoubleProperty rotation;
    private final IntegerProperty page;

    /**
     * @param abstractViewer    the viewer this service renders for
     * @param thumbnailRenderer whether this service renders thumbnails
     */
    public RenderService(AbstractViewer abstractViewer, boolean thumbnailRenderer) {
        super();
        setExecutor(abstractViewer.getExecutor());
        this.abstractViewer = abstractViewer;
        this.thumbnailRenderer = thumbnailRenderer;
        this.scale = new SimpleFloatProperty(1f);
        this.rotation = new SimpleDoubleProperty(0.0);
        this.page = new SimpleIntegerProperty(-1);
        InvalidationListener restartListener = o -> restart();
        page.addListener(restartListener);
        scale.addListener(restartListener);
        rotation.addListener(restartListener);
    }

    public float getScale() {
        return scale.get();
    }

    public FloatProperty scaleProperty() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale.set(scale);
    }

    public double getRotation() {
        return rotation.get();
    }

    public DoubleProperty rotationProperty() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation.set(rotation);
    }

    public int getPage() {
        return page.get();
    }

    public IntegerProperty pageProperty() {
        return page;
    }

    public void setPage(int page) {
        this.page.set(page);
    }

    @Override
    protected Task<Image> createTask() {
        return new RenderTask(thumbnailRenderer, getPage(), getScale(), getRotation());
    }

    private final class RenderTask extends Task<Image> {
        private final boolean thumbnail;
        private final int page;
        private final float scale;
        private final double rotation;

        RenderTask(boolean thumbnail, int page, float scale, double rotation) {
            this.thumbnail = thumbnail;
            this.page = page;
            this.scale = scale;
            this.rotation = rotation;
        }

        @Override
        protected Image call() {
            Document document = abstractViewer.getDocument();
            int numberOfPages = document != null ? document.getNumberOfPages() : 0;
            if (page < 0 || page >= numberOfPages) {
                return null;
            }
            boolean useCache = abstractViewer.isCacheThumbnails() && thumbnail;
            return document.renderPage(page, scale, rotation, useCache);
        }
    }
}
