package com.project;

import org.w3c.dom.Text;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.TextField;

public class Controller0 {
    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Datos requeridos");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private Button sendButton;
    @FXML
    private AnchorPane container;
    @FXML
    private TextField nameText, ageText;

    public void setNameAge(){
        Main.name = nameText.getText();
        try {
            Main.age = Integer.parseInt(ageText.getText());
        } catch (NumberFormatException e) {
            Main.age = -1;
        }
    }

    @FXML
    private void animateToView1(ActionEvent event) {
        String name = nameText.getText();
        String ageStr = ageText.getText();
        if (name == null || name.trim().isEmpty()) {
            showAlert("Por favor, introduce tu nombre.");
            return;
        }
        if (ageStr == null || ageStr.trim().isEmpty()) {
            showAlert("Por favor, introduce tu edad.");
            return;
        }
        try {
            Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            showAlert("La edad debe ser un número válido.");
            return;
        }
        setNameAge();
        Controller1 ctrl = (Controller1) UtilsViews.getController("View1");
        ctrl.setMessage();
        UtilsViews.setViewAnimating("View1");
    }

}