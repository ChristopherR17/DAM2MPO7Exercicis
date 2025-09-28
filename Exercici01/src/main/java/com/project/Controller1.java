package com.project;

import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

public class Controller1 {

    @FXML
    private Button backButton;
    @FXML
    private AnchorPane container;
    @FXML
    private TextField msgText;

    public void setMessage() {
        msgText.setText("Hola "+ Main.name + ", tens " + Main.age + " anys!");
    }

    @FXML
    private void animateToView0(ActionEvent event) {
        UtilsViews.setViewAnimating("View0");
    }
}