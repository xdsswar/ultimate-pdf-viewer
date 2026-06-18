/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.flow.NfxCell;
import com.sun.internals.flow.NfxListView;
import javafx.animation.PauseTransition;
import javafx.css.PseudoClass;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.nfx.pdfium.text.PdfTextChar;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static xss.it.ultimate.pdf.viewer.Assets.load;

/**
 * The right-hand search pane: a query field with previous/next navigation and a
 * live match counter, a row of find options (Highlight All, Match Case, Match
 * Diacritics, Whole Words) and a scrollable list of results. Typing searches as
 * you type (debounced); selecting a result navigates to and emphasizes the match.
 *
 * @author XDSSWAR
 */
public final class PdfSearchPanel extends AnchorPane {

    /** Debounce before a keystroke triggers a search (avoids a search per char). */
    private static final Duration SEARCH_DEBOUNCE = Duration.millis(220);

    /** Pseudo-class drawing the focus ring on the search-field container. */
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    /** Chevron icons for the previous/next navigation buttons. */
    private static final String PREV_ICON =
            "M9.265625 1.023438L2.925781 7.5L9.265625 13.972656L9.984375 13.277344L4.324219 7.5L9.984375 1.726563Z";
    private static final String NEXT_ICON =
            "M5.710938 2.007813L5.039063 2.742188L10.761719 8L5.039063 13.253906L5.710938 13.996094"
                    + "L11.839844 8.367188C11.941406 8.273438 12 8.140625 12 8C12 7.859375 11.941406 7.726563 11.839844 7.632813Z";

    /** Max characters of context shown on either side of the match. */
    private static final int CONTEXT_BEFORE = 22;
    private static final int CONTEXT_AFTER = 44;

    private final AbstractViewer viewer;

    private final TextField searchField;
    private final Label counter;
    private final Button prevButton;
    private final Button nextButton;
    private final ToggleButton highlightAll;
    private final ToggleButton matchCase;
    private final ToggleButton matchDiacritics;
    private final ToggleButton wholeWords;
    private final NfxListView<PdfSearchResult> resultsList;

    private final PauseTransition debounce = new PauseTransition(SEARCH_DEBOUNCE);

    /** Per-page character cache, used to extract context snippets by position. */
    private final Map<Integer, List<PdfTextChar>> pageCharsCache = new HashMap<>();

    /** Guards the result-selection <-> active-hit feedback loop. */
    private boolean syncingSelection;

    /**
     * Builds the search pane for the given viewer.
     *
     * @param viewer the owning viewer
     */
    public PdfSearchPanel(AbstractViewer viewer) {
        this.viewer = viewer;
        this.searchField = new TextField();
        this.counter = new Label();
        this.prevButton = new Button();
        this.nextButton = new Button();
        this.highlightAll = new ToggleButton("Highlight All");
        this.matchCase = new ToggleButton("Match Case");
        this.matchDiacritics = new ToggleButton("Match Diacritics");
        this.wholeWords = new ToggleButton("Whole Words");
        this.resultsList = new NfxListView<>();

        initialize();
    }

    private void initialize() {
        setMinWidth(0);
        getStylesheets().add(getUserAgentStylesheet());
        getStyleClass().add("pdf-search-panel");

        /* ---- header: search field + counter + prev/next + options ---- */
        searchField.setPromptText("Find in document");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Leading magnifier inside a focusable container that carries the border
        // + focus glow (the field itself is transparent).
        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent(viewer.getIconsBundle().getString("pdf.search.button.svg"));
        searchIcon.getStyleClass().add("search-input-icon");
        HBox inputBox = new HBox(6, searchIcon, searchField);
        inputBox.getStyleClass().add("search-input");
        inputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputBox, Priority.ALWAYS);
        searchField.focusedProperty().addListener((o, a, focused) ->
                inputBox.pseudoClassStateChanged(FOCUSED, focused));

        counter.getStyleClass().add("search-counter");

        prevButton.getStyleClass().addAll("search-nav", "search-prev");
        nextButton.getStyleClass().addAll("search-nav", "search-next");
        prevButton.setFocusTraversable(false);
        nextButton.setFocusTraversable(false);
        prevButton.setGraphic(chevron(PREV_ICON));
        nextButton.setGraphic(chevron(NEXT_ICON));
        prevButton.setText(null);
        nextButton.setText(null);

        HBox searchBar = new HBox(8, inputBox, counter, prevButton, nextButton);
        searchBar.getStyleClass().add("search-bar");
        searchBar.setAlignment(Pos.CENTER_LEFT);

        for (ToggleButton t : new ToggleButton[]{highlightAll, matchCase, matchDiacritics, wholeWords}) {
            t.getStyleClass().add("search-toggle");
            t.setFocusTraversable(false);
        }
        FlowPane options = new FlowPane(8, 8, highlightAll, matchCase, matchDiacritics, wholeWords);
        options.getStyleClass().add("search-options");

        Region divider = new Region();
        divider.getStyleClass().add("horizontal-divider");

        VBox header = new VBox(10, searchBar, options, divider);
        header.getStyleClass().add("search-header");

        /* ---- results list (fills the remaining height of the pane) ---- */
        resultsList.getStyleClass().add("search-results-list");
        resultsList.setCellHeight(46);
        // Force a SINGLE full-width column. The list reflows by width, so besides
        // capping max-cells-per-row at 1, search.css also sets a huge break-point
        // (author origin) which beats the list's user-agent defaults.
        resultsList.setMaxCellsPerRow(1);
        resultsList.setMinCellWidthBreakPoint(100000);
        resultsList.setLeftGap(0);
        resultsList.setRightGap(0);
        resultsList.setItems(FXCollections.observableArrayList(viewer.getSearchHits()));
        resultsList.setPlaceHolder(buildPlaceholder());
        initResultsCellFactory();
        VBox.setVgrow(resultsList, Priority.ALWAYS);

        // A single column: header on top, the NfxListView of results filling the rest.
        VBox content = new VBox(12, header, resultsList);
        content.getStyleClass().add("search-content");
        AnchorPane.setTopAnchor(content, 16.0);
        AnchorPane.setLeftAnchor(content, 10.0);
        AnchorPane.setRightAnchor(content, 10.0);
        AnchorPane.setBottomAnchor(content, 10.0);

        getChildren().add(content);

        bindBehavior();
        updateCounter();
    }

    /* ----------------------------------------------------------- behavior */

    private void bindBehavior() {
        // Find as you type (debounced); also search immediately on ENTER and move
        // to the next match when results already exist.
        debounce.setOnFinished(e -> viewer.setSearchText(searchField.getText()));
        searchField.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                debounce.stop();
                if (viewer.getSearchHits().isEmpty()) {
                    viewer.setSearchText(searchField.getText());
                } else if (e.isShiftDown()) {
                    viewer.previousSearchHit();
                } else {
                    viewer.nextSearchHit();
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });

        prevButton.setOnAction(e -> viewer.previousSearchHit());
        nextButton.setOnAction(e -> viewer.nextSearchHit());

        // Find options are two-way bound to the viewer's search state.
        highlightAll.selectedProperty().bindBidirectional(viewer.highlightAllProperty());
        matchCase.selectedProperty().bindBidirectional(viewer.matchCaseProperty());
        matchDiacritics.selectedProperty().bindBidirectional(viewer.matchDiacriticsProperty());
        wholeWords.selectedProperty().bindBidirectional(viewer.wholeWordsProperty());

        // Keep field, list selection and the counter in sync with the viewer state.
        viewer.searchTextProperty().addListener((o, a, b) -> {
            if (b != null && !b.equals(searchField.getText())) {
                searchField.setText(b);
            }
        });
        viewer.getSearchHits().addListener((ListChangeListener<PdfSearchResult>) c -> {
            pageCharsCache.clear();
            // Set a FRESH list each search: the list-view skin only re-evaluates its
            // placeholder (and re-attaches its content listener) on an items CHANGE,
            // not on an in-place mutation — so reuse of one list leaves "No matches".
            resultsList.setItems(FXCollections.observableArrayList(viewer.getSearchHits()));
            updateCounter();
        });
        viewer.activeSearchHitProperty().addListener((o, a, hit) -> {
            selectInList(hit);
            updateCounter();
        });

        // Clicking a result row focuses that hit.
        resultsList.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<PdfSearchResult>) c -> {
                    if (syncingSelection) {
                        return;
                    }
                    if (!resultsList.getSelectionModel().getSelectedItems().isEmpty()) {
                        viewer.setActiveSearchHit(resultsList.getSelectionModel().getSelectedItems().get(0));
                    }
                });
    }

    /** Selects (and reveals) the row for the given hit without re-entrancy. */
    private void selectInList(PdfSearchResult hit) {
        syncingSelection = true;
        try {
            resultsList.getSelectionModel().clearSelection();
            if (hit != null) {
                resultsList.getSelectionModel().select(hit);
                resultsList.scrollToItemIfNotVisible(hit);
            }
        } finally {
            syncingSelection = false;
        }
    }

    /** Updates the "i / n" (or "No results") match counter and nav-button state. */
    private void updateCounter() {
        int n = viewer.getSearchHits().size();
        prevButton.setDisable(n == 0);
        nextButton.setDisable(n == 0);
        if (n == 0) {
            String q = searchField.getText();
            counter.setText(q == null || q.isBlank() ? "" : "No results");
            return;
        }
        int i = viewer.getSearchHits().indexOf(viewer.getActiveSearchHit());
        counter.setText((i < 0 ? 1 : i + 1) + " / " + n);
    }

    /** Requests focus on the search field (called when the panel opens). */
    public void focusSearchField() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    /** Builds a navigation chevron icon from an SVG path. */
    private static SVGPath chevron(String path) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("search-nav-icon");
        return svg;
    }

    /* -------------------------------------------------------- cell factory */

    private void initResultsCellFactory() {
        resultsList.setCellFactory(list -> new NfxCell<>(resultsList) {
            @Override
            public void update(PdfSearchResult item) {
                super.update(item);
                if (item != null) {
                    setGraphics(buildCell(item));
                }
                setText(null);
            }
        });
    }

    /**
     * Builds a result row by extracting the text on the SAME visual line as the
     * match — located via the matched glyphs' position — rather than slicing the
     * raw page text by character index (which can stitch unrelated content).
     */
    private SearchResultCell buildCell(PdfSearchResult hit) {
        List<PdfTextChar> chars = pageChars(hit.pageIndex());
        int n = chars.size();
        int start = clamp(hit.charStart(), 0, n);
        int end = clamp(start + hit.charCount(), start, n); // exclusive
        if (start >= end) {
            return new SearchResultCell(hit.pageIndex() + 1, "", sanitize(hit.snippet()), "");
        }

        // Vertical band of the matched glyphs — defines "this line".
        double top = Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (int i = start; i < end; i++) {
            Rectangle2D b = chars.get(i).bounds();
            top = Math.min(top, b.getMinY());
            bottom = Math.max(bottom, b.getMaxY());
        }

        // Walk left/right along the same line, bounded by a context budget.
        int lineStart = start;
        while (lineStart > 0 && start - lineStart < CONTEXT_BEFORE
                && onLine(chars.get(lineStart - 1).bounds(), top, bottom)) {
            lineStart--;
        }
        int lineEnd = end;
        while (lineEnd < n && lineEnd - end < CONTEXT_AFTER
                && onLine(chars.get(lineEnd).bounds(), top, bottom)) {
            lineEnd++;
        }

        String before = sanitize(join(chars, lineStart, start));
        String match = sanitize(join(chars, start, end));
        String after = sanitize(join(chars, end, lineEnd));
        if (match.isEmpty()) {
            match = sanitize(hit.snippet());
        }
        boolean moreLeft = lineStart > 0 && onLine(chars.get(lineStart - 1).bounds(), top, bottom);
        boolean moreRight = lineEnd < n && onLine(chars.get(lineEnd).bounds(), top, bottom);
        if (moreLeft) {
            before = "… " + before;
        }
        if (moreRight) {
            after = after + " …";
        }
        return new SearchResultCell(hit.pageIndex() + 1, before, match, after);
    }

    /** Whether a glyph box sits on the line defined by the vertical band [top, bottom]. */
    private static boolean onLine(Rectangle2D b, double top, double bottom) {
        double cy = b.getMinY() + b.getHeight() / 2.0;
        return cy >= top && cy <= bottom;
    }

    /** Joins the text of characters in {@code [from, to)}. */
    private static String join(List<PdfTextChar> chars, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(chars.get(i).text());
        }
        return sb.toString();
    }

    /** Page characters (with positions), cached, used to extract snippets by line. */
    private List<PdfTextChar> pageChars(int pageIndex) {
        return pageCharsCache.computeIfAbsent(pageIndex, i -> {
            if (viewer.getDocument() == null) {
                return List.of();
            }
            PdfDocument pdf = viewer.getDocument().getPdfDocument();
            if (i < 0 || i >= pdf.getPageCount()) {
                return List.of();
            }
            return pdf.getPage(i).getChars();
        });
    }

    /**
     * Cleans a text fragment for display: expands ligatures/compatibility forms
     * (NFKC), drops non-printable "rare" characters (control, format/zero-width,
     * private-use box glyphs, unassigned, surrogates, replacement) and collapses
     * whitespace. Uses Unicode character categories so no literal odd glyphs are
     * embedded in the source.
     */
    private static String sanitize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (!isRare(c)) {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").strip();
    }

    /** Whether a character is non-printable / "rare" and should be dropped from display. */
    private static boolean isRare(char c) {
        if (c == '�') {
            return true; // replacement character
        }
        int type = Character.getType(c);
        return type == Character.CONTROL
                || type == Character.FORMAT          // soft hyphen, zero-width marks, BOM
                || type == Character.PRIVATE_USE     // box glyphs
                || type == Character.SURROGATE
                || type == Character.UNASSIGNED;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private Region buildPlaceholder() {
        Label label = new Label("No matches");
        label.getStyleClass().add("search-placeholder");
        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        return box;
    }

    @Override
    public String getUserAgentStylesheet() {
        return load("/xss/it/ultimate/pdf/viewer/css/search.css").toExternalForm();
    }
}
