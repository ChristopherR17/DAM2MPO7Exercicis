package com.exercici0601;

import com.utils.*;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

// Fes anar l'exemple amb:
// ./run.sh com.exercici0601.Main
// ./run.ps1 com.exercici0601.Main

public class Main extends Application {

    final int WINDOW_WIDTH = 300;
    final int WINDOW_HEIGHT = 400;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewMain", "/assets/viewMain.fxml");
        UtilsViews.addView(getClass(), "ViewCharacters", "/assets/viewCharacters.fxml");
        UtilsViews.addView(getClass(), "ViewGames", "/assets/viewGames.fxml");
        UtilsViews.addView(getClass(), "ViewConsoles", "/assets/viewConsoles.fxml");

        UtilsViews.addView(getClass(), "ViewCharacter", "/assets/viewCharacter.fxml");
        UtilsViews.addView(getClass(), "ViewGame", "/assets/viewGame.fxml");
        UtilsViews.addView(getClass(), "ViewConsole", "/assets/viewConsole.fxml");

        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Nintendo DB");
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
        stage.show();

        // Afegeix una icona només si no és un Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:icons/icon.png");
            stage.getIcons().add(icon);
        }
    }
}
