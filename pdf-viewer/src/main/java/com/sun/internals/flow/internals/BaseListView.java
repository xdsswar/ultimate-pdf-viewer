package com.sun.internals.flow.internals;


import com.sun.internals.flow.NfxCell;
import com.sun.internals.flow.NfxListView;
import com.sun.internals.flow.event.NfxEditEvent;
import com.sun.internals.flow.misc.SelectionModel;
import javafx.animation.*;
import javafx.beans.DefaultProperty;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.css.converter.SizeConverter;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.Duration;

import java.util.*;

/**
 * @author XDSSWAR
 * Created on 06/23/2024
 */
@DefaultProperty("items")
public final class BaseListView<T> extends ScrollPane {
    /**
     * The style class for the content.
     */
    private static final String CONTENT_STYLE_CLASS = "nfx-content";

    /**
     * The selection model for the list view.
     */
    private final SelectionModel<T> selectionModel;

    /**
     * The list of visible cells.
     */
    private final List<Node> visibleCells;

    /**
     * The map of items to their corresponding nodes.
     */
    private final Map<T, Node> itemToNodeMap;

    /**
     * The content pane.
     */
    private final VirtualPane contentPane;

    /**
     * The current number of cells per row.
     */
    private final IntegerProperty currentCellsPerRow;

    /**
     * Listener for changes in the selection model.
     */
    private final ListChangeListener<T> selectionModelChangeListener;

    /**
     * Listener for changes in the list of items.
     */
    private final ListChangeListener<T> listChangeListener;

    /**
     * Event handler property for the edit start event.
     */
    private ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditStart;

    /**
     * Event handler property for the edit commit event.
     */
    private ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCommit;

    /**
     * Event handler property for the edit cancel event.
     */
    private ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCancel;

    /**
     * Event handler for key pressed events.
     * Sets the selection mode to multiple if the CONTROL key is pressed.
     */
    private final EventHandler<KeyEvent> keyPressedEvent = keyEvent -> {
        if (keyEvent.getCode() == KeyCode.CONTROL) {
            setSelectionMode(SelectionModel.Mode.MULTIPLE);
        }
    };

    /**
     * Event handler for key released events.
     * Sets the selection mode to single if the CONTROL key is released.
     */
    private final EventHandler<KeyEvent> keyReleasedEvent = keyEvent -> {
        if (keyEvent.getCode() == KeyCode.CONTROL) {
            setSelectionMode(SelectionModel.Mode.SINGLE);
        }
    };

    /**
     * Parent ListView Container
     */
    private final NfxListView<T> parent;

    /**
     * Resizing flag
     */
    private boolean resizing = false;

    /**
     * Constructs an BaseListView.
     */
    public BaseListView(NfxListView<T> parent) {
        super();
        this.parent = parent;
        this.selectionModel = new SelectionModel<>();
        this.visibleCells = new ArrayList<>();
        this.itemToNodeMap = new HashMap<>();
        this.contentPane = new VirtualPane();
        this.currentCellsPerRow = new SimpleIntegerProperty(1);

        selectionModelChangeListener = c-> {
            while (c.next()){
                if (c.wasAdded()){
                    for (T t : c.getAddedSubList()) {
                        Node node = getCellNode(t);
                        if (node instanceof NfxCell<?> cell) {
                            cell.setSelected(true);
                        }
                    }
                }
                else if (c.wasRemoved()){
                    for (T t : c.getRemoved()) {
                        Node node = getCellNode(t);
                        //We don't want to unselect a removed cell, no point on doing it , right? :)
                        if (getItems().contains(t)  && node instanceof NfxCell<?> cell) {
                            cell.setSelected(false);
                        }
                    }
                }
            }
        };

        this.listChangeListener = c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (T item : c.getRemoved()) {
                        /*
                         * Remove if its selected
                         */
                        if (getSelectionModel().isSelected(item)){
                            getSelectionModel().getSelectedItems().remove(item);
                        }
                    }
                }
                setCount(getItems().size());
            }
            onUpdate();
        };

        initialize();
    }

    /**
     * Initializes the BaseListView.
     */
    private void initialize(){
        contentPane.getStyleClass().add(CONTENT_STYLE_CLASS);
        setContent(contentPane);
        setFitToWidth(true);
        widthProperty().addListener((obs, oldVal, newVal) -> handleResize());
        heightProperty().addListener((obs, oldVal, newVal) -> handleResize());
        vvalueProperty().addListener((obs, oldVal, newVal) -> updateCells());

        maxCellsPerRowProperty().addListener((obs, oldVal, newVal) -> handleResize());
        minCellWidthBreakPointProperty().addListener((obs, oldVal, newVal) -> handleResize());
        rightGapProperty().addListener((obs, oldVal, newVal) -> handleResize());
        currentCellsPerRow.addListener((obs, oldVal, newVal) -> updateCells());

        handleSelectionMode(getSelectionMode());
        selectionModeProperty().addListener((obs, o, mode) -> handleSelectionMode(mode));
        getSelectionModel().getSelectedItems().addListener(selectionModelChangeListener);


        itemsProperty().addListener((obs, o, n) -> {
            if (o != null) {
                o.removeListener(listChangeListener);
            }

            getItems().addListener(listChangeListener);
            /*
             * Clear selected items
             */
            getSelectionModel().clearSelection();
            setCount(n.size());
            onUpdate();
        });

        getItems().addListener(listChangeListener);

        cellFactoryProperty().addListener(obs -> onUpdate());

        cellHeightProperty().addListener(obs -> onUpdate());

        if (getScene() != null) {
            initializeKeyEvents();
        }

        sceneProperty().addListener(obs -> {
            if (getScene() != null) {
                initializeKeyEvents();
            }
        });

        onUpdate();

        if (getItems().isEmpty()){
            PauseTransition pt =new PauseTransition(Duration.millis(60));
            pt.setOnFinished(event -> {
                updateCells();
            });
            pt.play();
        }
    }


    /**
     * Property representing the count of items.
     */
    private IntegerProperty count;

    /**
     * Retrieves the current count value.
     *
     * @return the count value
     */
    public int getCount() {
        return countProperty().get();
    }

    /**
     * Returns the IntegerProperty containing the count.
     *
     * @return the IntegerProperty for the count
     */
    public IntegerProperty countProperty() {
        if (count == null) {
            count = new SimpleIntegerProperty(this, "count", 0);
        }
        return count;
    }

    /**
     * Sets the count value.
     *
     * @param count the new count value to set
     */
    private void setCount(int count) {
        countProperty().set(count);
    }


    /**
     * The items' property.
     */
    private ObjectProperty<ObservableList<T>> items;

    /**
     * Returns the items property.
     * @return the object property for the items
     */
    public ObjectProperty<ObservableList<T>> itemsProperty() {
        if (items == null) {
            items = new SimpleObjectProperty<>(this, "items", FXCollections.observableArrayList());
        }
        return items;
    }

    /**
     * Returns the list of items.
     * @return the observable list of items
     */
    public ObservableList<T> getItems() {
        return itemsProperty().get();
    }

    /**
     * Sets the list of items.
     * @param items the new list of items
     */
    public void setItems(ObservableList<T> items) {
        itemsProperty().set(items);
    }

    /**
     * The cell factory property.
     */
    private ObjectProperty<Callback<NfxListView<T>, NfxCell<T>>> cellFactory;

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
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<>(this, "cellFactory", defaultCellFactory());
        }
        return cellFactory;
    }

    /**
     * Sets the cell factory.
     * @param cellFactory the new cell factory callback
     */
    public void setCellFactory(Callback<NfxListView<T>, NfxCell<T>> cellFactory) {
        cellFactoryProperty().set(cellFactory);
    }

    /**
     * The selection mode property.
     */
    private ObjectProperty<SelectionModel.Mode> selectionMode;

    /**
     * Returns the selection mode.
     * @return the selection mode
     */
    public SelectionModel.Mode getSelectionMode() {
        return selectionModeProperty().get();
    }

    /**
     * Returns the selection mode property.
     * @return the object property for the selection mode
     */
    public ObjectProperty<SelectionModel.Mode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new SimpleObjectProperty<>(this, "selectionMode", SelectionModel.Mode.SINGLE);
        }
        return selectionMode;
    }

    /**
     * Sets the selection mode.
     * @param selectionMode the new selection mode
     */
    public void setSelectionMode(SelectionModel.Mode selectionMode) {
        selectionModeProperty().set(selectionMode);
    }

    /**
     * Allow un-select on click property.
     */
    private BooleanProperty allowUnselectOnClick;

    /**
     * Returns whether unselect on click is allowed.
     * @return true if unselect on click is allowed, false otherwise
     */
    public boolean isAllowUnselectOnClick() {
        return allowUnselectOnClickProperty().get();
    }

    /**
     * Returns allow un-select on click property.
     * @return the boolean property for allow un-select on click
     */
    public BooleanProperty allowUnselectOnClickProperty() {
        if (allowUnselectOnClick == null) {
            allowUnselectOnClick = new SimpleBooleanProperty(this, "allowUnselectOnClick", false);
        }
        return allowUnselectOnClick;
    }

    /**
     * Sets whether unselect on click is allowed.
     * @param allowUnselectOnClick true to allow un-select on click, false otherwise
     */
    public void setAllowUnselectOnClick(boolean allowUnselectOnClick) {
        allowUnselectOnClickProperty().set(allowUnselectOnClick);
    }

    /**
     * The editable property.
     */
    private BooleanProperty editable;

    /**
     * Returns whether the list view is editable.
     * @return true if the list view is editable, false otherwise
     */
    public boolean isEditable() {
        return editableProperty().get();
    }

    /**
     * Returns the editable property.
     * @return the boolean property for editable
     */
    public BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", false);
        }
        return editable;
    }

    /**
     * Sets whether the list view is editable.
     * @param editable true to make the list view editable, false otherwise
     */
    public void setEditable(boolean editable) {
        editableProperty().set(editable);
    }

    /**
     * The editing index property.
     */
    private IntegerProperty editingIndex;

    /**
     * Returns the index of the item currently being edited.
     * @return the editing index
     */
    public int getEditingIndex() {
        return editingIndexProperty().get();
    }

    /**
     * Returns the editing index property.
     * @return the integer property for the editing index
     */
    public IntegerProperty editingIndexProperty() {
        if (editingIndex == null){
            editingIndex = new SimpleIntegerProperty(this, "editingIndex", -1);
        }
        return editingIndex;
    }

    /**
     * Sets the index of the item currently being edited.
     * @param editingIndex the new editing index
     */
    public void setEditingIndex(int editingIndex) {
        editingIndexProperty().set(editingIndex);
    }

    /**
     * Returns the event handler for the edit commit event.
     * @return the event handler for the edit commit event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditCommit() {
        return onEditCommitProperty().get();
    }

    /**
     * Returns the property for the edit commit event handler.
     * @return the object property for the edit commit event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new ObjectPropertyBase<>() {
                @Override
                public Object getBean() {
                    return BaseListView.this;
                }

                @Override
                public String getName() {
                    return "onEditCommit";
                }

                @Override
                protected void invalidated() {
                    BaseListView.this.setEventHandler(NfxEditEvent.NFX_EDIT_COMMIT, this.get());
                }
            };
        }
        return onEditCommit;
    }

    /**
     * Sets the event handler for the edit commit event.
     * @param onEditCommit the event handler for the edit commit event
     */
    public void setOnEditCommit(EventHandler<NfxEditEvent<?>> onEditCommit) {
        onEditCommitProperty().set(onEditCommit);
    }

    /**
     * Returns the event handler for the edit start event.
     * @return the event handler for the edit start event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditStart() {
        return onEditStartProperty().get();
    }

    /**
     * Returns the property for the edit start event handler.
     * @return the object property for the edit start event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new ObjectPropertyBase<>() {
                @Override
                public Object getBean() {
                    return BaseListView.this;
                }

                @Override
                public String getName() {
                    return "onEditStart";
                }

                @Override
                protected void invalidated() {
                    BaseListView.this.setEventHandler(NfxEditEvent.NFX_EDIT_START, this.get());
                }
            };
        }
        return onEditStart;
    }

    /**
     * Sets the event handler for the edit start event.
     * @param onEditStart the event handler for the edit start event
     */
    public void setOnEditStart(EventHandler<NfxEditEvent<?>> onEditStart) {
        onEditStartProperty().set(onEditStart);
    }

    /**
     * Returns the event handler for the edit cancel event.
     * @return the event handler for the edit cancel event
     */
    public EventHandler<NfxEditEvent<?>> getOnEditCancel() {
        return onEditCancelProperty().get();
    }

    /**
     * Returns the property for the edit cancel event handler.
     * @return the object property for the edit cancel event handler
     */
    public ObjectProperty<EventHandler<NfxEditEvent<?>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new ObjectPropertyBase<>() {
                @Override
                public Object getBean() {
                    return BaseListView.this;
                }

                @Override
                public String getName() {
                    return "onEditCancel";
                }

                @Override
                protected void invalidated() {
                    BaseListView.this.setEventHandler(NfxEditEvent.NFX_EDIT_CANCEL, this.get());
                }
            };
        }
        return onEditCancel;
    }

    /**
     * Sets the event handler for the edit cancel event.
     * @param onEditCancel the event handler for the edit cancel event
     */
    public void setOnEditCancel(EventHandler<NfxEditEvent<?>> onEditCancel) {
        onEditCancelProperty().set(onEditCancel);
    }

    /*
     * =================================== STYLEABLES ==================================================================
     */

    /**
     * The cell height property.
     */
    private StyleableDoubleProperty cellHeight;

    /**
     * Returns the cell height.
     * @return the cell height
     */
    public double getCellHeight() {
        return cellHeightProperty().get();
    }

    /**
     * Sets the cell height.
     * @param value the new cell height
     */
    public void setCellHeight(double value) {
        cellHeightProperty().set(value);
    }

    /**
     * Returns the cell height property.
     * @return the styleable double property for cell height
     */
    public StyleableDoubleProperty cellHeightProperty() {
        if (cellHeight == null) {
            cellHeight = new SimpleStyleableDoubleProperty(
                    StyleableProperties.CELL_HEIGHT,
                    BaseListView.this,
                    "cellHeight",
                    50.0
            );
        }
        return cellHeight;
    }

    /**
     * The minimum cell width property.
     */
    private StyleableDoubleProperty minCellWidthBreakPoint;

    /**
     * Returns the minimum cell width.
     * @return the minimum cell width
     */
    public double getMinCellWidthBreakPoint() {
        return minCellWidthBreakPointProperty().get();
    }

    /**
     * Sets the minimum cell width.
     * @param value the new minimum cell width
     */
    public void setMinCellWidthBreakPoint(double value) {
        minCellWidthBreakPointProperty().set(value);
    }

    /**
     * Returns the minimum cell width property.
     * @return the styleable double property for minimum cell width
     */
    public StyleableDoubleProperty minCellWidthBreakPointProperty() {
        if (minCellWidthBreakPoint == null) {
            minCellWidthBreakPoint = new SimpleStyleableDoubleProperty(
                    StyleableProperties.MIN_CELL_WIDTH_BREAK_POINT,
                    BaseListView.this,
                    "minCellWidthBreakPoint",
                    100.0
            );
        }
        return minCellWidthBreakPoint;
    }

    /**
     * The maximum cells per row property.
     */
    private StyleableIntegerProperty maxCellsPerRow;

    /**
     * Returns the maximum cells per row.
     * @return the maximum cells per row
     */
    public int getMaxCellsPerRow() {
        return maxCellsPerRowProperty().get();
    }

    /**
     * Returns the maximum cells per row property.
     * @return the styleable integer property for maximum cells per row
     */
    public StyleableIntegerProperty maxCellsPerRowProperty() {
        if (maxCellsPerRow == null) {
            maxCellsPerRow = new SimpleStyleableIntegerProperty(
                    StyleableProperties.MAX_CELLS_PER_ROW,
                    BaseListView.this,
                    "maxCellsPerRow",
                    12
            );
        }
        return maxCellsPerRow;
    }

    /**
     * Sets the maximum cells per row.
     * @param maxCellsPerRow the new maximum cells per row
     */
    public void setMaxCellsPerRow(int maxCellsPerRow) {
        maxCellsPerRowProperty().set(maxCellsPerRow);
    }


    /**
     * The right gap property.
     */
    private StyleableDoubleProperty rightGap;

    /**
     * Returns the right gap.
     * @return the right gap
     */
    public double getRightGap() {
        return rightGapProperty().get();
    }

    /**
     * Returns the right gap property.
     * @return the styleable double property for right gap
     */
    public StyleableDoubleProperty rightGapProperty() {
        if (rightGap == null) {
            rightGap = new SimpleStyleableDoubleProperty(
                    StyleableProperties.RIGHT_GAP,
                    BaseListView.this,
                    "rightGap",
                    15.0
            );
        }
        return rightGap;
    }

    /**
     * Sets the right gap.
     * @param rightGap the new right gap
     */
    public void setRightGap(double rightGap) {
        rightGapProperty().set(rightGap);
    }

    /**
     * The left gap property.
     */
    private StyleableDoubleProperty leftGap;

    /**
     * Returns the left gap.
     * @return the left gap
     */
    public double getLeftGap() {
        return leftGapProperty().get();
    }

    /**
     * Returns the left gap property.
     * @return the styleable double property for left gap
     */
    public StyleableDoubleProperty leftGapProperty() {
        if (leftGap == null) {
            leftGap = new SimpleStyleableDoubleProperty(
                    StyleableProperties.LEFT_GAP,
                    BaseListView.this,
                    "leftGap",
                    5.0
            );
        }
        return leftGap;
    }

    /**
     * Sets the left gap.
     * @param leftGap the new left gap
     */
    public void setLeftGap(double leftGap) {
        leftGapProperty().set(leftGap);
    }



    /*
     * ============================================= UTILS =============================================================
     */


    /**
     * Scrolls to the cell containing the specified item.
     * @param item the item to scroll to
     */
    public void scrollToItem(T item) {
        scrollToItemInternal(item);
    }

    /**
     * Returns the selection model for the list view.
     * @return the selection model
     */
    public SelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    /**
     * Refreshes the list view.
     * Temporarily sets the items to an empty list and then resets them to trigger a refresh.
     */
    public void refresh() {
        getSelectionModel().clearSelection();
        onUpdate();
    }


    /*
     * ========================================= INTERNALS =============================================================
     */

    /**
     * Handles changes to the selection mode.
     * @param selectionMode the new selection mode
     */
    private void handleSelectionMode(SelectionModel.Mode selectionMode) {
        Objects.requireNonNull(selectionMode);
        selectionModel.setSelectionMode(selectionMode);
    }

    /**
     * Initializes key event filters to manage selection mode changes.
     */
    private void initializeKeyEvents() {
        getScene().addEventFilter(KeyEvent.KEY_PRESSED, new WeakEventHandler<>(keyPressedEvent));
        getScene().addEventFilter(KeyEvent.KEY_RELEASED, new WeakEventHandler<>(keyReleasedEvent));
    }

    /**
     * Handles the resizing logic of the virtual flow.
     */
    private void handleResize() {
        double availableWidth = getWidth() - getRightGap()- getLeftGap();
        int calculatedCellsPerRow = Math.min(maxCellsPerRow.get(), Math.max(1, (int) (availableWidth / minCellWidthBreakPoint.get())));
        if (currentCellsPerRow.get() != calculatedCellsPerRow) {
            currentCellsPerRow.set(calculatedCellsPerRow);
        }
        updateCells();
    }


    /**
     * Updates the cells in the virtual flow.
     */
    private void updateCells() {
        if (resizing) {
            return;
        }
        resizing = true;

        try {
            double availableWidth = getWidth() - getRightGap() - getLeftGap();
            int cellsPerRow = currentCellsPerRow.get();
            double cellWidth = availableWidth / cellsPerRow;
            double cellHeight = getCellHeight();

            int rowCount = (int) Math.ceil((double) getItems().size() / cellsPerRow);
            double contentHeight = rowCount * cellHeight;

            contentPane.setMinHeight(contentHeight);
            contentPane.setPrefHeight(contentHeight);

            int firstVisibleRow = (int) (getVvalue() * (contentHeight - getHeight()) / cellHeight);
            int visibleRowCount = (int) Math.ceil(getHeight() / cellHeight) + 1; //Extra row just in case, so don't fuck with it

            firstVisibleRow = Math.max(0, firstVisibleRow);
            int lastVisibleRow = Math.min(firstVisibleRow + visibleRowCount, rowCount);

            List<Node> newVisibleCells = new ArrayList<>();

            for (int row = firstVisibleRow; row < lastVisibleRow; row++) {
                for (int col = 0; col < cellsPerRow; col++) {
                    int index = row * cellsPerRow + col;
                    if (index >= getItems().size()) {
                        break;
                    }
                    T item = getItems().get(index);
                    Node cellNode = getCellNode(item);

                    cellNode.resize(cellWidth, cellHeight);
                    cellNode.relocate(col * cellWidth + getLeftGap(), row * cellHeight);

                    newVisibleCells.add(cellNode);
                    if (!contentPane.getChildren().contains(cellNode)) {
                        contentPane.getChildren().add(cellNode);
                    }
                }
            }

            for (Node cell : visibleCells) {
                if (!newVisibleCells.contains(cell)) {
                    contentPane.getChildren().remove(cell);
                }
            }

            visibleCells.clear();
            visibleCells.addAll(newVisibleCells);
        }finally {
            resizing = false;
        }
    }

    /**
     * Returns the node associated with the given item.
     * @param item the item for which to get the node
     * @return the node associated with the item
     */
    private Node getCellNode(T item) {
        if (!itemToNodeMap.containsKey(item)) {
            Node cellNode = createCell(item).getNode();
            itemToNodeMap.put(item, cellNode);
        }
        return itemToNodeMap.get(item);
    }

    /**
     * Returns the NfxCell associated with the given item.
     * @param item the item for which to get the cell
     * @return the NfxCell associated with the item
     */
    @SuppressWarnings("all")
    private NfxCell<T> getCell(T item) {
        if (item == null){
            return null;
        }
        return (NfxCell<T>) getCellNode(item);
    }


    /**
     * Returns the first item in the virtual flow.
     * @return the first item, or null if the list is empty
     */
    public T getFirstItem() {
        return getItems().isEmpty() ? null : getItems().get(0);
    }

    /**
     * Returns the last item in the virtual flow.
     * @return the last item, or null if the list is empty
     */
    public T getLastItem() {
        return getItems().isEmpty() ? null : getItems().get(getItems().size() - 1);
    }

    /**
     * Scrolls to the cell containing the specified item.
     * @param item the item to scroll to
     */
    private void scrollToItemInternal(T item) {
        int index = getItems().indexOf(item);
        if (index >= 0) {
            int cellsPerRow = currentCellsPerRow.get();
            int row = index / cellsPerRow;
            double cellHeight = getCellHeight();
            double contentHeight = contentPane.getHeight();
            double viewHeight = getHeight();
            double value = Math.min((row * cellHeight) / (contentHeight - viewHeight), 1.0);

            // fast and slow at end lol
            Interpolator interpolator = new Interpolator() {
                @Override
                protected double curve(double t) {
                    return 1 - Math.pow(1 - t, 7); // fucking shit
                }
            };

            Timeline timeline = new Timeline();
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(.5),
                    new KeyValue(vvalueProperty(), value, interpolator));
            timeline.getKeyFrames().add(keyFrame);
            timeline.setOnFinished(event -> updateCells());
            timeline.play();
        }
    }

    /**
     * Creates a cell for the given item.
     * @param item the item for which to create the cell
     * @return the created cell
     */
    private Cell<T, ?> createCell(T item){
        NfxCell<T> nfxCell = getCellFactory().call(this.parent);
        nfxCell.update(item);
        return Cell.wrap(nfxCell);
    }

    /**
     * Called when an update is needed.
     * Resets the state and handles resizing.
     */
    private void onUpdate() {
        reset();
        handleResize();
    }

    /**
     * Resets the state of the virtual flow.
     * Clears the content pane, item-to-node map, and visible cells list.
     */
    private void reset() {
        contentPane.getChildren().clear();
        itemToNodeMap.clear();
        visibleCells.clear();
    }


    /**
     * Styleables class
     */
    private static class StyleableProperties {
        /**
         * CssMetaData for cell height.
         */
        private static final CssMetaData<BaseListView<?>, Number> CELL_HEIGHT =
                new CssMetaData<>("-nfx-cell-height", SizeConverter.getInstance(), 50.0) {
                    @Override
                    public boolean isSettable(BaseListView<?> n) {
                        return n.cellHeight == null || !n.cellHeight.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(BaseListView<?> n) {
                        return n.cellHeightProperty();
                    }
                };

        /**
         * CssMetaData for minimum cell width.
         */
        private static final CssMetaData<BaseListView<?>, Number> MIN_CELL_WIDTH_BREAK_POINT =
                new CssMetaData<>("-nfx-min-cell-width-break-point", SizeConverter.getInstance(), 100.0) {
                    @Override
                    public boolean isSettable(BaseListView<?> n) {
                        return n.minCellWidthBreakPoint == null || !n.minCellWidthBreakPoint.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(BaseListView<?> n) {
                        return n.minCellWidthBreakPointProperty();
                    }
                };

        /**
         * CssMetaData for maximum cells per row.
         */
        private static final CssMetaData<BaseListView<?>, Number> MAX_CELLS_PER_ROW =
                new CssMetaData<>("-nfx-max-cells-per-row", SizeConverter.getInstance(), 12) {
                    @Override
                    public boolean isSettable(BaseListView<?> n) {
                        return n.maxCellsPerRow == null || !n.maxCellsPerRow.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(BaseListView<?> n) {
                        return n.maxCellsPerRowProperty();
                    }
                };

        /**
         * CssMetaData for right gap.
         */
        private static final CssMetaData<BaseListView<?>, Number> RIGHT_GAP =
                new CssMetaData<>("-nfx-right-gap", SizeConverter.getInstance(), 15) {
                    @Override
                    public boolean isSettable(BaseListView<?> n) {
                        return n.rightGap == null || !n.rightGap.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(BaseListView<?> n) {
                        return n.rightGapProperty();
                    }
                };

        /**
         * CssMetaData for left gap.
         */
        private static final CssMetaData<BaseListView<?>, Number> LEFT_GAP =
                new CssMetaData<>("-nfx-left-gap", SizeConverter.getInstance(), 5d) {
                    @Override
                    public boolean isSettable(BaseListView<?> n) {
                        return n.leftGap == null || !n.leftGap.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(BaseListView<?> n) {
                        return n.leftGapProperty();
                    }
                };

        /**
         * List of all styleable properties.
         */
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> cssMetaData = new ArrayList<>(Control.getClassCssMetaData());
            cssMetaData.add(CELL_HEIGHT);
            cssMetaData.add(MIN_CELL_WIDTH_BREAK_POINT);
            cssMetaData.add(MAX_CELLS_PER_ROW);
            cssMetaData.add(RIGHT_GAP);
            cssMetaData.add(LEFT_GAP);
            STYLEABLES = Collections.unmodifiableList(cssMetaData);
        }
    }

    /**
     * Returns the class-level CSS metadata.
     * @return the list of CssMetaData
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * Returns the control-level CSS metadata.
     * @return the list of CssMetaData
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * Returns the default cell factory.
     * @return the callback used to create cells
     */
    private Callback<NfxListView<T>, NfxCell<T>> defaultCellFactory() {
        return new Callback<>() {
            @Override
            public NfxCell<T> call(NfxListView<T> listView) {
                return new NfxCell<>(listView) {
                    @Override
                    public void update(T item) {
                        super.update(item);
                        setText(String.format("%s", item));
                    }
                };
            }
        };
    }
}
