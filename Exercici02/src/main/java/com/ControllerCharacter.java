package com.exercici0601;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.utils.UtilsViews;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;

public class ControllerCharacter implements Initializable{

    @FXML
    private ImageView imgArrowBack;

    @FXML
    private Label name = new Label();
    @FXML
    private Label game = new Label();
    @FXML
    private Label colorL = new Label();
    @FXML
    private ImageView img;
    @FXML
    private Circle circle = new Circle();

    private String nameChar = "";
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Path imagePath = null;
        try {
            URL imageURL = getClass().getResource("/assets/images0601/arrow-back.png");
            Image image = new Image(imageURL.toExternalForm());
            imgArrowBack.setImage(image);
        } catch (Exception e) {
            System.err.println("Error loading image asset: " + imagePath);
            e.printStackTrace();
        }
    }

    public void loadCharacter(String nameChar) {
        this.nameChar = nameChar;
        try {
            URL jsonFileURL = getClass().getResource("/assets/data/characters.json");
            Path path = Paths.get(jsonFileURL.toURI());
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JSONArray jsonInfo = new JSONArray(content);

            for (int i = 0; i < jsonInfo.length(); i++) {
                JSONObject character = jsonInfo.getJSONObject(i);
                
                if (this.nameChar.equalsIgnoreCase(character.getString("name"))) {
                    name.setText(character.getString("name"));
                    game.setText(character.getString("game"));
                    colorL.setText(character.getString("color"));
                    circle.setStyle("-fx-fill: " + character.getString("color"));
                    try {
                        String imagePath = character.getString("image");
                        Image image = new Image("/assets/images0601/" + imagePath);
                        img.setImage(image);
                    } catch (Exception e) {
                        System.err.println("Error loading image asset: " + character.getString("image"));
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack(MouseEvent event) {
        UtilsViews.setViewAnimating("ViewCharacters");
    }
}
