package xss.it.nfx.pdfium;

import com.xss.it.nfx.pdfium.PdfDocumentImpl;
import xss.it.nfx.pdfium.text.PdfSearchResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * An open PDF document — the entry point to the nfx-pdfium engine.
 *
 * <p>Open a document with one of the {@code open(...)} factories, then read its
 * pages, text and search results. A document owns native resources and must be
 * {@link #close() closed} when no longer needed (it is {@link AutoCloseable}, so
 * a try-with-resources block is ideal). Opening copies the source bytes into
 * native memory, so the caller may discard the input immediately.</p>
 *
 * <pre>{@code
 * try (PdfDocument doc = PdfDocument.open(Path.of("file.pdf"))) {
 *     PdfPage page = doc.getPage(0);
 *     Image img = page.render(2.0, 0);   // 144 DPI
 *     String text = page.getText();
 * }
 * }</pre>
 *
 * <p>The engine is not thread-safe; calls are serialized internally, but callers
 * should treat a document as owned by one logical flow at a time.</p>
 *
 * @author XDSSWAR
 */
public interface PdfDocument extends AutoCloseable {

    /**
     * Opens a document from raw bytes.
     *
     * @param bytes    the PDF bytes
     * @param password the password, or {@code null} if the document is not encrypted
     * @return the opened document
     * @throws PdfException if the document cannot be opened
     */
    static PdfDocument open(byte[] bytes, String password) {
        return new PdfDocumentImpl(bytes, password);
    }

    /**
     * Opens an unencrypted document from raw bytes.
     *
     * @param bytes the PDF bytes
     * @return the opened document
     */
    static PdfDocument open(byte[] bytes) {
        return open(bytes, (String) null);
    }

    /**
     * Opens a (possibly encrypted) document, prompting for a password via the
     * callback if needed.
     *
     * @param bytes    the PDF bytes
     * @param callback supplies passwords on demand
     * @return the opened document
     * @throws PdfException if opening fails or the password prompt is cancelled
     */
    static PdfDocument open(byte[] bytes, PdfPasswordCallback callback) {
        return new PdfDocumentImpl(bytes, callback);
    }

    /**
     * Opens a (possibly encrypted) document from a file, prompting for a password
     * via the callback if needed.
     *
     * @param file     the PDF file
     * @param callback supplies passwords on demand
     * @return the opened document
     */
    static PdfDocument open(Path file, PdfPasswordCallback callback) {
        try {
            return open(Files.readAllBytes(file), callback);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }
    }

    /**
     * Opens a (possibly encrypted) document from a file, prompting for a password
     * via the callback if needed.
     *
     * @param file     the PDF file
     * @param callback supplies passwords on demand
     * @return the opened document
     */
    static PdfDocument open(File file, PdfPasswordCallback callback) {
        return open(file.toPath(), callback);
    }

    /**
     * Opens a document from a file path.
     *
     * @param file the PDF file
     * @return the opened document
     * @throws UncheckedIOException if the file cannot be read
     */
    static PdfDocument open(Path file) {
        try {
            return open(Files.readAllBytes(file), (String) null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }
    }

    /**
     * Opens a document from a file.
     *
     * @param file the PDF file
     * @return the opened document
     */
    static PdfDocument open(File file) {
        return open(file.toPath());
    }

    /**
     * Opens a document from a stream. The stream is fully read but not closed.
     *
     * @param stream the PDF stream
     * @return the opened document
     * @throws UncheckedIOException if the stream cannot be read
     */
    static PdfDocument open(InputStream stream) {
        try {
            return open(stream.readAllBytes(), (String) null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF stream", e);
        }
    }

    /**
     * The number of pages in the document.
     *
     * @return the page count
     */
    int getPageCount();

    /**
     * Returns a page by index. Pages are cached, so repeated calls for the same
     * index return the same instance.
     *
     * @param index the zero-based page index
     * @return the page
     */
    PdfPage getPage(int index);

    /**
     * Extracts the text of the whole document, pages separated by newlines.
     *
     * @return the document text
     */
    String getText();

    /**
     * Searches the entire document for the given text.
     *
     * @param query the text to find (case-insensitive)
     * @return all matches across all pages, in page then reading order
     */
    List<PdfSearchResult> search(String query);

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
