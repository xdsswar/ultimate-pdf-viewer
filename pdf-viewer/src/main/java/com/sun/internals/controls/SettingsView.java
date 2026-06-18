package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.config.ViewerSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import xss.it.ultimate.pdf.viewer.enums.Fit;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;

import java.util.function.Function;

/**
 * The "Settings" card shown centered inside an {@link OverlayPane}. Lets the user
 * pick predetermined viewer preferences — render DPI, thumbnail DPI, the default
 * page-view mode and fit, and whether the thumbnails panel opens with a document.
 *
 * <p>Saving writes the choices to the persistent {@link ViewerSettings} store and
 * applies them to the viewer immediately via {@link AbstractViewer#applySettings()}.</p>
 *
 * @author XDSSWAR
 */
public final class SettingsView extends VBox {

    /** DPI presets offered in the (editable) render-DPI box. */
    private static final Integer[] DPI_PRESETS = {150, 200, 300, 450, 600, 900};
    private static final Integer[] THUMB_DPI_PRESETS = {48, 72, 96, 144};

    private final AbstractViewer viewer;
    private final ViewerSettings settings;

    private final ComboBox<Integer> pageDpi = new ComboBox<>();
    private final ComboBox<Integer> thumbDpi = new ComboBox<>();
    private final ComboBox<PageViewMode> viewMode = new ComboBox<>();
    private final ComboBox<Fit> fit = new ComboBox<>();
    private final CheckBox showThumbs = new CheckBox("Show the thumbnails panel when a document opens");

    /**
     * Builds the settings card.
     *
     * @param viewer   the owning viewer (settings are applied to it on save)
     * @param settings the persistent settings store (mutated + saved on save)
     * @param iconSvg  SVG path for the header gear badge
     * @param onClose  invoked when the dialog is dismissed (cancel/close/save)
     */
    public SettingsView(AbstractViewer viewer, ViewerSettings settings, String iconSvg, Runnable onClose) {
        this.viewer = viewer;
        this.settings = settings;
        getStyleClass().addAll("pdf-modal-card", "pdf-settings");
        setMaxHeight(USE_PREF_SIZE);

        getChildren().addAll(
                buildHeader(iconSvg, onClose),
                buildBody(),
                buildFooter(onClose));
        loadFromSettings();
    }

    /* ----------------------------------------------------------------- layout */

    private HBox buildHeader(String iconSvg, Runnable onClose) {
        SVGPath gear = new SVGPath();
        gear.setContent(iconSvg);
        gear.getStyleClass().add("pdf-modal-badge-icon");
        StackPane badge = new StackPane(gear);
        badge.getStyleClass().addAll("pdf-modal-badge", "pdf-modal-badge-accent");

        Label title = new Label("Settings");
        title.getStyleClass().add("pdf-modal-title");
        Label subtitle = new Label("Default viewer preferences");
        subtitle.getStyleClass().add("pdf-modal-subtitle");
        VBox titles = new VBox(title, subtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeX = new Button("✕");
        closeX.getStyleClass().add("pdf-modal-close");
        closeX.setFocusTraversable(false);
        closeX.setOnAction(event -> onClose.run());

        HBox header = new HBox(badge, titles, spacer, closeX);
        header.getStyleClass().add("pdf-modal-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox buildBody() {
        pageDpi.setEditable(true);
        pageDpi.setItems(FXCollections.observableArrayList(DPI_PRESETS));
        pageDpi.setConverter(intConverter());
        pageDpi.setMaxWidth(Double.MAX_VALUE);

        thumbDpi.setItems(FXCollections.observableArrayList(THUMB_DPI_PRESETS));
        thumbDpi.setConverter(intConverter());
        thumbDpi.setMaxWidth(Double.MAX_VALUE);

        viewMode.setItems(FXCollections.observableArrayList(PageViewMode.PAGE_BY_PAGE, PageViewMode.CONTINUOUS));
        viewMode.setConverter(converter(m -> m == PageViewMode.CONTINUOUS ? "Continuous" : "Page by Page"));
        viewMode.setMaxWidth(Double.MAX_VALUE);

        fit.setItems(FXCollections.observableArrayList(Fit.NONE, Fit.HORIZONTAL, Fit.VERTICAL));
        fit.setConverter(converter(SettingsView::fitLabel));
        fit.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(
                row("Page render DPI", pageDpi,
                        "Higher is crisper on screen and in print, but uses more memory."),
                row("Thumbnail render DPI", thumbDpi, null),
                row("Default page layout", viewMode, null),
                row("Default fit", fit, null),
                wrap(showThumbs));
        body.getStyleClass().add("pdf-settings-body");
        return body;
    }

    /** A captioned settings row: caption, control, and an optional hint line. */
    private VBox row(String caption, Region control, String hint) {
        Label label = new Label(caption);
        label.getStyleClass().add("pdf-settings-label");
        VBox box = new VBox(label, control);
        box.getStyleClass().add("pdf-settings-row");
        if (hint != null) {
            Label h = new Label(hint);
            h.getStyleClass().add("pdf-settings-hint");
            h.setWrapText(true);
            box.getChildren().add(h);
        }
        return box;
    }

    private VBox wrap(Region control) {
        VBox box = new VBox(control);
        box.getStyleClass().add("pdf-settings-row");
        return box;
    }

    private HBox buildFooter(Runnable onClose) {
        Button reset = new Button("Restore Defaults");
        reset.getStyleClass().add("pdf-modal-button-secondary");
        reset.setFocusTraversable(false);
        reset.setOnAction(event -> loadDefaults());

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("pdf-modal-button-secondary");
        cancel.setCancelButton(true);
        cancel.setOnAction(event -> onClose.run());

        Button save = new Button("Save");
        save.getStyleClass().add("pdf-modal-button");
        save.setDefaultButton(true);
        save.setOnAction(event -> {
            saveAndApply();
            onClose.run();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(reset, spacer, cancel, save);
        footer.getStyleClass().add("pdf-modal-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        return footer;
    }

    /* --------------------------------------------------------------- behavior */

    /** Populates the controls from the current saved settings. */
    private void loadFromSettings() {
        setDpiValue(pageDpi, (int) Math.round(settings.getPageRenderDpi()));
        setDpiValue(thumbDpi, (int) Math.round(settings.getThumbnailRenderDpi()));
        viewMode.setValue(settings.getPageViewMode());
        fit.setValue(settings.getFit());
        showThumbs.setSelected(settings.isShowThumbnails());
    }

    /** Resets the controls (not yet persisted) to the built-in defaults. */
    private void loadDefaults() {
        setDpiValue(pageDpi, (int) Math.round(ViewerSettings.DEF_PAGE_DPI));
        setDpiValue(thumbDpi, (int) Math.round(ViewerSettings.DEF_THUMB_DPI));
        viewMode.setValue(ViewerSettings.DEF_VIEW_MODE);
        fit.setValue(ViewerSettings.DEF_FIT);
        showThumbs.setSelected(ViewerSettings.DEF_SHOW_THUMBS);
    }

    /** Reads the controls into the store, persists, and applies to the viewer. */
    private void saveAndApply() {
        settings.setPageRenderDpi(readDpi(pageDpi, ViewerSettings.DEF_PAGE_DPI));
        settings.setThumbnailRenderDpi(readDpi(thumbDpi, ViewerSettings.DEF_THUMB_DPI));
        settings.setPageViewMode(viewMode.getValue());
        settings.setFit(fit.getValue());
        settings.setShowThumbnails(showThumbs.isSelected());
        settings.save();
        viewer.applySettings();
    }

    /* ----------------------------------------------------------------- helpers */

    /** Sets a combo's value, adding it to the item list if it's not a preset. */
    private static void setDpiValue(ComboBox<Integer> combo, int value) {
        if (!combo.getItems().contains(value)) {
            combo.getItems().add(value);
        }
        combo.setValue(value);
    }

    /** Reads a DPI from an editable combo: the typed editor text wins, then the value. */
    private static double readDpi(ComboBox<Integer> combo, double fallback) {
        String text = combo.isEditable() && combo.getEditor() != null ? combo.getEditor().getText() : null;
        if (text != null && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the selected value
            }
        }
        Integer value = combo.getValue();
        return value != null ? value : fallback;
    }

    private static String fitLabel(Fit f) {
        return switch (f) {
            case HORIZONTAL -> "Fit Width";
            case VERTICAL -> "Fit Height";
            default -> "None (free zoom)";
        };
    }

    private static StringConverter<Integer> intConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                try {
                    return Integer.valueOf(text.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };
    }

    private static <T> StringConverter<T> converter(Function<T, String> toLabel) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toLabel.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null; // display-only
            }
        };
    }
}
