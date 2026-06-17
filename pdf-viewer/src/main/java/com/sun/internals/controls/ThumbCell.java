/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
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
import com.sun.internals.ThumbData;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A single page thumbnail: the rendered page inside a bordered box, with a
 * page-number badge overlaid at the bottom. The {@code :selected} pseudo-class
 * marks the currently shown page (styled via CSS).
 *
 * @author XDSSWAR
 * Created on 06/20/2025
 */
public final class ThumbCell extends StackPane {

    /** Thumbnail render scale (points-to-pixels). */
    private static final float THUMB_SCALE = 0.5f;

    /** CSS pseudo-class applied to the current (shown) page's thumbnail. */
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final StackPane box;
    private final ImageView imageView;
    private final Label badge;
    private final NfxCircularLoader loader;
    private final AbstractViewer viewer;

    public ThumbCell(AbstractViewer viewer) {
        this.viewer = viewer;
        box = new StackPane();
        imageView = new ImageView();
        badge = new Label();
        loader = new NfxCircularLoader();
        initialize();
    }

    private void initialize(){
        getStyleClass().add("pdf-thumb-cell");
        setAlignment(Pos.CENTER);

        // The page, fit within a uniform box (preserving aspect) so even tall
        // pages never overflow; the box border wraps it.
        imageView.setFitWidth(150.0);
        imageView.setFitHeight(190.0);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);

        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("box");
        box.getChildren().add(imageView);
        // Wrap the page tightly so the border hugs the page, not the cell width.
        box.setMaxWidth(USE_PREF_SIZE);
        box.setMaxHeight(USE_PREF_SIZE);

        badge.getStyleClass().add("page-badge");
        badge.setText("1");

        // Small loader shown over the cell while this page's thumbnail renders.
        loader.setRadius(9);
        loader.setStrokeWidth(2);
        loader.setAutoStart(false);
        loader.setMouseTransparent(true);

        getChildren().addAll(box, loader, badge);
        // Badge overlaps the bottom-center of the page (position tuned in CSS).
        StackPane.setAlignment(badge, Pos.BOTTOM_CENTER);

        events();
    }

    private void events(){
        handleThumb(getThumbData());
        thumbDataProperty().addListener((obs, o, t) -> {
            handleThumb(t);
            updateSelected();
        });

        // Highlight the thumbnail of the page currently shown in the viewer.
        viewer.pageProperty().addListener((obs, o, p) -> updateSelected());
        updateSelected();
    }

    /** Toggles the {@code :selected} pseudo-class when this is the current page. */
    private void updateSelected(){
        ThumbData data = getThumbData();
        boolean selected = data != null && viewer.getPage() == data.index();
        pseudoClassStateChanged(SELECTED, selected);
    }

    private ObjectProperty<ThumbData> thumbData;

    public ThumbData getThumbData() {
        return thumbDataProperty().get();
    }

    public ObjectProperty<ThumbData> thumbDataProperty() {
        if (thumbData == null){
            thumbData = new SimpleObjectProperty<>();
        }
        return thumbData;
    }

    public void setThumbData(ThumbData thumbData) {
        thumbDataProperty().set(thumbData);
    }

    private void handleThumb(ThumbData data){
        if (data == null) return;

        badge.setText(String.valueOf(data.pageIndex()));

        // Cell may be reused for another page: clear the stale image and show the loader.
        imageView.setImage(null);
        loader.setAutoStart(true);

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() {
                return viewer.getDocument().renderPage(data.index(), THUMB_SCALE, 0, true);
            }
        };

        task.valueProperty().addListener((obs, o, i) -> {
            Platform.runLater(()-> {
                loader.setAutoStart(false);
                imageView.setImage(i);
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> loader.setAutoStart(false)));

        viewer.getExecutor().submit(task);
    }
}
