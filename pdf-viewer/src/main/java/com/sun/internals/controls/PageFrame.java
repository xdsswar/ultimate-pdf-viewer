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

import javafx.css.PseudoClass;
import javafx.scene.layout.Region;

/**
 * A thin, mouse-transparent frame drawn around a page in the right content area
 * (single + continuous views). It is sized 1px larger than the page on every side
 * and positioned just outside it, so its CSS border appears as a ring around the
 * page without overlapping the rendered bitmap — keeping the page pixel-perfect.
 *
 * <p>Styled via the {@code .page-frame} style class; the {@code :selected}
 * pseudo-class marks the current page (e.g. a blue border).</p>
 *
 * @author XDSSWAR
 */
public final class PageFrame extends Region {

    /** Frame thickness, in logical pixels, added around the page on each side. */
    public static final double BORDER = 1.0;

    /** CSS pseudo-class applied to the frame of the current (selected) page. */
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    /**
     * Creates a page frame. It never intercepts mouse events, so text selection
     * and panning on the page underneath are unaffected.
     */
    public PageFrame() {
        getStyleClass().add("page-frame");
        setMouseTransparent(true);
    }

    /**
     * Toggles the {@code :selected} pseudo-class so the frame can be styled
     * differently for the current page.
     *
     * @param selected whether this frame wraps the current page
     */
    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
    }
}
