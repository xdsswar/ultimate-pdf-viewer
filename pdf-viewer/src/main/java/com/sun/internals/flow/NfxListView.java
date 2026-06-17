package com.sun.internals.flow;


import com.sun.internals.flow.event.NfxEditEvent;
import com.sun.internals.flow.internals.BaseListView;
import com.sun.internals.flow.misc.SelectionModel;
import com.sun.internals.flow.skin.NfxListViewSkin;
import javafx.beans.DefaultProperty;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.util.Callback;

import java.net.URL;


/**
 * @author XDSSWAR
 * Created on 06/28/2024
 */
@DefaultProperty("items")
public final class NfxListView<T> extends Control {
    /**
     * The style sheet for the BaseListView.
     */
    private static final String STYLE_SHEET = load("/xss/it/ultimate/pdf/viewer/css/nfx-list-view.css")
            .toExternalForm();

    /**
     * The style class for the BaseListView.
     */
    private static final String STYLE_CLASS = "nfx-list-view";

    /**
     * The delegate BaseListView.
     */
    private final BaseListView<T> delegate;

    /**
     * Constructs an NfxListView.
     * Initializes the delegate BaseListView.
     */
    public NfxListView() {
        delegate = new BaseListView<>(this);
        delegate.getStyleClass().add(STYLE_CLASS);
    }

    /**
     * Retrieves the current count of elements in the list.
     *
     * @return the number of elements in the list
     */
    public int getCount() {
        return countProperty().get();
    }

    /**
     * Returns the read-only integer property representing the count of elements in the list.
     *
     * @return a {@link ReadOnlyIntegerProperty} containing the count value
     */
    public ReadOnlyIntegerProperty countProperty() {
        return delegate.countProperty();
    }

    /**
     * Returns the items property.
     * @return the object property for the items
     */
    public ObjectProperty<ObservableList<T>> itemsProperty() {
        return delegate.itemsProperty();
    }

    /**
     * Returns the list of items.
     * @return the observable list of items
     */
    public ObservableList<T> getItems() {
        return delegate.getItems();
    }

    /**
     * Sets the list of items.
     * @param items the new list of items
     */
    public void setItems(ObservableList<T> items) {
        delegate.setItems(items == null ? FXCollections.observableArrayList() : items);
    }


    /**
     * Returns the cell factory.
     * @return the callback used to create cells
     */
    public Callback<NfxListView<T>, NfxCell<T>> getCellFactory() {
        return cellFactoryProperty().get();
    }

    /**
     * Returns the cell factory property.
     * @return the object property for the cell factory
     */
    public ObjectProperty<Callback<NfxListView<T>, NfxCell<T>>> cellFactoryProperty() {
        return delegate.cellFactoryProperty();
    }

    /**
     * Sets the cell factory.
     * @param cellFactory the new cell factory callback
     */
    public void setCellFactory(Callback<NfxListView<T>, NfxCell<T>> cellFactory) {
        delegate.setCellFactory(cellFactory);
    }


    /**
     * Returns the selection mode.
     * @return the selection mode
     */
    public SelectionModel.Mode getSelectionMode() {
        return delegate.getSelectionMode();
    }

    /**
     * Returns the selection mode property.
     * @return the object property for the selection mode
     */
    public ObjectProperty<SelectionModel.Mode> selectionModeProperty() {
        return delegate.selectionModeProperty();
    }

    /**
     * Sets the selection mode.
     * @param selectionMode the new selection mode
     */
    public void setSelectionMode(SelectionModel.Mode selectionMode) {
        delegate.setSelectionMode(selectionMode);
    }



    /**
     * Returns whether unselect on click is allowed.
     * @return true if unselect on click is allowed, false otherwise
     */
    public boolean isAllowUnselectOnClick() {
        return delegate.isAllowUnselectOnClick();
    }

    /**
     * Returns allow un-select on click property.
     * @return the boolean property for allow un-select on click
     */
    public BooleanProperty allowUnselectOnClickProperty() {
        return delegate.allowUnselectOnClickProperty();
    }

    /**
     * Sets whether unselect on click is allowed.
     * @param allowUnselectOnClick true to allow un-select on click, false otherwise
     */
    public void setAllowUnselectOnClick(boolean allowUnselectOnClick) {
        delegate.setAllowUnselectOnClick(allowUnselectOnClick);
    }


    /**
     * Returns whether the list view is editable.
     * @return true if the list view is editable, false otherwise
     */
    public boolean isEditable() {
        return delegate.isEditable();
    }

    /**
     * Returns the editable property.
     * @return the boolean property for editable
     */
    public BooleanProperty editableProperty() {
        return delegate.editableProperty();
    }

    /**
     * Sets whether the list view is editable.
     * @param editable true to make the list view editable, false otherwise
     */
    public void setEditable(boolean editable) {
        delegate.setEditable(editable);
    }

    /**
     * Returns the index of the item currently being edited.
     * @return the editing index
     */
    public int getEditingIndex() {
        return delegate.getEditingIndex();
    }

    /**
     * Returns the editing index property.
     * @return the integer property for the editing index
     */
    public IntegerProperty editingIndexProperty() {
        return delegate.editingIndexProperty();
    }

    /**
     * Sets the index of the item currently being edited.
     * @param editingIndex the new editing index
     */
    public void setEditingIndex(int editingIndex) {
        delegate.setEditingIndex(editingIndex);
    }

    /**
     * Returns the event handler for the edit commit event.
     * @return the event handler for the edit commit event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditCommit() {
        return delegate.getOnEditCommit();
    }

    /**
     * Returns the property for the edit commit event handler.
     * @return the object property for the edit commit event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCommitProperty() {
        return delegate.onEditCommitProperty();
    }

    /**
     * Sets the event handler for the edit commit event.
     * @param onEditCommit the event handler for the edit commit event
     */
    public void setOnEditCommit(EventHandler<NfxEditEvent<?>> onEditCommit) {
        delegate.setOnEditCommit(onEditCommit);
    }

    /**
     * Returns the event handler for the edit start event.
     * @return the event handler for the edit start event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditStart() {
        return delegate.getOnEditStart();
    }

    /**
     * Returns the property for the edit start event handler.
     * @return the object property for the edit start event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditStartProperty() {
        return delegate.onEditStartProperty();
    }

    /**
     * Sets the event handler for the edit start event.
     * @param onEditStart the event handler for the edit start event
     */
    public void setOnEditStart(EventHandler<NfxEditEvent<?>> onEditStart) {
        delegate.setOnEditStart(onEditStart);
    }

    /**
     * Returns the event handler for the edit cancel event.
     * @return the event handler for the edit cancel event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditCancel() {
        return delegate.getOnEditCancel();
    }

    /**
     * Returns the property for the edit cancel event handler.
     * @return the object property for the edit cancel event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCancelProperty() {
        return delegate.onEditCancelProperty();
    }

    /**
     * Sets the event handler for the edit cancel event.
     * @param onEditCancel the event handler for the edit cancel event
     */
    public void setOnEditCancel(EventHandler<NfxEditEvent<?>> onEditCancel) {
        delegate.setOnEditCancel(onEditCancel);
    }


    /**
     * Returns the cell height.
     * @return the cell height
     */
    public double getCellHeight() {
        return delegate.getCellHeight();
    }

    /**
     * Sets the cell height.
     * @param value the new cell height
     */
    public void setCellHeight(double value) {
        delegate.setCellHeight(value);
    }

    /**
     * Returns the cell height property.
     * @return the styleable double property for cell height
     */
    public StyleableDoubleProperty cellHeightProperty() {
        return delegate.cellHeightProperty();
    }


    /**
     * Returns the minimum cell width.
     * @return the minimum cell width
     */
    public double getMinCellWidthBreakPoint() {
        return delegate.getMinCellWidthBreakPoint();
    }

    /**
     * Sets the minimum cell width.
     * @param value the new minimum cell width
     */
    public void setMinCellWidthBreakPoint(double value) {
        delegate.setMinCellWidthBreakPoint(value);
    }

    /**
     * Returns the minimum cell width property.
     * @return the styleable double property for minimum cell width
     */
    public StyleableDoubleProperty minCellWidthBreakPointProperty() {
        return delegate.minCellWidthBreakPointProperty();
    }


    /**
     * Returns the maximum cells per row.
     * @return the maximum cells per row
     */
    public int getMaxCellsPerRow() {
        return delegate.getMaxCellsPerRow();
    }

    /**
     * Returns the maximum cells per row property.
     * @return the styleable integer property for maximum cells per row
     */
    public StyleableIntegerProperty maxCellsPerRowProperty() {
        return delegate.maxCellsPerRowProperty();
    }

    /**
     * Sets the maximum cells per row.
     * @param maxCellsPerRow the new maximum cells per row
     */
    public void setMaxCellsPerRow(int maxCellsPerRow) {
        delegate.setMaxCellsPerRow(maxCellsPerRow);
    }

    /**
     * Returns the right gap.
     * @return the right gap
     */
    public double getRightGap() {
        return delegate.getRightGap();
    }

    /**
     * Returns the right gap property.
     * @return the styleable double property for right gap
     */
    public StyleableDoubleProperty rightGapProperty() {
        return delegate.rightGapProperty();
    }

    /**
     * Sets the right gap.
     * @param rightGap the new right gap
     */
    public void setRightGap(double rightGap) {
        delegate.setRightGap(rightGap);
    }

    /**
     * Returns the left gap.
     * @return the left gap
     */
    public double getLeftGap() {
        return delegate.getLeftGap();
    }

    /**
     * Returns the left gap property.
     * @return the styleable double property for left gap
     */
    public StyleableDoubleProperty leftGapProperty() {
        return delegate.leftGapProperty();
    }

    /**
     * Sets the left gap.
     * @param leftGap the new left gap
     */
    public void setLeftGap(double leftGap) {
        delegate.setLeftGap(leftGap);
    }

    /**
     * Scrolls to the cell containing the specified item.
     * @param item the item to scroll to
     */
    public void scrollToItem(T item) {
        delegate.scrollToItem(item);
    }

    /**
     * Scrolls to the cell containing the specified item only if that cell is not
     * already fully visible; if it is fully visible the list is left untouched.
     * @param item the item to reveal
     */
    public void scrollToItemIfNotVisible(T item) {
        delegate.scrollToItemIfNotVisible(item);
    }

    /**
     * Returns the selection model for the list view.
     * @return the selection model
     */
    public SelectionModel<T> getSelectionModel() {
        return delegate.getSelectionModel();
    }

    /**
     * Property for managing the placeholder node.
     */
    private ObjectProperty<Node> placeHolder;

    /**
     * Retrieves the current placeholder node.
     *
     * @return the current placeholder Node
     */
    public Node getPlaceHolder() {
        return placeHolderProperty().get();
    }

    /**
     * Returns the ObjectProperty containing the placeholder node.
     *
     * @return the ObjectProperty for the placeholder node
     */
    public ObjectProperty<Node> placeHolderProperty() {
        if (placeHolder == null){
            placeHolder = new SimpleObjectProperty<>(this,"placeHolder", defaultPlaceholder());
        }
        return placeHolder;
    }

    /**
     * Sets the placeholder node.
     *
     * @param placeHolder the Node to set as the placeholder
     */
    public void setPlaceHolder(Node placeHolder) {
        placeHolderProperty().set(placeHolder);
    }


    /**
     * Refreshes the list view.
     * Temporarily sets the items to an empty list and then resets them to trigger a refresh.
     */
    public void refresh() {
        delegate.refresh();
    }

    /**
     * Returns the first item in the virtual flow.
     * @return the first item, or null if the list is empty
     */
    public T getFirstItem() {
        return delegate.getFirstItem();
    }

    /**
     * Returns the last item in the virtual flow.
     * @return the last item, or null if the list is empty
     */
    public T getLastItem() {
        return delegate.getLastItem();
    }

    /**
     * Creates a default placeholder node displaying a message when no items are available.
     *
     * @return a Label styled as a placeholder message
     */
    private Node defaultPlaceholder() {
        Label label = new Label("No items available");
        label.setStyle("-fx-font-size: 16; -fx-text-fill: gray;");
        return label;
    }

    @Override
    public String getUserAgentStylesheet() {
        return STYLE_SHEET;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new NfxListViewSkin<>(this, delegate);
    }

    /**
     * This method loads a URL for a given location.
     *
     * @param location The location of the resource to load.
     * @return A URL object representing the resource's location.
     */
    public static URL load(final String location) {
        return NfxListView.class.getResource(location);
    }
}
