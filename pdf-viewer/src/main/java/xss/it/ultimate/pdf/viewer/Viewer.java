package xss.it.ultimate.pdf.viewer;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.ultimate.pdf.viewer.enums.Fit;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;
import xss.it.ultimate.pdf.viewer.enums.ScreenMode;
import xss.it.ultimate.pdf.viewer.text.SearchResult;

import java.io.File;
import java.io.InputStream;

/**
 * @author XDSSWAR
 * Created on 01/27/2024
 */
public interface Viewer {
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
     * Unloads the current document from the PDF viewer control and resets various settings.
     */
    void unload();

    /**
     * Opens the print preview dialog (also available via Ctrl+P). Does nothing
     * when no document is loaded.
     */
    void print();

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


    /**
     * Gets the BooleanProperty for allowing full-screen mode.
     *
     * @return The BooleanProperty representing the allowFullScreen property.
     */
    BooleanProperty allowFullScreenProperty();

    /**
     * Gets the value of allowFullScreen.
     *
     * @return The current value of the allowFullScreen property.
     */
    boolean isAllowFullScreen();
    /**
     * Sets the value of allowFullScreen.
     *
     * @param allowFullScreen The new value for the allowFullScreen property.
     */
    void setAllowFullScreen(boolean allowFullScreen);

    /**
     * Gets the BooleanProperty controlling whether the toolbar is shown.
     *
     * @return The enableToolbar property.
     */
    BooleanProperty enableToolbarProperty();

    /**
     * Gets whether the toolbar is shown.
     *
     * @return {@code true} if the toolbar is shown, {@code false} otherwise.
     */
    boolean isEnableToolbar();

    /**
     * Sets whether the toolbar is shown. When {@code false}, the toolbar is
     * hidden and the content area expands to fill the freed space at the top.
     *
     * @param enableToolbar {@code true} to show the toolbar, {@code false} to hide it.
     */
    void setEnableToolbar(boolean enableToolbar);

    /**
     * Gets the BooleanProperty controlling whether search is enabled.
     *
     * @return The enableSearch property.
     */
    BooleanProperty enableSearchProperty();

    /**
     * Gets whether search is enabled.
     *
     * @return {@code true} if search is enabled, {@code false} otherwise.
     */
    boolean isEnableSearch();

    /**
     * Sets whether search is enabled. When {@code false}, the toolbar search
     * button is hidden, the right-click "Find" item (and its Ctrl+F accelerator)
     * is disabled, and any open search panel is closed.
     *
     * @param enableSearch {@code true} to enable search, {@code false} to disable it.
     */
    void setEnableSearch(boolean enableSearch);

    /**
     * Retrieves a handle to the loaded PDF document from the nfx-pdfium engine.
     *
     * @return The {@link PdfDocument} representing the PDF document, or
     *         {@code null} if no document is loaded.
     */
    PdfDocument getDocument();

    /**
     * Gets the property for the page view mode.
     *
     * @return The property for the page view mode.
     */
    ObjectProperty<PageViewMode> pageViewModeProperty();

    /**
     * Gets the current page view mode.
     *
     * @return The current page view mode.
     */
    PageViewMode getPageViewMode();

    /**
     * Sets the page view mode.
     *
     * @param pageViewMode The new page view mode.
     */
    void setPageViewMode(PageViewMode pageViewMode);

    /**
     * Gets the property for the number of columns in the continuous page view.
     * Defaults to 1; set to 2 for a facing-page (book) layout.
     *
     * @return The property for the continuous-view column count.
     */
    IntegerProperty pageColumnsProperty();

    /**
     * Gets the number of columns used by the continuous page view.
     *
     * @return The column count (>= 1).
     */
    int getPageColumns();

    /**
     * Sets the number of columns for the continuous page view (1 = single,
     * 2 = facing pages). Values below 1 are treated as 1.
     *
     * @param pageColumns The desired column count.
     */
    void setPageColumns(int pageColumns);
}
