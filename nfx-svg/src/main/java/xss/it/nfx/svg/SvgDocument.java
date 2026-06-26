package xss.it.nfx.svg;

import com.xss.it.nfx.svg.SvgDocumentImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A parsed SVG document - the entry point to the nfx-svg engine.
 *
 * <p>Load a document with one of the {@code load(...)} factories (from a file,
 * URL, stream, byte array, or a raw SVG {@link #loadContent(String) string}),
 * then read its intrinsic size or {@link #render(double, Color) render} it to a
 * crisp JavaFX image at any scale. A document owns native resources and should
 * be {@link #close() closed} when no longer needed (it is {@link AutoCloseable},
 * so a try-with-resources block is ideal). Loading copies the source bytes into
 * native memory, so the caller may discard the input immediately.</p>
 *
 * <pre>{@code
 * SvgView view = new SvgView(SvgDocument.load(Path.of("logo.svg")));
 * view.setFitWidth(256);   // crisp at any size, DPI-aware
 * }</pre>
 *
 * <p>Rendering is intentionally not part of this API: display an SVG through the
 * interactive, DPI-aware {@link xss.it.nfx.svg.scene.SvgView} node, which
 * re-rasterizes the vectors crisply at every size and zoom.</p>
 *
 * @author XDSSWAR
 */
public interface SvgDocument extends AutoCloseable {

    /**
     * Parses a document from raw bytes.
     *
     * @param bytes the SVG bytes (UTF-8 or UTF-16 XML)
     * @return the parsed document
     * @throws SvgException if the document cannot be parsed
     */
    static SvgDocument load(byte[] bytes) {
        return new SvgDocumentImpl(bytes);
    }

    /**
     * Parses a document from raw SVG markup.
     *
     * @param svgMarkup the SVG document text
     * @return the parsed document
     * @throws SvgException if the document cannot be parsed
     */
    static SvgDocument loadContent(String svgMarkup) {
        if (svgMarkup == null) {
            throw new SvgException("SVG content is null");
        }
        return load(svgMarkup.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses a document from a file path.
     *
     * @param file the SVG file
     * @return the parsed document
     * @throws UncheckedIOException if the file cannot be read
     */
    static SvgDocument load(Path file) {
        try {
            return load(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }
    }

    /**
     * Parses a document from a file.
     *
     * @param file the SVG file
     * @return the parsed document
     */
    static SvgDocument load(File file) {
        return load(file.toPath());
    }

    /**
     * Parses a document from a stream. The stream is fully read but not closed.
     *
     * @param stream the SVG stream
     * @return the parsed document
     * @throws UncheckedIOException if the stream cannot be read
     */
    static SvgDocument load(InputStream stream) {
        try {
            return load(stream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read SVG stream", e);
        }
    }

    /**
     * Parses a document from a URL (file, http, https, classpath resource, ...).
     *
     * @param url the SVG URL
     * @return the parsed document
     * @throws UncheckedIOException if the URL cannot be read
     */
    static SvgDocument load(URL url) {
        try (InputStream in = url.openStream()) {
            return load(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + url, e);
        }
    }

    /**
     * Parses a document from a URI.
     *
     * @param uri the SVG URI
     * @return the parsed document
     * @throws UncheckedIOException if the URI cannot be read
     */
    static SvgDocument load(URI uri) {
        try {
            return load(uri.toURL());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + uri, e);
        }
    }

    /**
     * The document's intrinsic width in user units (CSS px), resolved from its
     * {@code width}/{@code height}, else its {@code viewBox}, else the SVG
     * default.
     *
     * @return the intrinsic width
     */
    double getWidth();

    /**
     * The document's intrinsic height in user units (CSS px).
     *
     * @return the intrinsic height
     */
    double getHeight();

    /**
     * Whether this document has been closed.
     *
     * @return {@code true} if closed
     */
    boolean isClosed();

    /**
     * Closes the document and releases its native resources. Idempotent.
     */
    @Override
    void close();
}
