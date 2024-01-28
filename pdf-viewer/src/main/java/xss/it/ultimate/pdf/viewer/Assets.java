package xss.it.ultimate.pdf.viewer;

import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import java.io.InputStream;
import java.net.URL;

/**
 * @author XDSSWAR
 * Created on 09/12/2023
 */
public final class Assets {
    public static final boolean DEBUG = true;

    /**
     * SVG path data for a thumbnail icon.
     */
    public static final String THUMB_SVG = "M18.75 13.5h-3.5a.75.75 0 0 0-.75.75v4.5c0 .414.336.75.75.75h3.5a.75.75 0 0 0 .75-.75v-4.5a.75.75 0 0 0-.75-.75zm-10 0h-3.5a.75.75 0 0 0-.75.75v4.5c0 .414.336.75.75.75h3.5a.75.75 0 0 0 .75-.75v-4.5a.75.75 0 0 0-.75-.75zm10-11h-3.5a.75.75 0 0 0-.75.75v4.5c0 .414.336.75.75.75h3.5a.75.75 0 0 0 .75-.75v-4.5a.75.75 0 0 0-.75-.75zm-10 0h-3.5a.75.75 0 0 0-.75.75v4.5c0 .414.336.75.75.75h3.5a.75.75 0 0 0 .75-.75v-4.5a.75.75 0 0 0-.75-.75z";

    /**
     * SVG path data for an open icon.
     */
    public static final String OPEN_SVG = "M2.5 2C1.675781 2 1 2.675781 1 3.5L1 12.5C1 13.324219 1.675781 14 2.5 14L13.5 14C14.324219 14 15 13.324219 15 12.5L15 5.5C15 4.675781 14.324219 4 13.5 4L6.796875 4L6.144531 2.789063C5.882813 2.300781 5.375 2 4.824219 2 Z M 2.5 3L4.824219 3C5.007813 3 5.175781 3.101563 5.265625 3.261719L6.203125 5L13.5 5C13.78125 5 14 5.21875 14 5.5L14 7L5.21875 7C4.578125 7 4 7.414063 3.796875 8.023438L2.179688 12.878906C2.070313 12.789063 2 12.65625 2 12.5L2 3.5C2 3.21875 2.21875 3 2.5 3 Z M 5.21875 8L14 8L14 12.5C14 12.78125 13.78125 13 13.5 13L3.195313 13L4.746094 8.34375C4.816406 8.136719 5.003906 8 5.21875 8Z";

    /**
     * SVG path data for a save icon.
     */
    public static final String SAVE_SVG = "M2.59375 1C1.71875 1 1 1.71875 1 2.59375L1 12.40625C1 13.28125 1.71875 14 2.59375 14L12.40625 14C13.28125 14 14 13.28125 14 12.40625L14 4.042969L10.957031 1 Z M 2.59375 2L3 2L3 5C3 5.546875 3.453125 6 4 6L10 6C10.546875 6 11 5.546875 11 5L11 2.457031L13 4.457031L13 12.40625C13 12.742188 12.738281 13 12.40625 13L11 13L11 10C11 9.453125 10.546875 9 10 9L5 9C4.453125 9 4 9.453125 4 10L4 13L2.59375 13C2.257813 13 2 12.738281 2 12.40625L2 2.59375C2 2.257813 2.257813 2 2.59375 2 Z M 4 2L7 2L7 4L9 4L9 2L10 2L10 5L4 5 Z M 5 10L10 10L10 13L5 13Z";

     /**
     * SVG path data for a print icon.
     */
    public static final String PRINT_SVG = "M4 2L4 5L2.5 5C1.675781 5 1 5.675781 1 6.5L1 10.5C1 11.324219 1.675781 12 2.5 12L4 12L4 14L12 14L12 12L13.5 12C14.324219 12 15 11.324219 15 10.5L15 6.5C15 5.675781 14.324219 5 13.5 5L12 5L12 2 Z M 5 3L11 3L11 5L5 5 Z M 2.5 6L13.5 6C13.78125 6 14 6.21875 14 6.5L14 10.5C14 10.78125 13.78125 11 13.5 11L12 11L12 9L4 9L4 11L2.5 11C2.21875 11 2 10.78125 2 10.5L2 6.5C2 6.21875 2.21875 6 2.5 6 Z M 3.5 7C3.222656 7 3 7.222656 3 7.5C3 7.777344 3.222656 8 3.5 8C3.777344 8 4 7.777344 4 7.5C4 7.222656 3.777344 7 3.5 7 Z M 5 10L11 10L11 13L5 13Z";

    /**
     * SVG path data for an export icon.
     */
    public static final String EXPORT_SVG = "M7 1L7 9.328125L4.71875 7.140625L4.03125 7.859375L7.5 11.191406L10.96875 7.859375L10.28125 7.140625L8 9.328125L8 1 Z M 1 10L1 12.5C1 13.324219 1.675781 14 2.5 14L12.5 14C13.324219 14 14 13.324219 14 12.5L14 10L13 10L13 12.5C13 12.78125 12.78125 13 12.5 13L2.5 13C2.21875 13 2 12.78125 2 12.5L2 10Z";

    /**
     * SVG content for the first page icon.
     */
    public static final String FIRST_SVG = "M3 6L3 18L5 18L5 12.353516L13 18L13 12.353516L21 18L21 6L13 11.646484L13 6L5 11.646484L5 6L3 6 z";

    /**
     * SVG content for the previous page icon.
     */
    public static final String PREV_SVG = "M11 6L2.5 12L11 18L11 6 z M 20 6L11.5 12L20 18L20 6 z";

    /**
     * SVG content for the next page icon.
     */
    public static final String NEXT_SVG = "M4 6L4 18L12.5 12L4 6 z M 13 6L13 18L21.5 12L13 6 z";

    /**
     * SVG content for the last page icon.
     */
    public static final String LAST_SVG = "M3 6L3 18L11 12.353516L11 18L19 12.353516L19 18L21 18L21 6L19 6L19 11.646484L11 6L11 11.646484L3 6 z";

    /**
     * The SVG content for the zoom in button.
     */
    public static final String ZOOM_IN_ZVG = "M13,11h4v2H13v4H11V13H7V11h4V7h2Zm9,1A10,10,0,1,1,12,2,10,10,0,0,1,22,12Zm-2,0a8,8,0,1,0-8,8A8,8,0,0,0,20,12Z";

    /**
     * The SVG content for the zoom out button.
     */
    public static final String ZOOM_OUT_ZVG = "M7,11H17v2H7Zm15,1A10,10,0,1,1,12,2,10,10,0,0,1,22,12Zm-2,0a8,8,0,1,0-8,8A8,8,0,0,0,20,12Z";

    /**
     * The constant string representing "fit to height" action.
     */
    public static final String FIT_TO_HEIGHT = "M7.5 0.0859375L4.03125 3.554688L4.738281 4.261719L7 2L7 13.023438L4.738281 10.761719L4.03125 11.46875L7.5 14.9375L10.96875 11.46875L10.261719 10.761719L8 13.023438L8 2L10.261719 4.261719L10.96875 3.554688Z";


    /**
     * The constant string representing "fit to width" action.
     */
    public static final String FIT_TO_WIDTH = "M2 2L2 6L3 6L3 3.7089844L6.1464844 6.8535156L6.8535156 6.1464844L3.7089844 3L6 3L6 2L2 2 z M 10 2L10 3L12.291016 3L9.1464844 6.1464844L9.8535156 6.8535156L13 3.7089844L13 6L14 6L14 2L10 2 z M 6.1464844 9.1464844L3 12.291016L3 10L2 10L2 14L6 14L6 13L3.7089844 13L6.8535156 9.8535156L6.1464844 9.1464844 z M 9.8535156 9.1464844L9.1464844 9.8535156L12.291016 13L10 13L10 14L14 14L14 10L13 10L13 12.291016L9.8535156 9.1464844 z";


    /**
     * Loader
     */
    public static Image LOADER = new Image(load("/xss/it/ultimate/pdf/viewer/images/loader.gif").toExternalForm());

    /**
     * PDF
     */
    public static final FileChooser.ExtensionFilter PDF_EXTENSION_FILTER = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf");


    /**
     * This method loads a URL for a given location.
     * @param location The location of the resource to load.
     * @return A URL object representing the resource's location.
     */
    public static URL load(final String location){
        return Assets.class.getResource(location);
    }

    /**
     * Retrieves an InputStream for a given resource location using the class loader.
     *
     * @param location The resource location.
     * @return An InputStream for the specified resource.
     */
    public static InputStream stream(final String location){
        return Assets.class.getResourceAsStream(location);
    }

}
