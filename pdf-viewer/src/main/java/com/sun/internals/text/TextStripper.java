package com.sun.internals.text;

import com.sun.internals.PdfDocument;
import javafx.geometry.Rectangle2D;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author XDSSWAR
 * Created on 01/24/2024
 */
public final class TextStripper extends PDFTextStripper {
    /**
     * The current index indicating the position within the search results.
     */
    private int index;

    /**
     * A list of SearchResult objects representing search results in the PDF document.
     */
    private final List<SearchResult> searchResults;

    /**
     * The text that was searched for within the PDF document.
     */
    private final String searchText;

    private final PdfDocument document;


    /**
     * Constructs a TextStripper object for extracting text from the provided PDF document
     * and searching for the specified text.
     *
     * @param document   The PdfDocument to extract text from.
     * @param searchText The text to search for within the PDF document.
     * @throws IOException If there is an error initializing the TextStripper.
     */
    public TextStripper(PdfDocument document, String searchText) throws IOException {
        super();
        this.document = document;
        this.searchText = searchText;
        this.index = -1;
        this.searchResults = new ArrayList<>();
    }

    /**
     * Initializes processing for the specified PDF page.
     *
     * @param page The PDPage object representing the PDF page to be processed.
     */
    @Override
    protected void startPage(PDPage page) {
        index++;
    }

    /**
     * Writes the specified text using the positions provided by the list of TextPosition objects.
     *
     * @param text           The text to be written.
     * @param textPositions  A list of TextPosition objects representing the positions of characters in the text.
     */
    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        if (StringUtils.containsIgnoreCase(text, searchText)) {
            SearchResult sr = new SearchResult(searchText, text, index, calculateMarkerPosition(searchText, text, textPositions));
            searchResults.add(sr);
        }
    }

    /**
     * Gets the list of search results found within the PDF document.
     *
     * @return A list of SearchResult objects representing search results.
     */
    public List<SearchResult> getSearchResults() throws IOException {
        this.writeText(this.document.getDocument(), new DummyWriter());
        return searchResults;
    }

    /**
     * Calculates the position (e.g., a rectangle) within a list of TextPosition objects where the specified
     * searchText matches snippetText based on given search criteria.
     *
     * @param searchText     The text to search for within snippetText.
     * @param snippetText    The text snippet to search within.
     * @param textPositions  A list of TextPosition objects representing the positions of characters in snippetText.
     * @return The calculated position (e.g., a rectangle), or null if no match is found.
     */
    private Rectangle2D calculateMarkerPosition(String searchText, String snippetText, List<TextPosition> textPositions) {
        int textPositionStartIndex = calculateTextPositionStartIndex(searchText, snippetText, textPositions);

        float x1 = Float.MAX_VALUE;
        float x2 = 0;
        float y1 = Float.MAX_VALUE;
        float y2 = 0;

        for (int textPositionIndex = textPositionStartIndex; textPositionIndex < textPositionStartIndex + searchText.length(); textPositionIndex++) {
            TextPosition position = textPositions.get(textPositionIndex);

            x1 = Math.min(x1, position.getXDirAdj());
            x2 = Math.max(x2, position.getXDirAdj() + position.getWidth());
            y1 = Math.min(y1, position.getYDirAdj() - position.getHeight());
            y2 = Math.max(y2, position.getYDirAdj());
        }

        x1 -= 2;
        x2 += 2;
        y1 -= 2;
        y2 += 2;

        return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
    }


    /**
     * Calculates the start index within a list of TextPosition objects where the specified searchText
     * begins to match snippetText based on given search criteria.
     *
     * @param searchText     The text to search for within snippetText.
     * @param snippetText    The text snippet to search within.
     * @param textPositions  A list of TextPosition objects representing the positions of characters in snippetText.
     * @return The calculated start index, or -1 if no match is found.
     */
    private int calculateTextPositionStartIndex(String searchText, String snippetText, List<TextPosition> textPositions) {
        int snippetTextStartIndex = snippetText.toLowerCase().indexOf(searchText.toLowerCase());
        int startIndexDecreaseDelta = 0;
        // If any TextPosition (up to the snippetTextStartIndex) contains more then one character, we have to account for that.
        for (int i = 0; i < snippetTextStartIndex; i++) {
            int numberOfCharactersInTextPosition = textPositions.get(i).getUnicode().length();
            if (numberOfCharactersInTextPosition > 1) {
                startIndexDecreaseDelta = startIndexDecreaseDelta + (numberOfCharactersInTextPosition - 1);
            }
        }
        return snippetTextStartIndex - startIndexDecreaseDelta;
    }


    /**
     * A custom Writer implementation that does nothing when writing, flushing, or closing.
     * It is used as a placeholder or dummy writer when no actual writing is needed.
     */
    private static class DummyWriter extends Writer {

        @Override
        public void write(char[] cBuf, int off, int len) {
            // Empty implementation, does nothing.
        }

        @Override
        public void flush() {
            // Empty implementation, does nothing.
        }

        @Override
        public void close() {
            // Empty implementation, does nothing.
        }
    }

}
