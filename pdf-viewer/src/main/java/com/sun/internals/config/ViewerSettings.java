package com.sun.internals.config;

import xss.it.ultimate.pdf.viewer.enums.Fit;
import xss.it.ultimate.pdf.viewer.enums.PageViewMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistent, user-scoped viewer preferences, stored as a small properties file
 * under {@code ~/.ultimate-pdf-viewer/settings.properties}.
 *
 * <p>Holds the predetermined viewer settings the user picks in the Settings
 * dialog (render DPI, default view mode, default fit, …) plus a little remembered
 * UI state (whether the thumbnails panel was last shown). Loading never fails:
 * a missing or corrupt file simply yields the built-in defaults. Saving is
 * best-effort and silently degrades if the location is not writable, so a
 * read-only home directory never breaks the viewer.</p>
 *
 * @author XDSSWAR
 */
public final class ViewerSettings {

    /** Folder under the user home where preferences live. */
    private static final String DIR = ".ultimate-pdf-viewer";
    private static final String FILE = "settings.properties";

    private static final String K_PAGE_DPI = "page.render.dpi";
    private static final String K_THUMB_DPI = "thumbnail.render.dpi";
    private static final String K_VIEW_MODE = "page.view.mode";
    private static final String K_FIT = "fit";
    private static final String K_SHOW_THUMBS = "show.thumbnails";

    /** Defaults — kept in sync with the viewer's own property defaults. */
    public static final double DEF_PAGE_DPI = 300;
    public static final double DEF_THUMB_DPI = 72;
    public static final PageViewMode DEF_VIEW_MODE = PageViewMode.PAGE_BY_PAGE;
    public static final Fit DEF_FIT = Fit.NONE;
    public static final boolean DEF_SHOW_THUMBS = false;

    /** Sensible bounds so a hand-edited file can't push the renderer off a cliff. */
    public static final double MIN_DPI = 72;
    public static final double MAX_DPI = 1200;

    private double pageRenderDpi = DEF_PAGE_DPI;
    private double thumbnailRenderDpi = DEF_THUMB_DPI;
    private PageViewMode pageViewMode = DEF_VIEW_MODE;
    private Fit fit = DEF_FIT;
    private boolean showThumbnails = DEF_SHOW_THUMBS;

    private ViewerSettings() {
    }

    /* ------------------------------------------------------------- load/save */

    /** The settings file path ({@code ~/.ultimate-pdf-viewer/settings.properties}). */
    public static Path file() {
        return Path.of(System.getProperty("user.home", "."), DIR, FILE);
    }

    /**
     * Loads the saved settings, falling back to defaults for anything missing or
     * unreadable. Always returns a usable instance.
     *
     * @return the loaded (or default) settings
     */
    public static ViewerSettings load() {
        ViewerSettings s = new ViewerSettings();
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return s;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException | RuntimeException e) {
            return s; // unreadable/corrupt — use defaults
        }
        s.pageRenderDpi = clampDpi(parseDouble(props.getProperty(K_PAGE_DPI), DEF_PAGE_DPI));
        s.thumbnailRenderDpi = clampDpi(parseDouble(props.getProperty(K_THUMB_DPI), DEF_THUMB_DPI));
        s.pageViewMode = parseEnum(PageViewMode.class, props.getProperty(K_VIEW_MODE), DEF_VIEW_MODE);
        s.fit = parseEnum(Fit.class, props.getProperty(K_FIT), DEF_FIT);
        s.showThumbnails = parseBoolean(props.getProperty(K_SHOW_THUMBS), DEF_SHOW_THUMBS);
        return s;
    }

    /**
     * Writes the current settings to disk, creating the folder if needed.
     * Best-effort: any I/O failure is swallowed so it never disrupts the UI.
     */
    public void save() {
        Path path = file();
        Properties props = new Properties();
        props.setProperty(K_PAGE_DPI, Integer.toString((int) Math.round(pageRenderDpi)));
        props.setProperty(K_THUMB_DPI, Integer.toString((int) Math.round(thumbnailRenderDpi)));
        props.setProperty(K_VIEW_MODE, pageViewMode.name());
        props.setProperty(K_FIT, fit.name());
        props.setProperty(K_SHOW_THUMBS, Boolean.toString(showThumbnails));
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "Ultimate PDF Viewer settings");
            }
        } catch (IOException | RuntimeException ignored) {
            // Read-only home or similar — preferences just won't persist.
        }
    }

    /* --------------------------------------------------------------- parsing */

    private static double clampDpi(double v) {
        return Math.max(MIN_DPI, Math.min(MAX_DPI, v));
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /* -------------------------------------------------------- accessors */

    public double getPageRenderDpi() {
        return pageRenderDpi;
    }

    public void setPageRenderDpi(double pageRenderDpi) {
        this.pageRenderDpi = clampDpi(pageRenderDpi);
    }

    public double getThumbnailRenderDpi() {
        return thumbnailRenderDpi;
    }

    public void setThumbnailRenderDpi(double thumbnailRenderDpi) {
        this.thumbnailRenderDpi = clampDpi(thumbnailRenderDpi);
    }

    public PageViewMode getPageViewMode() {
        return pageViewMode;
    }

    public void setPageViewMode(PageViewMode pageViewMode) {
        this.pageViewMode = pageViewMode == null ? DEF_VIEW_MODE : pageViewMode;
    }

    public Fit getFit() {
        return fit;
    }

    public void setFit(Fit fit) {
        this.fit = fit == null ? DEF_FIT : fit;
    }

    public boolean isShowThumbnails() {
        return showThumbnails;
    }

    public void setShowThumbnails(boolean showThumbnails) {
        this.showThumbnails = showThumbnails;
    }
}
