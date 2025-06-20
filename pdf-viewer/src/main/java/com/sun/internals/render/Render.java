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

package com.sun.internals.render;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.RenderDestination;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author XDSSWAR
 * Created on 10/17/2024
 */
public final class Render extends PDFRenderer {
    /**
     * A lock used to ensure thread-safe access to shared resources.
     * This ReentrantLock allows for explicit locking mechanisms in concurrent operations.
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Constructs a Render instance with the specified PDDocument.
     *
     * @param document the PDDocument to be used for rendering
     */
    public Render(PDDocument document) {
        super(document);
    }

    /**
     * Renders a specific page of the PDF as a PdfImage with the given scale and color scheme.
     *
     * @param pageIndex the index of the page to render (0-based)
     * @param scale the scale factor for rendering the page
     * @return the rendered page as a PdfImage
     * @throws IOException if an I/O error occurs during rendering
     */
    public BufferedImage render(int pageIndex, float scale) throws IOException {
        lock.lock();
        BufferedImage image;
        try {
            image =  renderImage(pageIndex, scale, ImageType.RGB ,RenderDestination.VIEW);
        } finally {
            lock.unlock();
        }
        return image;
    }



}
