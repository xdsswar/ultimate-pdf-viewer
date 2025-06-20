package com.sun.internals.viewer;

import com.sun.internals.AbstractViewer;
import com.sun.internals.PageData;
import com.sun.internals.PdfDocument;
import com.sun.internals.controls.ContinuousPageViewer;
import com.sun.internals.controls.PdfSearchPanel;
import com.sun.internals.controls.PdfToolBar;
import com.sun.internals.controls.SinglePageViewer;
import com.sun.internals.document.Document;
import com.sun.internals.enums.NavButtonState;
import com.sun.internals.enums.Operation;
import com.sun.internals.enums.SearchPanelStatus;
import com.sun.internals.helpers.Animation;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.controls.PageView;
import xss.it.ultimate.pdf.viewer.enums.ColorScheme;
import xss.it.ultimate.pdf.viewer.enums.Fit;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;
import xss.it.ultimate.pdf.viewer.enums.ScreenMode;
import xss.it.ultimate.pdf.viewer.text.SearchResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * @author XDSSWAR
 * Created on 01/26/2024
 */
public final class PdfAbstractViewerImpl extends AbstractViewer {
    /**
     * The ResourceBundle containing icons for the PDF viewer.
     */
    private final static ResourceBundle iconsBundle;

    static {
        /*
         * Init the bundle
         */
        iconsBundle = ResourceBundle.getBundle("xss/it/ultimate/pdf/viewer/res/icons");

        /*
         * Load fonts
         */
        Font.loadFont(Assets.stream("/xss/it/ultimate/pdf/viewer/fonts/Lato-Regular.ttf"),12);
    }

    /**
     * The toolbar for controlling the PDF viewer.
     */
    private final PdfToolBar toolbar;

    /**
     * The split pane that divides the viewer into multiple panes.
     */
    private final AnchorPane widePane;

    /**
     * The left anchor pane within the viewer.
     */
    private final AnchorPane leftPane;

    /**
     * The center anchor pane within the viewer.
     */
    private final AnchorPane centerPane;

    /**
     * The right anchor pane within the viewer.
     */
    private final AnchorPane rightPane;

    /**
     * Search panel
     */
    private final PdfSearchPanel pdfSearchPanel;

    /**
     * A static Executor used for managing asynchronous tasks.
     * This field is initially set to null and should be initialized with an Executor instance when needed.
     */
    private static Executor EXECUTOR = null;


    /*
     * =================================================================================================================
     *
     *                                           P R O P E R T I E S
     *
     * =================================================================================================================
     */

    /**
     * Represents the currently selected page view for the PdfViewerPane.
     */
    private ObjectProperty<PageView> pageView;

    /**
     * Gets the property for the currently selected page view.
     *
     * @return The ObjectProperty for the page view.
     */
    @Override
    public ObjectProperty<PageView> pageViewProperty() {
        if (pageView == null) {
            pageView = new SimpleObjectProperty<>(this, "pageView", new SinglePageViewer(this));
        }
        return pageView;
    }

    /**
     * Gets the currently selected page view.
     *
     * @return The currently selected page view.
     */
    @Override
    public PageView getPageView() {
        return pageViewProperty().get();
    }

    /**
     * Sets the currently selected page view.
     *
     * @param pageView The page view to set as the current selection.
     */
    @Override
    public void setPageView(PageView pageView) {
        this.pageViewProperty().set(pageView);
    }


    /**
     * A property representing whether to show thumbnails in the PDF viewer.
     */
    private BooleanProperty showThumbnails;

    /**
     * Gets the BooleanProperty representing whether to show thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for showing thumbnails.
     */
    @Override
    public BooleanProperty showThumbnailsProperty() {
        if (showThumbnails == null) {
            showThumbnails = new SimpleBooleanProperty(this, "showThumbnails", true);
        }
        return showThumbnails;
    }

    /**
     * Gets the value of whether to show thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be shown, false otherwise.
     */
    @Override
    public boolean isShowThumbnails() {
        return this.showThumbnailsProperty().get();
    }

    /**
     * Sets whether to show thumbnails in the PDF viewer.
     *
     * @param showThumbnails True to show thumbnails, false to hide them.
     */
    @Override
    public void setShowThumbnails(boolean showThumbnails) {
        this.showThumbnailsProperty().set(showThumbnails);
    }

    /**
     * A property representing whether to cache thumbnails in the PDF viewer.
     */
    private BooleanProperty cacheThumbnails;

    /**
     * Gets the BooleanProperty representing whether to cache thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for caching thumbnails.
     */
    @Override
    public BooleanProperty cacheThumbnailsProperty() {
        if (cacheThumbnails == null) {
            cacheThumbnails = new SimpleBooleanProperty(this, "cacheThumbnails", true);
        }
        return cacheThumbnails;
    }

    /**
     * Gets the value of whether to cache thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be cached, false otherwise.
     */
    @Override
    public boolean isCacheThumbnails() {
        return this.cacheThumbnailsProperty().get();
    }

    /**
     * Sets whether to cache thumbnails in the PDF viewer.
     *
     * @param cacheThumbnails True to cache thumbnails, false to disable caching.
     */
    @Override
    public void setCacheThumbnails(boolean cacheThumbnails) {
        this.cacheThumbnailsProperty().set(cacheThumbnails);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty minZoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    @Override
    public DoubleProperty minZoomFactorProperty() {
        if (minZoomFactor == null) {
            minZoomFactor = new SimpleDoubleProperty(this, "minZoomFactor", .25);
        }
        return minZoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    @Override
    public double getMinZoomFactor() {
        return this.minZoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param minZoomFactor The desired zoom factor.
     */
    @Override
    public void setMinZoomFactor(double minZoomFactor) {
        this.minZoomFactorProperty().set(minZoomFactor);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty maxZoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    @Override
    public DoubleProperty maxZoomFactorProperty() {
        if (maxZoomFactor == null) {
            maxZoomFactor = new SimpleDoubleProperty(this, "maxZoomFactor", 10);
        }
        return maxZoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    @Override
    public double getMaxZoomFactor() {
        return this.maxZoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param maxZoomFactor The desired zoom factor.
     */
    @Override
    public void setMaxZoomFactor(double maxZoomFactor) {
        this.maxZoomFactorProperty().set(maxZoomFactor);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty zoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    @Override
    public DoubleProperty zoomFactorProperty() {
        if (zoomFactor == null) {
            zoomFactor = new SimpleDoubleProperty(this, "zoomFactor", 1);
        }
        return zoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    @Override
    public double getZoomFactor() {
        return this.zoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param zoomFactor The desired zoom factor.
     */
    @Override
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactorProperty().set(zoomFactor);
    }

    /**
     * A property representing the rotation angle for the PDF pages.
     */
    private DoubleProperty pageRotation;

    /**
     * Gets the DoubleProperty representing the rotation angle for the PDF pages.
     *
     * @return The DoubleProperty for the page rotation angle.
     */
    @Override
    public DoubleProperty pageRotationProperty() {
        if (pageRotation == null) {
            pageRotation = new SimpleDoubleProperty(this, "pageRotation") {
                @Override
                public void set(double newValue) {
                    // super.set(newValue % 360);
                    var rotation = Math.floor(newValue / 90) * 90;
                    if (rotation < 0) rotation += 360;
                    if (rotation >= 360) rotation -= 360;
                    super.set(rotation % 360.0);
                    if (getDocument()!=null){
                        getDocument().setPageRotation(getPage(),rotation);
                    }
                }
            };
        }
        return pageRotation;
    }

    /**
     * Gets the rotation angle for the PDF pages.
     *
     * @return The current rotation angle in degrees.
     */
    @Override
    public double getPageRotation() {
        return this.pageRotationProperty().get();
    }

    /**
     * Sets the rotation angle for the PDF pages.
     *
     * @param pageRotation The desired rotation angle in degrees.
     */
    @Override
    public void setPageRotation(double pageRotation) {
        this.pageRotationProperty().set(pageRotation);
    }

    /**
     * A property representing the current page number in the PDF viewer.
     */
    private IntegerProperty page;

    /**
     * Gets the IntegerProperty representing the current page number in the PDF viewer.
     *
     * @return The IntegerProperty for the page number.
     */
    @Override
    public IntegerProperty pageProperty() {
        if (page == null) {
            page = new SimpleIntegerProperty(this, "page"){
                @Override
                public void set(int newValue) {
                    if (getDocument() != null && newValue >= 0 && newValue < getDocument().getNumberOfPages()){
                        super.set(newValue);
                        PageData data = getDocument().getPageRotation(newValue);
                        setPageRotation(data.getRotationAngle());
                    }
                }
            };
        }
        return page;
    }

    /**
     * Gets the current page number in the PDF viewer.
     *
     * @return The current page number.
     */
    @Override
    public int getPage() {
        return this.pageProperty().get();
    }

    /**
     * Sets the current page number in the PDF viewer.
     *
     * @param page The desired page number.
     */
    @Override
    public void setPage(int page) {
        this.pageProperty().set(page);
    }

    /**
     * An ObjectProperty representing the fit mode for a PDF page.
     * It can be set to one of the Fit enum values (VERTICAL, HORIZONTAL, NONE).
     */
    private ObjectProperty<Fit> fit;

    /**
     * Getter for the fit mode of the PDF page. Lazily initializes it to Fit.NONE if null.
     *
     * @return The Fit enum value representing the fit mode.
     */
    @Override
    public ObjectProperty<Fit> fitProperty() {
        if (fit == null) {
            fit = new SimpleObjectProperty<>(this, "fit", Fit.NONE);
        }
        return fit;
    }

    /**
     * Gets the fit mode for the PDF page.
     *
     * @return The Fit enum value representing the fit mode (VERTICAL, HORIZONTAL, NONE).
     */
    @Override
    public Fit getFit() {
        return fitProperty().get();
    }

    /**
     * Sets the fit mode for the PDF page.
     *
     * @param fit The Fit enum value to set as the fit mode.
     */
    @Override
    public void setFit(Fit fit) {
        fitProperty().set(fit);
    }


    /**
     * A FloatProperty representing the DPI (dots per inch) for thumbnail rendering.
     * The DPI value determines the quality and resolution of the rendered thumbnails.
     */
    private FloatProperty thumbnailRenderDpi;

    /**
     * Returns the FloatProperty for controlling the thumbnail rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 72f.
     *
     * @return The thumbnailRenderDpi property.
     */
    @Override
    public FloatProperty thumbnailRenderDpiProperty() {
        if (thumbnailRenderDpi == null) {
            thumbnailRenderDpi = new SimpleFloatProperty(this, "thumbnailRenderDpi", 72f);
        }
        return thumbnailRenderDpi;
    }

    /**
     * Gets the current value of the thumbnailRenderDpi property.
     *
     * @return The DPI value for thumbnail rendering.
     */
    @Override
    public float getThumbnailRenderDpi() {
        return this.thumbnailRenderDpiProperty().get();
    }

    /**
     * Sets the thumbnailRenderDpi property to the specified DPI value.
     *
     * @param thumbnailRenderDpi The new DPI value for thumbnail rendering.
     */
    @Override
    public void setThumbnailRenderDpi(float thumbnailRenderDpi) {
        this.thumbnailRenderDpiProperty().set(thumbnailRenderDpi);
    }

    /**
     * A property representing the size of thumbnails in the PDF viewer.
     */
    private DoubleProperty thumbnailSize;

    /**
     * Gets the DoubleProperty representing the size of thumbnails in the PDF viewer.
     * If the property is null, it initializes it with a default value.
     *
     * @return The DoubleProperty for the thumbnail size.
     */
    @Override
    public DoubleProperty thumbnailSizeProperty() {
        if (thumbnailSize == null) {
            thumbnailSize = new SimpleDoubleProperty(this, "thumbnailSize", 200d);
        }
        return thumbnailSize;
    }

    /**
     * Gets the value of the thumbnail size in the PDF viewer.
     *
     * @return The current thumbnail size.
     */
    @Override
    public double getThumbnailSize() {
        return this.thumbnailSizeProperty().get();
    }

    /**
     * Sets the size of thumbnails in the PDF viewer.
     *
     * @param thumbnailSize The desired thumbnail size.
     */
    @Override
    public void setThumbnailSize(double thumbnailSize) {
        this.thumbnailSizeProperty().set(thumbnailSize);
    }

    /**
     * A FloatProperty representing the DPI (dots per inch) for page rendering.
     * The DPI value determines the quality and resolution of the rendered pages.
     */
    private FloatProperty pageRenderDpi;

    /**
     * Returns the FloatProperty for controlling the page rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 300f.
     *
     * @return The pageRenderDpi property.
     */
    @Override
    public FloatProperty pageRenderDpiProperty() {
        if (pageRenderDpi == null) {
            pageRenderDpi = new SimpleFloatProperty(this, "pageRenderDpi", 300f);
        }
        return pageRenderDpi;
    }

    /**
     * Gets the current value of the pageRenderDpi property.
     *
     * @return The DPI value for page rendering.
     */
    @Override
    public float getPageRenderDpi() {
        return this.pageRenderDpiProperty().get();
    }

    /**
     * Sets the pageRenderDpi property to the specified DPI value.
     *
     * @param pageRenderDpi The new DPI value for page rendering.
     */
    @Override
    public void setPageRenderDpi(float pageRenderDpi) {
        this.pageRenderDpiProperty().set(pageRenderDpi);
    }

    /**
     * An object property representing the document to be displayed in the PDF viewer.
     */
    private ObjectProperty<Document> document;

    /**
     * Gets the ObjectProperty representing the document to be displayed in the PDF viewer.
     * If the property is null, it initializes it.
     *
     * @return The ObjectProperty for the document.
     */
    @Override
    public ObjectProperty<Document> documentProperty() {
        if (document == null) {
            document = new SimpleObjectProperty<>(this, "document", null);
        }
        return document;
    }

    /**
     * Gets the value of the document to be displayed in the PDF viewer.
     *
     * @return The current document.
     */
    @Override
    public Document getDocument() {
        return this.documentProperty().get();
    }

    /**
     * Sets the document to be displayed in the PDF viewer.
     *
     * @param document The document to be displayed.
     */
    @Override
    public void setDocument(Document document) {
        this.documentProperty().set(document);
    }


    /**
     * Represents the currently selected search result as an ObjectProperty.
     */
    private ObjectProperty<SearchResult> selectedSearchResult;

    /**
     * Gets the ObjectProperty for the currently selected search result.
     *
     * @return The ObjectProperty for the selected search result.
     */
    @Override
    public ObjectProperty<SearchResult> selectedSearchResultObjectProperty() {
        if (selectedSearchResult == null) {
            selectedSearchResult = new SimpleObjectProperty<>(this, "selectedSearchResult");
        }
        return selectedSearchResult;
    }

    /**
     * Gets the currently selected search result.
     *
     * @return The selected search result.
     */
    @Override
    public SearchResult getSelectedSearchResult() {
        return selectedSearchResultObjectProperty().get();
    }

    /**
     * Sets the currently selected search result.
     *
     * @param selectedSearchResult The search result to set as selected.
     */
    @Override
    public void setSelectedSearchResult(SearchResult selectedSearchResult) {
        this.selectedSearchResultObjectProperty().set(selectedSearchResult);
    }

    /**
     * Represents a list of search results as a ListProperty.
     */
    private ListProperty<SearchResult> searchResults;

    /**
     * Gets the ListProperty for the list of search results.
     *
     * @return The ListProperty for the search results.
     */
    @Override
    public ListProperty<SearchResult> searchResultsProperty() {
        if (searchResults == null) {
            searchResults = new SimpleListProperty<>(this, "searchResults", FXCollections.observableArrayList());
        }
        return searchResults;
    }

    /**
     * Gets the list of search results.
     *
     * @return The list of search results as an ObservableList.
     */
    @Override
    public ObservableList<SearchResult> getSearchResults() {
        return searchResultsProperty().get();
    }

    /**
     * Sets the list of search results.
     *
     * @param searchResults The list of search results to set.
     */
    @Override
    public void setSearchResults(ObservableList<SearchResult> searchResults) {
        this.searchResultsProperty().set(searchResults);
    }

    /**
     * Represents the color for displaying search results as an ObjectProperty.
     */
    private ObjectProperty<Color> searchResultColor;

    /**
     * Gets the ObjectProperty for the color used to display search results.
     *
     * @return The ObjectProperty for the search result color.
     */
    @Override
    public ObjectProperty<Color> searchResultColorProperty() {
        if (searchResultColor == null) {
            searchResultColor = new SimpleObjectProperty<>(this, "searchResultColor", Color.RED);
        }
        return searchResultColor;
    }

    /**
     * Gets the color used for displaying search results.
     *
     * @return The search result color.
     */
    @Override
    public Color getSearchResultColor() {
        return searchResultColorProperty().get();
    }

    /**
     * Sets the color for displaying search results.
     *
     * @param color The color to set for search results.
     */
    @Override
    public void setSearchResultColor(Color color) {
        this.searchResultColorProperty().set(color);
    }

    /**
     * Represents the search text as a StringProperty.
     */
    private StringProperty searchText;

    /**
     * Gets the StringProperty for the search text.
     *
     * @return The StringProperty for the search text.
     */
    @Override
    public StringProperty searchTextProperty() {
        if (searchText == null) {
            searchText = new SimpleStringProperty(this, "searchText");
        }
        return searchText;
    }

    /**
     * Gets the search text.
     *
     * @return The search text.
     */
    @Override
    public String getSearchText() {
        return searchTextProperty().get();
    }

    /**
     * Sets the search text.
     *
     * @param searchText The text to set as the search text.
     */
    @Override
    public void setSearchText(String searchText) {
        this.searchTextProperty().set(searchText);
    }


    /**
     * Represents the state of navigation buttons (e.g., First, Previous, Next, Last).
     */
    private ObjectProperty<NavButtonState> navButtonsState;

    /**
     * Gets the ObjectProperty for navButtonsState and initializes it with DISABLE_BOTH state by default.
     *
     * @return The ObjectProperty for navButtonsState.
     */
    @Override
    public ObjectProperty<NavButtonState> navButtonsStateProperty(){
        if (navButtonsState == null){
            navButtonsState = new SimpleObjectProperty<>(this, "navButtonsState", NavButtonState.DISABLE_BOTH);
        }
        return navButtonsState;
    }

    /**
     * Gets the current state of navigation buttons.
     *
     * @return The current state of navigation buttons.
     */
    @Override
    public NavButtonState getNavButtonsState() {
        return navButtonsStateProperty().get();
    }

    /**
     * Sets the state of navigation buttons.
     *
     * @param navButtonsState The state to set for navigation buttons.
     */
    @Override
    public void setNavButtonsState(NavButtonState navButtonsState) {
        this.navButtonsStateProperty().set(navButtonsState);
    }

    /**
     * Property representing the current screen mode (e.g., full-screen or normal).
     */
    private ObjectProperty<ScreenMode> screenMode;

    /**
     * Gets the {@link ObjectProperty} for the screen mode.
     *
     * @return The object property for the screen mode.
     */
    @Override
    public ObjectProperty<ScreenMode> screenModeProperty(){
        if (screenMode == null){
            screenMode = new SimpleObjectProperty<>(this, "screenMode", ScreenMode.NORMAL);
        }
        return screenMode;
    }

    /**
     * Gets the current screen mode.
     *
     * @return The current screen mode.
     */
    @Override
    public ScreenMode getScreenMode() {
        return screenModeProperty().get();
    }

    /**
     * Sets the screen mode to the specified value.
     *
     * @param screenMode The new screen mode to set.
     */
    @Override
    public void setScreenMode(ScreenMode screenMode) {
        this.screenModeProperty().set(screenMode);
    }

    /**
     * Represents the property determining whether full-screen mode is allowed.
     */
    private BooleanProperty allowFullScreen;

    /**
     * Gets the BooleanProperty for allowing full-screen mode.
     *
     * @return The BooleanProperty representing the allowFullScreen property.
     */
    @Override
    public BooleanProperty allowFullScreenProperty() {
        if (allowFullScreen == null) {
            allowFullScreen = new SimpleBooleanProperty(this, "allowFullScreen", false);
        }
        return allowFullScreen;
    }

    /**
     * Gets the value of allowFullScreen.
     *
     * @return The current value of the allowFullScreen property.
     */
    @Override
    public boolean isAllowFullScreen() {
        return allowFullScreenProperty().get();
    }

    /**
     * Sets the value of allowFullScreen.
     *
     * @param allowFullScreen The new value for the allowFullScreen property.
     */
    @Override
    public void setAllowFullScreen(boolean allowFullScreen) {
        this.allowFullScreenProperty().set(allowFullScreen);
    }


    /**
     * Object property representing the color scheme used for rendering pages.
     */
    public ObjectProperty<ColorScheme> pageColorScheme;

    /**
     * Object property representing the color scheme used for rendering pages.
     *
     * @return the object property for the page color scheme
     */
    @Override
    public ObjectProperty<ColorScheme> pageColorSchemeProperty() {
        if (pageColorScheme == null){
            pageColorScheme = new SimpleObjectProperty<>(this, "pageColorScheme", ColorScheme.RGB);
        }
        return pageColorScheme;
    }


    /**
     * Getter for the page color scheme.
     *
     * @return the current page color scheme
     */
    @Override
    public ColorScheme getPageColorScheme() {
        return pageColorSchemeProperty().get();
    }

    /**
     * Setter for the page color scheme.
     *
     * @param pageColorScheme the new page color scheme to set
     */
    @Override
    public void setPageColorScheme(ColorScheme pageColorScheme) {
        this.pageColorSchemeProperty().set(pageColorScheme);
    }

    /**
     * Represents the operation property.
     */
    private ObjectProperty<Operation> operation;

    /**
     * Gets the operation property.
     *
     * @return The operation property.
     */
    @Override
    public ObjectProperty<Operation> operationProperty(){
        if (operation == null){
            operation = new SimpleObjectProperty<>(this,"operation", Operation.NONE);
        }
        return operation;
    }

    /**
     * Gets the current operation.
     *
     * @return The current operation.
     */
    @Override
    public Operation getOperation() {
        return operationProperty().get();
    }

    /**
     * Sets the operation.
     *
     * @param operation The operation to set.
     */
    @Override
    public void setOperation(Operation operation) {
        this.operationProperty().set(operation);
    }

    /**
     * Represents the property for the page view mode in the viewer.
     */
    private ObjectProperty<PageViewMode> pageViewMode;

    /**
     * Gets the property for the page view mode.
     *
     * @return The property for the page view mode.
     */
    @Override
    public ObjectProperty<PageViewMode> pageViewModeProperty(){
        if (pageViewMode == null){
            pageViewMode = new SimpleObjectProperty<>(this,"pageViewMode", PageViewMode.PAGE_BY_PAGE);
        }
        return pageViewMode;
    }

    /**
     * Gets the current page view mode.
     *
     * @return The current page view mode.
     */
    @Override
    public PageViewMode getPageViewMode() {
        return pageViewModeProperty().get();
    }

    /**
     * Sets the page view mode.
     *
     * @param pageViewMode The new page view mode.
     */
    @Override
    public void setPageViewMode(PageViewMode pageViewMode) {
        this.pageViewModeProperty().set(pageViewMode);
    }


    /**
     * ObjectProperty representing the status of a search panel.
     */
    private ObjectProperty<SearchPanelStatus> searchPanelStatus;

    /**
     * Retrieves the ObjectProperty representing the search panel status.
     *
     * @return The ObjectProperty for search panel status.
     */
    @Override
    public ObjectProperty<SearchPanelStatus> searchPanelStatusProperty() {
        if (searchPanelStatus == null) {
            searchPanelStatus = new SimpleObjectProperty<>(this, "searchPanelStatus", SearchPanelStatus.CLOSED);
        }
        return searchPanelStatus;
    }

    /**
     * Gets the current status of the search panel.
     *
     * @return The current SearchPanelStatus.
     */
    @Override
    public SearchPanelStatus getSearchPanelStatus() {
        return searchPanelStatusProperty().get();
    }

    /**
     * Sets the status of the search panel.
     *
     * @param searchPanelStatus The new SearchPanelStatus.
     */
    @Override
    public void setSearchPanelStatus(SearchPanelStatus searchPanelStatus) {
        this.searchPanelStatusProperty().set(searchPanelStatus);
    }


    /*
     * =================================================================================================================
     *
     *                                           E N D  P R O P E R T I E S
     *
     * =================================================================================================================
     */


    /**
     * Constructs a new PdfViewerPane.
     */
    public PdfAbstractViewerImpl() {
        toolbar = new PdfToolBar(this);
        pdfSearchPanel = new PdfSearchPanel(this);
        widePane = new AnchorPane();
        leftPane = new AnchorPane();
        centerPane = new AnchorPane();
        rightPane = new AnchorPane();
        loadPageView(getPageView());

        /*
         * Initialize method call.
         */
        initialize();
    }


    /**
     * Initializes the control. Override this method to add custom initialization logic.
     */
    private void initialize(){
        getStyleClass().add("pdf-viewer");
        getStylesheets().add(getUserAgentStylesheet());

        AnchorPane.setLeftAnchor(toolbar, 0.0);
        AnchorPane.setRightAnchor(toolbar, 0.0);
        AnchorPane.setTopAnchor(toolbar, 0.0);
        toolbar.setMaxHeight(50.0);
        toolbar.setPrefHeight(50.0);

        AnchorPane.setBottomAnchor(widePane, 0.0);
        AnchorPane.setLeftAnchor(widePane, 0.0);
        AnchorPane.setRightAnchor(widePane, 0.0);
        AnchorPane.setTopAnchor(widePane, 50.0);

        widePane.getStyleClass().add("pdf-viewer-split-pane");

        leftPane.setMinHeight(0.0);
        leftPane.setMinWidth(300.0);
        leftPane.getStyleClass().add("pdf-viewer-left-pane");


        centerPane.getStyleClass().add("pdf-viewer-center-pane");

        rightPane.setPrefWidth(0.0);
        rightPane.getStyleClass().add("pdf-viewer-right-pane");

        getChildren().add(toolbar);


        //Left
        leftPane.setPrefWidth(0);
        AnchorPane.setLeftAnchor(leftPane, 0d);
        AnchorPane.setTopAnchor(leftPane, 0d);
        AnchorPane.setBottomAnchor(leftPane, 0d);


        AnchorPane.setLeftAnchor(centerPane, 300d);
        AnchorPane.setTopAnchor(centerPane, 0d);
        AnchorPane.setBottomAnchor(centerPane, 0d);
        AnchorPane.setRightAnchor(centerPane, 0d);


        //Right
        rightPane.setPrefWidth(0);
        AnchorPane.setTopAnchor(rightPane, 0d);
        AnchorPane.setBottomAnchor(rightPane, 0d);
        AnchorPane.setRightAnchor(rightPane, 0d);

        widePane.getChildren().addAll(leftPane, centerPane, rightPane);

        AnchorPane.setLeftAnchor(pdfSearchPanel, 0d);
        AnchorPane.setTopAnchor(pdfSearchPanel, 0d);
        AnchorPane.setBottomAnchor(pdfSearchPanel, 0d);
        AnchorPane.setRightAnchor(pdfSearchPanel, 0d);
        rightPane.getChildren().add(pdfSearchPanel);

        getChildren().add(widePane);

        initializeEvents();
    }

    /**
     * Initializes event handlers for this control. Override this method to add custom event handling logic.
     */
    private void initializeEvents(){
        /*
         * Doc listener
         */
        documentProperty().addListener((obs, old, document) -> {
            if (old != null){
                try {
                    old.close();
                } catch (IOException e) {
                    //Notify here if something goes wrong
                    throw new RuntimeException(e);
                }
            }
        });

        /*
         * Change viewPort
         */
        currentViewPortProperty().addListener((observable, oldValue, newValue) ->
                switchViewport(null, getPage()));


        /*
         * Search panel
         */
        handleSearchPanel(getSearchPanelStatus());
        searchPanelStatusProperty().addListener((obs, o, status) -> handleSearchPanel(status));

        /*
         * Page mode
         */
        pageViewModeProperty().addListener((obs, o, mode) -> {
            switch (mode){
                case CONTINUOUS -> {
                   // setPageView(new ContinuousPageViewer(this));
                    setPageView(new SinglePageViewer(this));
                }
                case PAGE_BY_PAGE -> {
                    setPageView(new SinglePageViewer(this));
                }
            }
        });

        /*
         * Page View
         */
        pageViewProperty().addListener((obs, o, view) -> loadPageView(view));
    }


    private void handleSearchPanel(SearchPanelStatus status){
        if (Objects.requireNonNull(status) == SearchPanelStatus.OPEN) {
            Timeline t = Animation.doResizeAnimated(rightPane, centerPane, 360d, 200, false);
            t.setOnFinished(event -> {

            });
            t.play();
        } else {
            Timeline t = Animation.doResizeAnimated(rightPane, centerPane, 0d, 200, false);
            t.setOnFinished(event -> {
                //pdfSearchPanel.setOpacity(0);
            });
            t.play();
        }
    }

    /**
     * Loads and displays the specified PageView in the PdfViewerPane.
     *
     * @param pageView The PageView to load and display.
     */
    private void loadPageView(PageView pageView){
        AnchorPane.setLeftAnchor((Node) pageView, 0d);
        AnchorPane.setTopAnchor((Node) pageView, 0d);
        AnchorPane.setRightAnchor((Node) pageView, 0d);
        AnchorPane.setBottomAnchor((Node) pageView, 0d);
        this.centerPane.getChildren().clear();
        this.centerPane.getChildren().add((Node) pageView);
    }


    /**
     * Gets the Executor instance used for managing asynchronous tasks.
     *
     * @return The Executor instance.
     */
    @Override
    public Executor getExecutor() {
        if (EXECUTOR == null) {
            EXECUTOR= Executors.newFixedThreadPool(4,r->{
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });
        }
        return EXECUTOR;
    }


    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees counterclockwise (to the left).
     */
    @Override
    public void rotateLeft() {
        setPageRotation(getPageRotation() - 90);
    }

    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees clockwise (to the right).
     */
    @Override
    public void rotateRight() {
        setPageRotation(getPageRotation() + 90);
    }

    /**
     * Navigates to the next page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    @Override
    public boolean gotoNextPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(Math.min(getDocument().getNumberOfPages() - 1, getPage() + 1));
        return currentPage != getPage();
    }

    /**
     * Navigates to the previous page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    @Override
    public boolean gotoPreviousPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(Math.max(0, getPage() - 1));
        return currentPage != getPage();
    }

    /**
     * Navigates to the last page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    @Override
    public boolean gotoLastPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(getDocument().getNumberOfPages() - 1);
        return currentPage != getPage();
    }

    /**
     * Navigates to the first page of the document.
     *
     * @return True if the navigation to the first page was successful, false otherwise.
     */
    @Override
    public boolean gotoFirstPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(0);
        return currentPage != getPage();
    }


    /**
     * Opens a file dialog to select and open a PDF document.
     * Displays a file dialog, allows the user to choose a PDF file, and loads it if selected.
     */
    @Override
    public void open(){
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open");
        chooser.getExtensionFilters().add(Assets.PDF_EXTENSION_FILTER);
        final File file= chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;
        unload();
        load(file);
    }

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param supplier A supplier providing the document to load.
     * @throws NullPointerException If the supplier is null.
     */
    @Override
    public void load(Supplier<Document> supplier) {
        Objects.requireNonNull(supplier, "Supplier can not be null.");
        load(supplier.get());
    }

    /**
     * Loads a document from an InputStream into the PDF viewer control.
     *
     * @param stream The InputStream containing the document.
     */
    @Override
    public void load(InputStream stream) {
        load(() -> {
            try {
                return new PdfDocument(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Loads a document from a File into the PDF viewer control.
     *
     * @param file The File representing the document.
     */
    @Override
    public void load(File file) {
        load(() -> {
            try {
                return new PdfDocument(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param document The document to load.
     * @throws NullPointerException If the document is null.
     */
    @Override
    public void load(Document document) {
        Objects.requireNonNull(document, "Document can not be null");
        setDocument(document);
    }

    /**
     * Unloads the current document from the PDF viewer control and resets various settings.
     */
    @Override
    public void unload() {
        setDocument(null);
        setZoomFactor(1);
        setRotate(0);
    }

    /**
     * Resets the viewer to its initial state by setting zoom factor to 1,
     * rotation to 0, and clearing the page index input.
     */
    private void resetViewer(){
        setFit(Fit.NONE);
        setZoomFactor(1);
        setRotate(0);
    }




    /*
     * =================================================================================================================
     *
     *                                   H E L P E R S
     *
     * =================================================================================================================
     */

    /**
     * An ObjectProperty representing the current viewport for displaying the content.
     * The viewport is defined by a Rectangle2D representing the visible region of the content.
     */
    private ObjectProperty<Rectangle2D> currentViewPort;

    /**
     * Returns the ObjectProperty for controlling the current viewport.
     * If not initialized, a new ObjectProperty is created with a default viewport.
     *
     * @return The currentViewPort property.
     */
    @Override
    public ObjectProperty<Rectangle2D> currentViewPortProperty() {
        if (currentViewPort == null) {
            currentViewPort = new SimpleObjectProperty<>(
                    new Rectangle2D(0.0, 0.0, 1.0, 1.0)
            );
        }
        return currentViewPort;
    }

    /**
     * Gets the current viewport, represented as a Rectangle2D.
     *
     * @return The current viewport.
     */
    @Override
    public Rectangle2D getCurrentViewPort() {
        return this.currentViewPortProperty().get();
    }

    /**
     * Sets the current viewport to the specified Rectangle2D.
     *
     * @param currentViewPort The new viewport to set.
     */
    @Override
    public void setCurrentViewPort(Rectangle2D currentViewPort) {
        this.currentViewPortProperty().set(currentViewPort);
    }


    /**
     * Switches the viewport for the document being displayed.
     *
     * @param oldPage The old page being displayed (can be null).
     * @param newPage The new page to be displayed (can be null).
     */
    @Override
    public void switchViewport(Integer oldPage, Integer newPage) {
        if (getDocument() == null){
            return;
        }
        //FIXED: When new doc added we can no longer set viewport for old pages
        if (oldPage != null && getDocument().getNumberOfPages()>oldPage) {
            getDocument().setViewport(oldPage, null);
        }
        if (newPage != null) {
            Rectangle2D vp = getCurrentViewPort();
            Rectangle2D vpn = new Rectangle2D(
                    Math.max(vp.getMinX(), 0.0),
                    Math.max(vp.getMinY(), 0.0),
                    Math.min(vp.getWidth(), 1.0),
                    Math.min(vp.getHeight(), 1.0)
            );
            if (!vpn.equals(new Rectangle2D(0.0, 0.0, 1.0, 1.0))) {
                getDocument().setViewport(newPage, vpn);
            } else {
                getDocument().setViewport(newPage, null);
            }
        }
    }


    /**
     * Returns the path to the custom CSS stylesheet for this control.
     * Override this method to specify a custom stylesheet for styling the control's appearance.
     *
     * @return The path to the custom CSS stylesheet file.
     */
    @Override
    public String getUserAgentStylesheet() {
        return Assets.load("/xss/it/ultimate/pdf/viewer/css/viewer.css").toExternalForm();
    }

    /**
     * Returns the ResourceBundle containing icon resources for this control.
     * Override this method to provide a custom ResourceBundle for icons used by the control.
     *
     * @return The ResourceBundle containing icon resources.
     */
    @Override
    public ResourceBundle getIconsBundle() {
        return iconsBundle;
    }

}