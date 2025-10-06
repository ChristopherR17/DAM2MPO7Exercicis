package com.project;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

public class ControllerMobile0 {

    @FXML
    private AnchorPane rootPaneMobile0;

    public void initialize() {
            try {
            // Listener tamaño ventana
            rootPaneMobile0.widthProperty().addListener((obs, oldVal, newVal) -> {
                //System.out.println("rootPaneMobile0.widthProperty=" + newVal);
                if ((double) newVal > 800) {
                    //System.out.println("SE ACTIVA LISTENER EN VISTA viewMobile0");
                    UtilsViews.setView("viewDesktop");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void setCharacters() throws Exception {
        // Preparar siguiente vista
        ControllerMobile1 controllerMobile1 = (ControllerMobile1) UtilsViews.getController("viewMobile1");
        controllerMobile1.setCharacters();

        // Cambiar a la siguiente vista
        UtilsViews.setView("viewMobile1");
    }

    @FXML
    private void setConsoles() throws Exception {
        // Preparar siguiente vista
        ControllerMobile1 controllerMobile1 = (ControllerMobile1) UtilsViews.getController("viewMobile1");
        controllerMobile1.setConsoles();

        // Cambiar a la siguiente vista
        UtilsViews.setView("viewMobile1");
    }

    @FXML
    private void setGames() throws Exception {
        // Preparar siguiente vista
        ControllerMobile1 controllerMobile1 = (ControllerMobile1) UtilsViews.getController("viewMobile1");
        controllerMobile1.setGames();

        // Cambiar a la siguiente vista
        UtilsViews.setView("viewMobile1");
    }
}
