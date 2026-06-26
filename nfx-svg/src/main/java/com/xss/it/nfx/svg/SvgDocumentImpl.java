package com.xss.it.nfx.svg;

import com.xss.it.nfx.svg.ffm.Svg;
import com.xss.it.nfx.svg.ffm.SvgFfmException;
import com.xss.it.nfx.svg.ffm.SvgNative;
import xss.it.nfx.svg.SvgDocument;
import xss.it.nfx.svg.SvgException;

/**
 * Default {@link SvgDocument} implementation backed by a native Skia handle.
 * Internal - consumers only ever see the {@link SvgDocument} interface.
 *
 * @author XDSSWAR
 */
public final class SvgDocumentImpl implements SvgDocument {

    /** The native document. */
    private final SvgNative document;

    private boolean closed;

    /**
     * Parses a document from bytes.
     *
     * @param bytes the SVG bytes
     */
    public SvgDocumentImpl(byte[] bytes) {
        try {
            this.document = Svg.load(bytes);
        } catch (SvgFfmException e) {
            throw new SvgException("Failed to parse SVG", e);
        }
    }

    /** Native handle, for internal helpers (renderer). */
    public SvgNative native_() {
        return document;
    }

    @Override
    public double getWidth() {
        return document.width();
    }

    @Override
    public double getHeight() {
        return document.height();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        document.close();
    }
}
