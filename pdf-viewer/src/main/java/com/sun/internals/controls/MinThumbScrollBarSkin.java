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

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.layout.Region;

/**
 * A {@link ScrollBar} skin that enforces a minimum thumb length. The stock skin
 * sizes the thumb strictly proportionally (visible / total), so with very large
 * content — e.g. a PDF of hundreds of pages — the thumb shrinks to a sub-pixel
 * sliver and effectively disappears; {@code -fx-min-height} on the thumb is not
 * honored.
 *
 * <p>The minimum length comes from the thumb's CSS {@code -fx-min-height}
 * (vertical) / {@code -fx-min-width} (horizontal) — this skin just makes the
 * stock skin honor it. The thumb is re-clamped not only on layout but also
 * whenever the scroll value or range changes (the default skin repositions the
 * thumb on value change outside the normal layout pass, which is why scrolling
 * to the end would otherwise drop the enlarged thumb).</p>
 *
 * @author XDSSWAR
 */
public final class MinThumbScrollBarSkin extends ScrollBarSkin {

    /**
     * Creates the skin and keeps the thumb clamped as the scroll state changes.
     *
     * @param control the scroll bar
     */
    public MinThumbScrollBarSkin(ScrollBar control) {
        super(control);
        // Re-clamp whenever the skin would reposition the thumb on its own.
        control.valueProperty().addListener(o -> clampThumb());
        control.visibleAmountProperty().addListener(o -> clampThumb());
        control.minProperty().addListener(o -> clampThumb());
        control.maxProperty().addListener(o -> clampThumb());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        clampThumb();
    }

    /**
     * Sizes the thumb to {@code max(natural, minThumb)} and positions it for the
     * current value, within the track's bounds (so it never overflows the ends).
     */
    private void clampThumb() {
        ScrollBar sb = getSkinnable();
        Node thumbNode = sb.lookup(".thumb");
        Node trackNode = sb.lookup(".track");
        if (!(thumbNode instanceof Region thumb) || !(trackNode instanceof Region track)) {
            return;
        }
        double range = sb.getMax() - sb.getMin();
        if (range <= 0) {
            return;
        }
        double frac = Math.max(0, Math.min(1, (sb.getValue() - sb.getMin()) / range));
        double visible = sb.getVisibleAmount() / range; // 0..1 visible fraction

        if (sb.getOrientation() == Orientation.VERTICAL) {
            double minThumb = thumb.getMinHeight(); // from CSS -fx-min-height
            double trackLen = track.getHeight();
            double natural = trackLen * visible;
            // Only step in when the natural thumb would be below the CSS minimum; in
            // the normal case leave the skin's own layout (so dragging stays accurate).
            if (minThumb > 0 && trackLen > minThumb && natural < minThumb) {
                double travel = trackLen - minThumb;
                thumb.resizeRelocate(thumb.getLayoutX(), track.getLayoutY() + frac * travel,
                        thumb.getWidth(), minThumb);
            }
        } else {
            double minThumb = thumb.getMinWidth(); // from CSS -fx-min-width
            double trackLen = track.getWidth();
            double natural = trackLen * visible;
            if (minThumb > 0 && trackLen > minThumb && natural < minThumb) {
                double travel = trackLen - minThumb;
                thumb.resizeRelocate(track.getLayoutX() + frac * travel, thumb.getLayoutY(),
                        minThumb, thumb.getHeight());
            }
        }
    }
}
