package com.sun.internals.render;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.RenderDestination;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.nio.IntBuffer;

/**
 * @author XDSSWAR
 * Created on 09/12/2023
 */
public final class Render extends PDFRenderer {

    /**
     * Creates a new PDFRenderer.
     *
     * @param document the document to render
     */
    public Render(PDDocument document) {
        super(document);
    }


    /**
     * <p>
     * Renders the image of a specific page in the specified image type and scale.
     * </p>
     *
     * @param pageIndex The index of the page to render the image from.
     * @param imageType The ImageType specifying the type of the rendered image.
     * @param scale     The scale to apply to the rendered image.
     * @return The rendered image as a JavaFX Image object.
     * @throws IOException If an I/O error occurs during rendering.
     */
    public Image renderImage(int pageIndex, ImageType imageType, float scale) throws IOException {
        return toFxImage(
                renderImage(pageIndex,
                        scale,
                        imageType,
                        RenderDestination.VIEW
                )
        );
    }

    /**
     * Converts a BufferedImage to a JavaFX Image.
     *
     * @param bufferedImage The BufferedImage to be converted.
     * @return The JavaFX Image equivalent of the input BufferedImage.
     */
    private static Image toFxImage(BufferedImage bufferedImage) {
        int bw = bufferedImage.getWidth();
        int bh = bufferedImage.getHeight();
        switch (bufferedImage.getType()) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                break;
            default:
                BufferedImage converted =
                        new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2d = converted.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_RESOLUTION_VARIANT, RenderingHints.VALUE_RESOLUTION_VARIANT_DPI_FIT);

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