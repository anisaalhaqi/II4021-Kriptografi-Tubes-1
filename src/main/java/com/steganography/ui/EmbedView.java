package com.steganography.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.io.File;

import com.steganography.utils.common.Scheme;
import com.steganography.video.EmbedResult;
import com.steganography.video.Embedder;
import com.steganography.video.Reader;

public class EmbedView extends VBox {
    private final Stage stage;

    // Input
    private File coverVideoFile;
    private final Label coverVideoLabel;
    private final Button coverChangeButton;
    private final Button coverDiscardButton;
    private final ToggleGroup msgTypeGroup;
    private final TextArea textInput;
    private File secretFile;
    private final Label secretFileLabel;
    private final Button secretChangeButton;
    private final Button secretDiscardButton;
    private final VBox textInputBox;
    private final VBox fileInputBox;
    private final CheckBox encryptCheckBox;
    private final TextField encKeyField;
    private final HBox encKeyBox;
    private final ToggleGroup modeGroup;
    private final TextField stegoKeyField;
    private final HBox stegoKeyBox;
    private final ComboBox<Scheme> lsbSchemeBox;
    private final TextField outputNameField;

    // Results
    private final Label capacityLabel;
    private final Label mseLabel;
    private final Label psnrLabel;
    private final VBox terminalLog;
    private final VBox histogramBox;
    private final ProgressBar progressBar;
    private final ScrollPane outputScroll;

    private static final class EmbedTaskResult {
        private final String outputName;
        private final double mse;
        private final double psnr;
        private final int[][] origHist;
        private final int[][] stegHist;

        private EmbedTaskResult(String outputName, double mse, double psnr, int[][] origHist, int[][] stegHist) {
            this.outputName = outputName;
            this.mse = mse;
            this.psnr = psnr;
            this.origHist = origHist;
            this.stegHist = stegHist;
        }
    }

    public EmbedView(Stage stage) {
        this.stage = stage;
        this.setSpacing(12);
        this.setPadding(new Insets(16, 20, 16, 20));
        this.getStyleClass().add("content-area");
        this.setFillWidth(true);
        this.setMinHeight(0);

        // Terminal prompt
        getChildren().add(createPrompt("embed"));

        HBox workspace = new HBox(18);
        workspace.getStyleClass().add("workspace-split");
        workspace.setFillHeight(false);
        workspace.setMinHeight(0);
        VBox formColumn = new VBox(12);
        formColumn.getStyleClass().add("form-column");
        HBox.setHgrow(formColumn, Priority.ALWAYS);
        formColumn.setMinHeight(0);

        // Cover Video
        Label coverTitle = new Label(">> Cover Video");
        coverTitle.getStyleClass().add("section-title");

        coverVideoLabel = new Label("No file selected");
        coverVideoLabel.getStyleClass().add("file-path");

        Button coverBtn = new Button("Select Video");
        coverBtn.getStyleClass().add("file-button");
        coverBtn.setOnAction(e -> selectCoverVideo());
        coverChangeButton = createInlineActionButton("Change", this::selectCoverVideo);
        coverDiscardButton = createInlineActionButton("Discard", this::clearCoverVideo);

        capacityLabel = new Label("");
        capacityLabel.getStyleClass().add("capacity-label");

        HBox coverRow = new HBox(10, coverBtn, coverVideoLabel, coverChangeButton, coverDiscardButton);
        coverRow.setAlignment(Pos.CENTER_LEFT);

        // Message Type
        Label msgTitle = new Label(">> Secret Message");
        msgTitle.getStyleClass().add("section-title");

        msgTypeGroup = new ToggleGroup();
        RadioButton textRadio = new RadioButton("Text");
        textRadio.setToggleGroup(msgTypeGroup);
        textRadio.setSelected(true);
        textRadio.getStyleClass().add("radio-button");

        RadioButton fileRadio = new RadioButton("File");
        fileRadio.setToggleGroup(msgTypeGroup);
        fileRadio.getStyleClass().add("radio-button");

        HBox msgTypeRow = new HBox(16, textRadio, fileRadio);

        // Text input
        textInput = new TextArea();
        textInput.setPromptText("Enter your secret message here...");
        textInput.setPrefRowCount(3);
        textInput.setWrapText(true);
        textInputBox = new VBox(6, textInput);

        // File input
        secretFileLabel = new Label("No file selected");
        secretFileLabel.getStyleClass().add("file-path");
        Button fileBtn = new Button("📂 Select Secret File");
        fileBtn.getStyleClass().add("file-button");
        fileBtn.setOnAction(e -> selectSecretFile());
        secretChangeButton = createInlineActionButton("Change", this::selectSecretFile);
        secretDiscardButton = createInlineActionButton("Discard", this::clearSecretFile);
        HBox fileRow = new HBox(10, fileBtn, secretFileLabel, secretChangeButton, secretDiscardButton);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileInputBox = new VBox(6, fileRow);
        fileInputBox.setVisible(false);
        fileInputBox.setManaged(false);

        msgTypeGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            boolean isText = newVal == textRadio;
            textInputBox.setVisible(isText);
            textInputBox.setManaged(isText);
            fileInputBox.setVisible(!isText);
            fileInputBox.setManaged(!isText);
        });

        // Encryption
        Label encTitle = new Label(">> Encryption");
        encTitle.getStyleClass().add("section-title");

        encryptCheckBox = new CheckBox("Enable A5/1 Encryption");
        encryptCheckBox.getStyleClass().add("check-box");

        encKeyField = new TextField();
        encKeyField.setPromptText("Enter encryption key...");
        Label encKeyLabel = new Label("Key:");
        encKeyLabel.getStyleClass().add("field-label");
        encKeyBox = new HBox(10, encKeyLabel, encKeyField);
        encKeyBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(encKeyField, Priority.ALWAYS);
        encKeyBox.setVisible(false);
        encKeyBox.setManaged(false);

        encryptCheckBox.selectedProperty().addListener((obs, old, newVal) -> {
            encKeyBox.setVisible(newVal);
            encKeyBox.setManaged(newVal);
        });

        // Embedding Mode
        Label modeTitle = new Label(">> Embedding Mode");
        modeTitle.getStyleClass().add("section-title");

        modeGroup = new ToggleGroup();
        RadioButton seqRadio = new RadioButton("Sequential");
        seqRadio.setToggleGroup(modeGroup);
        seqRadio.setSelected(true);
        seqRadio.getStyleClass().add("radio-button");

        RadioButton rndRadio = new RadioButton("Random");
        rndRadio.setToggleGroup(modeGroup);
        rndRadio.getStyleClass().add("radio-button");

        HBox modeRow = new HBox(16, seqRadio, rndRadio);

        stegoKeyField = new TextField();
        stegoKeyField.setPromptText("Enter stego-key...");
        Label stegoKeyLabel = new Label("Stego-Key:");
        stegoKeyLabel.getStyleClass().add("field-label");
        stegoKeyBox = new HBox(10, stegoKeyLabel, stegoKeyField);
        stegoKeyBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stegoKeyField, Priority.ALWAYS);
        stegoKeyBox.setVisible(false);
        stegoKeyBox.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            boolean isRandom = newVal == rndRadio;
            stegoKeyBox.setVisible(isRandom);
            stegoKeyBox.setManaged(isRandom);
        });

        Label schemeTitle = new Label(">> LSB Scheme");
        schemeTitle.getStyleClass().add("section-title");

        lsbSchemeBox = new ComboBox<>();
        lsbSchemeBox.getItems().addAll(Scheme.values());
        lsbSchemeBox.getSelectionModel().select(Scheme.RGB_332);
        lsbSchemeBox.setMaxWidth(Double.MAX_VALUE);

        // Output
        Label outTitle = new Label(">> Output");
        outTitle.getStyleClass().add("section-title");

        outputNameField = new TextField();
        outputNameField.setPromptText("output_stego.avi");
        Label outLabel = new Label("Filename:");
        outLabel.getStyleClass().add("field-label");
        HBox outRow = new HBox(10, outLabel, outputNameField);
        outRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(outputNameField, Priority.ALWAYS);

        // Embed Button
        Button embedBtn = new Button("Embed Message");
        embedBtn.getStyleClass().add("action-button");
        embedBtn.setOnAction(e -> doEmbed());
        embedBtn.setMaxWidth(Double.MAX_VALUE);
        attachHoverFade(embedBtn);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        mseLabel = new Label("");
        mseLabel.getStyleClass().add("metric-value");
        psnrLabel = new Label("");
        psnrLabel.getStyleClass().add("metric-value");

        terminalLog = new VBox(6);
        terminalLog.getStyleClass().add("terminal-log");

        histogramBox = new VBox();
        histogramBox.setVisible(false);
        histogramBox.setManaged(false);

        VBox outputContent = new VBox(14, terminalLog, histogramBox);
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

        Label outputHeader = new Label("~/embed/output.log");
        outputHeader.getStyleClass().add("output-header");
        Label outputSubheader = new Label("runtime stream");
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

        // Add form children
        formColumn.getChildren().addAll(coverTitle, coverRow, capacityLabel,
                new Separator(), msgTitle, msgTypeRow, textInputBox,
                fileInputBox, new Separator(), encTitle, encryptCheckBox,
                encKeyBox, new Separator(), modeTitle, modeRow, stegoKeyBox,
                new Separator(), schemeTitle, lsbSchemeBox,
                new Separator(), outTitle, outRow, new Separator(), embedBtn);

        workspace.getChildren().addAll(formColumn, outputColumn);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        getChildren().add(workspace);

        updateCoverVideoState();
        updateSecretFileState();
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

    private void selectCoverVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Cover Video");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI Video", "*.avi"));
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            coverVideoFile = file;
            coverVideoLabel.setText(file.getName());
            updateCoverVideoState();

            Task<String> capTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    Reader reader = new Reader(file);
                    reader.readMetadata();
                    long capBytes = (long) reader.getWidth() * reader.getHeight() * reader.getTotalFrames();
                    String codec = reader.getVideoCodecName() != null ? reader.getVideoCodecName() : "unknown";
                    return String.format(
                            "Capacity: %,d bytes (%.2f KB) | %d frames | %dx%d @ %.1f fps | codec=%s",
                            capBytes, capBytes / 1024.0,
                            reader.getTotalFrames(), reader.getWidth(),
                            reader.getHeight(), reader.getFrameRate(), codec);
                }
            };
            capTask.setOnSucceeded(e -> capacityLabel.setText(capTask.getValue()));
            capTask.setOnFailed(e -> capacityLabel.setText("Error reading video"));
            new Thread(capTask).start();
        }
    }

    private void selectSecretFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Secret File");
        File file = chooser.showOpenDialog(stage);
        
        if (file != null) {
            secretFile = file;
            secretFileLabel.setText(file.getName() + " (" + file.length() + " bytes)");
            updateSecretFileState();
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

    private void clearCoverVideo() {
        coverVideoFile = null;
        coverVideoLabel.setText("No file selected");
        capacityLabel.setText("");
        outputNameField.setPromptText("output_stego.avi");
        updateCoverVideoState();
    }

    private void clearSecretFile() {
        secretFile = null;
        secretFileLabel.setText("No file selected");
        updateSecretFileState();
    }

    private void updateCoverVideoState() {
        boolean hasFile = coverVideoFile != null && coverVideoFile.exists();
        coverChangeButton.setVisible(hasFile);
        coverChangeButton.setManaged(hasFile);
        coverDiscardButton.setVisible(hasFile);
        coverDiscardButton.setManaged(hasFile);
    }

    private void updateSecretFileState() {
        boolean hasFile = secretFile != null && secretFile.exists();
        secretChangeButton.setVisible(hasFile);
        secretChangeButton.setManaged(hasFile);
        secretDiscardButton.setVisible(hasFile);
        secretDiscardButton.setManaged(hasFile);
    }

    private boolean isTextMode() {return msgTypeGroup.getToggles().get(0).isSelected();}
    private boolean isRandomMode() {return modeGroup.getToggles().get(1).isSelected();}

    private void doEmbed() {
        if (coverVideoFile == null || !coverVideoFile.exists()) {
            showError("Please select a cover video file.");
            return;
        }
        if (isTextMode() && (textInput.getText() == null || textInput.getText().isEmpty())) {
            showError("Please enter a text message.");
            return;
        }
        if (!isTextMode() && (secretFile == null || !secretFile.exists())) {
            showError("Please select a secret file.");
            return;
        }
        if (encryptCheckBox.isSelected() && (encKeyField.getText() == null || encKeyField.getText().isEmpty())) {
            showError("Please enter an encryption key.");
            return;
        }
        if (isRandomMode() && (stegoKeyField.getText() == null || stegoKeyField.getText().isEmpty())) {
            showError("Please enter a stego-key.");
            return;
        }

        String outputName = outputNameField.getText();
        if (outputName == null || outputName.isEmpty()) {
            outputName = "output_stego.avi";
        }
        if (!outputName.toLowerCase().endsWith(".avi")) {
            outputName += ".avi";
        }

        final String finalOutputName = outputName;

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(-1);

        resetOutput();

        Task<EmbedTaskResult> embedTask = new Task<>() {
            @Override
            protected EmbedTaskResult call() throws Exception {
                updateMessage("Reading cover video...");

                Reader reader = new Reader(coverVideoFile);
                reader.readMetadata();

                boolean encrypt = encryptCheckBox.isSelected();
                String encKey = encKeyField.getText();
                String stegoKey = stegoKeyField.getText();
                Embedder embedder = new Embedder();
                File outputFile = new File(coverVideoFile.getParentFile(), finalOutputName);
                Scheme scheme = lsbSchemeBox.getValue();

                updateMessage("Embedding and writing stego-video...");
                EmbedResult result;
                if (isTextMode()) {
                    result = embedder.embedText(coverVideoFile, outputFile, textInput.getText(), isRandomMode() ? stegoKey : null, encrypt ? encKey : null, scheme);
                } else {
                    result = embedder.embedFile(coverVideoFile, outputFile, secretFile, isRandomMode() ? stegoKey : null, encrypt ? encKey : null, scheme);
                }
                updateMessage("Computing metrics...");
                return new EmbedTaskResult(finalOutputName, result.mse, result.psnr, result.origHist, result.stegHist);
            }
        };

        final String[] lastMessage = {""};
        embedTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && !newMsg.isBlank() && !newMsg.equals(lastMessage[0])) {
                appendTerminalLine("info-label", newMsg);
                lastMessage[0] = newMsg;
            }
        });

        embedTask.setOnSucceeded(e -> {
            EmbedTaskResult result = embedTask.getValue();

            appendTerminalLine("result-label", "Embedding finished.");
            appendTerminalLine("file-path", "Output: " + result.outputName);
            mseLabel.setText(String.format("MSE: %.4f", result.mse));
            String psnrText = Double.isInfinite(result.psnr) ? "INF" : String.format("%.2f", result.psnr);
            psnrLabel.setText("PSNR: " + psnrText + " dB");
            appendTerminalLine("metric-value", mseLabel.getText());
            appendTerminalLine("metric-value", psnrLabel.getText());

            buildHistogramChart(result.origHist, result.stegHist);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            appendPrompt();
        });

        embedTask.setOnFailed(e -> {
            Throwable ex = embedTask.getException();
            if (ex instanceof OutOfMemoryError) {
                showError("Embedding failed: out of memory. Try shorter/lower-resolution video or run with higher heap (-Xmx).");
            } else {
                showError("Embedding failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            }
            progressBar.setVisible(false);
            progressBar.setManaged(false);
        });

        Thread thread = new Thread(embedTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void buildHistogramChart(int[][] origHist, int[][] stegHist) {
        histogramBox.getChildren().clear();

        String[] channelNames = {"Red", "Green", "Blue"};
        String[] origColors = {"#f38ba8", "#a6e3a1", "#89b4fa"};
        String[] stegColors = {"#eba0ac", "#94e2d5", "#b4befe"};

        for (int ch = 0; ch < 3; ch++) {
            NumberAxis xAxis = new NumberAxis(0, 255, 32);
            xAxis.setLabel("Intensity");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Frequency");

            AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
            chart.setTitle(channelNames[ch] + " Channel Histogram");
            chart.setCreateSymbols(false);
            chart.setPrefHeight(180);
            chart.setLegendVisible(true);
            chart.setAnimated(false);

            XYChart.Series<Number, Number> origSeries = new XYChart.Series<>();
            origSeries.setName("Cover");
            XYChart.Series<Number, Number> stegSeries = new XYChart.Series<>();
            stegSeries.setName("Stego");

            for (int i = 0; i < 256; i += 2) {
                origSeries.getData().add(new XYChart.Data<>(i, origHist[ch][i]));
                stegSeries.getData().add(new XYChart.Data<>(i, stegHist[ch][i]));
            }

            chart.getData().add(origSeries);
            chart.getData().add(stegSeries);

            final int channel = ch;
            chart.applyCss();
            javafx.application.Platform.runLater(() -> {
                if (origSeries.getNode() != null)
                    origSeries.getNode()
                            .setStyle("-fx-stroke: " + origColors[channel]
                                    + "; -fx-fill: " + origColors[channel]
                                    + "33;");
                if (stegSeries.getNode() != null)
                    stegSeries.getNode()
                            .setStyle("-fx-stroke: " + stegColors[channel]
                                    + "; -fx-fill: " + stegColors[channel]
                                    + "33;");
            });

            histogramBox.getChildren().add(chart);
        }

        histogramBox.setVisible(true);
        histogramBox.setManaged(true);
    }

    private void showError(String message) {
        appendTerminalLine("error-label", message);
        appendPrompt();
    }

    private void resetOutput() {
        terminalLog.getChildren().clear();
        histogramBox.getChildren().clear();
        histogramBox.setVisible(false);
        histogramBox.setManaged(false);
        appendPrompt();
        scrollOutputToBottom();
    }

    private void appendPrompt() {
        terminalLog.getChildren().add(createPrompt("embed"));
        scrollOutputToBottom();
    }

    private void appendTerminalLine(String styleClass, String text) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.getStyleClass().addAll("terminal-line", styleClass);
        terminalLog.getChildren().add(line);
        scrollOutputToBottom();
    }

    private void scrollOutputToBottom() {Platform.runLater(() -> outputScroll.setVvalue(1.0));}

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