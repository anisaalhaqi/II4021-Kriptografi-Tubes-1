package com.steganography.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import com.steganography.crypto.MD5;
import com.steganography.crypto.SHA256;
import com.steganography.video.ExtractResult;
import com.steganography.video.Extractor;

public class ExtractView extends VBox {
    private final Stage stage;

    // Input
    private File stegoVideoFile;
    private final Label stegoVideoLabel;
    private final Button stegoChangeButton;
    private final Button stegoDiscardButton;
    private final TextField encKeyField;
    private final TextField stegoKeyField;

    // Results
    private final VBox terminalLog;
    private final VBox actionBox;
    private final Button saveFileBtn;
    private final ProgressBar progressBar;
    private final ScrollPane outputScroll;

    private byte[] extractedData;
    private String extractedFileName;

    public ExtractView(Stage stage) {
        this.stage = stage;
        this.setSpacing(12);
        this.setPadding(new Insets(16, 20, 16, 20));
        this.getStyleClass().add("content-area");
        this.setFillWidth(true);
        this.setMinHeight(0);

        getChildren().add(createPrompt("extract"));

        HBox workspace = new HBox(18);
        workspace.getStyleClass().add("workspace-split");
        workspace.setFillHeight(false);
        workspace.setMinHeight(0);

        VBox formColumn = new VBox(12);
        formColumn.getStyleClass().add("form-column");
        HBox.setHgrow(formColumn, Priority.ALWAYS);
        formColumn.setMinHeight(0);

        Label stegoTitle = new Label(">> Stego Video");
        stegoTitle.getStyleClass().add("section-title");

        stegoVideoLabel = new Label("No file selected");
        stegoVideoLabel.getStyleClass().add("file-path");

        Button stegoBtn = new Button("Select Stego Video");
        stegoBtn.getStyleClass().add("file-button");
        stegoBtn.setOnAction(e -> selectStegoVideo());
        stegoChangeButton = createInlineActionButton("Change", this::selectStegoVideo);
        stegoDiscardButton = createInlineActionButton("Discard", this::clearStegoVideo);

        HBox stegoRow = new HBox(10, stegoBtn, stegoVideoLabel, stegoChangeButton, stegoDiscardButton);
        stegoRow.setAlignment(Pos.CENTER_LEFT);

        Label keysTitle = new Label(">> Keys");
        keysTitle.getStyleClass().add("section-title");

        encKeyField = new TextField();
        encKeyField.setPromptText("A5/1 key (leave empty if not encrypted)");
        Label encLabel = new Label("A5/1 Key:");
        encLabel.getStyleClass().add("field-label");
        HBox encRow = new HBox(10, encLabel, encKeyField);
        encRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(encKeyField, Priority.ALWAYS);

        stegoKeyField = new TextField();
        stegoKeyField.setPromptText("Stego-key (leave empty if sequential)");
        Label stgLabel = new Label("Stego-Key:");
        stgLabel.getStyleClass().add("field-label");
        HBox stgRow = new HBox(10, stgLabel, stegoKeyField);
        stgRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stegoKeyField, Priority.ALWAYS);

        Button extractBtn = new Button("Extract Message");
        extractBtn.getStyleClass().add("action-button");
        extractBtn.setOnAction(e -> doExtract());
        extractBtn.setMaxWidth(Double.MAX_VALUE);
        attachHoverFade(extractBtn);

        formColumn.getChildren().addAll(stegoTitle, stegoRow, new Separator(), keysTitle, encRow, stgRow, new Separator(), extractBtn);

        terminalLog = new VBox(6);
        terminalLog.getStyleClass().add("terminal-log");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        saveFileBtn = new Button("Save As");
        saveFileBtn.getStyleClass().add("save-button");
        saveFileBtn.setOnAction(e -> saveExtractedFile());
        attachHoverFade(saveFileBtn);

        actionBox = new VBox(saveFileBtn);
        actionBox.setVisible(false);
        actionBox.setManaged(false);

        VBox outputContent = new VBox(14, terminalLog, actionBox);
        outputContent.getStyleClass().add("terminal-content");

        outputScroll = new ScrollPane(outputContent);
        outputScroll.setFitToWidth(true);
        outputScroll.setFitToHeight(false);
        outputScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        outputScroll.getStyleClass().add("output-scroll");
        outputScroll.setMinHeight(0);
        DoubleBinding outputViewportHeight = Bindings.max(260.0, stage.heightProperty().subtract(355.0));
        outputScroll.prefViewportHeightProperty().bind(outputViewportHeight);
        outputScroll.prefHeightProperty().bind(outputViewportHeight.add(18.0));
        outputScroll.maxHeightProperty().bind(outputViewportHeight.add(18.0));
        VBox.setVgrow(outputScroll, Priority.ALWAYS);
        outputContent.heightProperty().addListener((obs, oldVal, newVal) -> scrollOutputToBottom());

        Label outputHeader = new Label("~/extract/output.log");
        outputHeader.getStyleClass().add("output-header");
        Label outputSubheader = new Label("decode stream");
        outputSubheader.getStyleClass().add("output-subheader");
        VBox outputTitle = new VBox(2, outputHeader, outputSubheader);
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("terminal-clear-button");
        clearButton.setOnAction(e -> resetOutput());
        Region outputSpacer = new Region();
        HBox.setHgrow(outputSpacer, Priority.ALWAYS);
        HBox outputToolbar = new HBox(10, outputTitle, outputSpacer, clearButton);
        outputToolbar.setAlignment(Pos.CENTER_LEFT);
        outputToolbar.getStyleClass().add("output-toolbar");

        VBox outputColumn = new VBox(12, outputToolbar, progressBar, outputScroll);
        outputColumn.getStyleClass().add("output-column");
        outputColumn.setMinWidth(340);
        outputColumn.setPrefWidth(380);
        outputColumn.setMaxWidth(440);
        outputColumn.setMinHeight(0);
        VBox.setVgrow(outputScroll, Priority.ALWAYS);

        workspace.getChildren().addAll(formColumn, outputColumn);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        getChildren().add(workspace);

        updateStegoVideoState();
        appendPrompt();
    }

    private HBox createPrompt(String command) {
        Label hostChip = new Label("steg");
        hostChip.getStyleClass().addAll("prompt-chip", "prompt-host-chip");

        Label pathChip = new Label("home");
        pathChip.getStyleClass().addAll("prompt-chip", "prompt-path-chip");

        Region pathArrow = new Region();
        pathArrow.getStyleClass().add("prompt-path-arrow");

        Label cmd = new Label(command);
        cmd.getStyleClass().add("prompt-command");
        HBox.setMargin(cmd, new Insets(0, 0, 0, 10));

        HBox prompt = new HBox(hostChip, pathChip, pathArrow, cmd);
        prompt.setAlignment(Pos.CENTER_LEFT);
        prompt.setPadding(new Insets(0, 0, 4, 0));
        prompt.getStyleClass().add("terminal-prompt");
        return prompt;
    }

    private void selectStegoVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Stego Video");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI Video", "*.avi"));
        File file = chooser.showOpenDialog(stage);
        
        if (file != null) {
            stegoVideoFile = file;
            stegoVideoLabel.setText(file.getName());
            updateStegoVideoState();
        }
    }

    private Button createInlineActionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("inline-action-button");
        button.setOnAction(e -> action.run());
        button.setVisible(false);
        button.setManaged(false);
        return button;
    }

    private void clearStegoVideo() {
        stegoVideoFile = null;
        stegoVideoLabel.setText("No file selected");
        updateStegoVideoState();
    }

    private void updateStegoVideoState() {
        boolean hasFile = stegoVideoFile != null && stegoVideoFile.exists();
        stegoChangeButton.setVisible(hasFile);
        stegoChangeButton.setManaged(hasFile);
        stegoDiscardButton.setVisible(hasFile);
        stegoDiscardButton.setManaged(hasFile);
    }

    private void doExtract() {
        if (stegoVideoFile == null || !stegoVideoFile.exists()) {
            showError("Please select a stego video file.");
            return;
        }

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(-1);
        actionBox.setVisible(false);
        actionBox.setManaged(false);
        resetOutput();

        String encKey = encKeyField.getText();
        String stegoKey = stegoKeyField.getText();

        Task<ExtractResult> extractTask = new Task<>() {
            @Override
            protected ExtractResult call() throws Exception {
                updateMessage("Reading stego video...");
                updateMessage("Extracting hidden data...");
                return new Extractor().extract(stegoVideoFile, stegoKey, encKey);
            }
        };

        final String[] lastMessage = {""};
        extractTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && !newMsg.isBlank() && !newMsg.equals(lastMessage[0])) {
                appendTerminalLine("info-label", newMsg);
                lastMessage[0] = newMsg;
            }
        });

        extractTask.setOnSucceeded(e -> {
            ExtractResult result = extractTask.getValue();

            StringBuilder info = new StringBuilder("Success: extraction finished.");
            if (result.wasEncrypted) {
                info.append(" [decrypted]");
            }
            if (result.wasRandom) {
                info.append(" [random]");
            }
            appendTerminalLine("result-label", info.toString());

            if (result.isFile) {
                extractedData = result.data;
                extractedFileName = result.fileName;
                appendTerminalLine("file-path", String.format("File: %s (%,d bytes)", result.fileName, result.data.length));
                appendDigestLines(result.data);
                actionBox.setVisible(true);
                actionBox.setManaged(true);
            } else {
                appendTerminalLine("field-label", "Extracted text:");
                appendTerminalBlock("terminal-line", result.getText());
                appendDigestLines(result.data);
            }

            progressBar.setVisible(false);
            progressBar.setManaged(false);
            appendPrompt();
        });

        extractTask.setOnFailed(e -> {
            Throwable ex = extractTask.getException();
            showError("Extraction failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            progressBar.setVisible(false);
            progressBar.setManaged(false);
        });

        Thread thread = new Thread(extractTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void saveExtractedFile() {
        if (extractedData == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Extracted File");
        if (extractedFileName != null && !extractedFileName.isEmpty()) {
            chooser.setInitialFileName(extractedFileName);
        }
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                Files.write(file.toPath(), extractedData);
                appendTerminalLine("result-label", "File saved: " + file.getName());
                appendPrompt();
            } catch (Exception e) {
                showError("Failed to save file: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        appendTerminalLine("error-label", message);
        appendPrompt();
    }

    private void resetOutput() {
        terminalLog.getChildren().clear();
        actionBox.setVisible(false);
        actionBox.setManaged(false);
        extractedData = null;
        extractedFileName = null;
        appendPrompt();
        scrollOutputToBottom();
    }

    private void appendPrompt() {
        terminalLog.getChildren().add(createPrompt("extract"));
        scrollOutputToBottom();
    }

    private void appendTerminalLine(String styleClass, String text) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.getStyleClass().addAll("terminal-line", styleClass);
        terminalLog.getChildren().add(line);
        scrollOutputToBottom();
    }

    private void appendTerminalBlock(String styleClass, String text) {
        String[] lines = text.split("\\R", -1);
        for (String lineText : lines) {
            appendTerminalLine(styleClass, lineText);
        }
    }

    private void appendDigestLines(byte[] data) {
        appendTerminalLine("info-label", "MD5: " + toHex(MD5.digest(data)));
        appendTerminalLine("info-label", "SHA-256: " + toHex(SHA256.digest(data)));
    }

    private void scrollOutputToBottom() {Platform.runLater(() -> outputScroll.setVvalue(1.0));}

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void attachHoverFade(Button button) {
        button.setOpacity(0.8);
        button.hoverProperty().addListener((obs, oldVal, hovered) -> {
            FadeTransition fade = new FadeTransition(Duration.millis(220), button);
            fade.setFromValue(button.getOpacity());
            fade.setToValue(hovered ? 1.0 : 0.8);
            fade.play();
        });
    }
}