/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */
 
package com.sun.internals;

import java.util.Objects;

/**
 * @author XDSSWAR
 * Created on 06/20/2025
 * <p>
 * Represents thumbnail metadata for a page in a document viewer.
 *
 * <p>This class stores the index of the thumbnail in a list (zero-based)
 * and the actual page number it represents (one-based).</p>
 *
 * <p>Equality and hashing are based on both index and pageIndex.</p>
 */
public class ThumbData {

    /** The zero-based index of the thumbnail in the list. */
    private final int index;

    /** The one-based page number this thumbnail represents. */
    private final int pageIndex;

    /**
     * Constructs a new ThumbData object with the given zero-based index.
     * Automatically calculates the page index as index + 1.
     *
     * @param index the zero-based index of the thumbnail
     */
    public ThumbData(int index) {
        this.index = index;
        this.pageIndex = index + 1;
    }

    /**
     * Returns the zero-based index of the thumbnail.
     *
     * @return the thumbnail index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the one-based page number this thumbnail represents.
     *
     * @return the page index
     */
    public int pageIndex() {
        return pageIndex;
    }

    /**
     * Checks equality between this object and another.
     * Returns true if both index and pageIndex match.
     *
     * @param o the object to compare to
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ThumbData thumbData)) return false;
        return index == thumbData.index && pageIndex == thumbData.pageIndex;
    }

    /**
     * Returns a hash code based on index and pageIndex.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(index, pageIndex);
    }

    /**
     * Returns a string representation of this ThumbData.
     *
     * @return a string showing index and pageIndex
     */
    @Override
    public String toString() {
        return "ThumbData{" +
                "index=" + index +
                ", pageIndex=" + pageIndex +
                '}';
    }
}
