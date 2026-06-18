package com.sun.internals.controls;

import com.sun.internals.document.Document;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfMetadata;
import xss.it.nfx.pdfium.PdfPage;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/**
 * The "Document Properties" card shown centered inside an {@link OverlayPane}.
 *
 * <p>Presents the document's file info ({@link Document#getFileName() name},
 * {@link Document#getFileSize() size}, {@link Document#isFastWebView()
 * linearization}) together with the engine {@link PdfMetadata metadata}
 * (title, author, dates, producer, version, …) and the first page's size,
 * grouped into File / Description / Document sections.</p>
 *
 * @author XDSSWAR
 */
public final class DocPropertiesView extends VBox {

    /** Placeholder shown for empty/absent values. */
    private static final String EMPTY = "—";

    /** Common paper sizes in PDF points (short side, long side), for naming. */
    private static final double[][] PAPER_SIZES = {
            {612, 792, 0},   // Letter
            {612, 1008, 1},  // Legal
            {792, 1224, 2},  // Tabloid
            {595, 842, 3},   // A4
            {842, 1191, 4},  // A3
            {420, 595, 5},   // A5
    };
    private static final String[] PAPER_NAMES = {"Letter", "Legal", "Tabloid", "A4", "A3", "A5"};

    /**
     * Builds the properties card for the given document.
     *
     * @param document the document to describe (must be non-null)
     * @param iconSvg  SVG path for the header badge icon
     * @param onClose  invoked when the dialog is dismissed
     */
    public DocPropertiesView(Document document, String iconSvg, Runnable onClose) {
        getStyleClass().addAll("pdf-modal-card", "pdf-doc-properties");
        // Stay at the natural content height so the card is centered by the
        // overlay rather than stretched to fill it; width stays bounded by CSS.
        setMaxHeight(USE_PREF_SIZE);

        PdfDocument pdf = document.getPdfDocument();
        PdfMetadata meta = pdf.getMetadata();

        getChildren().addAll(
                buildHeader(document, iconSvg, onClose),
                section("File",
                        "File name", orEmpty(document.getFileName()),
                        "File size", formatSize(document.getFileSize())),
                section("Description",
                        "Title", orEmpty(meta.title()),
                        "Author", orEmpty(meta.author()),
                        "Subject", orEmpty(meta.subject()),
                        "Keywords", orEmpty(meta.keywords()),
                        "Created", formatDate(meta.creationDate()),
                        "Modified", formatDate(meta.modificationDate()),
                        "Creator", orEmpty(meta.creator())),
                section("Document",
                        "PDF Producer", orEmpty(meta.producer()),
                        "PDF Version", orEmpty(meta.versionString()),
                        "Page Count", String.valueOf(document.getNumberOfPages()),
                        "Page Size", formatPageSize(pdf),
                        "Fast Web View", document.isFastWebView() ? "Yes" : "No"));

        Button close = new Button("Close");
        close.getStyleClass().add("pdf-modal-button");
        close.setOnAction(event -> onClose.run());
        HBox footer = new HBox(close);
        footer.getStyleClass().add("pdf-modal-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(footer);
    }

    /* ----------------------------------------------------------------- layout */

    /** Builds the icon-badge + title header with a corner close button. */
    private HBox buildHeader(Document document, String iconSvg, Runnable onClose) {
        SVGPath icon = new SVGPath();
        icon.setContent(iconSvg);
        icon.getStyleClass().add("pdf-modal-badge-icon");
        StackPane badge = new StackPane(icon);
        badge.getStyleClass().add("pdf-modal-badge");

        Label title = new Label("Document Properties");
        title.getStyleClass().add("pdf-modal-title");
        String name = document.getFileName();
        Label subtitle = new Label(name != null && !name.isBlank() ? name : "Untitled document");
        subtitle.getStyleClass().add("pdf-modal-subtitle");
        VBox titles = new VBox(title, subtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeX = new Button("✕");
        closeX.getStyleClass().add("pdf-modal-close");
        closeX.setOnAction(event -> onClose.run());

        HBox header = new HBox(badge, titles, spacer, closeX);
        header.getStyleClass().add("pdf-modal-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Builds a titled section: a small-caps label above a label/value grid.
     * The {@code pairs} are flattened label/value strings.
     */
    private VBox section(String title, String... pairs) {
        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("pdf-section-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("pdf-doc-properties-grid");
        ColumnConstraints keys = new ColumnConstraints();
        keys.setHalignment(HPos.LEFT);
        keys.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(keys, values);

        for (int i = 0, row = 0; i < pairs.length; i += 2, row++) {
            Label key = new Label(pairs[i]);
            key.getStyleClass().add("pdf-doc-properties-key");
            Label value = new Label(pairs[i + 1]);
            value.getStyleClass().add("pdf-doc-properties-value");
            value.setWrapText(true);
            grid.add(key, 0, row);
            grid.add(value, 1, row);
        }

        VBox box = new VBox(label, grid);
        box.getStyleClass().add("pdf-section");
        return box;
    }

    /* ------------------------------------------------------------ formatting */

    /** Returns the value, or the {@link #EMPTY} placeholder when blank. */
    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? EMPTY : value;
    }

    /** Formats a parsed date as a localized short-date / medium-time string. */
    private static String formatDate(Optional<LocalDateTime> date) {
        return date.map(d -> d.format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)))
                .orElse(EMPTY);
    }

    /** Formats a byte count like {@code "992 KB (1,016,315 bytes)"}. */
    private static String formatSize(long bytes) {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        String exact = nf.format(bytes) + " bytes";
        if (bytes < 1024) {
            return exact;
        }
        if (bytes < 1024L * 1024L) {
            return Math.round(bytes / 1024.0) + " KB (" + exact + ")";
        }
        return Math.round(bytes / (1024.0 * 1024.0)) + " MB (" + exact + ")";
    }

    /** Formats the first page's size, e.g. {@code "8.5 × 11 in (Letter, portrait)"}. */
    private static String formatPageSize(PdfDocument pdf) {
        if (pdf.getPageCount() <= 0) {
            return EMPTY;
        }
        PdfPage page = pdf.getPage(0);
        double wPt = page.getWidth();
        double hPt = page.getHeight();
        String dims = trim(wPt / 72.0) + " × " + trim(hPt / 72.0) + " in";
        String orientation = wPt > hPt ? "landscape" : "portrait";
        String paper = paperName(wPt, hPt);
        String descriptor = paper != null ? paper + ", " + orientation : orientation;
        return dims + " (" + descriptor + ")";
    }

    /** Matches a page size against common paper formats (orientation-agnostic). */
    private static String paperName(double wPt, double hPt) {
        double shortSide = Math.min(wPt, hPt);
        double longSide = Math.max(wPt, hPt);
        for (double[] size : PAPER_SIZES) {
            if (Math.abs(shortSide - size[0]) <= 4 && Math.abs(longSide - size[1]) <= 4) {
                return PAPER_NAMES[(int) size[2]];
            }
        }
        return null;
    }

    /** Formats a dimension with up to two decimals, dropping trailing zeros. */
    private static String trim(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        String s = String.valueOf(rounded);
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }
}
