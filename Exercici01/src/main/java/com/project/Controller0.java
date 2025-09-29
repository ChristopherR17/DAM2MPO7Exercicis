package com.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.TextField;

public class Controller0 {
    @FXML
    private Button sendButton;
    @FXML
    private AnchorPane container;
    @FXML
    private TextField nameText, ageText;

    private boolean validar() {
        String name = nameText.getText();
        String ageStr = ageText.getText();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Por favor, introduce tu nombre.");
            return false;
        }
        if (ageStr == null || ageStr.trim().isEmpty()) {
            showAlert("Por favor, introduce tu edad.");
            return false;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            showAlert("La edad debe ser un número válido.");
            return false;
        }
        if (age < 0) {
            showAlert("La edad no puede ser negativa.");
            return false;
        }

        Main.name = name;
        Main.age = age;
        return true;
    }

    @FXML
    private void animateToView1(ActionEvent event) {
        if (validar()){
            Controller1 ctrl = (Controller1) UtilsViews.getController("View1");
            ctrl.setMessage();
            UtilsViews.setViewAnimating("View1");
        }
        
    }

    @FXML
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Datos requeridos");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}