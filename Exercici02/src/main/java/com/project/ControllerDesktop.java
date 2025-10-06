package com.project;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;

public class ControllerDesktop {

    @FXML
    private AnchorPane rootPaneDesktop;
    @FXML
    private ComboBox<String> comboBox;
    @FXML
    private VBox yPane;
    @FXML
    private Circle circle;
    @FXML
    private VBox mainScreen;
    private JSONArray jsonInfoCharacters, jsonInfoConsoles, jsonInfoGames;
    private URL resource = this.getClass().getResource("/assets/listItem.fxml");

    public void initialize() {
        try {
            // Añade los valores del ComboBox
            comboBox.getItems().addAll("Characters", "Consoles", "Games");
            comboBox.getSelectionModel().selectFirst();

            // Listener del ComboBox
            comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                evaluateComboBoxOption(newValue);
            });

            // Obtener lista Characters
            URL jsonCharactersFileURL = getClass().getResource("/assets/characters.json");
            Path path = Paths.get(jsonCharactersFileURL.toURI());
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            jsonInfoCharacters = new JSONArray(content);

            // Obtener lista Consoles
            URL jsonConsolesFileURL = getClass().getResource("/assets/consoles.json");
            path = Paths.get(jsonConsolesFileURL.toURI());
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            jsonInfoConsoles = new JSONArray(content);

            // Obtener lista Games
            URL jsonGamesFileURL = getClass().getResource("/assets/games.json");
            path = Paths.get(jsonGamesFileURL.toURI());
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            jsonInfoGames = new JSONArray(content);

            // Actualiza la UI con los valores iniciales de los personajes
            setCharacters();

            // Listener para cambiar de vista según tamaño ventana
            rootPaneDesktop.widthProperty().addListener((obs, oldVal, newVal) -> {
                //System.out.println("rootPaneDesktop.widthProperty=" + newVal);
                if ((double) newVal < 610) {
                    //System.out.println("SE ACTIVA LISTENER EN VISTA viewDesktop");
                    UtilsViews.setView("viewMobile0");   
                }
            });

        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    @FXML
    private void setCharacters() throws Exception {
        // Borrar contenido listView
        yPane.getChildren().clear();

        // Generar la nueva lista a partir de 'jsonInfoCharacters'
        for (int i = 0; i < jsonInfoCharacters.length(); i++) {
            // Obtenir el objeto JSON individual (character)
            JSONObject character = jsonInfoCharacters.getJSONObject(i);

            // Extraer información necesaria del JSON
            String name = character.getString("name");

            // Carregar el template de 'listItem.fxml'
            FXMLLoader loader = new FXMLLoader(resource);
            Parent itemTemplate = loader.load();
            ControllerListItem itemController = loader.getController();

            // Asignar los valores a los controles del template
            itemController.setLableName(name);
            if (name.equals("Samus Aran")) {
                name = "samus";
            } else if (name.equals("Donkey Kong")) {
                name = "dk";
            }

            String urlImage = "/assets/images/character_" + name.toLowerCase() + ".png";
            itemController.setImatge("/assets/images/character_" + name.toLowerCase() + ".png");

            // Añadir el nuevo elemento a 'yPane'
            yPane.getChildren().add(itemTemplate);

            // Añadir listener para que al hacer click se actualice la pantalla central
            itemTemplate.setOnMouseClicked(e -> {
                updateMainScreen(character, urlImage);
            });
        }
    }

    @FXML
    private void setConsoles() throws Exception {
        // Borrar contenido listView
        yPane.getChildren().clear();

        // Generar la nueva lista a partir de 'jsonInfoCharacters'
        for (int i = 0; i < jsonInfoConsoles.length(); i++) {
            // Obtenir el objeto JSON individual (character)
            JSONObject console = jsonInfoConsoles.getJSONObject(i);

            // Extraer información necesaria del JSON
            String name = console.getString("name");

            // Carregar el template de 'listItem.fxml'
            FXMLLoader loader = new FXMLLoader(resource);
            Parent itemTemplate = loader.load();
            ControllerListItem itemController = loader.getController();

            // Asignar los valores a los controles del template
            itemController.setLableName(name);
            if (name.equals("Nintendo Switch")) {
                name = "switch";
            } else if (name.equals("Wii U")) {
                name = "wiiu";
            } else if (name.equals("Nintendo 64")) {
                name = "64";
            } else if (name.equals("Super Nintendo")) {
                name = "sn";
            }

            String urlImage = "/assets/images/nintendo_" + name.toLowerCase() + ".png";
            itemController.setImatge("/assets/images/nintendo_" + name.toLowerCase() + ".png");

            // Añadir el nuevo elemento a 'yPane'
            yPane.getChildren().add(itemTemplate);

            // Añadir listener para que al hacer click se actualice la pantalla central
            itemTemplate.setOnMouseClicked(e -> {
                updateMainScreen(console, urlImage);
            });
        }
    }

    @FXML
    private void setGames() throws Exception {
        // Borrar contenido listView
        yPane.getChildren().clear();

        // Generar la nueva lista a partir de 'jsonInfoCharacters'
        for (int i = 0; i < jsonInfoGames.length(); i++) {
            // Obtenir el objeto JSON individual (character)
            JSONObject game = jsonInfoGames.getJSONObject(i);

            // Extraer información necesaria del JSON
            String name = game.getString("name");

            // Carregar el template de 'listItem.fxml'
            FXMLLoader loader = new FXMLLoader(resource);
            Parent itemTemplate = loader.load();
            ControllerListItem itemController = loader.getController();

            // Asignar los valores a los controles del template
            itemController.setLableName(name);
            if (name.equals("The Legend of Zelda")) {
                name = "zelda";
            } else if (name.equals("Pokémon Red i Blue")) {
                name = "pred";
            } else if (name.equals("Mario Kart 64")) {
                name = "smk";
            } else if (name.equals("Donkey Kong")) {
                name = "dk";
            } else if (name.equals("Super Mario Bros")) {
                name = "smb";
            }

            String extension = ".png";
            if (name.equals("Metroid") || name.equals("pred") || name.equals("smk")) {
                extension = ".jpeg";
            }

            String urlImage = "/assets/images/game_" + name.toLowerCase() + extension;
            itemController.setImatge("/assets/images/game_" + name.toLowerCase() + extension);

            // Añadir el nuevo elemento a 'yPane'
            yPane.getChildren().add(itemTemplate);

            // Añadir listener para que al hacer click se actualice la pantalla central
            itemTemplate.setOnMouseClicked(e -> {
                updateMainScreen(game, urlImage);
            });
        }
    }

    @FXML
    private void evaluateComboBoxOption(String option) {
        try {
            switch (option) {
                case "Characters":
                    setCharacters();
                    break;
                case "Consoles":
                    setConsoles();
                    break;
                case "Games":
                    setGames();
                    break;
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void updateMainScreen(JSONObject jsonObject, String urlImage) {
        // Limpiar elementos de mainScreen
        mainScreen.getChildren().clear();

        // Preparar imagen
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(urlImage)));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(200);
            imageView.setPreserveRatio(true);

            mainScreen.getChildren().add(imageView);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        boolean isCharacter = jsonObject.has("game");
        boolean isConsole = jsonObject.has("procesador");
        boolean isGame = jsonObject.has("plot");

        // Nombre
        Label name = new Label(jsonObject.getString("name"));
        name.setStyle("-fx-font-size: 30px; -fx-fill: black; -fx-font-weight: bold;");
        mainScreen.getChildren().add(name);

        // Game
        if (isCharacter) {
            Label game = new Label(jsonObject.getString("game"));
            game.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(game);
        }

        // Date
        if (isConsole) {
            Label date = new Label(jsonObject.getString("date"));
            date.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(date);
        }

        // Procesador
        if (isConsole) {
            Label procesador = new Label(jsonObject.getString("procesador"));
            procesador.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(procesador);
        }

        // Units sold
        if (isConsole) {
            Label unitsSold = new Label(String.valueOf(jsonObject.getInt("units_sold")));
            unitsSold.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(unitsSold);
        }

        // Year
        if (isGame) {
            Label year = new Label(String.valueOf(jsonObject.getInt("year")));
            year.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(year);
        }

        // Type
        if (isGame) {
            Label type = new Label(jsonObject.getString("type"));
            type.setStyle("-fx-font-size: 20px;");
            mainScreen.getChildren().add(type);
        }

        // Plot
        if (isGame) {
            Label plot = new Label(jsonObject.getString("plot"));
            plot.setStyle("-fx-font-size: 15px;");
            plot.setWrapText(true);
            plot.setAlignment(Pos.CENTER);
            plot.setTextAlignment(TextAlignment.CENTER);
            mainScreen.getChildren().add(plot);
        }

        // Color
        if (isCharacter || isConsole) {
            circle = new Circle(20);
            circle.setStyle("-fx-fill: " + jsonObject.getString("color"));
            mainScreen.getChildren().add(circle);
        }
    }
}