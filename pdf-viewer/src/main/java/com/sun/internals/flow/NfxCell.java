package com.sun.internals.flow;

import com.sun.internals.flow.event.NfxEditEvent;
import javafx.beans.property.*;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.Objects;

/**
 * @author XDSSWAR
 * Created on 06/23/2024
 */
public class NfxCell<T> extends Region {
    /**
     * The style class for the cell.
     */
    private static final String STYLE_CLASS = "nfx-cell";

    /**
     * The item property.
     */
    private ObjectProperty<T> item;

    /**
     * The list view that contains the cell.
     */
    private final NfxListView<T> listView;

    /**
     * The pseudo-class for the selected state.
     */
    private static final PseudoClass PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("selected");

    /**
     * Flag indicating whether the index is being updated.
     */
    private volatile boolean updatingIndex = false;

    /**
     * Flag indicating whether the item was previously selected.
     */
    private volatile boolean wasSelected = false;

    /**
     * Constructs an NfxCell with the specified list view.
     * @param listView the list view that contains the cell
     */
    public NfxCell(NfxListView<T> listView) {
        super();
        this.listView = listView;

        initialize();
    }

    /**
     * Initializes the list view or virtual flow.
     */
    private void initialize() {
        getStyleClass().add(STYLE_CLASS);

        setOnMouseClicked(event -> {
            if (!getListView().getSelectionModel().getSelectedItems().contains(getItem())) {
                getListView().getSelectionModel().select(getItem());
            }
            else {
                if (getListView().isAllowUnselectOnClick()) {
                    if (event.isControlDown()) {
                        getListView().getSelectionModel().unselect(getItem());
                    }
                    else {
                        getListView().getSelectionModel().getSelectedItems().clear();
                        getListView().getSelectionModel().select(getItem());
                    }
                }
            }
        });

        selectedProperty().addListener((obs, o, selected) -> {
            if (selected){
                if (!getListView().getSelectionModel().getSelectedItems().contains(getItem())){
                    getListView().getSelectionModel().select(getItem());
                }
            }
        });

        listView.editingIndexProperty().addListener((observableValue, number, t1) -> {
            int index = t1.intValue();
            if (index == -1){
                setEditing(false);
                updatingIndex = false;
            }
        });
    }

    /**
     * The selected property.
     */
    private final BooleanProperty selected = new BooleanPropertyBase() {
        @Override
        public Object getBean() {
            return NfxCell.this;
        }

        @Override
        public String getName() {
            return "selected";
        }

        @Override
        protected void invalidated() {
            NfxCell.this.pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, isSelected());
        }
    };

    /**
     * The editing property.
     */
    private volatile BooleanProperty editing;

    /**
     * Returns whether the list view is in editing mode.
     * @return true if the list view is in editing mode, false otherwise
     */
    public final boolean isEditing() {
        return editingProperty().get();
    }

    /**
     * Returns the editing property.
     * @return the boolean property for editing
     */
    private BooleanProperty editingProperty() {
        if (editing == null) {
            editing = new SimpleBooleanProperty(this, "editing", false);
        }
        return editing;
    }

    /**
     * Returns the read-only editing property.
     * @return the read-only boolean property for editing
     */
    private ReadOnlyBooleanProperty readOnlyEditingProperty() {
        return editingProperty();
    }

    /**
     * Sets whether the list view is in editing mode.
     * @param editing true to enable editing mode, false otherwise
     */
    public final void setEditing(boolean editing) {
        editingProperty().set(editing);
    }


    /**
     * Sets the selected state of the cell.
     * @param selected the new selected state
     */
    public final void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    /**
     * Returns whether the cell is selected.
     * @return true if the cell is selected, false otherwise
     */
    public final boolean isSelected() {
        return selected.get();
    }

    /**
     * Returns the selected property.
     * @return the read-only boolean property for the selected state
     */
    public final ReadOnlyBooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * Returns the item property.
     * @return the object property for the item
     */
    private ObjectProperty<T> itemProperty() {
        if (item == null) {
            item = new SimpleObjectProperty<>(this, "item");
        }
        return item;
    }

    /**
     * Returns the item in the cell.
     * @return the item
     */
    public final T getItem() {
        return itemProperty().get();
    }

    /**
     * Sets the item in the cell.
     * @param item the item to set
     */
    final void setItem(T item) {
        itemProperty().set(item);
    }

    /**
     * Updates the cell with the given item.
     * @param item the item to update the cell with
     */
    public void update(T item) {
        setItem(item);
        setSelected(isSelectable(item));
        setEditing(getListView().getEditingIndex()==getIndex(item));
    }

    /**
     * Starts the edit process for the cell.
     * Fires an edit start event if the list view is editable.
     */
    public final void startEdit() {
        if (getListView().isEditable() && !updatingIndex) {
            updatingIndex = true;
            setEditing(true);
            getListView().setEditingIndex(getIndex(getItem()));
            getListView().fireEvent(
                    new NfxEditEvent<>(
                            getListView(),
                            NfxEditEvent.NFX_EDIT_START,
                            null,
                            getIndex(getItem())
                    )
            );
            requestFocus();
        }
    }

    /**
     * Commits the edit for the specified item.
     * Fires an edit commit event if the cell is in editing mode.
     * @param item the item being edited
     */
    public final void commitEdit(T item) {
        int index = getListView().getEditingIndex();
        wasSelected = getListView().getSelectionModel().isSelected(getItem());
        if (isEditing() && updatingIndex && index == getIndex(getItem())) {
            getListView().fireEvent(
                    new NfxEditEvent<>(
                            getListView(),
                            NfxEditEvent.NFX_EDIT_COMMIT,
                            item,
                            index
                    )
            );
            setEditing(false);

            getListView().getSelectionModel().getSelectedItems().remove(getItem());

            getListView().getItems().set(index, item);

            if (wasSelected) {
                getListView().getSelectionModel().getSelectedItems().add(item);
            }
            update(item);
            getListView().setEditingIndex(-1);
            wasSelected = false;
        }
    }

    /**
     * Cancels the edit process.
     * Fires an edit cancel event.
     */
    public final void cancelEdit() {
        int index = getIndex(getItem());
        if (getListView().getEditingIndex() != -1 &&  getListView().getEditingIndex() == index && isEditing()){
            setEditing(false);
            updatingIndex = false;
            getListView().setEditingIndex(-1);
            getListView().fireEvent(
                    new NfxEditEvent<>(
                            getListView(),
                            NfxEditEvent.NFX_EDIT_CANCEL,
                            null,
                            index
                    )
            );
        }
    }


    /**
     * Returns the list view that contains the cell.
     * @return the list view
     */
    public final NfxListView<T> getListView() {
        return listView;
    }

    /**
     * Returns the index of the specified item in the list view.
     * @param item the item to find the index of
     * @return the index of the item, or -1 if the list is empty or the item is not found
     */
    public final int getIndex(T item){
        return getListView().getItems().isEmpty() ? -1 : getListView().getItems().indexOf(item);
    }

    /**
     * Sets the text for the cell.
     * @param text the text to set
     */
    public final void setText(String text) {
        if (text != null) {
            Label label = new Label(text);
            label.getStyleClass().add("nfx-factory-label");
            setGraphics(label);
        }
    }

    /**
     * Sets the graphics for the cell.
     * @param graphics the graphics node to set
     */
    public final void setGraphics(Node graphics) {
        if (graphics != null) {
            getChildren().clear();
            getChildren().add(graphics);
        }
    }

    /**
     * Determines if the specified item is selectable.
     * @param item the item to check
     * @return true if the item is selectable, false otherwise
     */
    protected final boolean isSelectable(T item) {
        return getListView().getSelectionModel().getSelectedItems().stream()
                .anyMatch(selected -> Objects.equals(selected, item) || Objects.equals(getIndex(selected), getIndex(item)))
                || getListView().getSelectionModel().isSelected(item);
    }

    /**
     * Lays out the children nodes within the cell.
     */
    @Override
    protected final void layoutChildren() {
        for (Node child : getChildren()) {
            child.resizeRelocate(0,0, getWidth(), getHeight());
        }
    }
}
