package com.sun.internals.enums;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public enum NavButtonState {
    /**
     * Represents the state of navigation buttons in a viewer where the "Previous" button
     * should be disabled.
     */
    DISABLE_PREV,

    /**
     * Represents the state of navigation buttons in a viewer where the "Next" button
     * should be disabled.
     */
    DISABLE_NEXT,

    /**
     * Represents the state of navigation buttons in a viewer where both the "Previous" and
     * "Next" buttons should be disabled.
     */
    DISABLE_BOTH,

    /**
     * Represents the state where both the "Previous" and "Next" buttons are enabled.
     */
    ENABLE_BOTH
}
