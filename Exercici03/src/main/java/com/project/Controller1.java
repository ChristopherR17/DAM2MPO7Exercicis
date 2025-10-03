package com.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Controller1 {

    @FXML
    private Label msgText;

    public void setMessage() {
        // Default message, can be updated by other controllers
        msgText.setText("Resposta: gràcies per el missatge!");
    }

    @FXML
    private void animateToView0(ActionEvent event) {
        UtilsViews.setViewAnimating("View0");
    }
}
