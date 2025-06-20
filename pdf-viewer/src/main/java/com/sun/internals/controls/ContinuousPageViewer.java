/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.PdfDocument;
import com.sun.internals.RenderService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import xss.it.ultimate.pdf.viewer.controls.PageView;

/**
 * @author XDSSWAR
 * Created on 04/07/2024
 */
public final class ContinuousPageViewer extends AnchorPane implements PageView {
    /**
     * The PdfViewer associated with this scroll pane.
     */
    private final AbstractViewer abstractViewer;

    /**
     * The screen scale factor used for rendering.
     */
    private final double screenScale;

    /**
     * A SimpleDoubleProperty representing the current image scale.
     */
    private final SimpleDoubleProperty imageScale;


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
     * Style class
     */
    private static final String STYLE_CLASS = "continuous-page-viewer";


    /**
     * Constructs a ContinuousPageViewer object with the specified AbstractViewer.
     * @param abstractViewer The AbstractViewer to associate with the ContinuousPageViewer
     */
    public ContinuousPageViewer(AbstractViewer abstractViewer) {
        this.abstractViewer = abstractViewer;

        Screen primaryScreen = Screen.getPrimary();
        double screenDpi = primaryScreen.getDpi();
        screenScale = screenDpi / PdfDocument.DPI;
        imageScale = new SimpleDoubleProperty(1.0);
        imageScale.bind(abstractViewer.pageRenderDpiProperty().divide(PdfDocument.DPI));
        initialize();
    }


    private void initialize(){
        getStyleClass().add(STYLE_CLASS);

    }


    @Override
    public void reload() {

    }
}
