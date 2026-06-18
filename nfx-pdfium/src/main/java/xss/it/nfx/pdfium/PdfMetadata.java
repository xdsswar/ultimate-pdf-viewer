package xss.it.nfx.pdfium;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Document-level metadata read from a PDF's Info dictionary and trailer.
 *
 * <p>String fields hold the raw values exactly as stored in the document and
 * are never {@code null} — an absent entry is the empty string. The creation
 * and modification timestamps are exposed both as their raw PDF date strings
 * ({@link #creationDateRaw()} / {@link #modificationDateRaw()}) and parsed into
 * {@link LocalDateTime} via {@link #creationDate()} / {@link #modificationDate()}.</p>
 *
 * @param title                the document title
 * @param author               the author
 * @param subject              the subject
 * @param keywords             the keywords
 * @param creator              the application that created the original document
 * @param producer             the application that produced the PDF
 * @param creationDateRaw      the raw {@code CreationDate} string (PDF date format)
 * @param modificationDateRaw  the raw {@code ModDate} string (PDF date format)
 * @param version              the PDF version as an integer (e.g. {@code 14} for
 *                             1.4), or {@code -1} if unknown
 *
 * @author XDSSWAR
 */
public record PdfMetadata(
        String title,
        String author,
        String subject,
        String keywords,
        String creator,
        String producer,
        String creationDateRaw,
        String modificationDateRaw,
        int version) {

    /**
     * The PDF version in dotted form, e.g. {@code "1.4"}, or an empty string if
     * the version is unknown.
     *
     * @return the dotted version string
     */
    public String versionString() {
        if (version <= 0) {
            return "";
        }
        return (version / 10) + "." + (version % 10);
    }

    /**
     * The parsed creation timestamp, if the raw value is a recognizable PDF date.
     *
     * @return the creation date, or empty if absent/unparseable
     */
    public Optional<LocalDateTime> creationDate() {
        return parsePdfDate(creationDateRaw);
    }

    /**
     * The parsed modification timestamp, if the raw value is a recognizable PDF date.
     *
     * @return the modification date, or empty if absent/unparseable
     */
    public Optional<LocalDateTime> modificationDate() {
        return parsePdfDate(modificationDateRaw);
    }

    /**
     * Parses a PDF date string ({@code D:YYYYMMDDHHmmSS...} with an optional
     * leading {@code D:} and trailing time-zone offset) into a local date-time.
     * Missing trailing components default to their lowest legal value; the
     * time-zone offset is ignored.
     *
     * @param raw the raw PDF date string (may be {@code null} or empty)
     * @return the parsed date-time, or empty if it cannot be parsed
     */
    static Optional<LocalDateTime> parsePdfDate(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.startsWith("D:")) {
            s = s.substring(2);
        }
        // Collect the leading run of digits (the date-time); stop at the
        // time-zone marker (+, -, Z) or any apostrophe separators.
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        String n = digits.toString();
        if (n.length() < 4) {
            return Optional.empty();
        }
        try {
            int year = Integer.parseInt(n.substring(0, 4));
            int month = field(n, 4, 1);
            int day = field(n, 6, 1);
            int hour = field(n, 8, 0);
            int minute = field(n, 10, 0);
            int second = field(n, 12, 0);
            return Optional.of(LocalDateTime.of(
                    year,
                    clamp(month, 1, 12),
                    clamp(day, 1, 31),
                    clamp(hour, 0, 23),
                    clamp(minute, 0, 59),
                    clamp(second, 0, 59)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Reads a two-digit field at {@code start}, or {@code def} if not present. */
    private static int field(String n, int start, int def) {
        if (start + 2 > n.length()) {
            return def;
        }
        return Integer.parseInt(n.substring(start, start + 2));
    }

    /** Clamps {@code v} into {@code [lo, hi]}. */
    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
