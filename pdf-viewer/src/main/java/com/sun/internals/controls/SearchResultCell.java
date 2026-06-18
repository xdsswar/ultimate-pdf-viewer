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

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A single full-width row in the search-results list:
 *
 * <ul>
 *   <li>a header with a small page chip ("Page N"), and</li>
 *   <li>a context snippet beneath it in which the matched term is shown as a
 *       highlighted chip, flanked by the surrounding (dimmed) text.</li>
 * </ul>
 *
 * <p>Purely presentational — the owning {@link PdfSearchPanel} supplies the page
 * number and the three snippet fragments (text before the match, the match, and
 * text after) so this cell never touches the document.</p>
 *
 * @author XDSSWAR
 */
public final class SearchResultCell extends VBox {

    /**
     * Builds a result row.
     *
     * @param pageNumber the one-based page number shown in the chip
     * @param before     the snippet text immediately before the match
     * @param match      the matched text (shown as a highlighted chip)
     * @param after      the snippet text immediately after the match
     */
    public SearchResultCell(int pageNumber, String before, String match, String after) {
        getStyleClass().add("search-result-cell");
        setSpacing(3);

        // Header: a "Page N" chip on the left.
        Label page = new Label("Page " + pageNumber);
        page.getStyleClass().add("search-result-page");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(page, spacer);
        header.getStyleClass().add("search-result-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Snippet: dimmed context with the match shown as a highlighted chip.
        Text beforeText = new Text(before);
        beforeText.getStyleClass().add("search-result-context");
        Label matchChip = new Label(match);
        matchChip.getStyleClass().add("search-result-match");
        Text afterText = new Text(after);
        afterText.getStyleClass().add("search-result-context");

        TextFlow snippet = new TextFlow(beforeText, matchChip, afterText);
        snippet.getStyleClass().add("search-result-snippet");
        snippet.setMaxHeight(Double.MAX_VALUE);

        getChildren().addAll(header, snippet);
    }
}
