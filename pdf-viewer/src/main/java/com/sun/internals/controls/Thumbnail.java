package com.sun.internals.controls;

import com.sun.internals.PageData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * @author XDSSWAR
 * Created on 09/19/2023
 */
public final class Thumbnail extends VBox {
    /**
     * The StackPane used to hold the ImageView and numberLabel.
     */
    private final StackPane stackPane;

    /**
     * The ImageView for displaying an image.
     */
    private final ImageView imageView;

    /**
     * The Label for displaying a number or label.
     */
    private final Label numberLabel;

    /**
     * Constructs a Thumbnail object for the given PageData.
     *
     * @param pageData The PageData associated with this Thumbnail.
     */
    public Thumbnail(PageData pageData) {
        stackPane = new StackPane();
        imageView = new ImageView();
        numberLabel = new Label(String.format("%s",pageData.getPageNumber()+1));
        initialize();
    }

    /**
     * Initializes the object.
     * This method should be called to set up and configure the object's initial state.
     */
    private void initialize(){
        setAlignment(Pos.TOP_CENTER);
        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        getStyleClass().add("pdf-page-cell");
        stackPane.getStyleClass().add("pdf-page-cell-border");

        imageView.setFitHeight(180.0);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);
        VBox.setMargin(stackPane, new Insets(20.0, 20.0, 0.0, 20.0));

        numberLabel.getStyleClass().add("pdf-page-cell-label");
        VBox.setMargin(numberLabel, new Insets(20.0, 0.0, 20.0, 0.0));

        stackPane.getChildren().add(imageView);
        getChildren().add(stackPane);
        getChildren().add(numberLabel);
    }


    /**
     * Sets the image to be displayed in the ImageView.
     *
     * @param image The Image object to be displayed.
     */
    public void setImage(Image image){
        imageView.setImage(image);
    }
}
