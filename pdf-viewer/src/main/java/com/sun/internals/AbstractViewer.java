package com.sun.internals;

import com.sun.internals.enums.Operation;
import com.sun.internals.enums.SearchPanelStatus;
import xss.it.ultimate.pdf.viewer.controls.PageView;
import com.sun.internals.document.Document;
import xss.it.ultimate.pdf.viewer.enums.Fit;
import com.sun.internals.enums.NavButtonState;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;
import xss.it.ultimate.pdf.viewer.enums.ScreenMode;
import xss.it.ultimate.pdf.viewer.text.SearchResult;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import xss.it.nfx.pdfium.text.PdfSearchResult;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public abstract class AbstractViewer extends AnchorPane {

    /**
     * Gets the property for the currently selected page view.
     *
     * @return The ObjectProperty for the page view.
     */
    public abstract ObjectProperty<PageView> pageViewProperty();

    /**
     * Gets the currently selected page view.
     *
     * @return The currently selected page view.
     */
    public abstract PageView getPageView();

    /**
     * Sets the currently selected page view.
     *
     * @param pageView The page view to set as the current selection.
     */
    public abstract void setPageView(PageView pageView);

    /**
     * Gets the BooleanProperty representing whether to show thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for showing thumbnails.
     */
    public abstract BooleanProperty showThumbnailsProperty();

    /**
     * Gets the value of whether to show thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be shown, false otherwise.
     */
    public abstract boolean isShowThumbnails();
    /**
     * Sets whether to show thumbnails in the PDF viewer.
     *
     * @param showThumbnails True to show thumbnails, false to hide them.
     */
    public abstract void setShowThumbnails(boolean showThumbnails);

    /**
     * Gets the BooleanProperty representing whether to cache thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for caching thumbnails.
     */
    public abstract BooleanProperty cacheThumbnailsProperty();

    /**
     * Gets the value of whether to cache thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be cached, false otherwise.
     */
    public abstract boolean isCacheThumbnails();

    /**
     * Sets whether to cache thumbnails in the PDF viewer.
     *
     * @param cacheThumbnails True to cache thumbnails, false to disable caching.
     */
    public abstract void setCacheThumbnails(boolean cacheThumbnails);
    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public abstract DoubleProperty minZoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public abstract double getMinZoomFactor();

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param minZoomFactor The desired zoom factor.
     */
    public abstract void setMinZoomFactor(double minZoomFactor);

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public abstract DoubleProperty maxZoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public abstract double getMaxZoomFactor();
    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param maxZoomFactor The desired zoom factor.
     */
    public abstract void setMaxZoomFactor(double maxZoomFactor);

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public abstract DoubleProperty zoomFactorProperty();

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public abstract double getZoomFactor();
    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param zoomFactor The desired zoom factor.
     */
    public abstract void setZoomFactor(double zoomFactor);

    /**
     * Gets the DoubleProperty representing the rotation angle for the PDF pages.
     *
     * @return The DoubleProperty for the page rotation angle.
     */
    public abstract DoubleProperty pageRotationProperty();

    /**
     * Gets the rotation angle for the PDF pages.
     *
     * @return The current rotation angle in degrees.
     */
    public abstract double getPageRotation();
    /**
     * Sets the rotation angle for the PDF pages.
     *
     * @param pageRotation The desired rotation angle in degrees.
     */
    public abstract void setPageRotation(double pageRotation);

    /**
     * Gets the IntegerProperty representing the current page number in the PDF viewer.
     *
     * @return The IntegerProperty for the page number.
     */
    public abstract IntegerProperty pageProperty();
    /**
     * Gets the current page number in the PDF viewer.
     *
     * @return The current page number.
     */
    public abstract int getPage();

    /**
     * Sets the current page number in the PDF viewer.
     *
     * @param page The desired page number.
     */
    public abstract void setPage(int page);

    /**
     * Getter for the fit mode of the PDF page. Lazily initializes it to Fit.NONE if null.
     *
     * @return The Fit enum value representing the fit mode.
     */
    public abstract ObjectProperty<Fit> fitProperty();

    /**
     * Gets the fit mode for the PDF page.
     *
     * @return The Fit enum value representing the fit mode (VERTICAL, HORIZONTAL, NONE).
     */
    public abstract Fit getFit();

    /**
     * Sets the fit mode for the PDF page.
     *
     * @param fit The Fit enum value to set as the fit mode.
     */
    public abstract void setFit(Fit fit);

    /**
     * Returns the FloatProperty for controlling the thumbnail rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 72f.
     *
     * @return The thumbnailRenderDpi property.
     */
    public abstract FloatProperty thumbnailRenderDpiProperty();

    /**
     * Gets the current value of the thumbnailRenderDpi property.
     *
     * @return The DPI value for thumbnail rendering.
     */
    public abstract float getThumbnailRenderDpi();
    /**
     * Sets the thumbnailRenderDpi property to the specified DPI value.
     *
     * @param thumbnailRenderDpi The new DPI value for thumbnail rendering.
     */
    public abstract void setThumbnailRenderDpi(float thumbnailRenderDpi);

    /**
     * Gets the DoubleProperty representing the size of thumbnails in the PDF viewer.
     * If the property is null, it initializes it with a default value.
     *
     * @return The DoubleProperty for the thumbnail size.
     */
    public abstract DoubleProperty thumbnailSizeProperty();

    /**
     * Gets the value of the thumbnail size in the PDF viewer.
     *
     * @return The current thumbnail size.
     */
    public abstract double getThumbnailSize();

    /**
     * Sets the size of thumbnails in the PDF viewer.
     *
     * @param thumbnailSize The desired thumbnail size.
     */
    public abstract void setThumbnailSize(double thumbnailSize);

    /**
     * Returns the FloatProperty for controlling the page rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 300f.
     *
     * @return The pageRenderDpi property.
     */
    public abstract FloatProperty pageRenderDpiProperty();

    /**
     * Gets the current value of the pageRenderDpi property.
     *
     * @return The DPI value for page rendering.
     */
    public abstract float getPageRenderDpi();

    /**
     * Sets the pageRenderDpi property to the specified DPI value.
     *
     * @param pageRenderDpi The new DPI value for page rendering.
     */
    public abstract void setPageRenderDpi(float pageRenderDpi);

    /**
     * Gets the ObjectProperty representing the document to be displayed in the PDF viewer.
     * If the property is null, it initializes it.
     *
     * @return The ObjectProperty for the document.
     */
    public abstract ObjectProperty<Document> documentProperty();

    /**
     * Gets the value of the document to be displayed in the PDF viewer.
     *
     * @return The current document.
     */
    public abstract Document getDocument();

    /**
     * Sets the document to be displayed in the PDF viewer.
     *
     * @param document The document to be displayed.
     */
    public abstract void setDocument(Document document);

    /**
     * Shows the document properties dialog as a centered overlay. Does nothing
     * when no document is loaded.
     */
    public abstract void showDocumentProperties();

    /**
     * Opens the print preview dialog (also bound to Ctrl+P). Does nothing when
     * no document is loaded.
     */
    public abstract void print();

    /**
     * Opens the Settings dialog as a centered overlay, where the user picks the
     * default viewer preferences (render DPI, layout, fit, …).
     */
    public abstract void showSettings();

    /**
     * Pushes the persisted {@link com.sun.internals.config.ViewerSettings} onto
     * the live viewer properties (render DPI, thumbnail DPI, default layout/fit
     * and the thumbnails panel state). Called at startup and after the Settings
     * dialog saves.
     */
    public abstract void applySettings();

    /**
     * Gets the ObjectProperty for the currently selected search result.
     *
     * @return The ObjectProperty for the selected search result.
     */
    public abstract ObjectProperty<SearchResult> selectedSearchResultObjectProperty();

    /**
     * Gets the currently selected search result.
     *
     * @return The selected search result.
     */
    public abstract SearchResult getSelectedSearchResult();

    /**
     * Sets the currently selected search result.
     *
     * @param selectedSearchResult The search result to set as selected.
     */
    public abstract void setSelectedSearchResult(SearchResult selectedSearchResult);

    /**
     * Gets the ListProperty for the list of search results.
     *
     * @return The ListProperty for the search results.
     */
    public abstract ListProperty<SearchResult> searchResultsProperty();

    /**
     * Gets the list of search results.
     *
     * @return The list of search results as an ObservableList.
     */
    public abstract ObservableList<SearchResult> getSearchResults();

    /**
     * Sets the list of search results.
     *
     * @param searchResults The list of search results to set.
     */
    public abstract void setSearchResults(ObservableList<SearchResult> searchResults);
    /**
     * Gets the ObjectProperty for the color used to display search results.
     *
     * @return The ObjectProperty for the search result color.
     */
    public abstract ObjectProperty<Color> searchResultColorProperty();

    /**
     * Gets the color used for displaying search results.
     *
     * @return The search result color.
     */
    public abstract Color getSearchResultColor();

    /**
     * Sets the color for displaying search results.
     *
     * @param color The color to set for search results.
     */
    public abstract void setSearchResultColor(Color color);
    /**
     * Gets the StringProperty for the search text.
     *
     * @return The StringProperty for the search text.
     */
    public abstract StringProperty searchTextProperty();

    /**
     * Gets the search text.
     *
     * @return The search text.
     */
    public abstract String getSearchText();

    /**
     * Sets the search text.
     *
     * @param searchText The text to set as the search text.
     */
    public abstract void setSearchText(String searchText);

    /* ------------------------------------------------------------ find state */

    /**
     * The raw engine search hits for the current query (with per-line quads, used
     * to drive page highlights). Internal — not part of the public viewer API.
     *
     * @return the live list of search hits
     */
    public abstract ObservableList<PdfSearchResult> getSearchHits();

    /**
     * The hits that fall on the given page.
     *
     * @param pageIndex the zero-based page index
     * @return the hits on that page (possibly empty)
     */
    public abstract List<PdfSearchResult> hitsForPage(int pageIndex);

    /**
     * The active (focused) search hit — the one navigated to and emphasized.
     *
     * @return the active-hit property
     */
    public abstract ObjectProperty<PdfSearchResult> activeSearchHitProperty();

    /**
     * Gets the active (focused) search hit.
     *
     * @return the active hit, or {@code null}
     */
    public abstract PdfSearchResult getActiveSearchHit();

    /**
     * Sets the active (focused) search hit, navigating/emphasizing it.
     *
     * @param hit the hit to focus, or {@code null} to clear
     */
    public abstract void setActiveSearchHit(PdfSearchResult hit);

    /**
     * Whether every match is highlighted (vs only the active one).
     *
     * @return the highlight-all property
     */
    public abstract BooleanProperty highlightAllProperty();

    /**
     * Whether the search is case-sensitive.
     *
     * @return the match-case property
     */
    public abstract BooleanProperty matchCaseProperty();

    /**
     * Whether diacritics must match exactly.
     *
     * @return the match-diacritics property
     */
    public abstract BooleanProperty matchDiacriticsProperty();

    /**
     * Whether matches must be whole words.
     *
     * @return the whole-words property
     */
    public abstract BooleanProperty wholeWordsProperty();

    /**
     * Navigates to the next search hit (wrapping), if any.
     */
    public abstract void nextSearchHit();

    /**
     * Navigates to the previous search hit (wrapping), if any.
     */
    public abstract void previousSearchHit();



    /**
     * Gets the ObjectProperty for navButtonsState and initializes it with DISABLE_BOTH state by default.
     *
     * @return The ObjectProperty for navButtonsState.
     */
    public abstract ObjectProperty<NavButtonState> navButtonsStateProperty();

    /**
     * Gets the current state of navigation buttons.
     *
     * @return The current state of navigation buttons.
     */
    public abstract NavButtonState getNavButtonsState();
    /**
     * Sets the state of navigation buttons.
     *
     * @param navButtonsState The state to set for navigation buttons.
     */
    public abstract void setNavButtonsState(NavButtonState navButtonsState);

    /**
     * Returns the ResourceBundle containing icon resources for this control.
     * Override this method to provide a custom ResourceBundle for icons used by the control.
     *
     * @return The ResourceBundle containing icon resources.
     */
    public abstract ResourceBundle getIconsBundle();


    /**
     * Gets the Executor instance used for managing asynchronous tasks.
     *
     * @return The Executor instance.
     */
    public abstract ExecutorService getExecutor();


    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees counterclockwise (to the left).
     */
    public abstract void rotateLeft();

    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees clockwise (to the right).
     */
    public abstract void rotateRight();

    /**
     * Navigates to the next page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public abstract boolean gotoNextPage();

    /**
     * Navigates to the previous page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public abstract boolean gotoPreviousPage();

    /**
     * Navigates to the last page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public abstract boolean gotoLastPage();

    /**
     * Navigates to the first page of the document.
     *
     * @return True if the navigation to the first page was successful, false otherwise.
     */
    public abstract boolean gotoFirstPage();


    /**
     * Opens a file dialog to select and open a PDF document.
     * Displays a file dialog, allows the user to choose a PDF file, and loads it if selected.
     */
    public abstract void open();

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param supplier A supplier providing the document to load.
     * @throws NullPointerException If the supplier is null.
     */
    public abstract void load(Supplier<Document> supplier);

    /**
     * Loads a document from an InputStream into the PDF viewer control.
     *
     * @param stream The InputStream containing the document.
     */
    public abstract void load(InputStream stream);

    /**
     * Loads a document from a File into the PDF viewer control.
     *
     * @param file The File representing the document.
     */
    public abstract void load(File file);

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param document The document to load.
     * @throws NullPointerException If the document is null.
     */
    public abstract void load(Document document);

    /**
     * Unloads the current document from the PDF viewer control and resets various settings.
     */
    public abstract void unload();

    /**
     * Sets the current viewport to the specified Rectangle2D.
     *
     * @param currentViewPort The new viewport to set.
     */
    public abstract void setCurrentViewPort(Rectangle2D currentViewPort);

    /**
     * Gets the current viewport, represented as a Rectangle2D.
     *
     * @return The current viewport.
     */
    public abstract Rectangle2D getCurrentViewPort();

    /**
     * Returns the ObjectProperty for controlling the current viewport.
     * If not initialized, a new ObjectProperty is created with a default viewport.
     *
     * @return The currentViewPort property.
     */
    public abstract ObjectProperty<Rectangle2D> currentViewPortProperty();

    /**
     * Switches the viewport for the document being displayed.
     *
     * @param oldPage The old page being displayed (can be null).
     * @param newPage The new page to be displayed (can be null).
     */
    public abstract void switchViewport(Integer oldPage, Integer newPage);

    /**
     * Gets the {@link ObjectProperty} for the screen mode.
     *
     * @return The object property for the screen mode.
     */
    public abstract ObjectProperty<ScreenMode> screenModeProperty();
    /**
     * Gets the current screen mode.
     *
     * @return The current screen mode.
     */
    public abstract ScreenMode getScreenMode();

    /**
     * Sets the screen mode to the specified value.
     *
     * @param screenMode The new screen mode to set.
     */
    public abstract void setScreenMode(ScreenMode screenMode);


    /**
     * Gets the BooleanProperty for allowing full-screen mode.
     *
     * @return The BooleanProperty representing the allowFullScreen property.
     */
    public abstract BooleanProperty allowFullScreenProperty();

    /**
     * Gets the value of allowFullScreen.
     *
     * @return The current value of the allowFullScreen property.
     */
    public abstract boolean isAllowFullScreen();
    /**
     * Sets the value of allowFullScreen.
     *
     * @param allowFullScreen The new value for the allowFullScreen property.
     */
    public abstract void setAllowFullScreen(boolean allowFullScreen);

    /**
     * Gets the operation property.
     *
     * @return The operation property.
     */
    public abstract ObjectProperty<Operation> operationProperty();

    /**
     * Gets the current operation.
     *
     * @return The current operation.
     */
    public abstract Operation getOperation();

    /**
     * Sets the operation.
     *
     * @param operation The operation to set.
     */
    public abstract void setOperation(Operation operation);

    /**
     * Gets the property for the page view mode.
     *
     * @return The property for the page view mode.
     */
    public abstract ObjectProperty<PageViewMode> pageViewModeProperty();

    /**
     * Gets the current page view mode.
     *
     * @return The current page view mode.
     */
    public abstract PageViewMode getPageViewMode();

    /**
     * Sets the page view mode.
     *
     * @param pageViewMode The new page view mode.
     */
    public abstract void setPageViewMode(PageViewMode pageViewMode);


    /**
     * Retrieves the ObjectProperty representing the search panel status.
     *
     * @return The ObjectProperty for search panel status.
     */
    public abstract ObjectProperty<SearchPanelStatus> searchPanelStatusProperty();

    /**
     * Gets the current status of the search panel.
     *
     * @return The current SearchPanelStatus.
     */
    public abstract SearchPanelStatus getSearchPanelStatus();

    /**
     * Sets the status of the search panel.
     *
     * @param searchPanelStatus The new SearchPanelStatus.
     */
    public abstract void setSearchPanelStatus(SearchPanelStatus searchPanelStatus);

    /**
     * Gets the property for the number of columns in the continuous page view
     * (1 = single column, 2 = facing pages).
     *
     * @return The continuous-view column-count property.
     */
    public abstract IntegerProperty pageColumnsProperty();

    /**
     * Gets the continuous-view column count.
     *
     * @return The column count (>= 1).
     */
    public abstract int getPageColumns();

    /**
     * Sets the continuous-view column count (values below 1 are treated as 1).
     *
     * @param pageColumns The desired column count.
     */
    public abstract void setPageColumns(int pageColumns);

}
