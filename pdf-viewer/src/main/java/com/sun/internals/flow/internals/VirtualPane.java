package com.sun.internals.flow.internals;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * @author XDSSWAR
 * Created on 06/22/2024
 */
public final class VirtualPane extends Region {
    /**
     * Returns the list of children nodes.
     * @return the observable list of children nodes
     */
    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /**
     * Lays out the children nodes within the control.
     */
    @Override
    protected void layoutChildren() {}
}
