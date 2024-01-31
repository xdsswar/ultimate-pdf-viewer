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

package com.sun.internals.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * @author XDSSWAR
 * Created on 01/30/2024
 */
public class Animation {
    /**
     * Creates a Timeline for fading in a child Node within a Pane.
     *
     * @param parent      The parent Pane.
     * @param targetWidth The target width for the parent Pane.
     * @param child       The child Node to be faded in.
     * @param delay       The delay before the fade-in starts (in milliseconds).
     * @return The Timeline for the fade-in animation.
     */
    public static Timeline fadeIn(Pane parent, double targetWidth ,Node child, int delay){
        Timeline t = new Timeline();
        t.setAutoReverse(false);
        t.setCycleCount(1);
        Duration d= Duration.millis(delay);
        KeyFrame k1 = new KeyFrame(d, new KeyValue(parent.prefWidthProperty(), targetWidth));
        KeyFrame k2 = new KeyFrame(d, new KeyValue(child.opacityProperty(), 1d));
        KeyFrame k3 = new KeyFrame(d, new KeyValue(parent.maxWidthProperty(), targetWidth));
        t.getKeyFrames().addAll(k1, k3);
        return t;
    }

    /**
     * Creates a Timeline for fading out a child Node within a Pane.
     *
     * @param parent      The parent Pane.
     * @param targetWidth The target width for the parent Pane.
     * @param delay       The delay before the fade-out starts (in milliseconds).
     * @return The Timeline for the fade-out animation.
     */
    public static Timeline doResizeAnimated(Pane parent, Parent center, double targetWidth , int delay, boolean left){
        Timeline t = new Timeline();
        t.setAutoReverse(false);
        t.setCycleCount(1);
        Duration d= Duration.millis(delay);
        KeyFrame k1 = new KeyFrame(d, new KeyValue(parent.prefWidthProperty(), targetWidth));

        WritableValue<Double> value = new WritableValue<>() {
            @Override
            public Double getValue() {
                return left ? AnchorPane.getLeftAnchor(center) : AnchorPane.getRightAnchor(center);
            }

            @Override
            public void setValue(Double aDouble) {
                if (left) {
                    AnchorPane.setLeftAnchor(center, aDouble);
                }else {
                    AnchorPane.setRightAnchor(center, aDouble);
                }
            }
        };

        KeyFrame k2 = new KeyFrame(d, new KeyValue(value, targetWidth));
        t.getKeyFrames().addAll(k1, k2);
        return t;
    }
}
