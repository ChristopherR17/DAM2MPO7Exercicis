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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class ControllerConsole implements Initializable {

    @FXML
    private ImageView imgArrowBack;

    @FXML
    private Circle circle = new Circle();
    @FXML
    private Label color = new Label();
    @FXML
    private Label name = new Label();
    @FXML
    private Label date = new Label();
    @FXML
    private Label procesador = new Label();
    @FXML
    private Label unitsSold = new Label();

    @FXML
    private ImageView img;

    private String nameConsole = "";

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

    public void loadConsole(String nameConsole) {
        this.nameConsole = nameConsole;
        try {
            URL jsonFileURL = getClass().getResource("/assets/data/consoles.json");
            Path path = Paths.get(jsonFileURL.toURI());
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JSONArray jsonInfo = new JSONArray(content);

            for (int i = 0; i < jsonInfo.length(); i++) {
                JSONObject consoles = jsonInfo.getJSONObject(i);

                if (this.nameConsole.equalsIgnoreCase(consoles.getString("name"))) {
                    name.setText(consoles.getString("name"));
                    date.setText(consoles.getString("date"));
                    procesador.setText(consoles.getString("procesador"));
                    unitsSold.setText(String.valueOf(consoles.getInt("units_sold")));
                    color.setText(consoles.getString("color"));
                    circle.setStyle("-fx-fill: " + consoles.getString("color"));
                    try {
                        String imagePath = consoles.getString("image");
                        Image image = new Image("/assets/images0601/" + imagePath);
                        img.setImage(image);
                    } catch (Exception e) {
                        System.err.println("Error loading image asset: " + consoles.getString("image"));
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
        UtilsViews.setViewAnimating("ViewConsoles");
    }
}
