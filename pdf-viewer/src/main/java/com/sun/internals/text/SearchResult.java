package com.sun.internals.text;

import javafx.geometry.Rectangle2D;

import java.util.Objects;

/**
 * @author XDSSWAR
 * Created on 01/24/2024
 */
public final class SearchResult implements Comparable<SearchResult>{
    /**
     * The text to search for within the PDF document.
     */
    private final String searchText;

    /**
     * The snippet of text containing the search result found in the PDF document.
     */
    private final String textSnippet;

    /**
     * The page number where the search result was found.
     */
    private final int pageNumber;

    /**
     * The rectangular marker indicating the location of the search result on the page.
     */
    private final Rectangle2D marker;

    /**
     * Constructs a SearchResult object with the provided search-related information.
     *
     * @param searchText   The text that was searched for within the PDF document.
     * @param textSnippet  The snippet of text containing the search result.
     * @param pageNumber   The page number where the search result was found.
     * @param marker       The rectangular marker indicating the search result's location.
     */
    public SearchResult(String searchText, String textSnippet, int pageNumber, Rectangle2D marker) {
        this.searchText = searchText;
        this.textSnippet = textSnippet;
        this.pageNumber = pageNumber;
        this.marker = marker;
    }

    /**
     * Gets the search text that was searched for within the PDF document.
     *
     * @return The search text.
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Gets the snippet of text containing the search result found in the PDF document.
     *
     * @return The text snippet.
     */
    public String getTextSnippet() {
        return textSnippet;
    }

    /**
     * Gets the page number where the search result was found.
     *
     * @return The page number.
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Gets the rectangular marker indicating the location of the search result on the page.
     *
     * @return The marker rectangle.
     */
    public Rectangle2D getMarker() {
        return marker;
    }

    /**
     * Gets a scaled version of the marker rectangle based on the provided scale factor.
     *
     * @param scale The scaling factor to apply to the marker rectangle.
     * @return A scaled Rectangle2D.
     */
    public Rectangle2D getMarker(float scale){
        return new Rectangle2D(marker.getMinX() * scale, marker.getMinY() * scale, marker.getWidth() * scale, marker.getHeight() * scale);
    }

    /**
     * Compares this SearchResult with another SearchResult based on criteria specific to your needs.
     *
     * @param o The SearchResult to compare with.
     * @return A negative integer, zero, or a positive integer as this SearchResult is less than, equal to,
     *         or greater than the specified SearchResult.
     */
    @Override
    public int compareTo(SearchResult o) {
        int result = Integer.compare(pageNumber, o.pageNumber);
        if (result == 0) {
            result = Double.compare(getMarker().getMinY(), o.getMarker().getMinY());
        }
        return result;
    }

    /**
     * Compares this SearchResult with another object to determine if they are equal.
     *
     * @param o The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return pageNumber == that.pageNumber
                && Objects.equals(searchText, that.searchText)
                && Objects.equals(textSnippet, that.textSnippet)
                && Objects.equals(marker, that.marker);
    }

    /**
     * Calculates the hash code for this SearchResult based on a specific hashing strategy.
     *
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(searchText, textSnippet, pageNumber, marker);
    }
}
