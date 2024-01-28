package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.document.Document;
import com.sun.internals.enums.NavButtonState;
import com.sun.internals.enums.Operation;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.enums.Fit;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;
import xss.it.ultimate.pdf.viewer.enums.ScreenMode;

import java.io.File;
import java.io.IOException;
import java.util.function.UnaryOperator;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public final class PdfToolBar extends HBox {
    /**
     * Container for the left side of the header.
     */
    private final HBox headerLeftContainer;

    /**
     * Button for open pdf.
     */
    private final Button openBtn;

    /**
     * SVG icon for the open button.
     */
    private final SVGPath openSvg;

    /**
     * Button for save button.
     */
    private final Button saveBtn;

    /**
     * SVG icon for the save button.
     */
    private final SVGPath saveSvg;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep1;

    /**
     * Container for navigation buttons.
     */
    private final HBox navBox;

    /**
     * Button to navigate to the first page.
     */
    private final Button firstPageBtn;

    /**
     * SVG icon for the first page button.
     */
    private final SVGPath fpSvg;

    /**
     * Button to navigate to the previous page.
     */
    private final Button prevPageBtn;

    /**
     * SVG icon for the previous page button.
     */
    private final SVGPath ppSvg;

    /**
     * Input field for page number.
     */
    private final TextField pageInput;

    /**
     * Button to navigate to the next page.
     */
    private final Button nextPageBtn;

    /**
     * SVG icon for the next page button.
     */
    private final SVGPath npSvg;

    /**
     * Button to navigate to the last page.
     */
    private final Button lastPageBtn;

    /**
     * SVG icon for the last page button.
     */
    private final SVGPath lpSvg;

    /**
     * Label displaying page numbers.
     */
    private final Label pagesLabel;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep2;

    /**
     * Container for zoom-related components.
     */
    private final HBox zoomBox;

    /**
     * Input field for zoom percentage.
     */
    private final TextField zoomInput;

    /**
     * Button to open zoom options menu.
     */
    private final Button zoomMenuBtn;

    /**
     * SVG icon for the zoom menu button.
     */
    private final SVGPath zmSvg;

    /**
     * Button to zoom in.
     */
    private final Button zoomInBtn;

    /**
     * SVG icon for the zoom in button.
     */
    private final SVGPath ziSvg;

    /**
     * Button to zoom out.
     */
    private final Button zoomOutBtn;

    /**
     * SVG icon for the zoom out button.
     */
    private final SVGPath zoSvg;

    /**
     * Container for the right side of the header.
     */
    private final HBox headerRightContainer;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep3;

    /**
     * Button to open the viewer menu.
     */
    private final Button menuBtn;

    /**
     * SVG icon for the menu button.
     */
    private final SVGPath mSvg;

    /**
     * The main viewer component.
     */
    private final AbstractViewer abstractViewer;

    /**
     * Zoom context menu
     */
    private final ContextMenu contextMenu;

    /**
     * Fit to width menu item
     */
    private final MenuItem fitToWidthMenuItem;

    /**
     * Fit to height menu item
     */
    private final MenuItem fitToHeightMenuItem;

    /**
     * Full screen menu item
     */
    private final MenuItem fullScreenMenuItem;

    /**
     * SVG icon for fitToWidth
     */
    private final SVGPath ftWSvg;

    /**
     * SVG icon for fitToHeight
     */
    private final SVGPath ftHSvg;

    /**
     * SVG icon for full screen menu item
     */
    private final SVGPath fullSvg;

    /**
     * Button representing the "Pan" operation.
     */
    private final Button panBtn;

    /**
     * SVGPath for the "Pan" operation button.
     */
    private final SVGPath panSvg;

    /**
     * Button representing the "Text Select" operation.
     */
    private final Button textBtn;

    /**
     * SVGPath for the "Text Select" operation button.
     */
    private final SVGPath textSelectSvg;

    /**
     * AnchorPane used as a separator.
     */
    private final AnchorPane sep4;

    /**
     * ContextMenu for additional options.
     */
    private final ContextMenu optionsContextMenu;

    /**
     * Represents a menu item for continuous page navigation.
     */
    private final MenuItem continuousPageMenuItem;

    /**
     * Represents a menu item for page-by-page navigation.
     */
    private final MenuItem pageByPageMenuItem;

    /**
     * Represents a menu item for rotating the page clockwise.
     */
    private final MenuItem rotateClockWise;

    /**
     * Represents a menu item for rotating the page counterclockwise.
     */
    private final MenuItem rotateCounterClockWise;

    /**
     * The zoom increase/decrement value
     */
    private static final double ZOOM_DEC = .25;

    /**
     * A listener to detect full screen exit and update the screen mode.
     * Don't get it wrong, I just want to keep a screen mode for future implementations.
     */
    private ChangeListener<Boolean> fullScreenListener = null;

    /**
     * Constructs a PDF toolbar associated with the specified viewer.
     *
     * @param abstractViewer The viewer to which this toolbar belongs.
     */
    public PdfToolBar(AbstractViewer abstractViewer) {
        this.abstractViewer = abstractViewer;
        headerLeftContainer = new HBox();
        openBtn = new Button();
        openSvg = new SVGPath();
        saveBtn = new Button();
        saveSvg= new SVGPath();
        sep1 = new AnchorPane();
        navBox = new HBox();
        firstPageBtn = new Button();
        fpSvg = new SVGPath();
        prevPageBtn = new Button();
        ppSvg = new SVGPath();
        pageInput = new TextField();
        nextPageBtn = new Button();
        npSvg = new SVGPath();
        lastPageBtn = new Button();
        lpSvg = new SVGPath();
        pagesLabel = new Label();
        sep2 = new AnchorPane();
        zoomBox = new HBox();
        zoomInput = new TextField();
        zoomMenuBtn = new Button();
        zmSvg = new SVGPath();
        zoomInBtn = new Button();
        ziSvg = new SVGPath();
        zoomOutBtn = new Button();
        zoSvg = new SVGPath();
        headerRightContainer = new HBox();
        sep3 = new AnchorPane();
        menuBtn = new Button();
        mSvg = new SVGPath();
        ftHSvg = new SVGPath();
        ftWSvg = new SVGPath();
        fullSvg = new SVGPath();
        sep4 = new AnchorPane();
        panBtn = new Button();
        panSvg = new SVGPath();
        textBtn = new Button();
        textSelectSvg = new SVGPath();
        contextMenu = new ContextMenu();
        optionsContextMenu = new ContextMenu();
        fitToWidthMenuItem = new MenuItem(" Fit to Width");
        fitToHeightMenuItem = new MenuItem(" Fit to Height");
        fullScreenMenuItem = new MenuItem(" Full Screen");

        continuousPageMenuItem = new MenuItem(" Continuous Page");
        pageByPageMenuItem = new MenuItem(" Page by Page");
        rotateClockWise = new MenuItem(" Rotate Clockwise");
        rotateCounterClockWise = new MenuItem(" Rotate Counterclockwise");

        /*
         * Initialize
         */
        initialize();


        /*
         * Events
         */
        setUpZoomInput();
        setupPageInput();

        initializeEvents();
    }


    /**
     * Initializes the PdfToolBar by setting up its layout and components.
     */
    private void initialize(){
        getStyleClass().add("pdf-toolbar");
        getStylesheets().add(getUserAgentStylesheet());
        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        setPrefHeight(50.0);
        setMinHeight(50);
        setMaxHeight(50);

        headerLeftContainer.setAlignment(Pos.CENTER_LEFT);
        headerLeftContainer.setPrefWidth(10000.0);

        openBtn.setMnemonicParsing(false);
        openBtn.getStyleClass().add("pdf-toolbar-button");
        HBox.setMargin(openBtn, new Insets(0.0, 0.0, 0.0, 20.0));

        openSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.open.pdf.svg"));
        openSvg.getStyleClass().add("pdf-toolbar-button-icon");
        openBtn.setGraphic(openSvg);

        saveBtn.setMnemonicParsing(false);
        saveBtn.getStyleClass().add("pdf-toolbar-button");
        HBox.setMargin(saveBtn, new Insets(0.0, 0.0, 0.0, 20.0));

        saveSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.save.pdf.svg"));
        saveSvg.getStyleClass().add("pdf-toolbar-button-icon");
        saveSvg.setScaleX(1.1);
        saveSvg.setScaleY(1.1);
        saveBtn.setGraphic(saveSvg);


        sep1.setPrefHeight(40.0);
        sep1.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep1, new Insets(0.0, 0.0, 0.0, 20.0));

        navBox.setAlignment(Pos.CENTER);
        navBox.getStyleClass().add("pdf-toolbar-nav-box");
        HBox.setMargin(navBox, new Insets(0.0, 0.0, 0.0, 20.0));

        firstPageBtn.setMnemonicParsing(false);
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button-start");

        fpSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.first.last.page.svg"));
        fpSvg.setRotate(180.0);
        fpSvg.getStyleClass().add("pdf-toolbar-button-icon");
        firstPageBtn.setGraphic(fpSvg);

        prevPageBtn.setLayoutX(11.0);
        prevPageBtn.setLayoutY(11.0);
        prevPageBtn.setMnemonicParsing(false);
        prevPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        ppSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        ppSvg.setRotate(180.0);
        ppSvg.getStyleClass().add("pdf-toolbar-button-icon");
        prevPageBtn.setGraphic(ppSvg);

        pageInput.setPrefHeight(25.0);
        pageInput.setPrefWidth(60.0);
        pageInput.getStyleClass().add("pdf-toolbar-nav-box-input");
        pageInput.setFocusTraversable(false);

        nextPageBtn.setMnemonicParsing(false);
        nextPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        npSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        npSvg.getStyleClass().add("pdf-toolbar-button-icon");
        nextPageBtn.setGraphic(npSvg);

        lastPageBtn.setMnemonicParsing(false);
        lastPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        lastPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button-end");

        lpSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.first.last.page.svg"));
        lpSvg.getStyleClass().add("pdf-toolbar-button-icon");
        lastPageBtn.setGraphic(lpSvg);

        pagesLabel.getStyleClass().add("pdf-toolbar-nav-box-label");
        pagesLabel.setMinWidth(50);
        pagesLabel.setMaxWidth(50);
        HBox.setMargin(pagesLabel, new Insets(0.0, 0.0, 0.0, 5.0));

        sep2.setPrefHeight(40.0);
        sep2.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep2, new Insets(0.0, 0.0, 0.0, 20.0));

        zoomBox.setAlignment(Pos.BOTTOM_LEFT);
        zoomBox.setPrefHeight(100.0);
        zoomBox.getStyleClass().add("pdf-toolbar-nav-box");
        HBox.setMargin(zoomBox, new Insets(0.0, 0.0, 0.0, 20.0));

        zoomInput.setPrefHeight(25.0);
        zoomInput.setPrefWidth(60.0);
        zoomInput.getStyleClass().add("pdf-toolbar-nav-zoom-input");
        zoomInput.setText(String.format("%s%s", (int)(abstractViewer.getZoomFactor()*100),"%"));
        zoomInput.setFocusTraversable(false);

        zoomMenuBtn.setMnemonicParsing(false);
        zoomMenuBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        zmSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.zoom.menu.svg"));
        zmSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomMenuBtn.setGraphic(zmSvg);

        zoomInBtn.setLayoutX(71.0);
        zoomInBtn.setLayoutY(11.0);
        zoomInBtn.setMnemonicParsing(false);
        zoomInBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        ziSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.zoom.in.svg"));
        ziSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomInBtn.setGraphic(ziSvg);

        zoomOutBtn.setLayoutX(101.0);
        zoomOutBtn.setLayoutY(11.0);
        zoomOutBtn.setMnemonicParsing(false);
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button-end");


        zoSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.zoom.out.svg"));
        zoSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomOutBtn.setGraphic(zoSvg);

        sep4.setPrefHeight(40.0);
        sep4.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep4, new Insets(0.0, 0.0, 0.0, 20.0));

        panBtn.setMnemonicParsing(false);
        panBtn.getStyleClass().add("pdf-toolbar-button");

        panSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.pan.tool.svg"));
        panSvg.getStyleClass().add("pdf-toolbar-button-icon");
        panBtn.setGraphic(panSvg);
        HBox.setMargin(panBtn, new Insets(0.0, 0.0, 0.0, 20.0));

        textBtn.setMnemonicParsing(false);
        textBtn.getStyleClass().add("pdf-toolbar-button");

        textSelectSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.text.selection.tool.svg"));
        textSelectSvg.getStyleClass().add("pdf-toolbar-button-icon");
        textBtn.setGraphic(textSelectSvg);
        textBtn.setDisable(true);//Enable later when Text selection is implemented
        HBox.setMargin(textBtn, new Insets(0.0, 0.0, 0.0, 20.0));

        headerRightContainer.setAlignment(Pos.CENTER_RIGHT);
        headerRightContainer.setMinWidth(80.0);
        headerRightContainer.setPrefHeight(100.0);
        headerRightContainer.setPrefWidth(200.0);

        sep3.setPrefHeight(40.0);
        sep3.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep3, new Insets(0.0, 20.0, 0.0, 0.0));

        menuBtn.setMnemonicParsing(false);
        menuBtn.getStyleClass().add("pdf-toolbar-button");

        mSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.menu.svg"));
        mSvg.getStyleClass().add("pdf-toolbar-button-icon");
        menuBtn.setGraphic(mSvg);
        HBox.setMargin(menuBtn, new Insets(0.0, 20.0, 0.0, 0.0));

        headerLeftContainer.getChildren().add(openBtn);
        headerLeftContainer.getChildren().add(saveBtn);

        headerLeftContainer.getChildren().add(sep1);
        navBox.getChildren().add(firstPageBtn);
        navBox.getChildren().add(prevPageBtn);
        navBox.getChildren().add(pageInput);
        navBox.getChildren().add(nextPageBtn);
        navBox.getChildren().add(lastPageBtn);
        headerLeftContainer.getChildren().add(navBox);
        headerLeftContainer.getChildren().add(pagesLabel);
        headerLeftContainer.getChildren().add(sep2);
        zoomBox.getChildren().add(zoomInput);
        zoomBox.getChildren().add(zoomMenuBtn);
        zoomBox.getChildren().add(zoomOutBtn);
        zoomBox.getChildren().add(zoomInBtn);
        headerLeftContainer.getChildren().add(zoomBox);

        headerLeftContainer.getChildren().add(sep4);
        headerLeftContainer.getChildren().add(panBtn);
        headerLeftContainer.getChildren().add(textBtn);

        getChildren().add(headerLeftContainer);
        headerRightContainer.getChildren().add(sep3);
        headerRightContainer.getChildren().add(menuBtn);

        ftWSvg.getStyleClass().add("pdf-toolbar-button-icon");
        ftWSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.fit.to.width"));
        fitToWidthMenuItem.setGraphic(ftWSvg);

        ftHSvg.getStyleClass().add("pdf-toolbar-button-icon");
        ftHSvg.setContent(this.abstractViewer.getIconsBundle().getString("pdf.fit.to.height"));
        fitToHeightMenuItem.setGraphic(ftHSvg);

        /*
         * Options context
         */
        optionsContextMenu.getStyleClass().add("pdf-toolbar-context-menu-title");
        setupOptionsContextMenu();

        /*
         * Add s separator
         */
        final SeparatorMenuItem separator = new SeparatorMenuItem();
        contextMenu.getStyleClass().add("pdf-toolbar-context-menu");
        contextMenu.getItems().addAll(fitToWidthMenuItem, fitToHeightMenuItem, separator);

        /*
         * Add remaining needed items automatically
         */
        int inc =25;//Increase val
        for (int i = inc ; i <= 1000; i = i + inc) {
            if (i == 150){
                inc = 50;
            }
            if (i == 200){
                inc = 200;
            }
            if (i == 400){
                inc = 400;
            }
            if (i == 800){
                inc = 200;// To obtain 1000, dirty but works :)
            }
            contextMenu.getItems().add(buildForPercent(String.format(" %s%s",i,"%")));
        }

        fullSvg.getStyleClass().add("pdf-toolbar-button-icon");
        fullSvg.setScaleX(1.3);
        fullSvg.setScaleY(1.3);
        fullScreenMenuItem.setGraphic(fullSvg);

        final SeparatorMenuItem lastSeparator = new SeparatorMenuItem();
        contextMenu.getItems().add(lastSeparator);
        contextMenu.getItems().add(fullScreenMenuItem);

        getChildren().add(headerRightContainer);
    }


    /**
     * Initializes event handlers and listeners for the PdfToolBar.
     */
    private void initializeEvents() {
        /*
         * Open pdf
         */
        openBtn.setOnAction(event -> abstractViewer.open());

        /*
         * Save pdf
         */
        saveBtn.setOnAction(event -> {
            try {
                save();
            } catch (IOException e) {
                //TODO: Let know the Error, dont be lazy :)
                throw new RuntimeException(e);
            }
        });

        /*
         * Doc listener
         */
        abstractViewer.documentProperty().addListener((obs, old, document) -> {
            if (document != null) {
                if (abstractViewer.getPageView() != null){
                    abstractViewer.getPageView().reload();
                }
                pageInput.setText(String.format("%s", 1));
                pagesLabel.setText(String.format("of %s", document.getNumberOfPages()));
                abstractViewer.gotoFirstPage();
            }
            else {
                pagesLabel.setText("");
            }
            assignNavButtonsState();
            checkDocument(document);
        });
        checkDocument(abstractViewer.getDocument());

        /*
         * Page
         */
        abstractViewer.pageProperty().addListener((obs, old, page) -> {
            abstractViewer.switchViewport(old.intValue(), page.intValue());
            int index =page.intValue()+1;
            pageInput.setText(String.format("%s",index));
            assignNavButtonsState();
        });

        pageInput.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)){
                if (pageInput.getText().isEmpty() || pageInput.getText().isBlank()){
                    int curIndex = abstractViewer.getPage();
                    curIndex++;
                    //We keep same index since no value was passed :)
                    pageInput.setText(String.format("%s", curIndex));
                    return;
                }
                int index = Integer.parseInt(pageInput.getText());
                if (index > abstractViewer.getDocument().getNumberOfPages()){
                    index = abstractViewer.getDocument().getNumberOfPages();
                }
                if (index <= 0){
                    index = 1;
                }
                //Since we use page indexes starting at 1 but pdfbox page count starts at 0
                // we subtract 1 to the index
                index--;
                abstractViewer.setPage(index);
            }
        });

        /*
         * Page nav buttons
         */
        firstPageBtn.setOnAction(event -> abstractViewer.gotoFirstPage());
        prevPageBtn.setOnAction(event -> abstractViewer.gotoPreviousPage());
        nextPageBtn.setOnAction(event -> abstractViewer.gotoNextPage());
        lastPageBtn.setOnAction(event -> abstractViewer.gotoLastPage());

        /*
         * Nav buttons state
         */
        assignNavButtonsState();
        abstractViewer.navButtonsStateProperty().addListener((obs, o, state) -> checkNavButtonsState(state));

        /*
         * Zoom actions
         */
        abstractViewer.zoomFactorProperty().addListener((obs, ozf, zf) -> {
            int percent = (int) (zf.doubleValue() * 100);
            zoomInput.setText(String.format("%s%s", percent,"%"));
            updateZoomButtons();
        });

        zoomInBtn.setOnAction(event->{
            contextMenu.hide();
            abstractViewer.setFit(Fit.NONE);
            double factor = abstractViewer.getZoomFactor() + ZOOM_DEC;
            abstractViewer.setZoomFactor(Math.min(factor, abstractViewer.getMaxZoomFactor()));
        });

        zoomOutBtn.setOnAction(event->{
            contextMenu.hide();
            abstractViewer.setFit(Fit.NONE);
            double factor = abstractViewer.getZoomFactor() - ZOOM_DEC;
            abstractViewer.setZoomFactor(Math.max(factor, abstractViewer.getMinZoomFactor()));
        });

        zoomInput.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER)){
                event.consume();
                String num = zoomInput.getText().replace("%","");
                //We do a bit of hacking here to not let the zoom input empty ot just with a % symbol
                //wen the user just hit ENTER without any proper value typed for zoom
                boolean updateZoomLabel = false;
                if (num.isBlank() || num.isEmpty()){
                    num = String.format("%s", (abstractViewer.getZoomFactor()*100));
                    updateZoomLabel = true;
                }
                double percent = Double.parseDouble(num);
                abstractViewer.setZoomFactor(percent /100);
                if (updateZoomLabel){
                    zoomInput.setText(String.format("%s%s",((int)percent),"%"));
                }
            }
        });

        zoomInput.setOnMouseClicked(event -> contextMenu.hide());

        /*
         * Fit setup
         */
        abstractViewer.fitProperty().addListener((obs, o, fit) -> checkFit(fit));
        fitToWidthMenuItem.setOnAction(event-> abstractViewer.setFit(Fit.HORIZONTAL));
        fitToHeightMenuItem.setOnAction(event-> abstractViewer.setFit(Fit.VERTICAL));

        /*
         * Full Screen mode
         */
        checkFullScreenMenuEnable(abstractViewer.isAllowFullScreen());
        abstractViewer.allowFullScreenProperty().addListener((obs, o, allow) -> checkFullScreenMenuEnable(allow));

        abstractViewer.screenModeProperty().addListener((obs, o, mode) -> {
            Stage ownerWindow = (Stage) this.getScene().getWindow();
            handleScreenMode(ownerWindow, mode);

            /*
             * If the listener is null it means it has not been initialized and not passed yet to the
             * window fullScreenProperty, so we can proceed to add it just for this time.
             */
            if (fullScreenListener == null) {
                fullScreenListener = (obs1, o1, full) -> {
                    if (full) {
                        abstractViewer.setScreenMode(ScreenMode.FULL_SCREEN);
                    } else {
                        abstractViewer.setScreenMode(ScreenMode.NORMAL);
                    }
                };
                ownerWindow.fullScreenProperty().addListener(fullScreenListener);
            }
        });

        fullScreenMenuItem.setOnAction(event->{
            switch (abstractViewer.getScreenMode()){
                case FULL_SCREEN -> abstractViewer.setScreenMode(ScreenMode.NORMAL);
                case NORMAL -> abstractViewer.setScreenMode(ScreenMode.FULL_SCREEN);
            }
        });

        handleScreenMode(null, abstractViewer.getScreenMode());

        /*
         * Show Menu
         */
        zoomMenuBtn.setOnAction(event-> contextMenu.show(zoomBox, Side.BOTTOM,0,4));
        this.contextMenu.setOnShowing(event-> zmSvg.getStyleClass().add("pdf-toolbar-operation-selected-btn-icon"));
        this.contextMenu.setOnHiding(event-> zmSvg.getStyleClass().remove("pdf-toolbar-operation-selected-btn-icon"));

        /*
         * Pan/Text selection
         */
        handleOperation(abstractViewer.getOperation());
        abstractViewer.operationProperty().addListener((obs, o, operation) -> handleOperation(operation));

        panBtn.setOnAction(event -> {
            Operation currentOperation = abstractViewer.getOperation();
            if (currentOperation == Operation.PAN) {
                abstractViewer.setOperation(Operation.NONE);
            } else if (currentOperation == Operation.NONE || currentOperation == Operation.SELECT) {
                abstractViewer.setOperation(Operation.PAN);
            }
        });

        textBtn.setOnAction(event -> {
            Operation currentOperation = abstractViewer.getOperation();
            if (currentOperation == Operation.SELECT) {
                abstractViewer.setOperation(Operation.NONE);
            } else if (currentOperation == Operation.PAN || currentOperation == Operation.NONE) {
                abstractViewer.setOperation(Operation.SELECT);
            }
        });

        /*
         * Options
         */
        menuBtn.setOnAction(event->{
            optionsContextMenu.show(menuBtn, Side.BOTTOM,-170,4);
        });

        /*
         * Page Rotation
         */
        rotateClockWise.setOnAction(event -> {
            double currentRt = abstractViewer.getPageRotation();
            currentRt = currentRt + 90;
            abstractViewer.setPageRotation(currentRt);
        });

        rotateCounterClockWise.setOnAction(event -> {
            double currentRt = abstractViewer.getPageRotation();
            currentRt = currentRt - 90;
            abstractViewer.setPageRotation(currentRt);
        });

    }



    /*
     * =================================================================================================================
     *
     *                                             U T I L S
     *
     * =================================================================================================================
     */

    /**
     * Saves the current state of the document. This method may throw an IOException.
     * Ensure to handle the exception appropriately.
     */
    private void save() throws IOException {
        PDDocument doc;
        if (abstractViewer.getDocument() == null){
            return;//TODO: implement a dialog to let know there is no pdf file opened yet.
        }
        doc = abstractViewer.getDocument().getDocument() == null ? null :  abstractViewer.getDocument().getDocument();

        if (doc == null){
            return;//TODO: implement a dialog to let know there is no pdf file opened yet.
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF");
        chooser.getExtensionFilters().add(Assets.PDF_EXTENSION_FILTER);
        File file = chooser.showSaveDialog(saveBtn.getScene().getWindow());
        if (file != null){
            String path = file.getAbsolutePath();
            if (!path.endsWith(".pdf")){
                path=String.format("%s.pdf",path);
                file = new File(path);
            }
            doc.save(file);
        }
    }

    /**
     * Handles the specified operation.
     *
     * @param operation The operation to handle.
     */
    private void handleOperation(Operation operation){
        switch (operation){
            case PAN -> {
                panBtn.getStyleClass().add("pdf-toolbar-operation-selected-btn");
                panSvg.getStyleClass().add("pdf-toolbar-operation-selected-btn-icon");
                textBtn.getStyleClass().remove("pdf-toolbar-operation-selected-btn");
                textSelectSvg.getStyleClass().remove("pdf-toolbar-operation-selected-btn-icon");
            }
            case SELECT -> {
                panBtn.getStyleClass().remove("pdf-toolbar-operation-selected-btn");
                panSvg.getStyleClass().remove("pdf-toolbar-operation-selected-btn-icon");
                textBtn.getStyleClass().add("pdf-toolbar-operation-selected-btn");
                textSelectSvg.getStyleClass().add("pdf-toolbar-operation-selected-btn-icon");
            }
            default ->{
                panBtn.getStyleClass().remove("pdf-toolbar-operation-selected-btn");
                panSvg.getStyleClass().remove("pdf-toolbar-operation-selected-btn-icon");
                textBtn.getStyleClass().remove("pdf-toolbar-operation-selected-btn");
                textSelectSvg.getStyleClass().remove("pdf-toolbar-operation-selected-btn-icon");
            }
        }
    }

    /**
     * Set up the value zoom mechanism.
     * This method is responsible for configuring and initializing the zoom input component.
     */
    private void setUpZoomInput(){
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            /*
             * Allow digits and a single percent symbol
             */
            if (newText.matches("[0-9]*%")) {
                return change;
            }
            return null;
        };

        final TextFormatter<String> formatter = new TextFormatter<>(filter);
        zoomInput.setTextFormatter(formatter);
    }

    /**
     * Sets up the input field for page numbers to allow only digits and handles leading zeros.
     */
    private void setupPageInput(){
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            /*
             * Allow digits and handle leading zeros
             */
            if (newText.matches("[0-9]*")) {
                // Remove leading zeros for numbers with more than one digit
                if (newText.length() > 1 && newText.startsWith("0")) {
                    newText = newText.replaceFirst("^0+(?!$)", "");
                    change.setText(newText);
                    change.setRange(change.getRangeStart(), change.getRangeStart() + newText.length());
                    change.setCaretPosition(change.getCaretPosition() - 1);
                }
                // Handle single "0" as "1"
                else if (newText.equals("0")) {
                    change.setText("1");
                }
                return change;
            }
            return null;
        };

        final TextFormatter<String> formatter = new TextFormatter<>(filter);
        pageInput.setTextFormatter(formatter);
    }

    /**
     * Handles the application's screen mode (full screen or normal).
     *
     * @param stage The JavaFX stage to be configured.
     * @param mode The desired screen mode (FULL_SCREEN or NORMAL).
     */
    private void handleScreenMode(Stage stage, ScreenMode mode){
        switch (mode) {
            case FULL_SCREEN -> {
                if (stage!=null) {
                    stage.setFullScreen(true);
                }
                fullScreenMenuItem.setText(" Exit Full Screen");
                fullSvg.setContent(abstractViewer.getIconsBundle().getString("pdf.normal.screen.mode"));
            }
            case NORMAL -> {
                if (stage!=null) {
                    stage.setFullScreen(false);
                }
                fullScreenMenuItem.setText(" Full Screen");
                fullSvg.setContent(abstractViewer.getIconsBundle().getString("pdf.full.screen.mode"));
            }
        }
    }

    /**
     * Checks the enable state of the full-screen menu item based on whether full-screen mode is allowed.
     * Disables the full-screen menu item if full-screen mode is not allowed.
     *
     * @param fullScreenAllowed Indicates whether full-screen mode is allowed.
     */
    private void checkFullScreenMenuEnable(boolean fullScreenAllowed){
        fullScreenMenuItem.setDisable(!fullScreenAllowed);
    }

    /**
     * Checks the current document and updates the state of related UI elements.
     *
     * @param doc The current document (can be null if no document is loaded).
     */
    private void checkDocument(Document doc){
        zoomBox.setDisable(doc == null);
        panBtn.setDisable(doc == null);
        //textBtn.setDisable(doc == null);
        continuousPageMenuItem.setDisable(doc == null || abstractViewer.getPageViewMode().equals(PageViewMode.CONTINUOUS));
        pageByPageMenuItem.setDisable(doc == null || abstractViewer.getPageViewMode().equals(PageViewMode.PAGE_BY_PAGE));
        rotateClockWise.setDisable(doc == null);
        rotateCounterClockWise.setDisable(doc == null);
        saveBtn.setDisable(doc == null);
    }


    /**
     * Assigns the state of navigation buttons based on the current page index and the
     * total number of pages in the document. It determines whether to enable or disable
     * the "Previous" and "Next" buttons accordingly.
     */
    private void assignNavButtonsState() {
        if (abstractViewer.getDocument() == null || abstractViewer.getDocument().getNumberOfPages() == 1) {
            abstractViewer.setNavButtonsState(NavButtonState.DISABLE_BOTH);
        } else {
            int index = abstractViewer.getPage();
            int total = abstractViewer.getDocument().getNumberOfPages();

            if (index == 0) {
                abstractViewer.setNavButtonsState(NavButtonState.DISABLE_PREV);
            } else if (index == total - 1) {
                abstractViewer.setNavButtonsState(NavButtonState.DISABLE_NEXT);
            } else {
                abstractViewer.setNavButtonsState(NavButtonState.ENABLE_BOTH);
            }
        }
        checkNavButtonsState(abstractViewer.getNavButtonsState());
    }

    /**
     * Updates the state of navigation buttons based on the specified NavButtonState.
     *
     * @param state The NavButtonState to set the navigation buttons to.
     */
    private void checkNavButtonsState(NavButtonState  state){
        switch (state){
            case DISABLE_NEXT -> {
                firstPageBtn.setDisable(false);
                prevPageBtn.setDisable(false);
                nextPageBtn.setDisable(true);
                lastPageBtn.setDisable(true);
                pageInput.setDisable(false);
            }
            case DISABLE_PREV -> {
                firstPageBtn.setDisable(true);
                prevPageBtn.setDisable(true);
                nextPageBtn.setDisable(false);
                lastPageBtn.setDisable(false);
                pageInput.setDisable(false);
            }
            case ENABLE_BOTH -> {
                firstPageBtn.setDisable(false);
                prevPageBtn.setDisable(false);
                nextPageBtn.setDisable(false);
                lastPageBtn.setDisable(false);
                pageInput.setDisable(false);
            }
            default -> {
                firstPageBtn.setDisable(true);
                prevPageBtn.setDisable(true);
                nextPageBtn.setDisable(true);
                lastPageBtn.setDisable(true);
                pageInput.setDisable(true);
            }
        }
    }

    /**
     * Updates the state of zoom-related buttons based on the current zoom level.
     * This method is called to reflect changes in zoom levels.
     */
    private void updateZoomButtons() {
        double currentZoomFactor = abstractViewer.getZoomFactor();
        double maxZoomFactor = abstractViewer.getMaxZoomFactor();
        double minZoomFactor = abstractViewer.getMinZoomFactor();
        zoomInBtn.setDisable(currentZoomFactor >= maxZoomFactor);
        zoomOutBtn.setDisable(currentZoomFactor <= minZoomFactor);
    }

    /**
     * Checks the fit mode and updates button states accordingly.
     *
     * @param fit The fit mode (VERTICAL, HORIZONTAL, or NONE).
     */
    private void checkFit(Fit fit){
        switch (fit){
            case VERTICAL -> {
                fitToHeightMenuItem.setDisable(true);
                fitToWidthMenuItem.setDisable(false);
            }
            case HORIZONTAL -> {
                fitToWidthMenuItem.setDisable(true);
                fitToHeightMenuItem.setDisable(false);
            }
            default -> {
                fitToHeightMenuItem.setDisable(false);
                fitToWidthMenuItem.setDisable(false);
            }
        }
    }


    /**
     * Build a MenuItem for a given percentage text.
     *
     * @param text The text representing a zoom percentage (e.g., "200%").
     * @return A MenuItem configured to set the zoom factor when selected.
     */
    private MenuItem buildForPercent(String text){
        MenuItem m = new MenuItem(text);
        m.setOnAction(actionEvent -> {
            /*
             * W can not forget to set fit to NONE
             */
            abstractViewer.setFit(Fit.NONE);
            int p = Integer.parseInt(text.replace("%", "").replaceAll("\\s+", ""));
            abstractViewer.setZoomFactor((double) p /100);
        });
        return m;
    }

    /**
     * Builds a MenuItem for the context menu.
     *
     * @param text   The text of the menu item.
     * @param event  The event handler for the menu item.
     * @return A MenuItem with the specified text and event handler.
     */
    private MenuItem buildForMenu(String text, EventHandler<ActionEvent> event){
        MenuItem m = new MenuItem(text);
        m.setOnAction(event);
        return m;
    }

    /**
     * Builds a SeparatorMenuItem with a title label for the context menu.
     *
     * @param text The text of the title label.
     * @return A SeparatorMenuItem with a title label.
     */
    private SeparatorMenuItem buildForTitle(String text){
        Label label = new Label(text);
        label.getStyleClass().add("pdf-toolbar-context-menu-tile");
        SeparatorMenuItem sp = new SeparatorMenuItem();
        sp.setContent(label);
        return sp;
    }

    /**
     * Sets up the context menu for additional viewing options.
     */
    private void setupOptionsContextMenu(){
        optionsContextMenu.getItems().add(buildForTitle("Page Transitions"));
        SVGPath cpSvg = build(abstractViewer.getIconsBundle().getString("pdf.options.continuous.pages"));
        cpSvg.setScaleX(.7);
        cpSvg.setScaleY(.7);
        continuousPageMenuItem.setGraphic(cpSvg);
        optionsContextMenu.getItems().add(continuousPageMenuItem);

        SVGPath pbpSvg = build(abstractViewer.getIconsBundle().getString("pdf.options.page.by.page"));
        pbpSvg.setScaleX(.7);
        pbpSvg.setScaleY(.7);
        pageByPageMenuItem.setGraphic(pbpSvg);

        optionsContextMenu.getItems().add(pageByPageMenuItem);
        optionsContextMenu.getItems().add(buildForTitle("Page Orientation"));

        SVGPath rtCw= build(abstractViewer.getIconsBundle().getString("pdf.options.rotate.clockwise"));
        rtCw.setScaleX(.7);
        rtCw.setScaleY(.7);
        rotateClockWise.setGraphic(rtCw);
        optionsContextMenu.getItems().add(rotateClockWise);

        SVGPath rtCcw = build(abstractViewer.getIconsBundle().getString("pdf.options.rotate.counterclockwise"));
        rtCcw.setScaleX(.7);
        rtCcw.setScaleY(.7);
        rotateCounterClockWise.setGraphic(rtCcw);
        optionsContextMenu.getItems().add(rotateCounterClockWise);
    }

    /**
     * Builds an SVGPath with the given content.
     *
     * @param content The content of the SVGPath.
     * @return The constructed SVGPath.
     */
    private SVGPath build(String content){
        SVGPath svg = new SVGPath();
        svg.getStyleClass().add("pdf-toolbar-button-icon");
        svg.setContent(content);
        return svg;
    }

    /**
     * Gets the user agent stylesheet for the PdfToolBar.
     * @return The URL of the user agent stylesheet.
     */
    @Override
    public String getUserAgentStylesheet() {
        return Assets.load("/xss/it/ultimate/pdf/viewer/css/toolbar.css").toExternalForm();
    }
}
