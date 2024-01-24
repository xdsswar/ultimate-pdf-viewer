package com.sun.internals.controls;

import com.sun.internals.PdfDocument;
import com.sun.internals.RenderService;
import com.sun.internals.enums.Fit;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import xss.it.ultimate.pdf.viewer.PdfViewer;

import static xss.it.ultimate.pdf.viewer.Assets.LOADER;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public final class ScalableScrollPane extends ScrollPane {

    /**
     * The PdfViewer associated with this scroll pane.
     */
    private final PdfViewer pdfViewer;

    /**
     * The rendering service for the main area of the viewer.
     */
    private final RenderService renderService;

    /**
     * The ImageView used for displaying rendered content.
     */
    private final ImageView imageView;

    /**
     * The screen scale factor used for rendering.
     */
    private final double screenScale;

    /**
     * A SimpleDoubleProperty representing the current image scale.
     */
    private final SimpleDoubleProperty imageScale;

    /**
     * The key scroll amount for navigation.
     */
    private final double keyScroll;

    /**
     * The mouse pointer used for zooming operations.
     */
    private Point2D zoomMousePointer;

    /**
     * A flag indicating whether zooming is locked.
     */
    private boolean zoomLock = false;

    /**
     * The zoom delta value for zooming operations.
     */
    private static final double ZOOM_DELTA = 0.1;

    /**
     * The count of scroll hits at the bottom of the content.
     */
    private int bhCount = 0;

    /**
     * The count of scroll hits at the top of the content.
     */
    private int thCount = 0;

    /**
     * Style class
     */
    private static final String STYLE_CLASS = "scalable-pane";

    /**
     * The StackPane that contains the ImageView and the loading/wait image.
     */
    private final StackPane pane;

    /**
     * Page wrapper/border
     */
    private final StackPane border;

    /**
     * Loader
     */
    private final ImageView loader;

    /**
     * Constructs a ScalableScrollPane with the specified PdfViewerSkin.
     *
     * @param pdfViewer The PdfViewer associated with this scroll pane.
     */
    public ScalableScrollPane(PdfViewer pdfViewer) {
        getStyleClass().add(STYLE_CLASS);
        this.pdfViewer = pdfViewer;
        this.renderService = new RenderService(pdfViewer, false);
        this.imageView = new ImageView();
        this.loader = new ImageView(LOADER);
        this.loader.setPreserveRatio(true);
        this.loader.setFitHeight(30);
        this.loader.setFitHeight(30);
        border = new StackPane();
        border.setAlignment(Pos.CENTER);
        border.setCache(false);
        border.maxWidthProperty().bind(this.imageView.fitWidthProperty());
        border.maxHeightProperty().bind(this.imageView.fitHeightProperty());
        border.getChildren().add(this.imageView);
        border.getStyleClass().add("page-border");
        pane = new StackPane(border);
        pane.getStyleClass().add("view-base");
        pane.setCache(false);
        pane.setAlignment(Pos.CENTER);
        setPannable(true);
        Screen primaryScreen = Screen.getPrimary();
        double screenDpi = primaryScreen.getDpi();
        screenScale = screenDpi / PdfDocument.DPI;
        imageScale = new SimpleDoubleProperty(1.0);
        imageScale.bind(pdfViewer.pageRenderDpiProperty().divide(PdfDocument.DPI));
        keyScroll = 0.002 * screenDpi;
        initView();
        initRender();
        initBindings();

        setContent(pane);
        setFitToWidth(true);
        events();
    }

    /**
     * Sets up the ImageView component for displaying rendered content within the viewer.
     */
    private void initView() {
        imageView.setPreserveRatio(true);
        imageView.imageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                layoutImage(newValue);
            }
        });
    }

    /**
     * Sets up the rendering service responsible for rendering the viewer's content.
     */
    private void initRender() {
        renderService.scaleProperty().bind(imageScale);
        renderService.pageProperty().bind(pdfViewer.pageProperty());
        renderService.rotationProperty().bind(pdfViewer.pageRotationProperty());

        renderService.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(()->{
                    imageView.setImage(newValue);
                });
            }
        });

        renderService.setOnScheduled(event -> {
            Platform.runLater(()->{
                border.getChildren().remove(imageView);
                if (!border.getChildren().contains(loader)) {
                    border.getChildren().add(loader);
                }
                //imageView.setOpacity(.2);
            });
        });

        renderService.setOnSucceeded(event -> {
           Platform.runLater(()->{
               //imageView.setOpacity(1.0);
               border.getChildren().remove(loader);
               if (!border.getChildren().contains(imageView)) {
                   border.getChildren().add(imageView);
               }
           });
        });
    }

    /**
     * Sets up data bindings between properties and UI elements within the viewer.
     */
    private void initBindings() {

        pdfViewer.fitProperty().addListener((obs, o, fit) -> {
            switch (fit){
                case VERTICAL -> fitHeight();
                case HORIZONTAL -> fitWidth();
            }
        });

        pdfViewer.zoomFactorProperty().addListener((observable, oldValue, newValue) -> {
            Point2D mousePointer = zoomMousePointer != null ? zoomMousePointer : new Point2D(getWidth() / 2, getHeight() / 2);
            zoom(oldValue.doubleValue(), newValue.doubleValue(), mousePointer);
        });
    }

    /**
     * Sets up mouse and keyboard events for interacting with the viewer's content.
     */
    private void events() {
        /*
         * Mouse scroll-wheel, do not touch unless you understand what's happening.
         */

        getContent().setOnScroll(event -> {
            if (event.isInertia()) {
                return;
            }
            if (event.isControlDown()) {
                if (isNotZoomable()) {
                    pdfViewer.setFit(Fit.NONE);
                }
                double oldZoom = pdfViewer.getZoomFactor();
                double oldVVal = getVvalue();
                double oldHVal = getHvalue();
                double delta = event.getDeltaY() > 0 ? ZOOM_DELTA : -ZOOM_DELTA;
                zoomMousePointer = sceneToLocal(new Point2D(event.getSceneX(), event.getSceneY()));
                pdfViewer.setZoomFactor(oldZoom + delta);
                if (oldZoom== pdfViewer.getMaxZoomFactor()){
                    setHvalue(oldHVal);
                    setVvalue(oldVVal);
                }
                event.consume();
            } else {
                double deltaY = event.getDeltaY();
                setVvalue(getVvalue() - deltaY * 6 / getContent().getLayoutBounds().getHeight());

                /*
                 * This is a custom scrolling impl for scrolling tp next/prev page
                 * when the page height is less that this control height,
                 * Otherwise we scroll to bottom/up the page and go next/prev page when
                 * the respective hit counter reached the desired value.
                 */
                if (getHeight() > border.getHeight()){
                    if (deltaY>0){                     //To the top
                        toPrevPage();
                    }
                    else if (deltaY<0){                 //To the bottom
                        toNextPage();
                    }
                }
                else {
                    if (getVvalue() == 1.0) {
                        if (bhCount == 1) {             // Scroll to the next page on the second hit at the bottom
                            bhCount = 0;                // Reset the count
                            toNextPage();
                        } else {
                            bhCount++;
                        }
                    } else if (getVvalue() == 0.0) {    // We are at the top of the ScrollPane
                        if (thCount == 1) {             // Scroll to the previous page on the second hit at the top
                            thCount = 0;                // Reset the count
                            toPrevPage();
                        } else {
                            thCount++;
                        }
                    } else {                            // Not at the top or bottom, reset the counts
                        thCount = 0;
                        bhCount = 0;
                    }
                }
                event.consume();
            }
        });

        // Scroll with keyboard
        setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            switch (code){
                case LEFT,UP,PAGE_UP -> {
                    if (getHeight() < border.getHeight()) {
                        setVvalue(getVvalue() - keyScroll);
                        if (getVvalue() == getVmin()){
                            if (thCount == 1) {             // Scroll to the previous page on the second hit at the top
                                thCount = 0;                // Reset the count
                                toPrevPage();
                            } else {
                                thCount++;
                            }
                        }else {
                            thCount=0;
                        }
                    }else {
                        if (pdfViewer.gotoPreviousPage()) {
                            setVvalue(getVmax());
                        }
                    }
                }
                case RIGHT, DOWN,PAGE_DOWN -> {
                    if (getHeight() < border.getHeight()) {
                        setVvalue(getVvalue() + keyScroll);
                        if (getVvalue() == getVmax()){
                            if (bhCount == 1) {             // Scroll to the next page on the second hit at the bottom
                                bhCount = 0;                // Reset the count
                                toNextPage();
                            } else {
                                bhCount++;
                            }
                        }else {
                            bhCount = 0;
                        }
                    }else {
                        if (pdfViewer.gotoNextPage()) {
                            setVvalue(getVmin());
                        }
                    }
                }
                case HOME -> {
                    if (pdfViewer.gotoFirstPage()) {
                        setVvalue(getVmin());
                    }
                }
                case END -> {
                    if (pdfViewer.gotoLastPage()){
                        setVvalue(getVmin());
                    }
                }
            }
        });

        // Window size changed
        viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            fitWidthOrHeight();
            calculateViewport();
            layoutImage(imageView.getImage());
        });

        // Scroll changed
        vvalueProperty().addListener((observable, oldValue, newValue) -> calculateViewport());
        hvalueProperty().addListener((observable, oldValue, newValue) -> calculateViewport());
    }


    /**
     * Zooms the viewer's content from an old zoom level to a new zoom level, centered around a specified point.
     *
     * @param oldZoom       The previous zoom level.
     * @param newZoom       The new zoom level.
     * @param mousePointer  The point around which the zoom should be centered.
     */
    private void zoom(double oldZoom, double newZoom, Point2D mousePointer) {
        if (newZoom < pdfViewer.getMinZoomFactor()){
            pdfViewer.setZoomFactor(pdfViewer.getMinZoomFactor());
            return;
        }
        if (newZoom > pdfViewer.getMaxZoomFactor()){
            pdfViewer.setZoomFactor(pdfViewer.getMaxZoomFactor());
            return;
        }

        Point2D mouseOffset = getViewportOffset().add(mousePointer);
        Point2D imageOffset = mouseOffset.multiply(1 / oldZoom);
        Point2D newMouseOffset = imageOffset.multiply(newZoom);
        Point2D offset = newMouseOffset.subtract(mousePointer);
        Point2D scrollValue = getViewportScroll(offset);
        layoutImage(imageView.getImage(), scrollValue);//was null
    }

    /**
     * Positions and scales the provided Image within the viewer's layout using default scroll values.
     *
     * @param image The Image to be positioned and scaled within the layout.
     */
    private void layoutImage(Image image) {
        layoutImage(image, new Point2D(getHvalue(), getVvalue()));
    }

    /**
     * Positions and scales the provided Image within the viewer's layout, considering scroll values.
     *
     * @param image       The Image to be positioned and scaled within the layout.
     * @param scrollValue The scroll values to apply for positioning.
     */
    private void layoutImage(Image image, Point2D scrollValue) {
        if (image == null) return;
        double fitWidth = (image.getWidth() / imageScale.get()) * screenScale * pdfViewer.getZoomFactor();
        double fitHeight = (image.getHeight() / imageScale.get()) * screenScale * pdfViewer.getZoomFactor();
        Bounds bounds = getViewportBounds();
        pane.setPrefSize(bounds.getWidth(),bounds.getHeight());
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        setHvalue(scrollValue.getX());
        setVvalue(scrollValue.getY());
        fitWidthOrHeight();
    }


    /**
     * Adjusts the viewer's content to fit either the width or height, depending on the aspect ratio.
     * This operation may change the scale and viewport.
     */
    private void fitWidthOrHeight() {
        if (pdfViewer.getFit().equals(Fit.VERTICAL)){
            fitHeight();
        }
        if (pdfViewer.getFit().equals(Fit.HORIZONTAL)){
            fitWidth();
        }
    }


    /**
     * Adjusts the viewer's content to fit the viewer's width while preserving the aspect ratio.
     * This operation may change the scale and viewport.
     */
    private void fitWidth() {
        if (zoomLock) {
            return;
        }
        zoomLock = true;
        try {
            double width = imageView.getFitWidth() / pdfViewer.getZoomFactor() + 10;
            double zoom = getWidth() / width;
            pdfViewer.setZoomFactor(zoom);
        } finally {
            zoomLock = false;
        }
    }

    /**
     * Adjusts the viewer's content to fit the viewer's height while preserving the aspect ratio.
     * This operation may change the scale and viewport.
     */
    private void fitHeight() {
        if (zoomLock) {
            return;
        }
        zoomLock = true;
        try {
            double height = imageView.getFitHeight() / pdfViewer.getZoomFactor() + 10;
            double zoom = getHeight() / height;
            pdfViewer.setZoomFactor(zoom);
        } finally {
            zoomLock = false;
        }
    }

    /**
     * Checks if the viewer is currently configured not to allow zooming.
     *
     * @return True if either horizontal or vertical fitting is enabled, indicating non-zoomable state.
     */
    private boolean isNotZoomable() {
        return pdfViewer.getFit().equals(Fit.HORIZONTAL) || pdfViewer.getFit().equals(Fit.VERTICAL);
    }

    /**
     * Calculates the offset of the current viewport relative to the content's top-left corner.
     *
     * @return A Point2D representing the horizontal and vertical offset values.
     */
    private Point2D getViewportOffset() {
        double offContentWidth = Math.max(0.0, getContent().getLayoutBounds().getWidth() - getViewportBounds().getWidth());
        double offContentHeight = Math.max(0.0, getContent().getLayoutBounds().getHeight() - getViewportBounds().getHeight());
        return new Point2D(offContentWidth * getHvalue(), offContentHeight * getVvalue());
    }

    /**
     * Calculates the scroll values needed to position the viewport with the given offset.
     *
     * @param offset The offset from the top-left corner of the content.
     * @return A Point2D representing the horizontal and vertical scroll values.
     */
    private Point2D getViewportScroll(Point2D offset) {
        double offContentWidth = Math.max(0.0, getContent().getLayoutBounds().getWidth() - getViewportBounds().getWidth());
        double offContentHeight = Math.max(0.0, getContent().getLayoutBounds().getHeight() - getViewportBounds().getHeight());
        return new Point2D(
                offContentWidth == 0.0 ? 0.0 : offset.getX() / offContentWidth,
                offContentHeight == 0.0 ? 0.0 : offset.getY() / offContentHeight
        );
    }

    /**
     * Calculates the current viewport based on the viewer's properties and sets it in the PdfViewerSkin.
     */
    private void calculateViewport() {
        Point2D offset = getViewportOffset();
        pdfViewer.setCurrentViewPort(
                new Rectangle2D(
                        offset.getX() / imageView.getFitWidth(),
                        offset.getY() / imageView.getFitHeight(),
                        getViewportBounds().getWidth() / imageView.getFitWidth(),
                        getViewportBounds().getHeight() / imageView.getFitHeight()
                )
        );
    }

    /**
     * Navigates to the next page of the PDF document.
     * If successful, sets the vertical scrollbar value to its minimum.
     */
    private void toNextPage() {
        if (pdfViewer.gotoNextPage()) {
            setVvalue(getVmin());
        }
    }

    /**
     * Navigates to the previous page of the PDF document.
     * If successful, sets the vertical scrollbar value to its maximum.
     */
    private void toPrevPage() {
        if (pdfViewer.gotoPreviousPage()) {
            setVvalue(getVmax());
        }
    }

    /**
     * Sets the viewport of the viewer to the specified Rectangle2D.
     *
     * @param vp The new viewport represented as a Rectangle2D.
     */
    public  void setViewport(Rectangle2D vp) {
        Point2D offset = new Point2D(vp.getMinX() * imageView.getFitWidth(), vp.getMinY() * imageView.getFitHeight());
        Point2D scroll = getViewportScroll(offset);
        setHvalue(scroll.getX());
        setVvalue(scroll.getY());
    }

    /**
     * Reloads the viewer's content by restarting the main area render service.
     */
    public void reload() {
        renderService.restart();
    }


}