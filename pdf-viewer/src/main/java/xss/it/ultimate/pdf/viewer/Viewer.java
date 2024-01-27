package xss.it.ultimate.pdf.viewer;

import com.sun.internals.ctrl.PageView;
import com.sun.internals.ctrl.SinglePageViewer;
import com.sun.internals.document.Document;
import com.sun.internals.enums.Fit;
import com.sun.internals.enums.NavButtonState;
import com.sun.internals.enums.ScreenMode;
import com.sun.internals.text.SearchResult;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public interface Viewer {

    /**
     * Gets the property for the currently selected page view.
     *
     * @return The ObjectProperty for the page view.
     */
    ObjectProperty<PageView> pageViewProperty();

    /**
     * Gets the currently selected page view.
     *
     * @return The currently selected page view.
     */
    PageView getPageView();

    /**
     * Sets the currently selected page view.
     *
     * @param pageView The page view to set as the current selection.
     */
    void setPageView(PageView pageView);

    /**
     * Gets the BooleanProperty representing whether to show thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for showing thumbnails.
     */
    BooleanProperty showThumbnailsProperty();

    /**
     * Gets the value of whether to show thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be shown, false otherwise.
     */
    boolean isShowThumbnails();
    /**
     * Sets whether to show thumbnails in the PDF viewer.
     *
     * @param showThumbnails True to show thumbnails, false to hide them.
     */
    void setShowThumbnails(boolean showThumbnails);

    /**
     * Gets the BooleanProperty representing whether to cache thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for caching thumbnails.
     */
    BooleanProperty cacheThumbnailsProperty();

    /**
     * Gets the value of whether to cache thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be cached, false otherwise.
     */
    boolean isCacheThumbnails();

    /**
     * Sets whether to cache thumbnails in the PDF viewer.
     *
     * @param cacheThumbnails True to cache thumbnails, false to disable caching.
     */
    void setCacheThumbnails(boolean cacheThumbnails);
    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    DoubleProperty minZoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    double getMinZoomFactor();

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param minZoomFactor The desired zoom factor.
     */
    void setMinZoomFactor(double minZoomFactor);

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    DoubleProperty maxZoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    double getMaxZoomFactor();
    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param maxZoomFactor The desired zoom factor.
     */
    void setMaxZoomFactor(double maxZoomFactor);

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    DoubleProperty zoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    double getZoomFactor();
    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param zoomFactor The desired zoom factor.
     */
    void setZoomFactor(double zoomFactor);

    /**
     * Gets the DoubleProperty representing the rotation angle for the PDF pages.
     *
     * @return The DoubleProperty for the page rotation angle.
     */
    DoubleProperty pageRotationProperty();

    /**
     * Gets the rotation angle for the PDF pages.
     *
     * @return The current rotation angle in degrees.
     */
    double getPageRotation();
    /**
     * Sets the rotation angle for the PDF pages.
     *
     * @param pageRotation The desired rotation angle in degrees.
     */
    void setPageRotation(double pageRotation);

    /**
     * Gets the IntegerProperty representing the current page number in the PDF viewer.
     *
     * @return The IntegerProperty for the page number.
     */
    IntegerProperty pageProperty();
    /**
     * Gets the current page number in the PDF viewer.
     *
     * @return The current page number.
     */
    int getPage();

    /**
     * Sets the current page number in the PDF viewer.
     *
     * @param page The desired page number.
     */
    void setPage(int page);

    /**
     * Getter for the fit mode of the PDF page. Lazily initializes it to Fit.NONE if null.
     *
     * @return The Fit enum value representing the fit mode.
     */
    ObjectProperty<Fit> fitProperty();

    /**
     * Gets the fit mode for the PDF page.
     *
     * @return The Fit enum value representing the fit mode (VERTICAL, HORIZONTAL, NONE).
     */
    Fit getFit();

    /**
     * Sets the fit mode for the PDF page.
     *
     * @param fit The Fit enum value to set as the fit mode.
     */
    void setFit(Fit fit);

    /**
     * Returns the FloatProperty for controlling the thumbnail rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 72f.
     *
     * @return The thumbnailRenderDpi property.
     */
    FloatProperty thumbnailRenderDpiProperty();

    /**
     * Gets the current value of the thumbnailRenderDpi property.
     *
     * @return The DPI value for thumbnail rendering.
     */
    float getThumbnailRenderDpi();
    /**
     * Sets the thumbnailRenderDpi property to the specified DPI value.
     *
     * @param thumbnailRenderDpi The new DPI value for thumbnail rendering.
     */
    void setThumbnailRenderDpi(float thumbnailRenderDpi);

    /**
     * Gets the DoubleProperty representing the size of thumbnails in the PDF viewer.
     * If the property is null, it initializes it with a default value.
     *
     * @return The DoubleProperty for the thumbnail size.
     */
    DoubleProperty thumbnailSizeProperty();

    /**
     * Gets the value of the thumbnail size in the PDF viewer.
     *
     * @return The current thumbnail size.
     */
    double getThumbnailSize();

    /**
     * Sets the size of thumbnails in the PDF viewer.
     *
     * @param thumbnailSize The desired thumbnail size.
     */
    void setThumbnailSize(double thumbnailSize);

    /**
     * Returns the FloatProperty for controlling the page rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 300f.
     *
     * @return The pageRenderDpi property.
     */
    FloatProperty pageRenderDpiProperty();

    /**
     * Gets the current value of the pageRenderDpi property.
     *
     * @return The DPI value for page rendering.
     */
    float getPageRenderDpi();

    /**
     * Sets the pageRenderDpi property to the specified DPI value.
     *
     * @param pageRenderDpi The new DPI value for page rendering.
     */
    void setPageRenderDpi(float pageRenderDpi);

    /**
     * Gets the ObjectProperty representing the document to be displayed in the PDF viewer.
     * If the property is null, it initializes it.
     *
     * @return The ObjectProperty for the document.
     */
    ObjectProperty<Document> documentProperty();

    /**
     * Gets the value of the document to be displayed in the PDF viewer.
     *
     * @return The current document.
     */
    Document getDocument();

    /**
     * Sets the document to be displayed in the PDF viewer.
     *
     * @param document The document to be displayed.
     */
    void setDocument(Document document);

    /**
     * Gets the ObjectProperty for the currently selected search result.
     *
     * @return The ObjectProperty for the selected search result.
     */
    ObjectProperty<SearchResult> selectedSearchResultObjectProperty();

    /**
     * Gets the currently selected search result.
     *
     * @return The selected search result.
     */
    SearchResult getSelectedSearchResult();

    /**
     * Sets the currently selected search result.
     *
     * @param selectedSearchResult The search result to set as selected.
     */
    void setSelectedSearchResult(SearchResult selectedSearchResult);

    /**
     * Gets the ListProperty for the list of search results.
     *
     * @return The ListProperty for the search results.
     */
    ListProperty<SearchResult> searchResultsProperty();

    /**
     * Gets the list of search results.
     *
     * @return The list of search results as an ObservableList.
     */
    ObservableList<SearchResult> getSearchResults();

    /**
     * Sets the list of search results.
     *
     * @param searchResults The list of search results to set.
     */
    void setSearchResults(ObservableList<SearchResult> searchResults);
    /**
     * Gets the ObjectProperty for the color used to display search results.
     *
     * @return The ObjectProperty for the search result color.
     */
    ObjectProperty<Color> searchResultColorProperty();

    /**
     * Gets the color used for displaying search results.
     *
     * @return The search result color.
     */
    Color getSearchResultColor();

    /**
     * Sets the color for displaying search results.
     *
     * @param color The color to set for search results.
     */
    void setSearchResultColor(Color color);
    /**
     * Gets the StringProperty for the search text.
     *
     * @return The StringProperty for the search text.
     */
    StringProperty searchTextProperty();

    /**
     * Gets the search text.
     *
     * @return The search text.
     */
    String getSearchText();

    /**
     * Sets the search text.
     *
     * @param searchText The text to set as the search text.
     */
    void setSearchText(String searchText);



    /**
     * Gets the ObjectProperty for navButtonsState and initializes it with DISABLE_BOTH state by default.
     *
     * @return The ObjectProperty for navButtonsState.
     */
    ObjectProperty<NavButtonState> navButtonsStateProperty();

    /**
     * Gets the current state of navigation buttons.
     *
     * @return The current state of navigation buttons.
     */
    NavButtonState getNavButtonsState();
    /**
     * Sets the state of navigation buttons.
     *
     * @param navButtonsState The state to set for navigation buttons.
     */
    void setNavButtonsState(NavButtonState navButtonsState);

    /**
     * Returns the ResourceBundle containing icon resources for this control.
     * Override this method to provide a custom ResourceBundle for icons used by the control.
     *
     * @return The ResourceBundle containing icon resources.
     */
    ResourceBundle getIconsBundle();


    /**
     * Gets the Executor instance used for managing asynchronous tasks.
     *
     * @return The Executor instance.
     */
    Executor getExecutor();


    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees counterclockwise (to the left).
     */
    void rotateLeft();

    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees clockwise (to the right).
     */
    void rotateRight();

    /**
     * Navigates to the next page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    boolean gotoNextPage();

    /**
     * Navigates to the previous page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    boolean gotoPreviousPage();

    /**
     * Navigates to the last page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    boolean gotoLastPage();

    /**
     * Navigates to the first page of the document.
     *
     * @return True if the navigation to the first page was successful, false otherwise.
     */
    boolean gotoFirstPage();


    /**
     * Opens a file dialog to select and open a PDF document.
     * Displays a file dialog, allows the user to choose a PDF file, and loads it if selected.
     */
    void open();

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param supplier A supplier providing the document to load.
     * @throws NullPointerException If the supplier is null.
     */
    void load(Supplier<Document> supplier);

    /**
     * Loads a document from an InputStream into the PDF viewer control.
     *
     * @param stream The InputStream containing the document.
     */
    void load(InputStream stream);

    /**
     * Loads a document from a File into the PDF viewer control.
     *
     * @param file The File representing the document.
     */
    void load(File file);

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param document The document to load.
     * @throws NullPointerException If the document is null.
     */
    void load(Document document);

    /**
     * Unloads the current document from the PDF viewer control and resets various settings.
     */
    void unload();

    /**
     * Sets the current viewport to the specified Rectangle2D.
     *
     * @param currentViewPort The new viewport to set.
     */
    void setCurrentViewPort(Rectangle2D currentViewPort);

    /**
     * Gets the current viewport, represented as a Rectangle2D.
     *
     * @return The current viewport.
     */
    Rectangle2D getCurrentViewPort();

    /**
     * Returns the ObjectProperty for controlling the current viewport.
     * If not initialized, a new ObjectProperty is created with a default viewport.
     *
     * @return The currentViewPort property.
     */
    ObjectProperty<Rectangle2D> currentViewPortProperty();

    /**
     * Switches the viewport for the document being displayed.
     *
     * @param oldPage The old page being displayed (can be null).
     * @param newPage The new page to be displayed (can be null).
     */
    void switchViewport(Integer oldPage, Integer newPage);

    /**
     * Gets the {@link ObjectProperty} for the screen mode.
     *
     * @return The object property for the screen mode.
     */
    ObjectProperty<ScreenMode> screenModeProperty();
    /**
     * Gets the current screen mode.
     *
     * @return The current screen mode.
     */
    ScreenMode getScreenMode();

    /**
     * Sets the screen mode to the specified value.
     *
     * @param screenMode The new screen mode to set.
     */
    void setScreenMode(ScreenMode screenMode);
}
