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
import se.scooterrental.model.Scooter;
import se.scooterrental.model.Sled;
import se.scooterrental.service.Inventory;

import java.util.Optional;

/**
 * Vy för att hantera utrustningslagret: lägga till, lista och filtrera items.
 */
public class ItemView extends BaseView {

    private final Inventory inventory;
    private TableView<Item> itemTable;
    private ObservableList<Item> itemList;

    public ItemView(Inventory inventory) {
        super("Utrustning & Lager");
        this.inventory = inventory;
        this.itemList = FXCollections.observableArrayList(inventory.getAllItems());
        setupUI();
    }

    @Override
    protected void setupUI() {
        Label title = new Label("Utrustningslager");
        title.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        itemTable = createItemTable();

        Button addButton = new Button("Lägg till Ny Utrustning");
        addButton.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setOnAction(e -> showAddItemDialog());

        Button refreshButton = new Button("Uppdatera Lista");
        refreshButton.setOnAction(e -> loadItems());

        HBox controlBox = new HBox(10, addButton, refreshButton);
        controlBox.setPadding(new Insets(10, 0, 0, 0));

        rootLayout.getChildren().addAll(title, itemTable, controlBox);
    }

    /**
     * Skapar och konfigurerar TableView för items.
     */
    private TableView<Item> createItemTable() {
        TableView<Item> table = new TableView<>();
        table.setItems(itemList);

        TableColumn<Item, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));

        TableColumn<Item, String> nameCol = new TableColumn<>("Namn");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, String> typeCol = new TableColumn<>("Typ");
        typeCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            String type = item.getClass().getSimpleName();
            return new javafx.beans.property.SimpleStringProperty(type);
        });

        TableColumn<Item, Double> priceCol = new TableColumn<>("Pris/timme");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentRentalPrice"));

        TableColumn<Item, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            String status = item.isAvailable() ? "Tillgänglig" : "Uthyrd";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        table.getColumns().addAll(idCol, nameCol, typeCol, priceCol, statusCol);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); // FIX: Deprecated warning
        return table;
    }

    /**
     * Visar dialog för att lägga till ny utrustning (Skoter eller Släde).
     */
    private void showAddItemDialog() {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle("Lägg till Utrustning");
        dialog.setHeaderText("Välj typ och fyll i detaljer.");

        ButtonType saveButtonType = new ButtonType("Spara", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 1. Välj typ
        ComboBox<String> typeComboBox = new ComboBox<>(FXCollections.observableArrayList("Scooter", "Sled"));
        typeComboBox.setPromptText("Välj Utrustningstyp");

        // 2. Gemensamma fält
        TextField nameField = new TextField();
        nameField.setPromptText("Namn/Modell");
        TextField priceField = new TextField();
        priceField.setPromptText("Pris per timme (t.ex. 150.0)");

        // 3. Typ-specifika fält (Initialt gömda)
        TextField licensePlateField = new TextField(); // Specifikt för Scooter (String)
        licensePlateField.setPromptText("Reg.skylt");
        TextField batteryLevelField = new TextField(); // Specifikt för Scooter (int)
        batteryLevelField.setPromptText("Batterinivå (%)");

        TextField sledTypeField = new TextField(); // Specifikt för Sled (String)
        sledTypeField.setPromptText("Slädtyp (Pulka/Kälke)");
        TextField maxWeightField = new TextField(); // Specifikt för Sled (int)
        maxWeightField.setPromptText("Maxkapacitet (kg)");

        Label licensePlateLabel = new Label("Reg.skylt:");
        Label batteryLevelLabel = new Label("Batterinivå (%):");
        Label sledTypeLabel = new Label("Slädtyp:");
        Label maxWeightLabel = new Label("Maxkapacitet (kg):");

        // Initial layout
        grid.add(new Label("Typ:"), 0, 0);
        grid.add(typeComboBox, 1, 0);
        grid.add(new Label("Namn:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Pris/timme:"), 0, 2);
        grid.add(priceField, 1, 2);

        // Dynamisk uppdatering baserat på val
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Rensa gamla fält
            grid.getChildren().removeAll(licensePlateLabel, licensePlateField, batteryLevelLabel, batteryLevelField,
                    sledTypeLabel, sledTypeField, maxWeightLabel, maxWeightField);

            int currentRow = 3;
            if ("Scooter".equals(newVal)) {
                grid.add(licensePlateLabel, 0, currentRow++);
                grid.add(licensePlateField, 1, currentRow - 1);
                grid.add(batteryLevelLabel, 0, currentRow++);
                grid.add(batteryLevelField, 1, currentRow - 1);
            } else if ("Sled".equals(newVal)) {
                grid.add(sledTypeLabel, 0, currentRow++);
                grid.add(sledTypeField, 1, currentRow - 1);
                grid.add(maxWeightLabel, 0, currentRow++);
                grid.add(maxWeightField, 1, currentRow - 1);
            }
            dialog.getDialogPane().getScene().getWindow().sizeToScene(); // Anpassa storleken
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String type = typeComboBox.getValue();
                String name = nameField.getText().trim();
                double price;

                try {
                    price = Double.parseDouble(priceField.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Ogiltigt prisformat.");
                    return null;
                }

                if (type == null || name.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Typ och Namn måste väljas.");
                    return null;
                }

                String newId = inventory.generateNewId(); // Använder Inventory för att generera ID
                Item newItem = null;

                try {
                    if ("Scooter".equals(type)) {
                        String licensePlate = licensePlateField.getText().trim();
                        int batteryLevel = Integer.parseInt(batteryLevelField.getText().trim());

                        // FIX: Korrekt konstruktoranrop: (id, name, price, String, int)
                        newItem = new Scooter(newId, name, price, licensePlate, batteryLevel);

                    } else if ("Sled".equals(type)) {
                        String sledType = sledTypeField.getText().trim();
                        int maxCapacity = Integer.parseInt(maxWeightField.getText().trim());

                        // FIX: Korrekt konstruktoranrop: (id, name, price, String, int)
                        newItem = new Sled(newId, name, price, sledType, maxCapacity);
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Fel", "Batterinivå/Maxkapacitet måste vara heltal.");
                    return null;
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Fel vid data", e.getMessage());
                    return null;
                }

                if (newItem != null) {
                    inventory.addItem(newItem);
                    showAlert(Alert.AlertType.INFORMATION, "Sparat", "Utrustning " + name + " sparad med ID: " + newId);
                    return newItem;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(item -> {
            loadItems(); // Uppdatera tabellen efter lyckad spara
        });
    }

    /**
     * Laddar om itemlistan från Inventory och uppdaterar TableView.
     */
    public void loadItems() {
        itemList.setAll(inventory.getAllItems());
        itemTable.refresh();
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