package com.sun.internals.ctrl;

import com.sun.internals.document.Document;
import com.sun.internals.enums.Fit;
import com.sun.internals.enums.NavButtonState;
import com.sun.internals.enums.ScreenMode;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.Viewer;

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
     * Button for toggling thumbnails view.
     */
    private final Button openBtn;

    /**
     * SVG icon for the thumbnails button.
     */
    private final SVGPath openSvg;

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
    private final Viewer viewer;

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
     * @param viewer The viewer to which this toolbar belongs.
     */
    public PdfToolBar(Viewer viewer) {
        this.viewer = viewer;
        headerLeftContainer = new HBox();
        openBtn = new Button();
        openSvg = new SVGPath();
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
        contextMenu = new ContextMenu();
        fitToWidthMenuItem = new MenuItem(" Fit to Width");
        fitToHeightMenuItem = new MenuItem(" Fit to Height");
        fullScreenMenuItem = new MenuItem(" Full Screen");

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

        openSvg.setContent(this.viewer.getIconsBundle().getString("pdf.open.pdf.svg"));
        openSvg.getStyleClass().add("pdf-toolbar-button-icon");
        openBtn.setGraphic(openSvg);

        sep1.setPrefHeight(40.0);
        sep1.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep1, new Insets(0.0, 0.0, 0.0, 20.0));

        navBox.setAlignment(Pos.CENTER);
        navBox.getStyleClass().add("pdf-toolbar-nav-box");
        HBox.setMargin(navBox, new Insets(0.0, 0.0, 0.0, 20.0));

        firstPageBtn.setMnemonicParsing(false);
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button-start");

        fpSvg.setContent(this.viewer.getIconsBundle().getString("pdf.first.last.page.svg"));
        fpSvg.setRotate(180.0);
        fpSvg.getStyleClass().add("pdf-toolbar-button-icon");
        firstPageBtn.setGraphic(fpSvg);

        prevPageBtn.setLayoutX(11.0);
        prevPageBtn.setLayoutY(11.0);
        prevPageBtn.setMnemonicParsing(false);
        prevPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        ppSvg.setContent(this.viewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        ppSvg.setRotate(180.0);
        ppSvg.getStyleClass().add("pdf-toolbar-button-icon");
        prevPageBtn.setGraphic(ppSvg);

        pageInput.setPrefHeight(25.0);
        pageInput.setPrefWidth(60.0);
        pageInput.getStyleClass().add("pdf-toolbar-nav-box-input");
        pageInput.setFocusTraversable(false);

        nextPageBtn.setMnemonicParsing(false);
        nextPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        npSvg.setContent(this.viewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        npSvg.getStyleClass().add("pdf-toolbar-button-icon");
        nextPageBtn.setGraphic(npSvg);

        lastPageBtn.setMnemonicParsing(false);
        lastPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        lastPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button-end");

        lpSvg.setContent(this.viewer.getIconsBundle().getString("pdf.first.last.page.svg"));
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
        zoomInput.setText(String.format("%s%s", (int)(viewer.getZoomFactor()*100),"%"));
        zoomInput.setFocusTraversable(false);

        zoomMenuBtn.setMnemonicParsing(false);
        zoomMenuBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        zmSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.menu.svg"));
        zmSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomMenuBtn.setGraphic(zmSvg);

        zoomInBtn.setLayoutX(71.0);
        zoomInBtn.setLayoutY(11.0);
        zoomInBtn.setMnemonicParsing(false);
        zoomInBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        ziSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.in.svg"));
        ziSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomInBtn.setGraphic(ziSvg);

        zoomOutBtn.setLayoutX(101.0);
        zoomOutBtn.setLayoutY(11.0);
        zoomOutBtn.setMnemonicParsing(false);
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button-end");


        zoSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.out.svg"));
        zoSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomOutBtn.setGraphic(zoSvg);

        headerRightContainer.setAlignment(Pos.CENTER_RIGHT);
        headerRightContainer.setMinWidth(80.0);
        headerRightContainer.setPrefHeight(100.0);
        headerRightContainer.setPrefWidth(200.0);

        sep3.setPrefHeight(40.0);
        sep3.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep3, new Insets(0.0, 20.0, 0.0, 0.0));

        menuBtn.setMnemonicParsing(false);
        menuBtn.getStyleClass().add("pdf-toolbar-button");

        mSvg.setContent(this.viewer.getIconsBundle().getString("pdf.menu.svg"));
        mSvg.getStyleClass().add("pdf-toolbar-button-icon");
        menuBtn.setGraphic(mSvg);
        HBox.setMargin(menuBtn, new Insets(0.0, 20.0, 0.0, 0.0));

        headerLeftContainer.getChildren().add(openBtn);
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
        getChildren().add(headerLeftContainer);
        headerRightContainer.getChildren().add(sep3);
        headerRightContainer.getChildren().add(menuBtn);

        ftWSvg.getStyleClass().add("pdf-toolbar-button-icon");
        ftWSvg.setContent(this.viewer.getIconsBundle().getString("pdf.fit.to.width"));
        fitToWidthMenuItem.setGraphic(ftWSvg);

        ftHSvg.getStyleClass().add("pdf-toolbar-button-icon");
        ftHSvg.setContent(this.viewer.getIconsBundle().getString("pdf.fit.to.height"));
        fitToHeightMenuItem.setGraphic(ftHSvg);

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
        openBtn.setOnAction(event -> viewer.open());

        /*
         * Doc listener
         */
        viewer.documentProperty().addListener((obs, old, document) -> {
            if (document != null) {
                if (viewer.getPageView() != null){
                    viewer.getPageView().reload();
                }
                pageInput.setText(String.format("%s", 1));
                pagesLabel.setText(String.format("of %s", document.getNumberOfPages()));
                viewer.gotoFirstPage();
            }
            else {
                pagesLabel.setText("");
            }
            assignNavButtonsState();
            checkDocument(document);
        });
        checkDocument(viewer.getDocument());

        /*
         * Page
         */
        viewer.pageProperty().addListener((obs, old, page) -> {
            viewer.switchViewport(old.intValue(), page.intValue());
            int index =page.intValue()+1;
            pageInput.setText(String.format("%s",index));
            assignNavButtonsState();
        });

        pageInput.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)){
                if (pageInput.getText().isEmpty() || pageInput.getText().isBlank()){
                    int curIndex = viewer.getPage();
                    curIndex++;
                    //We keep same index since no value was passed :)
                    pageInput.setText(String.format("%s", curIndex));
                    return;
                }
                int index = Integer.parseInt(pageInput.getText());
                if (index > viewer.getDocument().getNumberOfPages()){
                    index = viewer.getDocument().getNumberOfPages();
                }
                if (index <= 0){
                    index = 1;
                }
                //Since we use page indexes starting at 1 but pdfbox page count starts at 0
                // we subtract 1 to the index
                index--;
                viewer.setPage(index);
            }
        });

        /*
         * Page nav buttons
         */
        firstPageBtn.setOnAction(event -> viewer.gotoFirstPage());
        prevPageBtn.setOnAction(event -> viewer.gotoPreviousPage());
        nextPageBtn.setOnAction(event -> viewer.gotoNextPage());
        lastPageBtn.setOnAction(event -> viewer.gotoLastPage());

        /*
         * Nav buttons state
         */
        assignNavButtonsState();
        viewer.navButtonsStateProperty().addListener((obs, o, state) -> checkNavButtonsState(state));

        /*
         * Zoom actions
         */
        viewer.zoomFactorProperty().addListener((obs, ozf, zf) -> {
            int percent = (int) (zf.doubleValue() * 100);
            zoomInput.setText(String.format("%s%s", percent,"%"));
            updateZoomButtons();
        });

        zoomInBtn.setOnAction(event->{
            contextMenu.hide();
            viewer.setFit(Fit.NONE);
            double factor = viewer.getZoomFactor() + ZOOM_DEC;
            viewer.setZoomFactor(Math.min(factor, viewer.getMaxZoomFactor()));
        });

        zoomOutBtn.setOnAction(event->{
            contextMenu.hide();
            viewer.setFit(Fit.NONE);
            double factor =viewer.getZoomFactor() - ZOOM_DEC;
            viewer.setZoomFactor(Math.max(factor, viewer.getMinZoomFactor()));
        });

        zoomInput.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER)){
                event.consume();
                String num = zoomInput.getText().replace("%","");
                //We do a bit of hacking here to not let the zoom input empty ot just with a % symbol
                //wen the user just hit ENTER without any proper value typed for zoom
                boolean updateZoomLabel = false;
                if (num.isBlank() || num.isEmpty()){
                    num = String.format("%s", (viewer.getZoomFactor()*100));
                    updateZoomLabel = true;
                }
                double percent = Double.parseDouble(num);
                viewer.setZoomFactor(percent /100);
                if (updateZoomLabel){
                    zoomInput.setText(String.format("%s%s",((int)percent),"%"));
                }
            }
        });

        zoomInput.setOnMouseClicked(event -> contextMenu.hide());

        /*
         * Fit setup
         */
        viewer.fitProperty().addListener((obs, o, fit) -> checkFit(fit));
        fitToWidthMenuItem.setOnAction(event-> viewer.setFit(Fit.HORIZONTAL));
        fitToHeightMenuItem.setOnAction(event-> viewer.setFit(Fit.VERTICAL));

        /*
         * Full Screen mode
         */
        checkFullScreenMenuEnable(viewer.isAllowFullScreen());
        viewer.allowFullScreenProperty().addListener((obs, o, allow) -> checkFullScreenMenuEnable(allow));

        viewer.screenModeProperty().addListener((obs, o, mode) -> {
            Stage ownerWindow = (Stage) this.getScene().getWindow();
            handleScreenMode(ownerWindow, mode);

            /*
             * If the listener is null it means it has not been initialized and not passed yet to the
             * window fullScreenProperty, so we can proceed to add it just for this time.
             */
            if (fullScreenListener == null) {
                fullScreenListener = (obs1, o1, full) -> {
                    if (full) {
                        viewer.setScreenMode(ScreenMode.FULL_SCREEN);
                    } else {
                        viewer.setScreenMode(ScreenMode.NORMAL);
                    }
                };
                ownerWindow.fullScreenProperty().addListener(fullScreenListener);
            }
        });

        fullScreenMenuItem.setOnAction(event->{
            switch (viewer.getScreenMode()){
                case FULL_SCREEN -> viewer.setScreenMode(ScreenMode.NORMAL);
                case NORMAL -> viewer.setScreenMode(ScreenMode.FULL_SCREEN);
            }
        });



        handleScreenMode(null, viewer.getScreenMode());

        /*
         * Show Menu
         */
        zoomMenuBtn.setOnAction(event-> contextMenu.show(zoomBox, Side.BOTTOM,0,4));
    }

    /*
     * =================================================================================================================
     *
     *                                             U T I L S
     *
     * =================================================================================================================
     */
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
                fullSvg.setContent(viewer.getIconsBundle().getString("pdf.normal.screen.mode"));
            }
            case NORMAL -> {
                if (stage!=null) {
                    stage.setFullScreen(false);
                }
                fullScreenMenuItem.setText(" Full Screen");
                fullSvg.setContent(viewer.getIconsBundle().getString("pdf.full.screen.mode"));
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
    }


    /**
     * Assigns the state of navigation buttons based on the current page index and the
     * total number of pages in the document. It determines whether to enable or disable
     * the "Previous" and "Next" buttons accordingly.
     */
    private void assignNavButtonsState() {
        if (viewer.getDocument() == null || viewer.getDocument().getNumberOfPages() == 1) {
            viewer.setNavButtonsState(NavButtonState.DISABLE_BOTH);
        } else {
            int index = viewer.getPage();
            int total = viewer.getDocument().getNumberOfPages();

            if (index == 0) {
                viewer.setNavButtonsState(NavButtonState.DISABLE_PREV);
            } else if (index == total - 1) {
                viewer.setNavButtonsState(NavButtonState.DISABLE_NEXT);
            } else {
                viewer.setNavButtonsState(NavButtonState.ENABLE_BOTH);
            }
        }
        checkNavButtonsState(viewer.getNavButtonsState());
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
        double currentZoomFactor = viewer.getZoomFactor();
        double maxZoomFactor = viewer.getMaxZoomFactor();
        double minZoomFactor = viewer.getMinZoomFactor();
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
            viewer.setFit(Fit.NONE);
            int p = Integer.parseInt(text.replace("%", "").replaceAll("\\s+", ""));
            viewer.setZoomFactor((double) p /100);
        });
        return m;
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
