package xss.it.nfx.pdfium;

/**
 * Supplies a password on demand when opening an encrypted document.
 *
 * <p>Pass one of these to {@code PdfDocument.open(..., PdfPasswordCallback)}. It
 * is invoked only if the document is encrypted, and again for each wrong
 * password, so a UI can prompt and retry. Return {@code null} to cancel — the
 * open then fails with a {@link PdfException}.</p>
 *
 * <pre>{@code
 * PdfDocument doc = PdfDocument.open(file, attempt ->
 *     showPasswordDialog(attempt == 0 ? "Password:" : "Wrong password, retry:"));
 * }</pre>
 *
 * @author XDSSWAR
 */
@FunctionalInterface
public interface PdfPasswordCallback {

    /**
     * Returns the password to try.
     *
     * @param attempt the zero-based attempt number (0 = first prompt)
     * @return the password to try, or {@code null} to give up
     */
    String getPassword(int attempt);
}
