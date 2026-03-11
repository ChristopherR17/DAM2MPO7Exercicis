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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class Controller implements Initializable {

    // Models
    private static final String TEXT_MODEL   = "gemma3:1b";
    private static final String VISION_MODEL = "llava-phi3";

    @FXML private Button buttonSendRequest;
    @FXML private Button buttonBreak;
    @FXML private Button buttonPicture;
    @FXML private TextField textFieldPrompt;
    @FXML private VBox messagesBox;
    @FXML private HBox imagePreviewBox;
    @FXML private ImageView imagePreview;
    @FXML private Label thinkingLabel;
    
    // No cal textInfo perquè utilitzem messagesBox

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
    private volatile boolean isFirst = false;
    
    private ScrollPane scrollPane; // Necessitarem això per fer scroll automàtic

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setButtonsIdle();
        resetImageAttributes();
        
        // Obtenir referència al ScrollPane (és el pare del messagesBox)
        if (messagesBox != null && messagesBox.getParent() != null) {
            scrollPane = (ScrollPane) messagesBox.getParent().getParent();
        }
        
        // Amagar el preview de la imatge inicialment
        if (imagePreviewBox != null) {
            imagePreviewBox.setVisible(false);
            imagePreviewBox.setManaged(false);
        }
        
        // Amagar el thinking label inicialment
        if (thinkingLabel != null) {
            thinkingLabel.setVisible(false);
        }

        textFieldPrompt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                buttonSendRequest.fire();
            }
        });
    }

    // --- UI actions ---

    @FXML
    private void callStream(ActionEvent event) {
        String prompt = getPrompt();
        if (prompt.isEmpty()) {
            appendSystemMessage("Si us plau, escriu un missatge.");
            return;
        }
        
        // Mostrar missatge de l'usuari
        appendUserMessage(prompt);
        
        // BUIDAR EL TEXTFIELD - Això és important!
        Platform.runLater(() -> {
            textFieldPrompt.clear();
        });
        
        setButtonsRunning();
        isCancelled.set(false);

        ensureModelLoaded(TEXT_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> { 
                    appendSystemMessage("Error loading model: " + err.getMessage()); 
                    setButtonsIdle(); 
                });
                return;
            }
            executeTextRequest(TEXT_MODEL, "Responde en español: "+prompt, true);
        });
    }

    @FXML
    private void callPicture(ActionEvent event) {
        if (!imageLoaded || base64Image == null) {
            appendSystemMessage("Primer has de seleccionar una imatge.");
            setButtonsIdle();
            return;
        }
        
        String prompt = getPrompt();
        if (prompt.isEmpty()) {
            prompt = "Describe this image";
        }
        
        final String finalPrompt = prompt;
        
        // Mostrar missatge de l'usuari amb la imatge
        appendUserMessageWithImage(finalPrompt, selectedImageFile);
        
        // BUIDAR EL TEXTFIELD
        Platform.runLater(() -> {
            textFieldPrompt.clear();
        });
        
        setButtonsRunning();
        isCancelled.set(false);
        
        // Mostrar "Thinking..."
        if (thinkingLabel != null) {
            Platform.runLater(() -> thinkingLabel.setVisible(true));
        }

        ensureModelLoaded(VISION_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> { 
                    appendSystemMessage("Error loading model: " + err.getMessage()); 
                    setButtonsIdle();
                    if (thinkingLabel != null) thinkingLabel.setVisible(false);
                });
                return;
            }
            executeImageRequest(VISION_MODEL, "Responde en español: "+finalPrompt, base64Image);
            
            // Netejar la imatge després d'enviar
            Platform.runLater(() -> {
                resetImageAttributes();
                if (imagePreviewBox != null) {
                    imagePreviewBox.setVisible(false);
                    imagePreviewBox.setManaged(false);
                }
            });
        });
    }

    @FXML
    private void callBreak(ActionEvent event) {
        isCancelled.set(true);
        cancelStreamRequest();
        cancelCompleteRequest();
        Platform.runLater(() -> {
            appendSystemMessage("Petició aturada per l'usuari.");
            setButtonsIdle();
            resetImageAttributes();
            if (thinkingLabel != null) thinkingLabel.setVisible(false);
        });
    }

    // --- Request helpers ---

    // Text-only (stream or not)
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

        if (stream) {
            Platform.runLater(() -> {
                appendAssistantMessage(""); // Preparar per streaming
            });
            isFirst = true;

            streamRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    currentInputStream = response.body();
                    streamReadingTask = executorService.submit(() -> handleStreamResponse());
                    return response;
                })
                .exceptionally(e -> {
                    if (!isCancelled.get()) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            appendSystemMessage("Error en la connexió: " + e.getMessage());
                        });
                    }
                    Platform.runLater(this::setButtonsIdle);
                    return null;
                });

        } else {
            appendSystemMessage("Espera resposta completa...");

            completeRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String responseText = safeExtractTextResponse(response.body());
                    Platform.runLater(() -> { 
                        appendAssistantMessage(responseText);
                        setButtonsIdle(); 
                    });
                    return response;
                })
                .exceptionally(e -> {
                    if (!isCancelled.get()) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            appendSystemMessage("Error: " + e.getMessage());
                        });
                    }
                    Platform.runLater(this::setButtonsIdle);
                    return null;
                });
        }
    }

    // Image + prompt (non-stream) using vision model
    private void executeImageRequest(String model, String prompt, String base64Image) {
        JSONObject body = new JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("images", new JSONArray().put(base64Image))
            .put("stream", false)
            .put("keep_alive", "10m")
            .put("options", new JSONObject()
                .put("num_ctx", 2048)
                .put("num_predict", 256)
            );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.toString()))
            .build();

        completeRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                int code = resp.statusCode();
                String bodyStr = resp.body();

                String msg = tryParseAnyMessage(bodyStr);
                if (msg == null || msg.isBlank()) {
                    msg = (code >= 200 && code < 300) ? "(empty response)" : "HTTP " + code + ": " + bodyStr;
                }

                final String toShow = msg;
                Platform.runLater(() -> { 
                    appendAssistantMessage(toShow);
                    if (thinkingLabel != null) thinkingLabel.setVisible(false);
                    setButtonsIdle(); 
                });
                return resp;
            })
            .exceptionally(e -> {
                if (!isCancelled.get()) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        appendSystemMessage("Error en la petició: " + e.getMessage());
                        if (thinkingLabel != null) thinkingLabel.setVisible(false);
                    });
                }
                Platform.runLater(() -> setButtonsIdle());
                return null;
            });
    }

    // Stream reader for text responses
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
                
                final String currentResponse = fullResponse.toString();
                
                Platform.runLater(() -> {
                    updateLastAssistantMessage(currentResponse);
                });
            }
        } catch (Exception e) {
            if (!isCancelled.get()) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendSystemMessage("Error durant el streaming: " + e.getMessage());
                });
            }
        } finally {
            try { 
                if (currentInputStream != null) currentInputStream.close(); 
            } catch (Exception ignore) {}
            Platform.runLater(this::setButtonsIdle);
        }
    }

    // --- UI Helpers ---

    private void appendUserMessage(String message) {
        Label userLabel = new Label("👤 Usuari: " + message);
        userLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 8; -fx-background-radius: 10;");
        userLabel.setWrapText(true);
        userLabel.setMaxWidth(400);
        
        Platform.runLater(() -> {
            messagesBox.getChildren().add(userLabel);
            scrollToBottom();
        });
    }

    private void appendUserMessageWithImage(String message, File imageFile) {
        VBox userBox = new VBox(5);
        userBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 8; -fx-background-radius: 10;");
        userBox.setMaxWidth(400);
        
        if (message != null && !message.isEmpty()) {
            Label userLabel = new Label("👤 Usuari: " + message);
            userLabel.setWrapText(true);
            userBox.getChildren().add(userLabel);
        }
        
        if (imageFile != null && imageFile.exists()) {
            ImageView imgView = new ImageView(new Image(imageFile.toURI().toString()));
            imgView.setFitHeight(150);
            imgView.setFitWidth(150);
            imgView.setPreserveRatio(true);
            userBox.getChildren().add(imgView);
        }
        
        Platform.runLater(() -> {
            messagesBox.getChildren().add(userBox);
            scrollToBottom();
        });
    }

    private void appendAssistantMessage(String message) {
        Label assistantLabel = new Label("🤖 Assistent: " + message);
        assistantLabel.setStyle("-fx-background-color: #f1f8e9; -fx-padding: 8; -fx-background-radius: 10;");
        assistantLabel.setWrapText(true);
        assistantLabel.setMaxWidth(400);
        
        Platform.runLater(() -> {
            messagesBox.getChildren().add(assistantLabel);
            scrollToBottom();
        });
    }

    private void updateLastAssistantMessage(String message) {
        if (!messagesBox.getChildren().isEmpty()) {
            var lastNode = messagesBox.getChildren().get(messagesBox.getChildren().size() - 1);
            if (lastNode instanceof Label) {
                Label lastLabel = (Label) lastNode;
                if (lastLabel.getText().startsWith("🤖 Assistent:")) {
                    lastLabel.setText("🤖 Assistent: " + message);
                } else {
                    appendAssistantMessage(message);
                }
            } else {
                appendAssistantMessage(message);
            }
        } else {
            appendAssistantMessage(message);
        }
        scrollToBottom();
    }

    private void appendSystemMessage(String message) {
        Label systemLabel = new Label("ℹ️ " + message);
        systemLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        
        Platform.runLater(() -> {
            messagesBox.getChildren().add(systemLabel);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
    }

    // --- Small utils ---

    private String safeExtractTextResponse(String bodyStr) {
        try {
            JSONObject o = new JSONObject(bodyStr);
            String r = o.optString("response", null);
            if (r != null && !r.isBlank()) return r;
            if (o.has("message")) return o.optString("message");
            if (o.has("error"))   return "Error: " + o.optString("error");
        } catch (Exception ignore) {}
        return bodyStr != null && !bodyStr.isBlank() ? bodyStr : "(empty)";
    }

    private String tryParseAnyMessage(String bodyStr) {
        try {
            JSONObject o = new JSONObject(bodyStr);
            if (o.has("response")) return o.optString("response", "");
            if (o.has("message"))  return o.optString("message", "");
            if (o.has("error"))    return "Error: " + o.optString("error", "");
        } catch (Exception ignore) {}
        return null;
    }

    private void cancelStreamRequest() {
        if (streamRequest != null && !streamRequest.isDone()) {
            try {
                if (currentInputStream != null) {
                    currentInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (streamReadingTask != null) {
                streamReadingTask.cancel(true);
            }
            streamRequest.cancel(true);
        }
    }

    private void cancelCompleteRequest() {
        if (completeRequest != null && !completeRequest.isDone()) {
            completeRequest.cancel(true);
        }
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
                } catch (Exception ignore) {}

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
                        .thenAccept(r -> { 
                            Platform.runLater(() -> appendSystemMessage("Model " + modelName + " carregat correctament."));
                        });
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
            
            // Mostrar preview de la imatge
            if (imagePreviewBox != null && imagePreview != null) {
                Image img = new Image(file.toURI().toString());
                imagePreview.setImage(img);
                imagePreviewBox.setVisible(true);
                imagePreviewBox.setManaged(true);
            }
            
            appendSystemMessage("Imatge carregada: " + file.getName());
            textFieldPrompt.setPromptText("Escriu una pregunta sobre la imatge...");
            
        } catch (Exception e) {
            e.printStackTrace();
            appendSystemMessage("Error en carregar la imatge: " + e.getMessage());
            resetImageAttributes();
        }
    }
    
    @FXML
    private void clearImage() {
        resetImageAttributes();
        if (imagePreviewBox != null) {
            imagePreviewBox.setVisible(false);
            imagePreviewBox.setManaged(false);
        }
        textFieldPrompt.setPromptText("Escriu el teu missatge...");
    }

    @FXML
    private void resetImageAttributes() {
        imageLoaded = false;
        base64Image = null;
        selectedImageFile = null;
    }

    @FXML
    private void sendRequest(ActionEvent event) {
        if (imageLoaded) {
            callPicture(event);
        } else {
            callStream(event);
        }
    }
}