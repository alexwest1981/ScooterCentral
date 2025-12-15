package se.scooterrental.ui.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import se.scooterrental.model.Member;
import se.scooterrental.service.MemberRegistry;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vy för att visa, söka, redigera och ta bort medlemmar.
 * FIXAT: handleDeleteMember använder nu registry.removeMember() istället för att försöka
 * ändra i den låsta listan.
 * UPPDATERAD: Automatisk generering av e-post och validering.
 */
public class MemberView extends BaseView {

    private final MemberRegistry registry;
    private TableView<Member> table;
    private ObservableList<Member> memberList;

    // Filter & Sök
    private TextField searchField;
    private ComboBox<String> statusFilter; // "Alla", "STANDARD", "PREMIUM", "STUDENT"

    public MemberView(MemberRegistry registry) {
        super("Medlemsregister");
        this.registry = registry;
        this.memberList = FXCollections.observableArrayList(registry.getMembers());

        setupUI();
    }

    @Override
    protected void setupUI() {
        Label title = new Label("Medlemmar");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        rootLayout.getChildren().add(title);

        // --- Verktygsfält (Sök, Filter, Knappar) ---
        HBox topBox = new HBox(15);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        // Sök
        searchField = new TextField();
        searchField.setPromptText("Sök ID, Namn...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, nev) -> filterMembers());

        // Filter
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Alla", "STANDARD", "PREMIUM", "STUDENT");
        statusFilter.setValue("Alla");
        statusFilter.valueProperty().addListener((obs, old, nev) -> filterMembers());

        // Knappar
        Button addButton = new Button("Lägg till");
        addButton.getStyleClass().add("accent-button");
        addButton.setOnAction(e -> showMemberDialog(null));

        Button editButton = new Button("Redigera");
        editButton.setOnAction(e -> {
            Member selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showMemberDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "Varning", "Välj en medlem att redigera.");
            }
        });

        Button deleteButton = new Button("Ta bort");
        deleteButton.getStyleClass().add("red-button");
        deleteButton.setOnAction(e -> handleDeleteMember());

        topBox.getChildren().addAll(
                new Label("Sök:"), searchField,
                new Label("Status:"), statusFilter,
                addButton, editButton, deleteButton
        );
        rootLayout.getChildren().add(topBox);

        // --- Tabell ---
        table = new TableView<>();
        table.setItems(memberList);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Member, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("memberId"));

        TableColumn<Member, String> fnCol = new TableColumn<>("Förnamn");
        fnCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<Member, String> lnCol = new TableColumn<>("Efternamn");
        lnCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<Member, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Member, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(idCol, fnCol, lnCol, emailCol, statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Dubbelklick för att redigera
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Member selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) showMemberDialog(selected);
            }
        });

        rootLayout.getChildren().add(table);
    }

    /**
     * Öppnar dialog för att skapa eller redigera medlem.
     * @param memberToEdit Om null skapas en ny medlem.
     */
    private void showMemberDialog(Member memberToEdit) {
        boolean isEditing = (memberToEdit != null);
        Dialog<Member> dialog = new Dialog<>();
        dialog.setTitle(isEditing ? "Redigera Medlem" : "Ny Medlem");
        dialog.setHeaderText(isEditing ? "Ändra uppgifter för " + memberToEdit.getName() : "Fyll i uppgifter för ny medlem");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // --- ID Fält (Specialhantering) ---
        Label prefixLabel = new Label("M");
        prefixLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 0 0 0;");

        TextField idNumberField = new TextField();

        if (isEditing) {
            // Extrahera siffrorna från ID (t.ex. "M100" -> "100")
            String currentIdNum = memberToEdit.getMemberId().replace("M", "");
            idNumberField.setText(currentIdNum);
        } else {
            // Generera nytt ID för ny medlem
            String newId = registry.generateNewId(); // Returnerar t.ex. "1050"
            idNumberField.setText(newId);
        }

        // Tvinga fältet att bara acceptera siffror
        idNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                idNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        HBox idBox = new HBox(5, prefixLabel, idNumberField);
        idBox.setAlignment(Pos.CENTER_LEFT);

        // Övriga fält
        TextField firstNameField = new TextField(isEditing ? memberToEdit.getFirstName() : "");
        TextField lastNameField = new TextField(isEditing ? memberToEdit.getLastName() : "");
        TextField phoneField = new TextField(isEditing ? memberToEdit.getPhone() : "");
        TextField emailField = new TextField(isEditing ? memberToEdit.getEmail() : "");
        emailField.setPromptText("Tomt = auto-generera"); // Tydlighet för användaren

        ComboBox<Member.MemberStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(Member.MemberStatus.values()));
        statusBox.setValue(isEditing ? memberToEdit.getStatus() : Member.MemberStatus.STANDARD);

        grid.add(new Label("Medlems-ID (Nummer):"), 0, 0);
        grid.add(idBox, 1, 0);

        grid.add(new Label("Förnamn:"), 0, 1);
        grid.add(firstNameField, 1, 1);

        grid.add(new Label("Efternamn:"), 0, 2);
        grid.add(lastNameField, 1, 2);

        grid.add(new Label("Telefon:"), 0, 3);
        grid.add(phoneField, 1, 3);

        grid.add(new Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);

        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType(isEditing ? "Spara" : "Lägg till", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                try {
                    String newIdNum = idNumberField.getText().trim();
                    if (newIdNum.isEmpty()) throw new IllegalArgumentException("ID-nummer saknas");

                    // Om det är en ny medlem, kontrollera att ID inte är upptaget (om användaren ändrat det manuellt)
                    String fullNewId = "M" + newIdNum;

                    if (!isEditing) {
                        // Kolla dubblett om vi skapar ny
                        if (registry.findMemberById(fullNewId).isPresent()) {
                            throw new IllegalArgumentException("ID " + fullNewId + " är upptaget.");
                        }
                    } else {
                        // Om vi redigerar och byter ID, kolla dubblett
                        if (!fullNewId.equals(memberToEdit.getMemberId()) && registry.findMemberById(fullNewId).isPresent()) {
                            throw new IllegalArgumentException("ID " + fullNewId + " är upptaget.");
                        }
                    }

                    // Hämta värden från fält
                    String fName = firstNameField.getText().trim();
                    String lName = lastNameField.getText().trim();
                    String phone = phoneField.getText().trim();
                    String emailInput = emailField.getText().trim();
                    Member.MemberStatus status = statusBox.getValue();

                    // Validera grundläggande fält (Mer detaljerad validering sker i Member-objektet, men vi kollar här också för generering)
                    if (fName.isEmpty()) throw new IllegalArgumentException("Förnamn krävs.");
                    if (lName.isEmpty()) throw new IllegalArgumentException("Efternamn krävs.");

                    // --- HANTERA EMAIL (Auto-generering & Validering) ---
                    String finalEmail;
                    if (emailInput.isEmpty()) {
                        // Generera email: fornamn.efternamn@scooterrental.se
                        // Rensar bort specialtecken och gör lowercase
                        String safeFirst = fName.toLowerCase().replaceAll("[^a-z0-9]", "");
                        String safeLast = lName.toLowerCase().replaceAll("[^a-z0-9]", "");
                        finalEmail = safeFirst + "." + safeLast + "@scooterrental.se";
                    } else {
                        // Validera angiven email med Regex
                        // Kräver minst tecken + @ + tecken + . + 2-6 tecken
                        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,6}$";
                        if (!emailInput.matches(emailRegex)) {
                            throw new IllegalArgumentException("Ogiltig e-postadress. Måste innehålla @ och giltig domän.");
                        }
                        finalEmail = emailInput;
                    }

                    if (isEditing) {
                        // Uppdatera befintlig
                        memberToEdit.setMemberId(fullNewId);
                        memberToEdit.setFirstName(fName);
                        memberToEdit.setLastName(lName);
                        memberToEdit.setPhone(phone);
                        memberToEdit.setEmail(finalEmail); // Spara den genererade/validerade mailen
                        memberToEdit.setStatus(status);
                        registry.updateMember(memberToEdit); // Sparar också
                        return memberToEdit;
                    } else {
                        // Skapa ny
                        Member newMember = new Member(
                                fullNewId,
                                fName,
                                lName,
                                phone,
                                finalEmail, // Spara den genererade/validerade mailen
                                status
                        );
                        registry.addMember(newMember); // Sparar också
                        return newMember;
                    }

                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Fel", e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(m -> {
            filterMembers(); // Uppdatera listan
            showAlert(Alert.AlertType.INFORMATION, "Sparat", isEditing ? "Medlem uppdaterad." : "Ny medlem tillagd.");
        });
    }

    private void handleDeleteMember() {
        Member selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Varning", "Välj en medlem att ta bort.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Ta bort medlem");
        confirm.setHeaderText("Är du säker på att du vill ta bort " + selected.getName() + "?");
        confirm.setContentText("Detta går inte att ångra.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // FIX: Anropa registrets metod istället för att ta bort från den låsta listan
            boolean removed = registry.removeMember(selected);

            if (removed) {
                // registry.saveData(); <-- Behövs inte, removeMember gör detta
                filterMembers(); // Uppdatera UI
                showAlert(Alert.AlertType.INFORMATION, "Borttagen", "Medlemmen har tagits bort.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Fel", "Kunde inte ta bort medlemmen.");
            }
        }
    }

    private void filterMembers() {
        String query = searchField.getText().toLowerCase();
        String statusFilterVal = statusFilter.getValue();

        memberList.setAll(registry.getMembers().stream()
                .filter(m -> {
                    // Filtrera på text
                    if (query.isEmpty()) return true;
                    return m.getMemberId().toLowerCase().contains(query) ||
                            m.getFirstName().toLowerCase().contains(query) ||
                            m.getLastName().toLowerCase().contains(query);
                })
                .filter(m -> {
                    // Filtrera på status
                    if ("Alla".equals(statusFilterVal)) return true;
                    return m.getStatus().name().equals(statusFilterVal);
                })
                .collect(Collectors.toList()));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}