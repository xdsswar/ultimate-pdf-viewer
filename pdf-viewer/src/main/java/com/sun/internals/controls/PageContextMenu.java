package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.document.Document;
import com.sun.internals.enums.Operation;
import com.sun.internals.enums.SearchPanelStatus;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.shape.SVGPath;
import xss.it.nfx.pdfium.scene.PdfPageView;
import xss.it.ultimate.pdf.viewer.enums.Fit;

/**
 * A normal PDF-viewer right-click menu for a page: text operations (copy, select
 * all), find, zoom/fit, rotation, page navigation, print and document properties.
 *
 * <p>One instance is shared by a viewer and {@link #attachTo(PdfPageView)
 * attached} to each page node it manages. The engine node's own (standalone)
 * context menu is suppressed so this richer, viewer-aware one is used instead.
 * The action targets are resolved against the page that was right-clicked (kept
 * in {@link #target}) so a multi-page continuous view copies the right page's
 * selection.</p>
 *
 * @author XDSSWAR
 */
public final class PageContextMenu {

    /** Zoom step for the in/out items (mirrors the toolbar). */
    private static final double ZOOM_STEP = 0.1;

    private final AbstractViewer viewer;
    private final ContextMenu menu = new ContextMenu();

    /** The page view that was right-clicked; actions resolve against it. */
    private PdfPageView target;

    private final MenuItem copyItem;
    private final MenuItem findItem;
    private final MenuItem panItem;
    private final MenuItem prevPageItem;
    private final MenuItem nextPageItem;
    private final MenuItem zoomInItem;
    private final MenuItem zoomOutItem;
    private final MenuItem printItem;
    private final MenuItem propertiesItem;

    /**
     * Builds the menu for a viewer.
     *
     * @param viewer the owning viewer whose actions the items invoke
     */
    public PageContextMenu(AbstractViewer viewer) {
        this.viewer = viewer;
        // Reuse the shared menu look (.pdf-context-menu) plus a page-specific hook.
        menu.getStyleClass().addAll("pdf-context-menu", "pdf-page-context-menu");
        menu.setAutoHide(true);

        copyItem = item("Copy", new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN),
                e -> { if (target != null) target.copySelection(); });
        MenuItem selectAllItem = item("Select All",
                new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN),
                e -> { if (target != null) target.selectAll(); });

        findItem = item("Find", new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                e -> openFind());

        // Pan toggles the viewer's pan operation on/off (mirrors the toolbar).
        panItem = item("Pan", null, e -> togglePan());
        SVGPath panIcon = icon("pdf.pan.tool.svg");
        // The pan glyph reads small at the shared icon scale; nudge it up a touch.
        panIcon.setScaleX(1.1);
        panIcon.setScaleY(1.1);
        panItem.setGraphic(panIcon);

        zoomInItem = item("Zoom In", null, e -> zoom(ZOOM_STEP));
        zoomOutItem = item("Zoom Out", null, e -> zoom(-ZOOM_STEP));
        MenuItem fitWidthItem = item("Fit Width", null, e -> viewer.setFit(Fit.HORIZONTAL));
        MenuItem fitPageItem = item("Fit Page", null, e -> viewer.setFit(Fit.VERTICAL));

        MenuItem rotateCwItem = item("Rotate Clockwise", null, e -> viewer.rotateRight());
        rotateCwItem.setGraphic(icon("pdf.options.rotate.clockwise"));
        MenuItem rotateCcwItem = item("Rotate Counterclockwise", null, e -> viewer.rotateLeft());
        rotateCcwItem.setGraphic(icon("pdf.options.rotate.counterclockwise"));

        prevPageItem = item("Previous Page", null, e -> viewer.gotoPreviousPage());
        nextPageItem = item("Next Page", null, e -> viewer.gotoNextPage());

        printItem = item("Print", new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN),
                e -> viewer.print());
        propertiesItem = item("Document Properties", null, e -> viewer.showDocumentProperties());

        menu.getItems().addAll(
                copyItem, selectAllItem,
                new SeparatorMenuItem(), findItem,
                new SeparatorMenuItem(), panItem,
                new SeparatorMenuItem(), zoomInItem, zoomOutItem, fitWidthItem, fitPageItem,
                new SeparatorMenuItem(), rotateCwItem, rotateCcwItem,
                new SeparatorMenuItem(), prevPageItem, nextPageItem,
                new SeparatorMenuItem(), printItem, propertiesItem);
    }

    /**
     * Attaches this menu to a page node: suppresses the node's built-in menu and
     * shows this one on right-click, targeting that node.
     *
     * @param pv the page node to attach to
     */
    public void attachTo(PdfPageView pv) {
        pv.setContextMenuEnabled(false); // replace the engine's standalone menu
        pv.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            target = pv;
            refresh();
            menu.show(pv, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        // Dismiss on any click on the page (a filter, so it runs before the text
        // layer consumes the press); clicks elsewhere are handled by auto-hide.
        pv.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (menu.isShowing()) {
                menu.hide();
            }
        });
    }

    /* -------------------------------------------------------------- helpers */

    /**
     * Builds the icon graphic for a menu item, reusing the same SVG paths as the
     * toolbar's options menu (scaled to match). Coloured via CSS so it tracks the
     * row's normal/hover/disabled text colour.
     *
     * @param key the icon key in the icons bundle
     * @return the icon node
     */
    private SVGPath icon(String key) {
        SVGPath svg = new SVGPath();
        svg.setContent(viewer.getIconsBundle().getString(key));
        svg.getStyleClass().add("pdf-context-menu-icon");
        svg.setScaleX(0.7);
        svg.setScaleY(0.7);
        return svg;
    }

    /** Builds a menu item with an optional accelerator hint and an action. */
    private MenuItem item(String text, KeyCombination accelerator, EventHandler<ActionEvent> action) {
        MenuItem mi = new MenuItem(text);
        if (accelerator != null) {
            mi.setAccelerator(accelerator);
        }
        mi.setOnAction(e -> {
            menu.hide();
            action.handle(e);
        });
        return mi;
    }

    /** Enables/disables items to match the current document and selection. */
    private void refresh() {
        Document document = viewer.getDocument();
        int pages = document != null ? document.getNumberOfPages() : 0;
        boolean hasDoc = pages > 0;

        // Enable Copy only when there is actual selected text. A bare click places
        // a caret (anchor == focus), which the selection model isn't "empty" for
        // but yields no copyable text — so test the extracted text, not isEmpty().
        boolean hasText = false;
        if (target != null) {
            String selected = target.getSelectedText();
            hasText = selected != null && !selected.isBlank();
        }
        copyItem.setDisable(!hasText);

        int page = viewer.getPage();
        prevPageItem.setDisable(!hasDoc || page <= 0);
        nextPageItem.setDisable(!hasDoc || page >= pages - 1);

        zoomInItem.setDisable(!hasDoc || viewer.getZoomFactor() >= viewer.getMaxZoomFactor());
        zoomOutItem.setDisable(!hasDoc || viewer.getZoomFactor() <= viewer.getMinZoomFactor());

        printItem.setDisable(!hasDoc);
        propertiesItem.setDisable(!hasDoc);

        // Find is disabled when search is turned off (this also disables its
        // Ctrl+F accelerator, the only keyboard path to the search panel).
        findItem.setDisable(!hasDoc || !viewer.isEnableSearch());

        // Reflect the current pan state so the item reads as a toggle.
        panItem.setDisable(!hasDoc);
        panItem.setText(viewer.getOperation() == Operation.PAN ? "Pan (on)" : "Pan");
    }

    /** Toggles the viewer's pan operation on/off (same behavior as the toolbar). */
    private void togglePan() {
        viewer.setOperation(viewer.getOperation() == Operation.PAN ? Operation.NONE : Operation.PAN);
    }

    /** Opens the search panel, pre-filling it with the current selection if any. */
    private void openFind() {
        if (target != null) {
            String selected = target.getSelectedText();
            if (selected != null && !selected.isBlank()) {
                viewer.setSearchText(selected.strip());
            }
        }
        viewer.setSearchPanelStatus(SearchPanelStatus.OPEN);
    }

    /** Applies a zoom delta with the same clamping/behavior as the toolbar. */
    private void zoom(double delta) {
        viewer.setFit(Fit.NONE);
        double factor = viewer.getZoomFactor() + delta;
        factor = Math.max(viewer.getMinZoomFactor(), Math.min(viewer.getMaxZoomFactor(), factor));
        viewer.setZoomFactor(factor);
    }
}
