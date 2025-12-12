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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.model.Member;
import se.scooterrental.persistence.ConfigHandler; // Importera ConfigHandler
import se.scooterrental.persistence.DataHandler;
import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.MembershipService;
import se.scooterrental.service.RentalService;
import se.scooterrental.ui.views.*;
import se.scooterrental.util.AutosaveThread;

import java.io.InputStream;
import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane mainLayout;
    private Scene mainScene; // Håller referens för att byta tema

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
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Dashboard
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

            // Inställningar
            SettingsView settingsView = new SettingsView(this::toggleTheme);
            Tab settingsTab = settingsView.getTab();
            configureTab(settingsTab, "antf-setting", "Inställningar");

            tabPane.getTabs().add(settingsTab);

        } else {
            tabPane.getTabs().add(itemsTab);
        }

        mainLayout = new BorderPane();

        // --- HEADER ---
        HBox logoContainer = new HBox(10);
        logoContainer.setAlignment(Pos.CENTER_LEFT);

        try {
            InputStream is = getClass().getResourceAsStream("/MenuLogo.png");
            if (is != null) {
                Image img = new Image(is);
                ImageView logoView = new ImageView(img);
                logoView.setFitHeight(62);
                logoView.setPreserveRatio(true);
                logoContainer.getChildren().add(logoView);
            } else {
                FontIcon logoIcon = new FontIcon("maki2-snowmobile-15");
                logoIcon.setIconSize(24);
                logoIcon.setIconColor(javafx.scene.paint.Color.web("#111827"));
                logoContainer.getChildren().add(logoIcon);
            }
        } catch (Exception e) { }

        Label logoLabel = new Label("SCOOTER CENTRAL");
        logoLabel.getStyleClass().add("logo");
        logoContainer.getChildren().add(logoLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon autosaveIcon = new FontIcon("mdi2c-cloud-check");
        autosaveIcon.setIconSize(18);
        autosaveIcon.setIconColor(javafx.scene.paint.Color.web("#10B981"));
        Label autosaveLabel = new Label("Autosparad");
        autosaveLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        autosaveIndicator = new HBox(5, autosaveLabel, autosaveIcon);
        autosaveIndicator.setAlignment(Pos.CENTER_RIGHT);
        autosaveIndicator.setPadding(new Insets(0, 15, 0, 0));
        autosaveIndicator.setOpacity(0);

        HBox topBar = new HBox(15, logoContainer, spacer, autosaveIndicator);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        Button logoutBtn = new Button("Logga ut");
        logoutBtn.setOnAction(e -> showLoginScreen());
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-cursor: hand;");

        topBar.getChildren().add(logoutBtn);

        mainLayout.setTop(topBar);
        mainLayout.setCenter(tabPane);

        mainScene = new Scene(mainLayout, 1100, 750);
        applyStyles(mainScene);

        // FIX: Applicera mörkt tema om det är aktiverat i config
        if (ConfigHandler.isDarkMode()) {
            toggleTheme(true);
        }

        primaryStage.setScene(mainScene);
        primaryStage.centerOnScreen();
    }

    private void toggleTheme(boolean isDark) {
        if (mainScene == null) return;

        // Riktig implementation av Dark Mode via CSS-klass
        if (isDark) {
            if (!mainScene.getRoot().getStyleClass().contains("dark-mode")) {
                mainScene.getRoot().getStyleClass().add("dark-mode");
            }
        } else {
            mainScene.getRoot().getStyleClass().remove("dark-mode");
        }
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