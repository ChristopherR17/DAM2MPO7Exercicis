package com.exercici0601;

import com.utils.*;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;


public class ControllerMain {

    @FXML
    private void toViewCharacters(MouseEvent event) {
        System.out.println("To View Characters");
        ControllerCharacters ctrlCharacters = (ControllerCharacters) UtilsViews.getController("ViewCharacters");
        ctrlCharacters.loadList();
        UtilsViews.setViewAnimating("ViewCharacters");
    }

    @FXML
    private void toViewGames(MouseEvent event) {
        System.out.println("To View Games");
        ControllerGames ctrlGames = (ControllerGames) UtilsViews.getController("ViewGames");
        ctrlGames.loadList();
        UtilsViews.setViewAnimating("ViewGames");
    }

    @FXML
    private void toViewConsoles(MouseEvent event) {
        System.out.println("To View Consoles");
        ControllerConsoles ctrlConsoles = (ControllerConsoles) UtilsViews.getController("ViewConsoles");
        ctrlConsoles.loadList();
        UtilsViews.setViewAnimating("ViewConsoles");
    }
}
