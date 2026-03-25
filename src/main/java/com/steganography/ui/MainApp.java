package com.steganography.ui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final double WINDOW_WIDTH = 1100;
    private static final double WINDOW_HEIGHT = 750;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("StegAVI");

        StackPane root = new StackPane();
        root.getStyleClass().add("main-container");

        VBox terminalWindow = new VBox();
        terminalWindow.getStyleClass().add("terminal-window");
        terminalWindow.setMaxWidth(960);
        terminalWindow.setMaxHeight(680);
        terminalWindow.setPrefWidth(960);
        terminalWindow.setPrefHeight(680);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab embedTab = createTab("Embed", new EmbedView(primaryStage));
        Tab extractTab = createTab("Extract", new ExtractView(primaryStage));

        tabPane.getTabs().addAll(embedTab, extractTab);

        VBox.setVgrow(tabPane, Priority.ALWAYS);
        terminalWindow.getChildren().add(tabPane);

        StackPane.setAlignment(terminalWindow, Pos.CENTER);
        root.getChildren().add(terminalWindow);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private ScrollPane createTabScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("tab-content-scroll");
        return scrollPane;
    }

    private Tab createTab(String title, VBox content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tab-title-label");

        StackPane titleWrap = new StackPane(titleLabel);
        titleWrap.getStyleClass().add("tab-title-wrap");
        StackPane.setAlignment(titleLabel, Pos.CENTER_LEFT);

        Tab tab = new Tab();
        tab.setText("");
        tab.setGraphic(titleWrap);
        tab.setContent(createTabScrollPane(content));
        return tab;
    }

    public static void main(String[] args) {launch(args);}
}