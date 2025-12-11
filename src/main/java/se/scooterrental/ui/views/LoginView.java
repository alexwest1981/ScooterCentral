package se.scooterrental.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.model.Member;
import se.scooterrental.persistence.ConfigHandler;
import se.scooterrental.service.MemberRegistry;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Vyn för inloggning. Hanterar valet mellan Admin och Medlem.
 * Uppdaterad: Använder ConfigHandler för lösenordskoll och PasswordField för maskning.
 */
public class LoginView extends VBox {

    private final MemberRegistry memberRegistry;
    private final Consumer<Boolean> onAdminLogin;  // Callback när admin loggar in
    private final Consumer<Member> onMemberLogin; // Callback när medlem loggar in

    public LoginView(MemberRegistry memberRegistry, Consumer<Boolean> onAdminLogin, Consumer<Member> onMemberLogin) {
        this.memberRegistry = memberRegistry;
        this.onAdminLogin = onAdminLogin;
        this.onMemberLogin = onMemberLogin;

        setupUI();
    }

    private void setupUI() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: #050608; -fx-padding: 40;");

        FontIcon logoIcon = new FontIcon("antf-lock");
        logoIcon.setIconSize(64);
        logoIcon.setIconColor(javafx.scene.paint.Color.WHITE);

        Label title = new Label("SCOOTER CENTRAL");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setStyle("-fx-text-fill: white;");

        Label subtitle = new Label("Snöskoteruthyrning");
        subtitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 14pt;");

        // --- Admin Login ---
        Button adminBtn = new Button("Logga in som Admin");
        adminBtn.getStyleClass().add("accent-button");
        adminBtn.setStyle("-fx-font-size: 14pt; -fx-min-width: 250px;");
        adminBtn.setOnAction(e -> showAdminLoginDialog());

        // --- Member Login ---
        Button memberBtn = new Button("Gå vidare som Medlem");
        memberBtn.setStyle("-fx-font-size: 14pt; -fx-min-width: 250px; -fx-background-color: #333; -fx-text-fill: white;");
        memberBtn.setOnAction(e -> showMemberLoginDialog());

        this.getChildren().addAll(logoIcon, title, subtitle, new Separator(), adminBtn, memberBtn);
    }

    private void showAdminLoginDialog() {
        // Vi skapar en egen Dialog istället för TextInputDialog för att kunna använda PasswordField
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Login");
        dialog.setHeaderText("Ange administratörslösenord");

        // Sätt ikon (valfritt, återanvänder samma som i huvudvyn)
        dialog.setGraphic(new FontIcon("antf-lock"));

        // Konfigurera knapparna
        ButtonType loginButtonType = new ButtonType("Logga in", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Skapa lösenordsfältet (Detta maskerar texten med ****)
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Lösenord");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 150, 10, 10)); // Lite padding för snyggare look
        content.getChildren().add(new Label("Lösenord:"));
        content.getChildren().add(passwordField);

        dialog.getDialogPane().setContent(content);

        // Begär fokus på lösenordsfältet när dialogen öppnas
        Platform.runLater(passwordField::requestFocus);

        // Konvertera resultatet till sträng när man klickar på Logga in
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(password -> {
            // Använd den nya ConfigHandler för att verifiera lösenordet
            if (ConfigHandler.verifyAdminPassword(password)) {
                onAdminLogin.accept(true);
            } else {
                showAlert("Fel lösenord", "Åtkomst nekad.");
            }
        });
    }

    private void showMemberLoginDialog() {
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Medlemsinloggning");
        idDialog.setHeaderText("Ange ditt Medlems-ID");
        idDialog.setContentText("ID:");

        Optional<String> result = idDialog.showAndWait();
        result.ifPresent(id -> {
            Optional<Member> member = memberRegistry.findMemberById(id);
            if (member.isPresent()) {
                onMemberLogin.accept(member.get());
            } else {
                showAlert("Hittades inte", "Ingen medlem med det ID:t hittades.");
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}