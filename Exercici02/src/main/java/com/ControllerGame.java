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
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class ControllerGame implements Initializable{
    @FXML
    private ImageView imgArrowBack;

    @FXML
    private Label name = new Label();
    @FXML
    private Label year = new Label();
    @FXML
    private Label type = new Label();
    @FXML
    private TextArea plot = new TextArea();
    @FXML
    private ImageView img;

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

    public void loadGame(String nameChar) {
        this.nameChar = nameChar;
        try {
            URL jsonFileURL = getClass().getResource("/assets/data/games.json");
            Path path = Paths.get(jsonFileURL.toURI());
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JSONArray jsonInfo = new JSONArray(content);

            for (int i = 0; i < jsonInfo.length(); i++) {
                JSONObject games = jsonInfo.getJSONObject(i);
                
                if (this.nameChar.equalsIgnoreCase(games.getString("name"))) {
                    name.setText(games.getString("name"));
                    year.setText(String.valueOf(games.getInt("year")));
                    type.setText(games.getString("type"));
                    plot.setText(games.getString("plot"));
                    try {
                        String imagePath = games.getString("image");
                        Image image = new Image("/assets/images0601/" + imagePath);
                        img.setImage(image);
                    } catch (Exception e) {
                        System.err.println("Error loading image asset: " + games.getString("image"));
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
        UtilsViews.setViewAnimating("ViewGames");
    }
}
