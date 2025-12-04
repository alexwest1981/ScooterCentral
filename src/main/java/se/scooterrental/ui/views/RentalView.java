package se.scooterrental.ui.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import se.scooterrental.model.Item;
import se.scooterrental.model.Member;
import se.scooterrental.model.Rental;
import se.scooterrental.model.PricePolicy;
import se.scooterrental.model.StandardPricePolicy;
import se.scooterrental.model.StudentPricePolicy;
import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.RentalService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Vy för att hantera uthyrningar, boka/avsluta och visa summeringar.
 */
public class RentalView extends BaseView {

    private final RentalService rentalService;
    private final MemberRegistry memberRegistry;
    private final Inventory inventory;

    private TableView<Rental> activeRentalsTable;
    private ObservableList<Rental> activeRentalsList;
    private Label revenueLabel;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RentalView(RentalService rentalService, MemberRegistry memberRegistry, Inventory inventory) {
        super("Uthyrning & Summering"); // Anropar BaseView konstruktor

        // --- FIX: Serviceobjekten tilldelas HÄR, efter super() ---
        this.rentalService = rentalService;
        this.memberRegistry = memberRegistry;
        this.inventory = inventory;

        // Initialisera ObservableList EFTER att rentalService är satt
        this.activeRentalsList = FXCollections.observableArrayList(rentalService.getActiveRentals());

        // Manuell anrop av setupUI() efter att alla beroenden är satta (Löser NPE)
        setupUI();
    }

    @Override
    protected void setupUI() {
        // 1. Summeringar (VG-krav)
        revenueLabel = new Label();
        updateSummaries(); // Kan nu anropas säkert

        VBox summaryBox = new VBox(5, new Label("Aktuella Summeringar"), revenueLabel);
        summaryBox.setStyle("-fx-border-color: #ccc; -fx-padding: 10; -fx-background-color: #f9f9f9;");
        rootLayout.getChildren().add(summaryBox);

        // 2. Aktiva Uthyrningar
        Label activeTitle = new Label("Aktiva Uthyrningar");
        activeTitle.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        activeRentalsTable = createActiveRentalsTable();

        // 3. Kontrollknappar
        Button startButton = new Button("Starta Ny Uthyrning");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> showStartRentalDialog());

        Button endButton = new Button("Avsluta Markerad Uthyrning");
        endButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        endButton.setOnAction(e -> handleEndRental());

        // NY KNAPP: För att uppdatera dynamiska kostnader
        Button refreshButton = new Button("Uppdatera Kostnader");
        refreshButton.setOnAction(e -> refreshTable());


        HBox controlBox = new HBox(10, startButton, endButton, refreshButton);
        controlBox.setPadding(new Insets(10, 0, 0, 0));

        rootLayout.getChildren().addAll(activeTitle, activeRentalsTable, controlBox);
    }

    /**
     * Skapar och konfigurerar TableView för aktiva uthyrningar.
     */
    private TableView<Rental> createActiveRentalsTable() {
        TableView<Rental> table = new TableView<>();
        table.setItems(activeRentalsList);

        // Kolumner
        TableColumn<Rental, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("rentalId"));

        TableColumn<Rental, String> memberCol = new TableColumn<>("Medlems-ID");
        memberCol.setCellValueFactory(new PropertyValueFactory<>("memberId"));

        TableColumn<Rental, String> itemCol = new TableColumn<>("Item-ID");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));

        TableColumn<Rental, LocalDateTime> startCol = new TableColumn<>("Starttid");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startCol.setCellFactory(column -> new TableCell<Rental, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : DATE_FORMATTER.format(item));
            }
        });

        TableColumn<Rental, PricePolicy> policyCol = new TableColumn<>("Policy");
        policyCol.setCellValueFactory(new PropertyValueFactory<>("pricePolicy"));
        policyCol.setCellFactory(column -> new TableCell<Rental, PricePolicy>() { // <-- FIX: Ändrat ListCell till TableCell
            @Override
            protected void updateItem(PricePolicy item, boolean empty) { // Använder PricePolicy som cell-typ
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getPolicyName());
            }
        });

        // Lägger till en kolumn för aktuell uppskattad kostnad (dynamisk beräkning)
        // OBS: Detta värde är beräknat vid laddning, kräver refresh för att uppdateras.
        TableColumn<Rental, Double> costCol = new TableColumn<>("Uppskattad Kostnad");
        costCol.setCellValueFactory(cellData -> {
            Rental rental = cellData.getValue();
            Optional<Item> itemOpt = inventory.findItemById(rental.getItemId());
            double basePrice = itemOpt.isPresent() ? itemOpt.get().getCurrentRentalPrice() : 0.0;

            // Beräkna kostnaden
            return new javafx.beans.property.SimpleDoubleProperty(rental.getCurrentCost(basePrice)).asObject();
        });


        table.getColumns().addAll(idCol, memberCol, itemCol, startCol, policyCol, costCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    /**
     * Tvingar omladdning och omberäkning av tabellen och summeringarna.
     */
    private void refreshTable() {
        activeRentalsTable.refresh();
        updateSummaries();
    }

    /**
     * Laddar om listan med aktiva uthyrningar.
     */
    private void loadActiveRentals() {
        activeRentalsList.setAll(rentalService.getActiveRentals());
        refreshTable();
    }

    /**
     * Uppdaterar summeringsetiketterna (Total Intäkt).
     */
    private void updateSummaries() {
        double revenue = rentalService.getTotalRevenue();
        int activeCount = rentalService.getActiveRentals().size();

        revenueLabel.setText(String.format(
                "Total Intäkt (avslutade): %.2f kr\nAktiva Uthyrningar: %d",
                revenue, activeCount));
    }

    /**
     * Visar dialog för att starta ny uthyrning.
     */
    private void showStartRentalDialog() {
        Dialog<Rental> dialog = new Dialog<>();
        dialog.setTitle("Starta Ny Uthyrning");
        dialog.setHeaderText("Välj Medlem, Utrustning och Prispolicy");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // 1. Medlems-ID
        TextField memberIdField = new TextField();
        memberIdField.setPromptText("Medlems ID");

        // 2. Item-ID (Endast tillgängliga items)
        List<Item> availableItems = inventory.getAvailableItems();
        ComboBox<Item> itemComboBox = new ComboBox<>(FXCollections.observableArrayList(availableItems));
        itemComboBox.setPromptText("Välj tillgänglig utrustning");
        itemComboBox.setCellFactory(lv -> new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getItemId() + " - " + item.getName());
            }
        });
        itemComboBox.setButtonCell(itemComboBox.getCellFactory().call(null)); // Visar samma format när valt

        // 3. Prispolicy (Dropdown)
        ComboBox<PricePolicy> policyComboBox = new ComboBox<>(FXCollections.observableArrayList(
                new StandardPricePolicy(),
                new StudentPricePolicy()
                // Lägg till PremiumPolicy här om den implementeras
        ));
        policyComboBox.setPromptText("Välj Prispolicy");
        policyComboBox.setCellFactory(lv -> new ListCell<PricePolicy>() {
            @Override
            protected void updateItem(PricePolicy policy, boolean empty) {
                super.updateItem(policy, empty);
                setText(empty ? null : policy.getPolicyName());
            }
        });
        policyComboBox.setButtonCell(policyComboBox.getCellFactory().call(null));
        policyComboBox.getSelectionModel().selectFirst();


        grid.add(new Label("Medlems ID:"), 0, 0);
        grid.add(memberIdField, 1, 0);
        grid.add(new Label("Utrustning:"), 0, 1);
        grid.add(itemComboBox, 1, 1);
        grid.add(new Label("Policy:"), 0, 2);
        grid.add(policyComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType startButtonType = new ButtonType("Starta Uthyrning", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == startButtonType) {
                String memberId = memberIdField.getText();
                Item selectedItem = itemComboBox.getValue();
                PricePolicy selectedPolicy = policyComboBox.getValue();

                if (memberId.isEmpty() || selectedItem == null || selectedPolicy == null) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Alla fält måste väljas.");
                    return null;
                }

                // Kontrollera att medlemmen existerar
                if (memberRegistry.findMemberById(memberId).isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Medlem ID hittades inte.");
                    return null;
                }

                // Använd RentalService för affärslogik
                Optional<Rental> rentalOpt = rentalService.startRental(
                        memberId, selectedItem.getItemId(), selectedPolicy);

                if (rentalOpt.isPresent()) {
                    showAlert(Alert.AlertType.INFORMATION, "Startad",
                            "Uthyrning startad! ID: " + rentalOpt.get().getRentalId());
                    return rentalOpt.get();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Kunde inte starta uthyrning. Item är kanske inte tillgänglig.");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(rental -> {
            loadActiveRentals(); // Uppdatera listor
        });
    }

    /**
     * Hanterar avslut av markerad uthyrning.
     */
    private void handleEndRental() {
        Rental selectedRental = activeRentalsTable.getSelectionModel().getSelectedItem();

        if (selectedRental == null) {
            showAlert(Alert.AlertType.WARNING, "Varning", "Välj en aktiv uthyrning att avsluta.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Avsluta Uthyrning");
        confirm.setHeaderText("Är du säker på att du vill avsluta uthyrning ID: " + selectedRental.getRentalId() + "?");
        confirm.setContentText("Slutpris kommer att beräknas.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            Optional<Double> finalPrice = rentalService.endRental(selectedRental.getRentalId());

            if (finalPrice.isPresent()) {
                showAlert(Alert.AlertType.INFORMATION, "Avslutad",
                        String.format("Uthyrning %d avslutad. Slutpris: %.2f kr", selectedRental.getRentalId(), finalPrice.get()));

                loadActiveRentals(); // Uppdatera listor
            } else {
                showAlert(Alert.AlertType.ERROR, "Fel", "Kunde inte avsluta uthyrningen.");
            }
        }
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