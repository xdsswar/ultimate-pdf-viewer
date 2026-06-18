package xss.it.ultimate.pdf.viewer;

import com.sun.internals.AbstractViewer;
import com.sun.internals.viewer.PdfAbstractViewerImpl;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.layout.AnchorPane;
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
public class PdfViewer extends AnchorPane implements Viewer {
    /**
     * The viewer responsible for displaying and interacting with the PDF document.
     */
    private final AbstractViewer viewer;

    /**
     * Initializes a new instance of the PdfViewer class.
     * Constructs and sets up the components and features required for PDF document viewing.
     */
    public PdfViewer() {
        super();
        this.viewer = new PdfAbstractViewerImpl();
        AnchorPane.setTopAnchor(this.viewer,0d);
        AnchorPane.setRightAnchor(this.viewer,0d);
        AnchorPane.setBottomAnchor(this.viewer,0d);
        AnchorPane.setLeftAnchor(this.viewer,0d);
        this.getChildren().add(this.viewer);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public BooleanProperty showThumbnailsProperty() {
        return viewer.showThumbnailsProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'show thumbnails' property value to the underlying viewer.
     */
    @Override
    public boolean isShowThumbnails() {
        return viewer.isShowThumbnails();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'show thumbnails' property value to the underlying viewer.
     */
    @Override
    public void setShowThumbnails(boolean showThumbnails) {
        viewer.setShowThumbnails(showThumbnails);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public BooleanProperty cacheThumbnailsProperty() {
        return viewer.cacheThumbnailsProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'cache thumbnails' property value to the underlying viewer.
     */
    @Override
    public boolean isCacheThumbnails() {
        return viewer.isCacheThumbnails();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'cache thumbnails' property value to the underlying viewer.
     */
    @Override
    public void setCacheThumbnails(boolean cacheThumbnails) {
        viewer.setCacheThumbnails(cacheThumbnails);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public DoubleProperty minZoomFactorProperty() {
        return viewer.minZoomFactorProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'minimum zoom factor' property value to the underlying viewer.
     */
    @Override
    public double getMinZoomFactor() {
        return viewer.getMinZoomFactor();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'minimum zoom factor' property value to the underlying viewer.
     */
    @Override
    public void setMinZoomFactor(double minZoomFactor) {
        viewer.setMinZoomFactor(minZoomFactor);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public DoubleProperty maxZoomFactorProperty() {
        return viewer.maxZoomFactorProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'maximum zoom factor' property value to the underlying viewer.
     */
    @Override
    public double getMaxZoomFactor() {
        return viewer.getMaxZoomFactor();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'maximum zoom factor' property value to the underlying viewer.
     */
    @Override
    public void setMaxZoomFactor(double maxZoomFactor) {
        viewer.setMaxZoomFactor(maxZoomFactor);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public DoubleProperty zoomFactorProperty() {
        return viewer.zoomFactorProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'zoom factor' property value to the underlying viewer.
     */
    @Override
    public double getZoomFactor() {
        return viewer.getZoomFactor();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'zoom factor' property value to the underlying viewer.
     */
    @Override
    public void setZoomFactor(double zoomFactor) {
        viewer.setZoomFactor(zoomFactor);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public DoubleProperty pageRotationProperty() {
        return viewer.pageRotationProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'page rotation' property value to the underlying viewer.
     */
    @Override
    public double getPageRotation() {
        return viewer.getPageRotation();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'page rotation' property value to the underlying viewer.
     */
    @Override
    public void setPageRotation(double pageRotation) {
        viewer.setPageRotation(pageRotation);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public IntegerProperty pageProperty() {
        return viewer.pageProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'current page' property value to the underlying viewer.
     */
    @Override
    public int getPage() {
        return viewer.getPage();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'current page' property value to the underlying viewer.
     */
    @Override
    public void setPage(int page) {
        viewer.setPage(page);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public ObjectProperty<Fit> fitProperty() {
        return viewer.fitProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'fit' property value to the underlying viewer.
     */
    @Override
    public Fit getFit() {
        return viewer.getFit();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'fit' property value to the underlying viewer.
     */
    @Override
    public void setFit(Fit fit) {
        viewer.setFit(fit);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public FloatProperty thumbnailRenderDpiProperty() {
        return viewer.thumbnailRenderDpiProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'thumbnail render DPI' property value to the underlying viewer.
     */
    @Override
    public float getThumbnailRenderDpi() {
        return viewer.getThumbnailRenderDpi();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'thumbnail render DPI' property value to the underlying viewer.
     */
    @Override
    public void setThumbnailRenderDpi(float thumbnailRenderDpi) {
        viewer.setThumbnailRenderDpi(thumbnailRenderDpi);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public DoubleProperty thumbnailSizeProperty() {
        return viewer.thumbnailSizeProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'thumbnail size' property value to the underlying viewer.
     */
    @Override
    public double getThumbnailSize() {
        return viewer.getThumbnailSize();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'thumbnail size' property value to the underlying viewer.
     */
    @Override
    public void setThumbnailSize(double thumbnailSize) {
        viewer.setThumbnailSize(thumbnailSize);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public FloatProperty pageRenderDpiProperty() {
        return viewer.pageRenderDpiProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'page render DPI' property value to the underlying viewer.
     */
    @Override
    public float getPageRenderDpi() {
        return viewer.getPageRenderDpi();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'page render DPI' property value to the underlying viewer.
     */
    @Override
    public void setPageRenderDpi(float pageRenderDpi) {
        viewer.setPageRenderDpi(pageRenderDpi);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public ObjectProperty<SearchResult> selectedSearchResultObjectProperty() {
        return viewer.selectedSearchResultObjectProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'selected search result' property value to the underlying viewer.
     */
    @Override
    public SearchResult getSelectedSearchResult() {
        return viewer.getSelectedSearchResult();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'selected search result' property value to the underlying viewer.
     */
    @Override
    public void setSelectedSearchResult(SearchResult selectedSearchResult) {
        viewer.setSelectedSearchResult(selectedSearchResult);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public ListProperty<SearchResult> searchResultsProperty() {
        return viewer.searchResultsProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'search results' property value to the underlying viewer.
     */
    @Override
    public ObservableList<SearchResult> getSearchResults() {
        return viewer.getSearchResults();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'search results' property value to the underlying viewer.
     */
    @Override
    public void setSearchResults(ObservableList<SearchResult> searchResults) {
        viewer.setSearchResults(searchResults);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public ObjectProperty<Color> searchResultColorProperty() {
        return viewer.searchResultColorProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'search result color' property value to the underlying viewer.
     */
    @Override
    public Color getSearchResultColor() {
        return viewer.getSearchResultColor();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'search result color' property value to the underlying viewer.
     */
    @Override
    public void setSearchResultColor(Color color) {
        viewer.setSearchResultColor(color);
    }

    /**
     * {@inheritDoc}
     * Delegates the property retrieval to the corresponding property in the underlying viewer.
     */
    @Override
    public StringProperty searchTextProperty() {
        return viewer.searchTextProperty();
    }

    /**
     * {@inheritDoc}
     * Delegates the retrieval of the 'search text' property value to the underlying viewer.
     */
    @Override
    public String getSearchText() {
        return viewer.getSearchText();
    }

    /**
     * {@inheritDoc}
     * Delegates the setting of the 'search text' property value to the underlying viewer.
     */
    @Override
    public void setSearchText(String searchText) {
        viewer.setSearchText(searchText);
    }

    /**
     * {@inheritDoc}
     * Delegates the rotation of the page to the left to the underlying viewer.
     */
    @Override
    public void rotateLeft() {
        viewer.rotateLeft();
    }

    /**
     * {@inheritDoc}
     * Delegates the rotation of the page to the right to the underlying viewer.
     */
    @Override
    public void rotateRight() {
        viewer.rotateRight();
    }

    /**
     * {@inheritDoc}
     * Delegates the navigation to the next page to the underlying viewer.
     */
    @Override
    public boolean gotoNextPage() {
        return viewer.gotoNextPage();
    }

    /**
     * {@inheritDoc}
     * Delegates the navigation to the previous page to the underlying viewer.
     */
    @Override
    public boolean gotoPreviousPage() {
        return viewer.gotoPreviousPage();
    }

    /**
     * {@inheritDoc}
     * Delegates the navigation to the last page to the underlying viewer.
     */
    @Override
    public boolean gotoLastPage() {
        return viewer.gotoLastPage();
    }

    /**
     * {@inheritDoc}
     * Delegates the navigation to the first page to the underlying viewer.
     */
    @Override
    public boolean gotoFirstPage() {
        return viewer.gotoFirstPage();
    }

    /**
     * {@inheritDoc}
     * Delegates the opening of the document to the underlying viewer.
     */
    @Override
    public void open() {
        viewer.open();
    }

    /**
     * {@inheritDoc}
     * Delegates the loading of the document from an InputStream to the underlying viewer.
     */
    @Override
    public void load(InputStream stream) {
        viewer.load(stream);
    }

    /**
     * {@inheritDoc}
     * Delegates the loading of the document from a File to the underlying viewer.
     */
    @Override
    public void load(File file) {
        viewer.load(file);
    }

    /**
     * {@inheritDoc}
     * Delegates the unloading of the document to the underlying viewer.
     */
    @Override
    public void unload() {
        viewer.unload();
    }

    /**
     * {@inheritDoc}
     * Delegates opening the print dialog to the underlying viewer.
     */
    @Override
    public void print() {
        viewer.print();
    }

    /**
     * {@inheritDoc}
     * Gets the property representing the screen mode from the underlying viewer.
     */
    @Override
    public ObjectProperty<ScreenMode> screenModeProperty() {
        return viewer.screenModeProperty();
    }

    /**
     * {@inheritDoc}
     * Gets the current screen mode from the underlying viewer.
     */
    @Override
    public ScreenMode getScreenMode() {
        return viewer.getScreenMode();
    }

    /**
     * {@inheritDoc}
     * Delegates setting the screen mode to the underlying viewer.
     */
    @Override
    public void setScreenMode(ScreenMode screenMode) {
        viewer.setScreenMode(screenMode);
    }

    /**
     * {@inheritDoc}
     * Gets the property representing whether full screen is allowed from the underlying viewer.
     */
    @Override
    public BooleanProperty allowFullScreenProperty() {
        return viewer.allowFullScreenProperty();
    }

    /**
     * {@inheritDoc}
     * Gets the value of whether full screen is allowed from the underlying viewer.
     */
    @Override
    public boolean isAllowFullScreen() {
        return viewer.isAllowFullScreen();
    }

    /**
     * {@inheritDoc}
     * Sets whether full screen is allowed on the underlying viewer.
     */
    @Override
    public void setAllowFullScreen(boolean allowFullScreen) {
        viewer.setAllowFullScreen(allowFullScreen);
    }

    /**
     * {@inheritDoc}
     * Gets the property controlling whether the toolbar is shown from the underlying viewer.
     */
    @Override
    public BooleanProperty enableToolbarProperty() {
        return viewer.enableToolbarProperty();
    }

    /**
     * {@inheritDoc}
     * Gets whether the toolbar is shown from the underlying viewer.
     */
    @Override
    public boolean isEnableToolbar() {
        return viewer.isEnableToolbar();
    }

    /**
     * {@inheritDoc}
     * Sets whether the toolbar is shown on the underlying viewer.
     */
    @Override
    public void setEnableToolbar(boolean enableToolbar) {
        viewer.setEnableToolbar(enableToolbar);
    }

    /**
     * {@inheritDoc}
     * Gets the property controlling whether search is enabled from the underlying viewer.
     */
    @Override
    public BooleanProperty enableSearchProperty() {
        return viewer.enableSearchProperty();
    }

    /**
     * {@inheritDoc}
     * Gets whether search is enabled from the underlying viewer.
     */
    @Override
    public boolean isEnableSearch() {
        return viewer.isEnableSearch();
    }

    /**
     * {@inheritDoc}
     * Sets whether search is enabled on the underlying viewer.
     */
    @Override
    public void setEnableSearch(boolean enableSearch) {
        viewer.setEnableSearch(enableSearch);
    }

    /**
     * {@inheritDoc}
     * Gets the loaded engine document from the underlying viewer.
     */
    @Override
    public PdfDocument getDocument() {
        var doc = viewer.getDocument();
        return doc == null ? null : doc.getPdfDocument();
    }

    /**
     * {@inheritDoc}
     * Gets the property for the page view mode from the underlying viewer.
     */
    @Override
    public ObjectProperty<PageViewMode> pageViewModeProperty() {
        return viewer.pageViewModeProperty();
    }

    /**
     * {@inheritDoc}
     * Gets the current page view mode from the underlying viewer.
     */
    @Override
    public PageViewMode getPageViewMode() {
        return viewer.getPageViewMode();
    }

    /**
     * {@inheritDoc}
     * Sets the page view mode in the underlying viewer.
     */
    @Override
    public void setPageViewMode(PageViewMode pageViewMode) {
        viewer.setPageViewMode(pageViewMode);
    }

    /**
     * {@inheritDoc}
     * Gets the continuous-view column-count property from the underlying viewer.
     */
    @Override
    public IntegerProperty pageColumnsProperty() {
        return viewer.pageColumnsProperty();
    }

    /**
     * {@inheritDoc}
     * Gets the continuous-view column count from the underlying viewer.
     */
    @Override
    public int getPageColumns() {
        return viewer.getPageColumns();
    }

    /**
     * {@inheritDoc}
     * Sets the continuous-view column count on the underlying viewer.
     */
    @Override
    public void setPageColumns(int pageColumns) {
        viewer.setPageColumns(pageColumns);
    }

}
