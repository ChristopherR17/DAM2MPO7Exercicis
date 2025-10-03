package com.project;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Controller0 {
    @FXML
    private Button sendButton;
    @FXML
    private AnchorPane container;
    @FXML
    private javafx.scene.control.ListView<Message> messagesList;
    @FXML
    private TextField messageField;

    @FXML
    private void onSendMessage(ActionEvent event) {
        String text = messageField.getText();
        if (text == null || text.trim().isEmpty()) return;

        addUserMessage(text);
        messageField.clear();

        // Run Ollama request in background
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Try HTTP API first
                String reply = tryOllamaHttp(text);
                if (reply != null) return reply;
                // Fallback to CLI
                reply = tryOllamaCli(text);
                if (reply != null) return reply;
                // Final fallback
                return "(No response) I couldn't reach Ollama — reply placeholder.";
            }
        };

        task.setOnSucceeded(t -> addBotMessage(task.getValue()));
        task.setOnFailed(t -> addBotMessage("(Error) Could not get reply."));

        Thread th = new Thread(task, "ollama-request");
        th.setDaemon(true);
        th.start();
    }

    private javafx.scene.image.Image loadImage(String resourcePath) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is != null) return new javafx.scene.image.Image(is);
        } catch (Exception e) {
            // ignore
        }
        try {
            java.io.File f = new java.io.File("Exercici03" + resourcePath);
            if (f.exists()) return new javafx.scene.image.Image(f.toURI().toString());
        } catch (Exception e) {
            // ignore
        }
        // final fallback: empty 1x1 image
        return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(new byte[0]));
    }

    @FXML
    private void initialize() {
        messagesList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(8);
                    if (item.getSender() == Message.Sender.BOT) {
                        // avatar left
                        javafx.scene.image.Image img = loadImage(item.getAvatarResource());
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(32);
                        iv.setFitHeight(32);
                        Label txt = new Label(item.getText());
                        txt.setWrapText(true);
                        txt.getStyleClass().add("bubble-bot");
                        row.getChildren().addAll(iv, txt);
                    } else {
                        Label txt = new Label(item.getText());
                        txt.setWrapText(true);
                        txt.getStyleClass().add("bubble-user");
                        javafx.scene.image.Image img = loadImage(item.getAvatarResource());
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(32);
                        iv.setFitHeight(32);
                        row.getChildren().addAll(txt, iv);
                    }
                    setGraphic(row);
                }
            }
        });
    }

    private void addUserMessage(String text) {
        Message m = new Message(Message.Sender.USER, text, "/assets/ieti.png");
        Platform.runLater(() -> {
            messagesList.getItems().add(m);
            messagesList.scrollTo(messagesList.getItems().size()-1);
        });
    }

    private void addBotMessage(String text) {
        Message m = new Message(Message.Sender.BOT, text, "/assets/xatIeti.png");
        Platform.runLater(() -> {
            messagesList.getItems().add(m);
            messagesList.scrollTo(messagesList.getItems().size()-1);
        });
    }

    // Try HTTP request to local Ollama server. This is a best-effort attempt and may need
    // adjustment depending on the Ollama API you have (endpoint/model names).
    private String tryOllamaHttp(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            // Common Ollama HTTP endpoints vary; try /api/completions
            String json = "{\"model\":\"gpt-4o-mini\",\"prompt\":\"" + escapeJson(prompt) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:11434/api/completions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // crude: return full body — you can parse JSON for 'text' field if available
                return response.body();
            }
        } catch (Exception e) {
            // ignore and fallback
        }
        return null;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Fallback: run local 'ollama' CLI if installed and in PATH. This also depends on your installed
    // ollama commands and model names. Adjust accordingly.
    private String tryOllamaCli(String prompt) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "query", "gpt-4o-mini", prompt);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            p.waitFor();
            String result = out.toString().trim();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }
}