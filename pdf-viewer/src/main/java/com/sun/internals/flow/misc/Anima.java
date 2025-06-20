package com.sun.internals.flow.misc;

import javafx.animation.*;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author XDSSWAR
 * Created on 08/28/2023
 */
public class Anima {

    /**
     * Creates a fade-in animation for a given Node.
     *
     * @param node  The Node to apply the fade-in animation to.
     * @param delay The duration of the fade-in animation in milliseconds.
     * @return A Timeline representing the fade-in animation.
     */
    public static Timeline fadeIn(Node node, int delay){
        Duration duration = Duration.millis(delay);
        return new Timeline(new KeyFrame(duration,new KeyValue(node.opacityProperty(),1d, Interpolator.EASE_BOTH)));
    }


    /**
     * Creates a fade-out animation for a given Node.
     *
     * @param node  The Node to apply the fade-out animation to.
     * @param delay The duration of the fade-out animation in milliseconds.
     * @return A Timeline representing the fade-out animation.
     */
    public static Timeline fadeOt(Node node, int delay){
        Duration duration = Duration.millis(delay);
        return new Timeline(new KeyFrame(duration,new KeyValue(node.opacityProperty(),0d, Interpolator.EASE_BOTH)));
    }

    /**
     * Creates a fade-in animation for the given stage with the specified delay.
     * @param stage The stage to apply the fade-in animation to.
     * @param delay The duration of the fade-in animation in milliseconds.
     * @return A Timeline representing the fade-in animation.
     */
    public static Timeline fadeIn(Stage stage, int delay){
        return new Timeline(
                new KeyFrame(Duration.millis(delay), new KeyValue(stage.opacityProperty(), 1))
        );
    }

    /**
     * Creates a fade-in animation for the given popup control.
     *
     * @param control the PopupControl to apply the fade-in animation to
     * @param delay the duration of the fade-in animation in milliseconds
     * @return a Timeline object representing the fade-in animation
     */
    public static Timeline fadeIn(PopupControl control, int delay) {
        return new Timeline(
                new KeyFrame(Duration.millis(delay), new KeyValue(control.opacityProperty(), 1))
        );
    }


    /**
     * Creates a parallel transition to fade out one node and fade in another node simultaneously.
     *
     * @param hide      the node to be hidden
     * @param show      the node to be shown
     * @param millis    the duration of the fade transitions in milliseconds
     * @param onHidden  the event handler to be invoked when the hide transition is finished
     * @param onShown   the event handler to be invoked when the show transition is finished
     * @return          the parallel transition
     */
    public static ParallelTransition fadeInOut(
            Node hide,
            Node show,
            int millis,
            EventHandler<ActionEvent> onHidden,
            EventHandler<ActionEvent> onShown
    ) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(millis), hide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(onHidden);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(millis*2), show);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(onShown);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(fadeOut, fadeIn);
        return parallelTransition;
    }

    /**
     * Creates a timeline to toggle the visibility of a left pane.
     *
     * @param leftPane     the left pane to toggle
     * @param container    the container pane containing the left pane
     * @param widthRange   the range of width to animate the left pane
     * @param delay        the duration of the animation in milliseconds
     * @return             the created Timeline
     */
    public static Timeline toggle(AnchorPane leftPane, AnchorPane container, double widthRange, int delay){
        Duration duration = Duration.millis(delay);
        WritableValue<Double> customAnchorValue= new WritableValue<>() {
            @Override
            public Double getValue() {
                return AnchorPane.getLeftAnchor(container);
            }

            @Override
            public void setValue(Double value) {
                AnchorPane.setLeftAnchor(container, value);
            }
        };
        return new Timeline(
                new KeyFrame(duration,new KeyValue(leftPane.prefWidthProperty() ,widthRange, Interpolator.EASE_BOTH)),
                new KeyFrame(duration,new KeyValue(customAnchorValue,widthRange, Interpolator.EASE_BOTH))
        );
    }
}
