package com.exercici0601;

import java.util.Objects;

import com.utils.UtilsViews;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXML;

public class ControllerItem {

    private String nameChar = "";
    
    @FXML
    private Label name;
    @FXML
    private ImageView img;

    @FXML
    private Label year;

    @FXML
    private Label date;

    /*Setters generales */
    public void setName(String name) {
        this.nameChar = name;
        this.name.setText(name);
    }
    public void setImatge(String imagePath) {
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            this.img.setImage(image);
        } catch (NullPointerException e) {
            System.err.println("Error loading image asset: " + imagePath);
            e.printStackTrace();
        }
    }

    /*Setters de Games */
    public void setYear(String year) {
        this.year.setText(year);
    }

    /*Setters de Consoles */
    public void setDate(String date){
        this.date.setText(date);
    }

    @FXML
    private void toViewCharacter(MouseEvent event) {
        System.out.println("To View Character");
        ControllerCharacter ctrl = (ControllerCharacter) UtilsViews.getController("ViewCharacter");
        ctrl.loadCharacter(nameChar);
        UtilsViews.setViewAnimating("ViewCharacter");
    }

    @FXML
    private void toViewGame(MouseEvent event) {
        System.out.println("To View Game");
        ControllerGame ctrl = (ControllerGame) UtilsViews.getController("ViewGame");
        ctrl.loadGame(nameChar);
        UtilsViews.setViewAnimating("ViewGame");
    }

    @FXML
    private void toViewConsole(MouseEvent event) {
        System.out.println("To View Console");
        ControllerConsole ctrl = (ControllerConsole) UtilsViews.getController("ViewConsole");
        ctrl.loadConsole(nameChar);
        UtilsViews.setViewAnimating("ViewConsole");
    }
}
