package com.project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

public class Controller implements Initializable {

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField promptField;
    @FXML
    private Button sendTextBtn;
    @FXML
    private Button sendImageBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private ImageView imageView;
    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private InputStream currentInputStream;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> streamReadingTask;
    private volatile boolean isFirst = false;

    // Models
    private static final String TEXT_MODEL   = "gemma3:1b";
    private static final String VISION_MODEL = "llava-phi3";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sendTextBtn.setOnAction(e -> sendText());
        sendImageBtn.setOnAction(e -> sendImage());
        cancelBtn.setOnAction(e -> cancelRequest());
        setButtonsIdle();
    }

    // --- Text request ---
    private void sendText() {
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty()) return;

        appendChat("You: " + prompt + "\n");
        promptField.clear();
        statusLabel.setText("Thinking...");
        isCancelled.set(false);
        isFirst = true; // 👈 Reiniciamos isFirst

        ensureModelLoaded(TEXT_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading model.");
                    appendChat("Error: Could not load text model.");
                });
                return;
            }
            executeTextRequest(TEXT_MODEL, prompt);
        });
    }

    private void executeTextRequest(String model, String prompt) {
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", true)
                .put("keep_alive", "10m");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

        streamRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    currentInputStream = response.body();
                    streamReadingTask = executorService.submit(this::handleStreamResponse);
                    return response;
                })
                .exceptionally(e -> {
                    if (!isCancelled.get()) {
                        e.printStackTrace();
                        Platform.runLater(() -> appendChat("Error during text generation."));
                    }
                    Platform.runLater(this::setButtonsIdle);
                    return null;
                });
    }

    private void handleStreamResponse() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentInputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCancelled.get()) break;
                if (line.isBlank()) continue;

                JSONObject jsonResponse = new JSONObject(line);
                String chunk = jsonResponse.optString("response", "");
                if (chunk.isEmpty()) continue;

                Platform.runLater(() -> {
                    if (isFirst) {
                        chatArea.appendText("Gemma3: " + chunk ); // 👈 Añadimos \n aquí
                        isFirst = false;
                    } else {
                        chatArea.appendText(chunk); // 👈 Sin modificar, preserva saltos internos
                    }
                });
            }
        } catch (Exception e) {
            if (!isCancelled.get()) {
                e.printStackTrace();
                Platform.runLater(() -> appendChat("Error during streaming."));
            }
        } finally {
            cleanupStream();
            Platform.runLater(() -> statusLabel.setText(""));
        }
    }

    // --- Image request ---
    private void sendImage() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(sendImageBtn.getScene().getWindow());
        if (file == null) return;

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
            Image img = new Image(new ByteArrayInputStream(bytes));
            imageView.setImage(img);
        } catch (IOException e) {
            appendChat("Error loading image: " + e.getMessage());
            return;
        }

        final String base64 = Base64.getEncoder().encodeToString(bytes);

        // 👇 Permitir al usuario añadir contexto textual (opcional)
        String userPrompt = promptField.getText().trim();
        if (userPrompt.isEmpty()) userPrompt = "Describe this image in detail.";

        appendChat("You sent an image");
        promptField.clear(); // Limpiamos el campo para no confundir
        statusLabel.setText("Thinking...");
        isCancelled.set(false);
        isFirst = true; // 👈 Reiniciamos isFirst

        // 👇 Creamos variables efectivamente finales explícitamente
        final String finalUserPrompt = userPrompt;
        final String finalBase64 = base64;

        ensureModelLoaded(VISION_MODEL).whenComplete((v, err) -> {
            if (err != null) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading vision model.");
                    appendChat("Error: Could not load vision model.");
                });
                return;
            }
            // 👇 Llamamos a un nuevo método para evitar captura de variables
            onModelLoadedForImage(finalUserPrompt, finalBase64);
        });
    }

    // Nuevo método para manejar la lógica posterior
    private void onModelLoadedForImage(String userPrompt, String base64) {
        executeImageRequest(VISION_MODEL, userPrompt, base64);
    }

    private void executeImageRequest(String model, String prompt, String base64Image) {
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("prompt", prompt)
                .put("images", new JSONArray().put(base64Image))
                .put("stream", false)
                .put("keep_alive", "10m");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

        completeRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    String msg = safeExtractTextResponse(resp.body());
                    Platform.runLater(() -> {
                        appendChat("Gemma3: " + msg);
                        statusLabel.setText("");
                    });
                    return resp;
                })
                .exceptionally(e -> {
                    if (!isCancelled.get()) {
                        e.printStackTrace();
                        Platform.runLater(() -> appendChat("Error during image analysis."));
                    }
                    Platform.runLater(this::setButtonsIdle);
                    return null;
                });
    }

    // --- Cancel ---
    private void cancelRequest() {
        isCancelled.set(true);
        cancelStreamRequest();
        cancelCompleteRequest();
        isFirst = true; // 👈 Reiniciamos isFirst
        Platform.runLater(() -> statusLabel.setText("Cancelled"));
    }

    private void cancelStreamRequest() {
        if (streamRequest != null && !streamRequest.isDone()) {
            try { if (currentInputStream != null) currentInputStream.close(); } catch (Exception ignore) {}
            if (streamReadingTask != null) streamReadingTask.cancel(true);
            streamRequest.cancel(true);
        }
    }

    private void cancelCompleteRequest() {
        if (completeRequest != null && !completeRequest.isDone()) {
            completeRequest.cancel(true);
            Platform.runLater(() -> appendChat("Request cancelled."));
        }
    }

    private void cleanupStream() {
        try { if (currentInputStream != null) currentInputStream.close(); } catch (Exception ignore) {}
        currentInputStream = null;
        streamReadingTask = null;
        streamRequest = null;
        isFirst = true; // 👈 Reiniciamos isFirst
    }

    // --- Utils ---
    private void appendChat(String text) {
        chatArea.appendText(text + "\n");
    }

    private String safeExtractTextResponse(String bodyStr) {
        if (bodyStr == null) return "(null)";
        if (bodyStr.isBlank()) return "(empty)";

        try {
            JSONObject o = new JSONObject(bodyStr);
            String r = o.optString("response", null);
            if (r != null) return r; // 👈 Devuelve tal cual, sin trim
            if (o.has("message")) return o.optString("message");
            if (o.has("error")) return "Error: " + o.optString("error");
        } catch (Exception ignore) {}

        return bodyStr; // 👈 Devuelve el cuerpo original sin modificar
    }

    // --- Model Loading ---
    private CompletableFuture<Void> ensureModelLoaded(String modelName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/ps"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> handleModelCheckResponse(resp, modelName));
    }

    private CompletableFuture<Void> handleModelCheckResponse(HttpResponse<String> resp, String modelName) {
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
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Failed to check model status."));
            return CompletableFuture.failedFuture(e);
        }

        if (loaded) {
            return CompletableFuture.completedFuture(null);
        }

        Platform.runLater(() -> statusLabel.setText("Loading model..."));

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
                    Platform.runLater(() -> statusLabel.setText("Model ready."));
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Failed to load model."));
                    return null;
                });
    }

    private void setButtonsIdle() {
        sendTextBtn.setDisable(false);
        sendImageBtn.setDisable(false);
        cancelBtn.setDisable(true); // 👈 Deshabilitamos cancelBtn si no hay nada para cancelar
    }
}