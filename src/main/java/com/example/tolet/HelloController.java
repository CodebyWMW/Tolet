package com.example.tolet;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    private Label messageText;

    @FXML
    private TextField nameInput;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Hello!");
    }

    @FXML
    protected void onShowInputClick() {
        String name = nameInput.getText();
        if (name == null || name.isBlank()) {
            messageText.setText("Please enter some text.");
            return;
        }

        messageText.setText("You typed: " + name.trim());
    }

    @FXML
    protected void onShowMessageClick() {
        messageText.setText("Message shown.");
    }

    @FXML
    protected void onClearClick() {
        nameInput.clear();
        messageText.setText("");
    }
}
