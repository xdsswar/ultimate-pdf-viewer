package com.sun.internals.text;

import java.text.DecimalFormat;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public final class Format {
    /**
     * Formats a double-precision number with two decimal places using a DecimalFormat.
     *
     * @param number The double-precision number to be formatted.
     * @return A string representation of the formatted number with exactly two decimal places.
     */
    public static String formatDouble(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        return decimalFormat.format(number);
    }
}
