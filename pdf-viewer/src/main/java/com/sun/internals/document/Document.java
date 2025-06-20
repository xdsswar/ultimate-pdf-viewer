package com.sun.internals.document;

import com.sun.internals.PageData;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.print.Pageable;
import java.io.IOException;
import java.nio.IntBuffer;

/**
 * @author XDSSWAR
 * Created on 09/16/2023
 */
public interface Document {

    /**
     * List of pages with additional properties. Data source for thumbnail list
     */
    ObservableList<PageData> getPagesList();

    /**
     * Sets rotation for given page
     */
    void setPageRotation(int pageNumber, double rotationAngle);

    /**
     * Gets rotation for given page
     */
    PageData getPageRotation(int pageNumber);

    /**
     * Sets viewport for given page
     */
    void setViewport(int pageNumber, Rectangle2D viewport);

    /**
     * Renders the page specified by the given number at the given scale.
     *
     * @param pageNumber   the page number
     * @param scale        the scale
     * @param rotationAngle the rotation angle
     * @param useCache     cache the page (used for thumbnails)
     * @return the generated fx image
     */
    BufferedImage renderPage(int pageNumber, float scale, double rotationAngle, boolean useCache) throws IOException;

    /**
     * Returns the total number of pages inside the document.
     *
     * @return the total number of pages
     */
    int getNumberOfPages();

    /**
     * Determines if the given page has a landscape orientation.
     *
     * @param pageNumber the page
     * @return true if the page has to be shown in landscape mode
     */
    boolean isLandscape(int pageNumber);

    /**
     * Gets the PDDocument associated with this instance of TextStripper.
     *
     * @return The PDDocument associated with this TextStripper.
     */
    PDDocument getDocument();

    /**
     * Closes the document.
     */
    void close() throws IOException;

    /**
     * Gets a Pageable object representing paginated content.
     *
     * @return A Pageable object for paginated content.
     */
    Pageable getPageable();


    /**
     * Gets the image type.
     * @return the image type.
     */
    ImageType getImageType();

    /**
     * Sets the image type.
     * @param imageType the image type to set.
     */
    void setImageType(ImageType imageType);


    /**
     * Converts a BufferedImage to a JavaFX Image, preserving transparency and color information.
     * If the BufferedImage is not of type ARGB, it is first converted to ensure compatibility.
     *
     * @param bufferedImage the BufferedImage to convert.
     * @return the converted JavaFX Image.
     */
    static Image toFxImage(BufferedImage bufferedImage) {
        int bw = bufferedImage.getWidth();
        int bh = bufferedImage.getHeight();
        switch (bufferedImage.getType()) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                break;
            default:
                BufferedImage converted =  new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2d = converted.createGraphics();
                g2d.setRenderingHint(
                        RenderingHints.KEY_RESOLUTION_VARIANT,
                        RenderingHints.VALUE_RESOLUTION_VARIANT_DPI_FIT
                );
                g2d.drawImage(bufferedImage, 0, 0, null);
                g2d.dispose();
                bufferedImage = converted;
                break;
        }

        WritableImage writableImage = new WritableImage(bw, bh);
        PixelWriter pw = writableImage.getPixelWriter();
        DataBufferInt db = (DataBufferInt)bufferedImage.getRaster().getDataBuffer();
        int[] data = db.getData();
        int offset = bufferedImage.getRaster().getDataBuffer().getOffset();
        int scan =  0;
        SampleModel sm = bufferedImage.getRaster().getSampleModel();
        if (sm instanceof SinglePixelPackedSampleModel) {
            scan = ((SinglePixelPackedSampleModel)sm).getScanlineStride();
        }
        PixelFormat<IntBuffer> pf = (bufferedImage.isAlphaPremultiplied() ?
                PixelFormat.getIntArgbPreInstance() :
                PixelFormat.getIntArgbInstance());
        pw.setPixels(0, 0, bw, bh, pf, data, offset, scan);
        return writableImage;
    }
}
