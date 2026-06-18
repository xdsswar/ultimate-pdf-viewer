package com.sun.internals.controls;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.function.Consumer;

/**
 * The "Password Required" card shown centered inside an {@link OverlayPane} when
 * opening an encrypted document.
 *
 * <p>Collects a password and reports it through the supplied callbacks. The
 * password can be revealed with an inline toggle, and the same card is reused
 * for retries: passing {@code showError} renders an "incorrect password" message.</p>
 *
 * @author XDSSWAR
 */
public final class PasswordView extends VBox {

    /** Pseudo-class used to draw the focus ring on the field container. */
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private final PasswordField hidden = new PasswordField();
    private final TextField shown = new TextField();
    private boolean revealed;

    /**
     * Builds the password prompt.
     *
     * @param fileName  the document file name (shown for context), or {@code null}
     * @param iconSvg   SVG path for the header lock badge
     * @param showError whether to show the "incorrect password" message
     * @param onSubmit  invoked with the entered password when the user confirms
     * @param onCancel  invoked when the user cancels
     */
    public PasswordView(String fileName, String iconSvg, boolean showError,
                        Consumer<String> onSubmit, Runnable onCancel) {
        getStyleClass().addAll("pdf-modal-card", "pdf-password");
        setMaxHeight(USE_PREF_SIZE);

        getChildren().addAll(
                buildHeader(fileName, iconSvg, onCancel),
                buildMessage(),
                buildField(),
                buildError(showError),
                buildFooter(onSubmit, onCancel));
    }

    /* ----------------------------------------------------------------- layout */

    /** Lock-badge header with title, file subtitle and a corner close button. */
    private HBox buildHeader(String fileName, String iconSvg, Runnable onCancel) {
        SVGPath lock = new SVGPath();
        lock.setContent(iconSvg);
        lock.getStyleClass().add("pdf-modal-badge-icon");
        StackPane badge = new StackPane(lock);
        badge.getStyleClass().addAll("pdf-modal-badge", "pdf-modal-badge-accent");

        Label title = new Label("Password Required");
        title.getStyleClass().add("pdf-modal-title");
        Label subtitle = new Label(fileName != null && !fileName.isBlank()
                ? fileName : "Protected document");
        subtitle.getStyleClass().add("pdf-modal-subtitle");
        VBox titles = new VBox(title, subtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeX = new Button("✕");
        closeX.getStyleClass().add("pdf-modal-close");
        closeX.setFocusTraversable(false);
        closeX.setOnAction(event -> onCancel.run());

        HBox header = new HBox(badge, titles, spacer, closeX);
        header.getStyleClass().add("pdf-modal-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Label buildMessage() {
        Label message = new Label("Enter the password to unlock and view this document.");
        message.getStyleClass().add("pdf-password-message");
        message.setWrapText(true);
        return message;
    }

    /** The password input with an inline show/hide toggle, in a focusable box. */
    private HBox buildField() {
        hidden.getStyleClass().add("pdf-password-input");
        hidden.setPromptText("Password");
        shown.getStyleClass().add("pdf-password-input");
        shown.setPromptText("Password");
        shown.setManaged(false);
        shown.setVisible(false);
        shown.textProperty().bindBidirectional(hidden.textProperty());

        StackPane inputStack = new StackPane(hidden, shown);
        inputStack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputStack, Priority.ALWAYS);

        Button reveal = new Button("Show");
        reveal.getStyleClass().add("pdf-password-reveal");
        reveal.setFocusTraversable(false);
        reveal.setOnAction(event -> toggleReveal(reveal));

        HBox fieldBox = new HBox(inputStack, reveal);
        fieldBox.getStyleClass().add("pdf-password-field");
        fieldBox.setAlignment(Pos.CENTER_LEFT);

        // Draw the focus ring on the container while either input has focus.
        Runnable refresh = () ->
                fieldBox.pseudoClassStateChanged(FOCUSED, hidden.isFocused() || shown.isFocused());
        hidden.focusedProperty().addListener((obs, was, is) -> refresh.run());
        shown.focusedProperty().addListener((obs, was, is) -> refresh.run());
        return fieldBox;
    }

    private Label buildError(boolean showError) {
        Label error = new Label("Incorrect password. Please try again.");
        error.getStyleClass().add("pdf-password-error");
        error.setManaged(showError);
        error.setVisible(showError);
        return error;
    }

    private HBox buildFooter(Consumer<String> onSubmit, Runnable onCancel) {
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("pdf-modal-button-secondary");
        cancel.setCancelButton(true);
        cancel.setOnAction(event -> onCancel.run());

        Button unlock = new Button("Unlock");
        unlock.getStyleClass().add("pdf-modal-button");
        unlock.setOnAction(event -> onSubmit.accept(hidden.getText()));

        // Enter in either input confirms.
        hidden.setOnAction(event -> onSubmit.accept(hidden.getText()));
        shown.setOnAction(event -> onSubmit.accept(hidden.getText()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(spacer, cancel, unlock);
        footer.getStyleClass().add("pdf-modal-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        return footer;
    }

    /* --------------------------------------------------------------- behavior */

    /** Swaps between the masked and plain inputs, keeping focus and caret. */
    private void toggleReveal(Button reveal) {
        revealed = !revealed;
        shown.setManaged(revealed);
        shown.setVisible(revealed);
        hidden.setManaged(!revealed);
        hidden.setVisible(!revealed);
        reveal.setText(revealed ? "Hide" : "Show");
        TextField active = revealed ? shown : hidden;
        active.requestFocus();
        active.positionCaret(active.getText().length());
    }

    /** Moves keyboard focus to the password field once attached to a scene. */
    public void requestFocusOnField() {
        Platform.runLater(hidden::requestFocus);
    }
}
