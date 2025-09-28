package com.project;

import org.w3c.dom.Text;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    public void setNameAge(){
        Main.name = nameText.getText();
        Main.age = Integer.parseInt(ageText.getText());
    }

    @FXML
    private void animateToView1(ActionEvent event) {
        setNameAge();

        Controller1 ctrl = (Controller1) UtilsViews.getController("View1");
        ctrl.setMessage();

        UtilsViews.setViewAnimating("View1");
    }

}