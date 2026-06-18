package xss.it.nfx.pdfium.text;

/**
 * Options controlling how a search term is matched against page text.
 *
 * <p>The flags compose: a query may, for example, be matched case-sensitively
 * <em>and</em> as a whole word. They map onto the behaviour of a browser-style
 * find bar (Match Case / Whole Words / Match Diacritics).</p>
 *
 * @param matchCase       when {@code true} the search is case-sensitive; when
 *                        {@code false} {@code A} matches {@code a}
 * @param matchWholeWords when {@code true} a match must be bounded by
 *                        non-alphanumeric characters (or the page edges), so
 *                        {@code "cat"} does not match inside {@code "category"}
 * @param matchDiacritics when {@code true} diacritics must match exactly; when
 *                        {@code false} they are ignored, so {@code "cafe"}
 *                        matches {@code "café"} and vice versa
 * @author XDSSWAR
 */
public record SearchOptions(boolean matchCase, boolean matchWholeWords, boolean matchDiacritics) {

    /**
     * The default options: case-insensitive, diacritics-insensitive and not
     * restricted to whole words — the most permissive match, and the behaviour
     * of the single-argument {@code search(String)} methods.
     */
    public static final SearchOptions DEFAULT = new SearchOptions(false, false, false);

    /**
     * Returns options identical to these but with the given case sensitivity.
     *
     * @param matchCase whether the search should be case-sensitive
     * @return the adjusted options
     */
    public SearchOptions withMatchCase(boolean matchCase) {
        return new SearchOptions(matchCase, matchWholeWords, matchDiacritics);
    }

    /**
     * Returns options identical to these but with the given whole-word setting.
     *
     * @param matchWholeWords whether the search should match whole words only
     * @return the adjusted options
     */
    public SearchOptions withMatchWholeWords(boolean matchWholeWords) {
        return new SearchOptions(matchCase, matchWholeWords, matchDiacritics);
    }

    /**
     * Returns options identical to these but with the given diacritics setting.
     *
     * @param matchDiacritics whether diacritics must match exactly
     * @return the adjusted options
     */
    public SearchOptions withMatchDiacritics(boolean matchDiacritics) {
        return new SearchOptions(matchCase, matchWholeWords, matchDiacritics);
    }
}
