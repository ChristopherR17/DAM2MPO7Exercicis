package com.project;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class ControllerMobile1 {

    @FXML
    private AnchorPane rootPaneMobile1;
    @FXML
    private VBox yPane;
    private JSONArray jsonInfoCharacters, jsonInfoConsoles, jsonInfoGames;
    private URL resource = this.getClass().getResource("/assets/listItem.fxml");

    public void initialize() {
        try {
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

            // Listener tamaño ventana
            rootPaneMobile1.widthProperty().addListener((obs, oldVal, newVal) -> {
                //System.out.println("rootPaneMobile1.widthProperty=" + newVal);
                if ((double) newVal > 800) {
                    //System.out.println("SE ACTIVA LISTENER EN VISTA viewMobile1");
                    UtilsViews.setView("viewDesktop");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void setCharacters() throws Exception {
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
            itemController.setImatge(urlImage);

            // Añadir el nuevo elemento a 'yPane'
            yPane.getChildren().add(itemTemplate);

            // Añadir listener para que al hacer click se actualice la pantalla central
            itemTemplate.setOnMouseClicked(e -> {
                // Preparar siguiente vista
                ControllerMobile2 controllerMobile2 = (ControllerMobile2) UtilsViews.getController("viewMobile2");
                controllerMobile2.updateMainScreen(character, urlImage);

                // Cambiar a la siguiente vista
                UtilsViews.setView("viewMobile2");
            });
        }
    }

    @FXML
    public void setConsoles() throws Exception {
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
                // Preparar siguiente vista
                ControllerMobile2 controllerMobile2 = (ControllerMobile2) UtilsViews.getController("viewMobile2");
                controllerMobile2.updateMainScreen(console, urlImage);

                // Cambiar a la siguiente vista
                UtilsViews.setView("viewMobile2");
            });
        }
    }

    @FXML
    public void setGames() throws Exception {
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
                System.out.println("holi");
                // Preparar siguiente vista
                ControllerMobile2 controllerMobile2 = (ControllerMobile2) UtilsViews.getController("viewMobile2");
                controllerMobile2.updateMainScreen(game, urlImage);

                // Cambiar a la siguiente vista
                UtilsViews.setView("viewMobile2");
            });
        }
    }

    @FXML
    private void toViewMobile0() {
        UtilsViews.setView("viewMobile0");
    }
}
