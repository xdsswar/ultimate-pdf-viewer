package xss.it.ultimate.pdf.viewer;

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
