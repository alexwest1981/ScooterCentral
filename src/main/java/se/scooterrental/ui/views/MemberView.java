package se.scooterrental.ui.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import se.scooterrental.model.Member;
import se.scooterrental.model.Member.MemberStatus; // Importerar MemberStatus
import se.scooterrental.service.MemberRegistry;

import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Vy för att hantera medlemsregister: lägga till, lista och ändra medlemmar.
 */
public class MemberView extends BaseView {

    private final MemberRegistry memberRegistry;
    private TableView<Member> memberTable;
    private ObservableList<Member> memberList;

    public MemberView(MemberRegistry memberRegistry) {
        super("Medlemmar & Register");
        this.memberRegistry = memberRegistry;

        // Initialisera listan från registret vid start
        this.memberList = FXCollections.observableArrayList(memberRegistry.getAllMembers());

        setupUI();
    }

    @Override
    protected void setupUI() {
        Label title = new Label("Medlemsregister");
        title.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        memberTable = createMemberTable();

        Button addButton = new Button("Lägg till Ny Medlem");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setOnAction(e -> showAddMemberDialog());

        // Knapp för att uppdatera (om man t.ex. vill ändra data i filerna)
        Button refreshButton = new Button("Uppdatera Lista");
        refreshButton.setOnAction(e -> loadMembers());

        HBox controlBox = new HBox(10, addButton, refreshButton);
        controlBox.setPadding(new Insets(10, 0, 0, 0));

        rootLayout.getChildren().addAll(title, memberTable, controlBox);
    }

    /**
     * Skapar och konfigurerar TableView för medlemmar.
     */
    private TableView<Member> createMemberTable() {
        TableView<Member> table = new TableView<>();
        table.setItems(memberList);

        TableColumn<Member, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Member, String> nameCol = new TableColumn<>("Namn");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Member, String> phoneCol = new TableColumn<>("Telefon");
        // OBS: Attributet för telefonnummer i Member.java måste ha en getter som heter getPhone()
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Member, MemberStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(idCol, nameCol, phoneCol, statusCol);
        // FIX: Byt ut deprecated CONSTRAINED_RESIZE_POLICY
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        return table;
    }

    /**
     * Visar dialog för att lägga till en ny medlem.
     */
    private void showAddMemberDialog() {
        Dialog<Member> dialog = new Dialog<>();
        dialog.setTitle("Lägg till Medlem");
        dialog.setHeaderText("Fyll i uppgifter för den nya medlemmen.");

        ButtonType saveButtonType = new ButtonType("Spara", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Namn");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Telefonnummer");

        // FIX: Använd ComboBox för att direkt välja rätt Enum-typ
        ComboBox<MemberStatus> statusComboBox = new ComboBox<>(FXCollections.observableArrayList(MemberStatus.values()));
        statusComboBox.getSelectionModel().selectFirst();

        grid.add(new Label("Namn:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Telefon:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusComboBox, 1, 2); // Använder ComboBox

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                MemberStatus status = statusComboBox.getValue(); // Hämta Enum direkt

                if (name.isEmpty() || phone.isEmpty() || status == null) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Alla fält måste fyllas i.");
                    return null;
                }

                // Generera ett unikt ID (alternativt låt MemberRegistry hantera detta)
                String newId = "M" + (memberRegistry.getAllMembers().size() + 100);

                try {
                    // FIX: Korrekt ordning och typer: (id, name, phone, status)
                    Member newMember = new Member(newId, name, phone, status);

                    // Lägg till i registret
                    memberRegistry.addMember(newMember);
                    showAlert(Alert.AlertType.INFORMATION, "Sparat", "Medlem " + name + " sparad med ID: " + newId);
                    return newMember;
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Fel vid registrering", e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(member -> {
            loadMembers(); // Uppdatera tabellen efter lyckad spara
        });
    }

    /**
     * Laddar om medlemslistan från MemberRegistry och uppdaterar TableView.
     */
    public void loadMembers() {
        memberList.setAll(memberRegistry.getAllMembers());
        memberTable.refresh();
    }

    /**
     * Hjälpmetod för att visa alert-meddelanden.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}