package com.sun.internals.print;

import com.sun.internals.print.PrintSettings.ScaleMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the printable <em>content</em> node for one sheet: an N-up grid of page
 * images, optionally topped and tailed with header/footer text bands. The node
 * is sized to the printable area (the page minus margins) and is used by
 * <strong>both</strong> the dialog preview and {@link PrintRunner}, so what the
 * user sees is exactly what prints.
 *
 * <p>All sizes are in a single caller-chosen unit (points when printing, preview
 * pixels when previewing); the composer is otherwise unit-agnostic, which is what
 * keeps the two paths identical.</p>
 *
 * @author XDSSWAR
 */
public final class SheetComposer {

    /** Gap between N-up cells, as a fraction of the content's shorter side. */
    private static final double GAP_FRACTION = 0.02;
    /** Header/footer band height, as a fraction of the content height. */
    private static final double BAND_FRACTION = 0.035;

    private SheetComposer() {
    }

    /**
     * Groups page indices into sheets of at most {@code pagesPerSheet} pages.
     *
     * @param pages         the ordered page indices to print
     * @param pagesPerSheet the N-up count (&gt;= 1)
     * @return a list of sheets, each a list of page indices
     */
    public static List<List<Integer>> sheets(List<Integer> pages, int pagesPerSheet) {
        int per = Math.max(1, pagesPerSheet);
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < pages.size(); i += per) {
            result.add(new ArrayList<>(pages.subList(i, Math.min(pages.size(), i + per))));
        }
        return result;
    }

    /**
     * Columns and rows for an N-up layout at the given orientation.
     *
     * @param pagesPerSheet the N-up count
     * @param landscape     whether the sheet is landscape
     * @return {@code [columns, rows]}
     */
    public static int[] grid(int pagesPerSheet, boolean landscape) {
        return switch (pagesPerSheet) {
            case 2 -> landscape ? new int[]{2, 1} : new int[]{1, 2};
            case 4 -> new int[]{2, 2};
            case 6 -> landscape ? new int[]{3, 2} : new int[]{2, 3};
            case 9 -> new int[]{3, 3};
            case 16 -> new int[]{4, 4};
            default -> {
                // Any other N (defensive — the UI restricts to the cases above):
                // a near-square grid so no page in the group is ever dropped.
                int cols = (int) Math.ceil(Math.sqrt(Math.max(1, pagesPerSheet)));
                int rows = (int) Math.ceil((double) Math.max(1, pagesPerSheet) / cols);
                yield new int[]{cols, rows};
            }
        };
    }

    /**
     * The text shown in the four header/footer corners of a sheet.
     *
     * @param headerLeft  top-left (e.g. document title), or {@code null}
     * @param headerRight top-right (e.g. date), or {@code null}
     * @param footerLeft  bottom-left, or {@code null}
     * @param footerRight bottom-right (e.g. "page 1 / 10"), or {@code null}
     */
    public record Banner(String headerLeft, String headerRight,
                         String footerLeft, String footerRight) {
    }

    /**
     * Builds the content node for one sheet.
     *
     * @param contentW       printable-area width
     * @param contentH       printable-area height
     * @param images         the rendered page images for this sheet (in order)
     * @param scaleMode      how each page fits its cell
     * @param customPercent  scale percent when {@code scaleMode} is CUSTOM
     * @param pagesPerSheet  the N-up count
     * @param landscape      whether the sheet is landscape
     * @param headersFooters whether to draw the header/footer bands
     * @param banner         the header/footer text (used only when enabled)
     * @return a region sized to {@code contentW x contentH}
     */
    public static Region buildContent(double contentW, double contentH, List<Image> images,
                                      ScaleMode scaleMode, double customPercent, int pagesPerSheet,
                                      boolean landscape, boolean headersFooters, Banner banner) {
        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(true);
        content.setMinSize(contentW, contentH);
        content.setPrefSize(contentW, contentH);
        content.setMaxSize(contentW, contentH);

        // Skip the bands on a degenerate, very small sheet — two bands would
        // otherwise eat the whole content and leave no room for the page.
        boolean bands = headersFooters && contentH > 60;
        double band = bands ? Math.max(8, contentH * BAND_FRACTION) : 0;
        if (bands) {
            content.getChildren().add(band(contentW, band,
                    banner == null ? null : banner.headerLeft(),
                    banner == null ? null : banner.headerRight()));
        }

        double gridH = Math.max(1, contentH - 2 * band);
        Region gridNode = grid(contentW, gridH, images, scaleMode, customPercent,
                pagesPerSheet, landscape);
        VBox.setVgrow(gridNode, Priority.ALWAYS);
        content.getChildren().add(gridNode);

        if (bands) {
            content.getChildren().add(band(contentW, band,
                    banner == null ? null : banner.footerLeft(),
                    banner == null ? null : banner.footerRight()));
        }
        return content;
    }

    /** Builds the N-up grid of centered, aspect-fit page images. */
    private static Region grid(double w, double h, List<Image> images, ScaleMode scaleMode,
                               double customPercent, int pagesPerSheet, boolean landscape) {
        int[] cr = grid(pagesPerSheet, landscape);
        int cols = cr[0];
        int rows = cr[1];
        double gap = pagesPerSheet > 1 ? Math.min(w, h) * GAP_FRACTION : 0;

        GridPane gp = new GridPane();
        gp.setHgap(gap);
        gp.setVgap(gap);
        gp.setAlignment(Pos.CENTER);
        gp.setMinSize(w, h);
        gp.setPrefSize(w, h);
        gp.setMaxSize(w, h);

        double cellW = (w - (cols - 1) * gap) / cols;
        double cellH = (h - (rows - 1) * gap) / rows;
        double factor = switch (scaleMode) {
            case CUSTOM -> Math.max(0.05, customPercent / 100.0);
            default -> 1.0;
        };

        for (int i = 0; i < images.size() && i < cols * rows; i++) {
            int row = i / cols;
            int col = i % cols;
            ImageView view = new ImageView(images.get(i));
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setFitWidth(cellW * factor);
            view.setFitHeight(cellH * factor);
            StackPane cell = new StackPane(view);
            cell.setAlignment(Pos.CENTER);
            cell.setMinSize(cellW, cellH);
            cell.setPrefSize(cellW, cellH);
            cell.setMaxSize(cellW, cellH);
            // Clip to the cell so a custom scale above 100% can't bleed into the
            // neighboring N-up cells or past the printable area.
            cell.setClip(new Rectangle(cellW, cellH));
            gp.add(cell, col, row);
        }
        return gp;
    }

    /** A header/footer band: left text, spacer, right text. */
    private static Node band(double w, double h, String left, String right) {
        Label l = bandLabel(left);
        Label r = bandLabel(right);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(l, spacer, r);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, h * 0.2, 0, h * 0.2));
        box.setMinSize(w, h);
        box.setPrefSize(w, h);
        box.setMaxSize(w, h);
        // The print node is never attached to a Scene, so the dialog stylesheet
        // doesn't reach it — set the band font and color inline (Lato is bundled
        // and loaded at startup, so it's identical on every platform). The font
        // size scales with the band so print (points) and preview (pixels) match.
        box.setStyle("-fx-font-family: 'Lato'; -fx-text-fill: #5f6368; "
                + "-fx-font-size: " + (h * 0.5) + "px;");
        box.getStyleClass().add("pdf-print-sheet-band");
        return box;
    }

    private static Label bandLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add("pdf-print-sheet-band-text");
        return label;
    }
}
