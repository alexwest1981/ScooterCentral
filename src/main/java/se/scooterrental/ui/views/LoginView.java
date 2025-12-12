package se.scooterrental.ui.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.model.Member;
import se.scooterrental.persistence.ConfigHandler;
import se.scooterrental.service.MemberRegistry;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Vyn för inloggning. Hanterar valet mellan Admin och Medlem.
 * Uppdaterad: Visar custom logo.png och använder säker inloggning.
 */
public class LoginView extends VBox {

    private final MemberRegistry memberRegistry;
    private final Consumer<Boolean> onAdminLogin;
    private final Consumer<Member> onMemberLogin;

    public LoginView(MemberRegistry memberRegistry, Consumer<Boolean> onAdminLogin, Consumer<Member> onMemberLogin) {
        this.memberRegistry = memberRegistry;
        this.onAdminLogin = onAdminLogin;
        this.onMemberLogin = onMemberLogin;

        setupUI();
    }

    private void setupUI() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: #ffffff; -fx-padding: 40;");

        // --- LOGOTYP ---
        // Försöker ladda logo.png från resources
        ImageView logoView = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream("/logo2.png");
            if (is != null) {
                Image image = new Image(is);
                logoView.setImage(image);
                logoView.setFitWidth(400); // Justera bredden så den passar snyggt
                logoView.setPreserveRatio(true);
                this.getChildren().add(logoView);
            } else {
                // Fallback om bilden saknas: Visa hänglås-ikonen
                FontIcon fallbackIcon = new FontIcon("antf-lock");
                fallbackIcon.setIconSize(64);
                fallbackIcon.setIconColor(javafx.scene.paint.Color.WHITE);
                this.getChildren().add(fallbackIcon);
            }
        } catch (Exception e) {
            System.err.println("Kunde inte ladda logotypen: " + e.getMessage());
        }

        // Label title = new Label("SCOOTER CENTRAL");
        // title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        // title.setStyle("-fx-text-fill: white;");

        // Label subtitle = new Label("Snöskoteruthyrning");
        // subtitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 14pt;");

        // --- Admin Login ---
        Button adminBtn = new Button("Logga in som Admin");
        adminBtn.getStyleClass().add("accent-button");
        adminBtn.setStyle("-fx-font-size: 14pt; -fx-min-width: 250px;");
        adminBtn.setOnAction(e -> showAdminLoginDialog());

        // --- Member Login ---
        Button memberBtn = new Button("Gå vidare som Medlem");
        memberBtn.setStyle("-fx-font-size: 14pt; -fx-min-width: 250px; -fx-background-color: #333; -fx-text-fill: white;");
        memberBtn.setOnAction(e -> showMemberLoginDialog());

        this.getChildren().addAll(new Separator(), adminBtn, memberBtn);
    }

    private void showAdminLoginDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Login");
        dialog.setHeaderText("Ange administratörslösenord");

        ButtonType loginButtonType = new ButtonType("Logga in", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Lösenord");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 150, 10, 10));
        content.getChildren().add(new Label("Lösenord:"));
        content.getChildren().add(passwordField);

        dialog.getDialogPane().setContent(content);

        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(password -> {
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