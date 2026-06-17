package com.xss.it.nfx.pdfium.ffm;

/**
 * A page's intrinsic size in PDF points (1 point = 1/72 inch). Internal API.
 *
 * @param width  page width in points
 * @param height page height in points
 * @author XDSSWAR
 */
public record PageSize(double width, double height) {
}
