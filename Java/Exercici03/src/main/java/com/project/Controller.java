package com.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class Controller implements Initializable {

    private static final String TEXT_MODEL = "gemma3:1b";
    private static final String VISION_MODEL = "llava-phi3";

    @FXML private Button buttonSendRequest;
    @FXML private Button buttonBreak;
    @FXML private Button buttonPicture;
    @FXML private TextField textFieldPrompt;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private HBox imagePreviewBox;
    @FXML private ImageView imagePreview;
    @FXML private Label thinkingLabel;

    private boolean imageLoaded;
    private String base64Image;
    private File selectedImageFile;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private InputStream currentInputStream;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> streamReadingTask;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setButtonsIdle();
        resetImageAttributes();
        setImagePreviewVisible(false);
        setThinking(false);

        textFieldPrompt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !buttonSendRequest.isDisabled()) {
                buttonSendRequest.fire();
            }
        });
    }

    @FXML
    private void sendRequest(ActionEvent event) {
        if (imageLoaded) {
            callPicture(event);
        } else {
            callStream(event);
        }
    }

    @FXML
    private void callStream(ActionEvent event) {
        String prompt = getPrompt().trim();
        if (prompt.isEmpty()) {
            appendSystemMessage("Si us plau, escriu un missatge.");
            return;
        }

        appendUserMessage(prompt);
        textFieldPrompt.clear();
        setButtonsRunning();
        isCancelled.set(false);

        ensureModelLoaded(TEXT_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    appendSystemMessage("Error carregant el model: " + err.getMessage());
                    setButtonsIdle();
                });
                return;
            }
            executeTextRequest(TEXT_MODEL, "Responde en español: " + prompt, true);
        });
    }

    @FXML
    private void callPicture(ActionEvent event) {
        if (!imageLoaded || base64Image == null) {
            appendSystemMessage("Primer has de seleccionar una imatge.");
            return;
        }

        String prompt = getPrompt().trim();
        if (prompt.isEmpty()) prompt = "Describe this image";

        appendUserMessageWithImage(prompt, selectedImageFile);
        textFieldPrompt.clear();
        setButtonsRunning();
        setThinking(true);
        isCancelled.set(false);

        final String finalPrompt = prompt;
        ensureModelLoaded(VISION_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    appendSystemMessage("Error carregant el model: " + err.getMessage());
                    setThinking(false);
                    setButtonsIdle();
                });
                return;
            }

            executeImageRequest(VISION_MODEL, "Responde en español: " + finalPrompt, base64Image);
            Platform.runLater(this::clearImage);
        });
    }

    @FXML
    private void callBreak(ActionEvent event) {
        isCancelled.set(true);
        cancelStreamRequest();
        cancelCompleteRequest();
        Platform.runLater(() -> {
            appendSystemMessage("Petició aturada per l'usuari.");
            setThinking(false);
            setButtonsIdle();
        });
    }

    private void executeTextRequest(String model, String prompt, boolean stream) {
        JSONObject body = new JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", stream)
            .put("keep_alive", "10m");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        Platform.runLater(() -> appendAssistantMessage(""));

        streamRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> {
                currentInputStream = response.body();
                streamReadingTask = executorService.submit(this::handleStreamResponse);
                return response;
            })
            .exceptionally(e -> {
                if (!isCancelled.get()) {
                    Platform.runLater(() -> appendSystemMessage("Error en la connexió: " + e.getMessage()));
                }
                Platform.runLater(this::setButtonsIdle);
                return null;
            });
    }

    private void executeImageRequest(String model, String prompt, String base64Image) {
        JSONObject body = new JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("images", new JSONArray().put(base64Image))
            .put("stream", false)
            .put("keep_alive", "10m")
            .put("options", new JSONObject()
                .put("num_ctx", 2048)
                .put("num_predict", 256));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        completeRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                String msg = tryParseAnyMessage(resp.body());
                if (msg == null || msg.isBlank()) {
                    msg = resp.statusCode() >= 200 && resp.statusCode() < 300
                        ? "(resposta buida)"
                        : "HTTP " + resp.statusCode() + ": " + resp.body();
                }

                final String toShow = msg;
                Platform.runLater(() -> {
                    appendAssistantMessage(toShow);
                    setThinking(false);
                    setButtonsIdle();
                });
                return resp;
            })
            .exceptionally(e -> {
                if (!isCancelled.get()) {
                    Platform.runLater(() -> appendSystemMessage("Error en la petició: " + e.getMessage()));
                }
                Platform.runLater(() -> {
                    setThinking(false);
                    setButtonsIdle();
                });
                return null;
            });
    }

    private void handleStreamResponse() {
        StringBuilder fullResponse = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentInputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCancelled.get()) break;
                if (line.isBlank()) continue;

                JSONObject jsonResponse = new JSONObject(line);
                String chunk = jsonResponse.optString("response", "");
                if (chunk.isEmpty()) continue;

                fullResponse.append(chunk);
                String currentResponse = fullResponse.toString();
                Platform.runLater(() -> updateLastAssistantMessage(currentResponse));
            }
        } catch (Exception e) {
            if (!isCancelled.get()) {
                Platform.runLater(() -> appendSystemMessage("Error durant el streaming: " + e.getMessage()));
            }
        } finally {
            try {
                if (currentInputStream != null) currentInputStream.close();
            } catch (Exception ignored) {}
            Platform.runLater(this::setButtonsIdle);
        }
    }

    private Label createBubble(String author, String message, boolean user) {
        Label label = new Label(author + "\n" + message);
        label.setWrapText(true);
        label.setMaxWidth(520);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setStyle(user
            ? "-fx-background-color: #e0f2fe; -fx-padding: 12; -fx-background-radius: 16; -fx-border-radius: 16; -fx-text-fill: #0f172a; -fx-font-size: 13px;"
            : "-fx-background-color: #ffffff; -fx-padding: 12; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-size: 13px;"
        );
        return label;
    }

    private void appendUserMessage(String message) {
        addMessage(createBubble("👤 Usuari", message, true));
    }

    private void appendUserMessageWithImage(String message, File imageFile) {
        VBox userBox = new VBox(8);
        userBox.setMaxWidth(520);
        userBox.setStyle("-fx-background-color: #e0f2fe; -fx-padding: 12; -fx-background-radius: 16; -fx-border-radius: 16;");

        Label userLabel = new Label("👤 Usuari\n" + message);
        userLabel.setWrapText(true);
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #0f172a;");
        userBox.getChildren().add(userLabel);

        if (imageFile != null && imageFile.exists()) {
            ImageView imgView = new ImageView(new Image(imageFile.toURI().toString()));
            imgView.setFitHeight(150);
            imgView.setFitWidth(150);
            imgView.setPreserveRatio(true);
            userBox.getChildren().add(imgView);
        }

        addMessage(userBox);
    }

    private void appendAssistantMessage(String message) {
        addMessage(createBubble("🤖 Assistent", message, false));
    }

    private void updateLastAssistantMessage(String message) {
        if (!messagesBox.getChildren().isEmpty()) {
            Node lastNode = messagesBox.getChildren().get(messagesBox.getChildren().size() - 1);
            if (lastNode instanceof Label lastLabel && lastLabel.getText().startsWith("🤖 Assistent")) {
                lastLabel.setText("🤖 Assistent\n" + message);
                scrollToBottom();
                return;
            }
        }
        appendAssistantMessage(message);
    }

    private void appendSystemMessage(String message) {
        Label systemLabel = new Label("ℹ️ " + message);
        systemLabel.setWrapText(true);
        systemLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic; -fx-padding: 4 2 4 2;");
        addMessage(systemLabel);
    }

    private void addMessage(Node node) {
        Platform.runLater(() -> {
            messagesBox.getChildren().add(node);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        if (scrollPane != null) {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }

    private String tryParseAnyMessage(String bodyStr) {
        try {
            JSONObject o = new JSONObject(bodyStr);
            if (o.has("response")) return o.optString("response", "");
            if (o.has("message")) return o.optString("message", "");
            if (o.has("error")) return "Error: " + o.optString("error", "");
        } catch (Exception ignored) {}
        return null;
    }

    private void cancelStreamRequest() {
        try {
            if (currentInputStream != null) currentInputStream.close();
        } catch (Exception ignored) {}

        if (streamReadingTask != null) streamReadingTask.cancel(true);
        if (streamRequest != null && !streamRequest.isDone()) streamRequest.cancel(true);
    }

    private void cancelCompleteRequest() {
        if (completeRequest != null && !completeRequest.isDone()) completeRequest.cancel(true);
    }

    private void setButtonsRunning() {
        buttonSendRequest.setDisable(true);
        buttonPicture.setDisable(true);
        buttonBreak.setDisable(false);
        textFieldPrompt.setDisable(true);
    }

    private void setButtonsIdle() {
        buttonSendRequest.setDisable(false);
        buttonPicture.setDisable(false);
        buttonBreak.setDisable(true);
        textFieldPrompt.setDisable(false);
        streamRequest = null;
        completeRequest = null;
        setThinking(false);
    }

    private void setThinking(boolean visible) {
        if (thinkingLabel != null) {
            thinkingLabel.setVisible(visible);
            thinkingLabel.setManaged(visible);
        }
    }

    private CompletableFuture<Void> ensureModelLoaded(String modelName) {
        return httpClient.sendAsync(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/ps"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            .thenCompose(resp -> {
                boolean loaded = false;
                try {
                    JSONObject o = new JSONObject(resp.body());
                    JSONArray models = o.optJSONArray("models");
                    if (models != null) {
                        for (int i = 0; i < models.length(); i++) {
                            String name = models.getJSONObject(i).optString("name", "");
                            String model = models.getJSONObject(i).optString("model", "");
                            if (name.startsWith(modelName) || model.startsWith(modelName)) {
                                loaded = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (loaded) return CompletableFuture.completedFuture(null);

                Platform.runLater(() -> appendSystemMessage("Carregant model " + modelName + "..."));

                String preloadJson = new JSONObject()
                    .put("model", modelName)
                    .put("stream", false)
                    .put("keep_alive", "10m")
                    .toString();

                HttpRequest preloadReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(preloadJson))
                    .build();

                return httpClient.sendAsync(preloadReq, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> Platform.runLater(() -> appendSystemMessage("Model " + modelName + " carregat correctament.")));
            });
    }

    @FXML
    private String getPrompt() {
        return textFieldPrompt.getText();
    }

    @FXML
    private void loadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona una imatge");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imatges", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp", "*.gif")
        );

        File initialDir = new File(System.getProperty("user.dir"));
        if (initialDir.exists() && initialDir.isDirectory()) {
            fc.setInitialDirectory(initialDir);
        }

        File file = fc.showOpenDialog(buttonPicture.getScene().getWindow());
        if (file == null) {
            appendSystemMessage("No s'ha seleccionat cap fitxer.");
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            base64Image = Base64.getEncoder().encodeToString(bytes);
            imageLoaded = true;
            selectedImageFile = file;

            imagePreview.setImage(new Image(file.toURI().toString()));
            setImagePreviewVisible(true);
            appendSystemMessage("Imatge carregada: " + file.getName());
            textFieldPrompt.setPromptText("Escriu una pregunta sobre la imatge...");
        } catch (Exception e) {
            appendSystemMessage("Error en carregar la imatge: " + e.getMessage());
            clearImage();
        }
    }

    @FXML
    private void clearImage() {
        resetImageAttributes();
        if (imagePreview != null) imagePreview.setImage(null);
        setImagePreviewVisible(false);
        textFieldPrompt.setPromptText("Escriu el teu missatge...");
    }

    private void setImagePreviewVisible(boolean visible) {
        if (imagePreviewBox != null) {
            imagePreviewBox.setVisible(visible);
            imagePreviewBox.setManaged(visible);
        }
    }

    @FXML
    private void resetImageAttributes() {
        imageLoaded = false;
        base64Image = null;
        selectedImageFile = null;
    }
}
