package com.sun.internals.flow.misc;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * @author XDSSWAR
 * Created on 06/23/2024
 */
public final class SelectionModel<T> {
    /**
     * Selection mode
     */
    public enum Mode {
        /**
         * Single mode.
         */
        SINGLE,

        /**
         * Multiple mode.
         */
        MULTIPLE
    }

    /**
     * The list of selected items.
     */
    private final ObservableList<T> selectedItems = FXCollections.observableArrayList();

    /**
     * The selection mode.
     */
    private Mode mode = Mode.SINGLE;

    /**
     * Returns the list of selected items.
     * @return the observable list of selected items
     */
    public ObservableList<T> getSelectedItems() {
        return selectedItems;
    }

    /**
     * Sets the selection mode.
     * @param mode the new selection mode
     */
    public void setSelectionMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Returns the current selection mode.
     * @return the current selection mode
     */
    public Mode getSelectionMode() {
        return mode;
    }

    /**
     * Selects an item.
     * @param item the item to be selected
     */
    public void select(T item) {
        if (mode == Mode.SINGLE) {
            selectedItems.clear();
            selectedItems.add(item);
        } else {
            if (!selectedItems.contains(item)) {
                selectedItems.add(item);
            }
        }
    }

    /**
     * Selects multiple items.
     * @param items the list of items to select
     */
    public void select(List<T> items) {
        selectedItems.clear();
        for (T item : items) {
            if (!isSelected(item)) {
                selectedItems.add(item);
            }
        }
    }


    /**
     * Unselects an item.
     * @param item the item to be unselected
     */
    public void unselect(T item) {
        selectedItems.remove(item);
    }

    /**
     * Clears the selection.
     */
    public void clearSelection() {
        selectedItems.clear();
    }

    /**
     * Checks if an item is selected.
     * @param item the item to check
     * @return true if the item is selected, false otherwise
     */
    public boolean isSelected(T item) {
        return selectedItems.contains(item);
    }

}
