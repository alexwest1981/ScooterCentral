package se.scooterrental;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.model.Member;
import se.scooterrental.persistence.DataHandler;
import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.MembershipService;
import se.scooterrental.service.RentalService;
import se.scooterrental.ui.views.DashboardView;
import se.scooterrental.ui.views.ItemView;
import se.scooterrental.ui.views.LoginView;
import se.scooterrental.ui.views.MemberView;
import se.scooterrental.ui.views.RentalView;
import se.scooterrental.util.AutosaveThread;

import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane mainLayout;

    private MemberRegistry memberRegistry;
    private Inventory inventory;
    private RentalService rentalService;
    private MembershipService membershipService;
    private AutosaveThread autosaveThread;

    private HBox autosaveIndicator;

    @Override
    public void init() throws Exception {
        DataHandler dataHandler = new DataHandler();
        memberRegistry = new MemberRegistry();
        inventory = new Inventory();
        membershipService = new MembershipService(memberRegistry);
        rentalService = new RentalService(memberRegistry, inventory);

        autosaveThread = new AutosaveThread(memberRegistry, inventory, rentalService);
        autosaveThread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Scooter Central - Login");
        showLoginScreen();
    }

    private void showLoginScreen() {
        LoginView loginView = new LoginView(
                memberRegistry,
                this::showAdminDashboard,
                this::showMemberDashboard
        );
        Scene scene = new Scene(loginView, 900, 650);
        applyStyles(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAdminDashboard(boolean success) {
        if (!success) return;
        setupMainLayout(true, null);
        configureAutosaveListener();
    }

    private void showMemberDashboard(Member member) {
        setupMainLayout(false, member);
        configureAutosaveListener();
    }

    private void configureAutosaveListener() {
        if (autosaveThread != null) {
            autosaveThread.setOnSaveCallback(() -> {
                Platform.runLater(this::showAutosaveAnimation);
            });
        }
    }

    private void showAutosaveAnimation() {
        if (autosaveIndicator == null) return;
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), autosaveIndicator);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        PauseTransition stay = new PauseTransition(Duration.seconds(2));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(1500), autosaveIndicator);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        new SequentialTransition(fadeIn, stay, fadeOut).play();
    }

    private void setupMainLayout(boolean isAdmin, Member currentMember) {
        primaryStage.setTitle("Scooter Central" + (isAdmin ? " - Admin" : " - Medlem"));

        TabPane tabPane = new TabPane();
        tabPane.setSide(Side.LEFT);

        // 1. Dashboard - Skickar nu med memberRegistry!
        DashboardView dashboardView = new DashboardView(rentalService, inventory, memberRegistry);
        Tab dashTab = dashboardView.getTab();
        configureTab(dashTab, "antf-dashboard", "Översikt");

        // 2. ItemView (Lager)
        ItemView itemView = new ItemView(inventory, rentalService, isAdmin, currentMember);
        Tab itemsTab = itemView.getTab();
        configureTab(itemsTab, "maki2-snowmobile-11", "Utrustning");

        // 3. Admin Specifika Vyer
        if (isAdmin) {
            tabPane.getTabs().addAll(dashTab, itemsTab);

            // Medlemmar
            MemberView memberView = new MemberView(memberRegistry);
            Tab membersTab = memberView.getTab();
            configureTab(membersTab, "antf-idcard", "Medlemmar");

            // Kassa (RentalView)
            RentalView rentalView = new RentalView(rentalService, memberRegistry, inventory);
            Tab rentalsTab = rentalView.getTab();
            configureTab(rentalsTab, "antf-shopping", "Kassa & Bokning");

            tabPane.getTabs().addAll(membersTab, rentalsTab);
        } else {
            tabPane.getTabs().add(itemsTab);
        }

        mainLayout = new BorderPane();

        // --- HEADER ---
        FontIcon logoIcon = new FontIcon("maki2-snowmobile-15");
        logoIcon.setIconSize(24);
        logoIcon.setIconColor(javafx.scene.paint.Color.web("#111827"));

        Label logoLabel = new Label("SCOOTER CENTRAL");
        logoLabel.getStyleClass().add("logo");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        FontIcon autosaveIcon = new FontIcon("mdi2c-cloud-check");
        autosaveIcon.setIconSize(18);
        autosaveIcon.setIconColor(javafx.scene.paint.Color.web("#10B981"));
        Label autosaveLabel = new Label("Autosparad");
        autosaveLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        autosaveIndicator = new HBox(5, autosaveLabel, autosaveIcon);
        autosaveIndicator.setAlignment(Pos.CENTER_RIGHT);
        autosaveIndicator.setPadding(new Insets(0, 15, 0, 0));
        autosaveIndicator.setOpacity(0);

        HBox topBar = new HBox(15, logoIcon, logoLabel, spacer, autosaveIndicator);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 15 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        Button logoutBtn = new Button("Logga ut");
        logoutBtn.setOnAction(e -> showLoginScreen());
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-cursor: hand;");

        topBar.getChildren().add(logoutBtn);

        mainLayout.setTop(topBar);
        mainLayout.setCenter(tabPane);

        Scene scene = new Scene(mainLayout, 1100, 750);
        applyStyles(scene);

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    private void configureTab(Tab tab, String iconCode, String tooltip) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.getStyleClass().add("sidebar-icon");
        tab.setGraphic(icon);
        tab.setText("");
        tab.setTooltip(new Tooltip(tooltip));
    }

    private void applyStyles(Scene scene) {
        try {
            URL cssResource = getClass().getResource("style.css");
            if (cssResource == null) {
                cssResource = getClass().getResource("/se/scooterrental/style.css");
            }
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Fel vid laddning av CSS: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (autosaveThread != null) autosaveThread.stopThread();
    }

    public static void main(String[] args) {
        launch(args);
    }
}