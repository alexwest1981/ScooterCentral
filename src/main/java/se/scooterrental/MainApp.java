package se.scooterrental;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import se.scooterrental.service.*;
import se.scooterrental.persistence.DataHandler;
import se.scooterrental.ui.views.*;
import se.scooterrental.util.*;


/**
 * Huvudapplikationsklass för Scooter Rental System.
 * Innehåller initialisering av services och UI.
 */
public class MainApp extends Application {

    private MemberRegistry memberRegistry;
    private Inventory inventory;
    private RentalService rentalService;
    private DataHandler dataHandler;
    private AutoSaveThread autoSaveThread;

    // UI Vyer
    private MemberView memberView;
    private ItemView itemView;
    private RentalView rentalView; // Behövs för att tvinga uppdatering efter laddning

    @Override
    public void init() throws Exception {
        // Initiera datalagring och register
        dataHandler = new DataHandler("data.json");

        memberRegistry = new MemberRegistry();
        inventory = new Inventory();

        // Försök ladda data vid start
        dataHandler.loadData(memberRegistry, inventory);

        // Initiera tjänster som bygger på register
        rentalService = new RentalService(memberRegistry, inventory);

        // Kör Autosave i bakgrunden (VG-krav)
        autoSaveThread = new AutoSaveThread(dataHandler, memberRegistry, inventory);
        autoSaveThread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Wigellkoncernen: Scooter- & Slädeutyrhning");

        // Initiera UI-vyer
        memberView = new MemberView(memberRegistry);
        itemView = new ItemView(inventory);
        rentalView = new RentalView(rentalService, memberRegistry, inventory);


        TabPane tabPane = new TabPane();

        // Skapa flikar och lägg till vyer
        Tab membersTab = new Tab("Medlemmar & Register", memberView.getLayout());
        membersTab.setClosable(false);

        Tab itemsTab = new Tab("Utrustning & Lager", itemView.getLayout());
        itemsTab.setClosable(false);

        Tab rentalsTab = new Tab("Uthyrning & Summering", rentalView.getLayout());
        rentalsTab.setClosable(false);

        tabPane.getTabs().addAll(membersTab, itemsTab, rentalsTab);

        // Skapa huvudlayout och Spara-knapp
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);

        // Spara-knapp i botten (likt mock-up)
        Button saveButton = new Button("Spara Data Nu");
        saveButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setOnAction(event -> saveData(saveButton)); // Skickar med knappen för feedback

        HBox buttonBar = new HBox(saveButton);
        buttonBar.setStyle("-fx-padding: 10; -fx-alignment: bottom-right;");
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Extra: Uppdatera vyer efter laddning (för att visa initierad data)
        memberView.loadMembers();
        itemView.loadItems();
        // rentalView uppdateras via sin konstruktor efter init()

    }

    /**
     * Hanterar manuell sparning av data.
     */
    private void saveData(Button feedbackButton) {
        try {
            dataHandler.saveData(memberRegistry, inventory);

            // Ge feedback till användaren
            String originalText = feedbackButton.getText();
            String originalStyle = feedbackButton.getStyle();

            feedbackButton.setText("Sparat!");
            feedbackButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

            // Återställ knappen efter 2 sekunder
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        feedbackButton.setText(originalText);
                        feedbackButton.setStyle(originalStyle);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Kunde inte spara data: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Säker avstängning av Autosave-tråden
        if (autoSaveThread != null) {
            autoSaveThread.stopThread();
            try {
                autoSaveThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Applikationen stängs ner.");
        // Data sparas automatiskt av AutoSaveThread innan avstängning
    }

    public static void main(String[] args) {
        launch(args);
    }
}