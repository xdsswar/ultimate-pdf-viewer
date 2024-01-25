package com.sun.internals.document;

import com.sun.internals.text.SearchResult;

import java.io.IOException;
import java.util.List;

/**
 * @author XDSSWAR
 * Created on 01/24/2024
 */
public interface Searchable extends Document {
    /**
     * Retrieves a list of search results for the specified search text within the PDF document.
     *
     * @param searchText The text to search for within the PDF document.
     * @return A list of SearchResult objects representing search results.
     * @throws IOException If there is an error during the search operation.
     */
    List<SearchResult> getSearchResults(String searchText) throws IOException;
}
