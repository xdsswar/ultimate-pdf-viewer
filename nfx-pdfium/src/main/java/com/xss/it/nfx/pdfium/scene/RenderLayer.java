package com.xss.it.nfx.pdfium.scene;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * Internal bottom layer of a {@code PdfPageView}: shows the rendered page bitmap.
 *
 * <p>The {@link ImageView} fills the layer bounds. The host sizes the layer to a
 * device-pixel-aligned size matching the rendered bitmap, so this fill is an
 * exact 1:1 device mapping (crisp) for the steady frame, while still stretching
 * the previous frame to the new size for instant zoom feedback.</p>
 *
 * @author XDSSWAR
 */
public final class RenderLayer extends Region {

    private final ImageView imageView = new ImageView();

    /**
     * Creates an empty render layer.
     */
    public RenderLayer() {
        imageView.setSmooth(true);
        imageView.setPreserveRatio(false);
        imageView.setManaged(false);
        getChildren().add(imageView);
        setPickOnBounds(false);
        getStyleClass().add("pdf-render-layer");
    }

    /**
     * Sets (or clears) the displayed page image.
     *
     * @param image the rendered bitmap, or {@code null} to clear
     */
    public void setImage(Image image) {
        imageView.setImage(image);
    }

    /**
     * The currently displayed image.
     *
     * @return the image, or {@code null}
     */
    public Image getImage() {
        return imageView.getImage();
    }

    @Override
    protected void layoutChildren() {
        imageView.setFitWidth(getWidth());
        imageView.setFitHeight(getHeight());
        imageView.relocate(0, 0);
    }
}
