package com.sun.internals.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TextField;

/**
 * @author XDSSWAR
 * Created on 09/18/2023
 */
public final class IntegerField extends TextField {
    /**
     * IntegerProperty representing an integer value.
     */
    private IntegerProperty integer;

    /**
     * Default constructor for IntegerField, initializing with a default integer value of 0.
     * Calls the parameterized constructor with the default value.
     */
    public IntegerField() {
        this(0);
    }

    /**
     * Parameterized constructor for IntegerField, allowing the initialization with a specific integer value.
     *
     * @param integer The integer value to initialize the field with.
     */
    public IntegerField(int integer) {
        // Set the provided integer value.
        setInteger(integer);

        // Perform any necessary setup or initialization.
        initialize();
    }


    /**
     * Initializes the IntegerField by adding listeners to synchronize text and integer properties.
     * Text input is filtered to allow only digits, and changes in the integer property update the displayed text.
     */
    private void initialize(){
        textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                setText(newValue.replaceAll("\\D", ""));
            }

            if (!newValue.isEmpty()) {
                setInteger(Integer.parseInt(getText()));
            }

        });

        integerProperty().addListener((observable, oldValue, newValue) -> {
            setText(String.format("%s",newValue));
        });
    }



    /**
     * Public method that provides access to the "integer" property.
     * If the property is not initialized, it creates a new SimpleIntegerProperty.
     * @return The "integer" property.
     */
    public IntegerProperty integerProperty() {
        if (integer == null) {
            integer = new SimpleIntegerProperty(this, "integer");
        }
        return integer;
    }

    /**
     * Getter method for retrieving the current integer value.
     * @return The current integer value.
     */
    public int getInteger() {
        return integerProperty().get();
    }

    /**
     * Setter method for setting a new integer value.
     * @param integer The new integer value to set.
     */
    public void setInteger(int integer) {
        this.integerProperty().set(integer);
    }

}
