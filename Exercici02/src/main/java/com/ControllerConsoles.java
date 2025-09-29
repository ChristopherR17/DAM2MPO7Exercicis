package com.exercici0601;

import com.utils.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import com.exercici0601.ControllerItem;


public class ControllerConsoles implements Initializable {

    @FXML
    private ImageView imgArrowBack;

    @FXML
    private VBox list = new VBox();

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

    public void loadList() {
        try {
            URL jsonFileURL = getClass().getResource("/assets/data/consoles.json");
            Path path = Paths.get(jsonFileURL.toURI());
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JSONArray jsonInfo = new JSONArray(content);

            list.getChildren().clear();
            for (int i = 0; i < jsonInfo.length(); i++) {
                JSONObject consoles = jsonInfo.getJSONObject(i);
                String name = consoles.getString("name");
                String date = consoles.getString("date");
                String image = consoles.getString("image");

                URL resource = this.getClass().getResource("/assets/subViewConsoles.fxml");
                FXMLLoader loader = new FXMLLoader(resource);
                Parent itemTemplate = loader.load();
                ControllerItem itemController = loader.getController();

                itemController.setName(name);
                itemController.setDate(date);
                itemController.setImatge("/assets/images0601/" + image);

                list.getChildren().add(itemTemplate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toViewMain(MouseEvent event) {
        UtilsViews.setViewAnimating("ViewMain");
    }
}
