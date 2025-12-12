package se.scooterrental.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.persistence.ConfigHandler;

import java.util.function.Consumer;

public class SettingsView extends BaseView {

    private final Consumer<Boolean> onThemeToggle; // Callback för att byta tema
    private final String appVersion = "1.0.3"; // Versionsnummer

    public SettingsView(Consumer<Boolean> onThemeToggle) {
        super("Inställningar");
        this.onThemeToggle = onThemeToggle;
        setupUI();
    }

    @Override
    protected void setupUI() {
        Label title = new Label("Inställningar");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        rootLayout.getChildren().add(title);

        // --- SEKTION 1: Säkerhet ---
        VBox securityBox = createSection("Säkerhet");

        PasswordField currentPass = new PasswordField();
        currentPass.setPromptText("Nuvarande lösenord");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Nytt lösenord");
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Bekräfta nytt lösenord");

        Button changePassBtn = new Button("Byt Admin-lösenord");
        changePassBtn.getStyleClass().add("accent-button");
        changePassBtn.setOnAction(e -> handleChangePassword(currentPass, newPass, confirmPass));

        GridPane passGrid = new GridPane();
        passGrid.setHgap(10); passGrid.setVgap(10);
        passGrid.add(new Label("Nuvarande:"), 0, 0); passGrid.add(currentPass, 1, 0);
        passGrid.add(new Label("Nytt:"), 0, 1); passGrid.add(newPass, 1, 1);
        passGrid.add(new Label("Bekräfta:"), 0, 2); passGrid.add(confirmPass, 1, 2);
        passGrid.add(changePassBtn, 1, 3);

        securityBox.getChildren().add(passGrid);


        // --- SEKTION 2: Utseende ---
        VBox appearanceBox = createSection("Utseende");

        ToggleButton themeToggle = new ToggleButton("Mörkt läge");
        FontIcon moonIcon = new FontIcon("mdi2w-weather-night");
        themeToggle.setGraphic(moonIcon);

        // 1. Läs in sparad inställning från ConfigHandler
        boolean isDarkMode = ConfigHandler.isDarkMode();
        themeToggle.setSelected(isDarkMode);
        updateToggleText(themeToggle, moonIcon, isDarkMode);

        // 2. Applicera temat direkt vid start av vyn (om MainApp inte redan gjort det)
        // Detta säkerställer att togglen matchar verkligheten
        if (isDarkMode) {
            // FIX: Vi använder Platform.runLater för att säkerställa att MainApp har hunnit skapa
            // klart scenen (mainScene) innan vi försöker applicera temat.
            Platform.runLater(() -> onThemeToggle.accept(true));
        }

        // 3. Lyssna på ändringar och SPARA till Config
        themeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            onThemeToggle.accept(newVal);
            ConfigHandler.setDarkMode(newVal); // Spara inställningen
            updateToggleText(themeToggle, moonIcon, newVal);
        });

        appearanceBox.getChildren().add(themeToggle);


        // --- SEKTION 3: Om Applikationen ---
        VBox aboutBox = createSection("Om Systemet");

        aboutBox.getChildren().addAll(
                new Label("Retro Scooter Central"),
                new Label("Version: " + appVersion),
                new Label("Utvecklad av: [Ditt Namn]"),
                new Label("Support: support@scootercentral.se")
        );


        rootLayout.getChildren().addAll(securityBox, appearanceBox, aboutBox);
    }

    private void updateToggleText(ToggleButton btn, FontIcon icon, boolean isDark) {
        if (isDark) {
            btn.setText("Mörkt läge (På)");
            icon.setIconLiteral("mdi2w-weather-night");
        } else {
            btn.setText("Mörkt läge (Av)");
            icon.setIconLiteral("mdi2w-white-balance-sunny");
        }
    }

    private VBox createSection(String title) {
        VBox box = new VBox(10);
        // ÄNDRING: Använder CSS-klass istället för hårdkodad style
        box.getStyleClass().add("settings-section");

        Label header = new Label(title);
        // Låt header följa CSS för labels, men tvinga storlek och vikt
        // FIX: Se till att den har en klass så vi kan styra färgen specifikt om det behövs
        header.getStyleClass().add("section-header");
        // BORTTAGET: header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); <--- Detta orsakade problemet

        box.getChildren().add(header);
        box.getChildren().add(new Separator());

        return box;
    }

    private void handleChangePassword(PasswordField curr, PasswordField nev, PasswordField conf) {
        String current = curr.getText();
        String newP = nev.getText();
        String confP = conf.getText();

        if (current.isEmpty() || newP.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Fel", "Fyll i alla fält.");
            return;
        }

        if (!ConfigHandler.verifyAdminPassword(current)) {
            showAlert(Alert.AlertType.ERROR, "Fel", "Felaktigt nuvarande lösenord.");
            return;
        }

        if (!newP.equals(confP)) {
            showAlert(Alert.AlertType.ERROR, "Fel", "Nya lösenorden matchar inte.");
            return;
        }

        if (ConfigHandler.setAdminPassword(newP)) {
            showAlert(Alert.AlertType.INFORMATION, "Succé", "Lösenordet har ändrats!");
            curr.clear(); nev.clear(); conf.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Fel", "Kunde inte spara konfigurationen.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.show();
    }
}