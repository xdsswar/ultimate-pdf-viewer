package com.sun.internals.controls;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A full-cover modal overlay: a semi-transparent scrim that dims everything
 * beneath it and hosts a single piece of content centered on top.
 *
 * <p>Reusable for any centered, modal-style panel (document properties, a
 * password prompt, …). The overlay is hidden and unmanaged until {@link #show}
 * is called, so it neither paints nor intercepts input while idle. Clicking the
 * scrim (outside the content) dismisses it; clicks on the content itself are
 * consumed so they never reach the scrim.</p>
 *
 * @author XDSSWAR
 */
public final class OverlayPane extends StackPane {

    /**
     * Creates an empty, hidden overlay. Add it to the viewer root anchored to
     * every edge so it covers the whole control when shown.
     */
    public OverlayPane() {
        getStyleClass().add("pdf-overlay");
        setVisible(false);
        setManaged(false);
        // A click that reaches the scrim itself (not the content) closes it.
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) {
                hide();
            }
        });
    }

    /**
     * Shows the given content centered over the scrim, with a brief fade + lift
     * entrance animation (scrim fades in, the card scales up from slightly small).
     *
     * @param content the node to display (replaces any previous content)
     */
    public void show(Node content) {
        // Swallow clicks on the content so they don't bubble out and close it.
        content.addEventHandler(MouseEvent.MOUSE_CLICKED, Event::consume);
        getChildren().setAll(content);
        setVisible(true);
        setManaged(true);
        toFront();
        animateIn(content);
    }

    /** Plays the scrim fade and card lift-in. */
    private void animateIn(Node content) {
        setOpacity(0);
        content.setScaleX(0.94);
        content.setScaleY(0.94);
        content.setTranslateY(8);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), 0),
                        new KeyValue(content.scaleXProperty(), 0.94),
                        new KeyValue(content.scaleYProperty(), 0.94),
                        new KeyValue(content.translateYProperty(), 8)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.translateYProperty(), 0, Interpolator.EASE_OUT)));
        timeline.play();
    }

    /** Hides the overlay and releases its content. */
    public void hide() {
        setVisible(false);
        setManaged(false);
        getChildren().clear();
    }

    /**
     * Whether the overlay is currently shown.
     *
     * @return {@code true} if visible
     */
    public boolean isShowing() {
        return isVisible();
    }
}
