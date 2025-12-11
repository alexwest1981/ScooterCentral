package se.scooterrental.ui.views;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import se.scooterrental.model.Item;
import se.scooterrental.model.Member;
import se.scooterrental.model.Rental;
import se.scooterrental.model.PricePolicy;
import se.scooterrental.model.StandardPricePolicy;
import se.scooterrental.model.StudentPricePolicy;
import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.RentalService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RentalView extends BaseView {

    private final RentalService rentalService;
    private final MemberRegistry memberRegistry;
    private final Inventory inventory;

    private TableView<Rental> activeRentalsTable;
    private ObservableList<Rental> activeRentalsList;

    private Timeline costTicker;

    public RentalView(RentalService rentalService, MemberRegistry memberRegistry, Inventory inventory) {
        super("Kassa & Bokning");

        this.rentalService = rentalService;
        this.memberRegistry = memberRegistry;
        this.inventory = inventory;

        this.activeRentalsList = FXCollections.observableArrayList(rentalService.getActiveRentals());

        setupUI();
        startTicker();
    }

    private void startTicker() {
        KeyFrame updateFrame = new KeyFrame(Duration.seconds(1), event -> {
            if (activeRentalsTable != null) {
                activeRentalsTable.refresh();
            }
        });

        costTicker = new Timeline(updateFrame);
        costTicker.setCycleCount(Timeline.INDEFINITE);
        costTicker.play();
    }

    public void stopTicker() {
        if (costTicker != null) costTicker.stop();
    }

    @Override
    protected void setupUI() {
        Label title = new Label("Kassa & Uthyrning");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        rootLayout.getChildren().add(title);

        Label subTitle = new Label("Hantera pågående uthyrningar eller skapa nya.");
        subTitle.setStyle("-fx-text-fill: #6B7280; -fx-padding: 0 0 20 0;");
        rootLayout.getChildren().add(subTitle);

        activeRentalsTable = createActiveRentalsTable();

        Button startButton = new Button("Starta Ny Uthyrning");
        startButton.getStyleClass().add("accent-button");
        startButton.setOnAction(e -> showStartRentalDialog());

        Button endButton = new Button("Avsluta & Betala");
        endButton.getStyleClass().add("red-button");
        endButton.setOnAction(e -> handleEndRental());

        Button refreshButton = new Button("Uppdatera Lista");
        refreshButton.setOnAction(e -> loadActiveRentals());

        HBox controlBox = new HBox(10, startButton, endButton, refreshButton);
        controlBox.setPadding(new Insets(10, 0, 0, 0));

        rootLayout.getChildren().addAll(activeRentalsTable, controlBox);
    }

    // --- UTCHECKNINGSFLÖDE ---

    private void handleEndRental() {
        Rental selected = activeRentalsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Varning", "Välj en uthyrning att checka ut.");
            return;
        }

        Optional<Item> itemOpt = inventory.findItemById(selected.getItemId());
        if (itemOpt.isPresent()) {
            Optional<Double> res = rentalService.endRental(selected.getRentalId());
            if (res.isPresent()) {
                double finalPrice = res.get();
                loadActiveRentals();

                // Starta betalningsdialog
                Optional<Member> member = memberRegistry.findMemberById(selected.getMemberId());
                showPaymentDialog(selected, itemOpt.get(), member.orElse(null), finalPrice);
            }
        }
    }

    private void showPaymentDialog(Rental rental, Item item, Member member, double price) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Betalning");
        dialog.setHeaderText("Uthyrning avslutad. Totalt belopp: " + String.format("%.2f kr", price));

        ButtonType receiptBtn = new ButtonType("Betala Nu (Kvitto)", ButtonBar.ButtonData.OK_DONE);
        ButtonType invoiceBtn = new ButtonType("Skapa Faktura", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn = new ButtonType("Stäng", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(receiptBtn, invoiceBtn, closeBtn);

        dialog.showAndWait().ifPresent(type -> {
            if (type == receiptBtn) {
                generateReceipt(rental, item, price);
            } else if (type == invoiceBtn) {
                generateInvoice(rental, item, member, price);
            }
        });
    }

    // --- KVITTO ---
    private void generateReceipt(Rental rental, Item item, double price) {
        Stage stage = new Stage();
        stage.setTitle("Kvitto - Utskrift");

        VBox slip = new VBox(5);
        slip.setPadding(new Insets(20));
        slip.setPrefWidth(300);
        slip.setAlignment(Pos.TOP_CENTER);
        slip.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");

        Label logo = new Label("SCOOTER CENTRAL");
        logo.setFont(Font.font("Courier New", FontWeight.BOLD, 18));

        slip.getChildren().addAll(
                logo,
                new Separator(),
                new Text("Datum: " + LocalDate.now()),
                new Text("Tid: " + LocalDateTime.now().toLocalTime().toString().substring(0, 5)),
                new Separator(),
                new Text("Produkt: " + item.getName()),
                new Text("Start: " + rental.getStartTime()),
                new Text("Pris/h: " + item.getCurrentRentalPrice() + " kr"),
                new Separator(),
                new Label("TOTALT: " + String.format("%.2f kr", price)) {{ setFont(Font.font("System", FontWeight.BOLD, 16)); }},
                new Label("Moms (25%): " + String.format("%.2f kr", price * 0.2)) {{ setStyle("-fx-font-size: 10px;"); }},
                new Separator(),
                new Text("Tack för att du hyr hos oss!")
        );

        Button printBtn = new Button("Skriv ut");
        printBtn.setOnAction(e -> printNode(slip));

        VBox root = new VBox(10, slip, printBtn);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root));
        stage.show();
    }

    // --- FAKTURA ---
    private void generateInvoice(Rental rental, Item item, Member member, double price) {
        Stage stage = new Stage();
        stage.setTitle("Faktura - Förhandsgranskning");

        VBox a4 = new VBox(20);
        a4.setPadding(new Insets(40));
        a4.setPrefSize(595, 842); // A4 mått i pixlar (ca 72 PPI)
        a4.setStyle("-fx-background-color: white;");

        // Header
        HBox header = new HBox();
        VBox companyInfo = new VBox(2,
                new Label("Scooter Central AB") {{ setFont(Font.font("Arial", FontWeight.BOLD, 20)); }},
                new Text("Strandgatan 1"),
                new Text("621 55 Visby"),
                new Text("Org.nr: 556000-0000")
        );
        VBox invoiceMeta = new VBox(2,
                new Label("FAKTURA") {{ setFont(Font.font("Arial", FontWeight.BOLD, 24)); }},
                new Text("Fakturanr: " + rental.getRentalId()),
                new Text("Datum: " + LocalDate.now()),
                new Text("Förfallodatum: " + LocalDate.now().plusDays(30))
        );
        invoiceMeta.setAlignment(Pos.TOP_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(companyInfo, spacer, invoiceMeta);

        // Mottagare
        VBox recipient = new VBox(2,
                new Label("Fakturamottagare:"),
                new Text(member != null ? member.getName() : "Okänd kund"),
                new Text(member != null ? member.getEmail() : ""),
                new Text(member != null ? member.getPhone() : "")
        );
        recipient.setPadding(new Insets(20, 0, 20, 0));

        // Rader
        GridPane lines = new GridPane();
        lines.setHgap(20); lines.setVgap(10);
        lines.setStyle("-fx-border-color: black; -fx-border-width: 1 0 1 0; -fx-padding: 10 0;");
        lines.add(new Label("Beskrivning") {{ setStyle("-fx-font-weight: bold;"); }}, 0, 0);
        lines.add(new Label("À-pris") {{ setStyle("-fx-font-weight: bold;"); }}, 1, 0);
        lines.add(new Label("Belopp") {{ setStyle("-fx-font-weight: bold;"); }}, 2, 0);

        lines.add(new Text("Hyra av " + item.getName()), 0, 1);
        lines.add(new Text(item.getCurrentRentalPrice() + " kr/h"), 1, 1);
        lines.add(new Text(String.format("%.2f kr", price)), 2, 1);

        // Total
        HBox totalBox = new HBox(10);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(20, 0, 0, 0));
        VBox totals = new VBox(5,
                new Text("Netto: " + String.format("%.2f kr", price * 0.8)),
                new Text("Moms (25%): " + String.format("%.2f kr", price * 0.2)),
                new Label("ATT BETALA: " + String.format("%.2f kr", price)) {{ setFont(Font.font("Arial", FontWeight.BOLD, 14)); }}
        );
        totals.setAlignment(Pos.CENTER_RIGHT);
        totalBox.getChildren().add(totals);

        a4.getChildren().addAll(header, recipient, lines, totalBox);

        Button printBtn = new Button("Skriv ut / Spara PDF");
        printBtn.setOnAction(e -> printNode(a4));

        ScrollPane scroll = new ScrollPane(a4);
        VBox root = new VBox(10, scroll, printBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 700, 900));
        stage.show();
    }

    private void printNode(javafx.scene.Node node) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(node.getScene().getWindow())) {
            boolean success = job.printPage(node);
            if (success) {
                job.endJob();
                showAlert(Alert.AlertType.INFORMATION, "Utskrift", "Skickat till skrivare.");
            }
        }
    }

    private TableView<Rental> createActiveRentalsTable() {
        TableView<Rental> table = new TableView<>();
        table.setItems(activeRentalsList);

        TableColumn<Rental, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("rentalId"));
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Rental, String> memberCol = new TableColumn<>("Medlems-ID");
        memberCol.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Rental, String> itemCol = new TableColumn<>("Item-ID");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        itemCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Rental, String> startCol = new TableColumn<>("Starttid");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Rental, Void> costCol = new TableColumn<>("Kostnad just nu");
        costCol.setStyle("-fx-alignment: CENTER;");
        costCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    Rental rental = getTableView().getItems().get(getIndex());
                    if (rental != null) {
                        Optional<Item> itemOpt = inventory.findItemById(rental.getItemId());
                        if (itemOpt.isPresent()) {
                            double basePrice = itemOpt.get().getCurrentRentalPrice();
                            double currentCost = rental.getCurrentCost(basePrice);
                            setText(String.format("%.2f kr", currentCost));
                        } else {
                            setText("N/A");
                        }
                    }
                }
            }
        });

        table.getColumns().addAll(idCol, memberCol, itemCol, startCol, costCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private void loadActiveRentals() {
        Platform.runLater(() -> {
            activeRentalsList.setAll(rentalService.getActiveRentals());
            activeRentalsTable.refresh();
        });
    }

    // showStartRentalDialog är oförändrad, kopiera in den från tidigare om du behöver
    // För att spara tecken i svaret utesluter jag den om du inte ber om den, då den är lång och oförändrad.
    // Men den måste finnas här för att kompilera!
    private void showStartRentalDialog() {
        Dialog<Rental> dialog = new Dialog<>();
        dialog.setTitle("Starta Ny Uthyrning");
        dialog.setHeaderText("Välj Medlem, Utrustning och Prispolicy");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        List<Member> allMembers = memberRegistry.getMembers();
        ComboBox<Member> memberComboBox = new ComboBox<>(FXCollections.observableArrayList(allMembers));
        memberComboBox.setPromptText("Välj Medlem");
        memberComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Member m, boolean e) { super.updateItem(m, e); setText(e || m == null ? null : m.getFirstName() + " " + m.getLastName() + " (" + m.getMemberId() + ")"); }
        });
        memberComboBox.setButtonCell(memberComboBox.getCellFactory().call(null));

        List<Item> availableItems = inventory.getAvailableItems();
        ComboBox<Item> itemComboBox = new ComboBox<>(FXCollections.observableArrayList(availableItems));
        itemComboBox.setPromptText("Välj utrustning");
        itemComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Item i, boolean e) { super.updateItem(i, e); setText(e || i == null ? null : i.getName() + " (" + i.getItemId() + ")"); }
        });
        itemComboBox.setButtonCell(itemComboBox.getCellFactory().call(null));

        ComboBox<PricePolicy> policyComboBox = new ComboBox<>(FXCollections.observableArrayList(new StandardPricePolicy(), new StudentPricePolicy()));
        policyComboBox.setPromptText("Välj Prispolicy");
        policyComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PricePolicy p, boolean e) { super.updateItem(p, e); setText(e || p == null ? null : p.getPolicyName()); }
        });
        policyComboBox.setButtonCell(policyComboBox.getCellFactory().call(null));
        policyComboBox.getSelectionModel().selectFirst();

        grid.add(new Label("Medlem:"), 0, 0); grid.add(memberComboBox, 1, 0);
        grid.add(new Label("Utrustning:"), 0, 1); grid.add(itemComboBox, 1, 1);
        grid.add(new Label("Policy:"), 0, 2); grid.add(policyComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType startBtn = new ButtonType("Starta", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startBtn, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == startBtn) {
                Member m = memberComboBox.getValue();
                Item i = itemComboBox.getValue();
                PricePolicy p = policyComboBox.getValue();
                if (m != null && i != null && p != null) {
                    if (rentalService.rentItem(m.getMemberId(), i.getItemId(), p)) {
                        return new Rental("TEMP", m.getMemberId(), i.getItemId(), p);
                    } else { showAlert(Alert.AlertType.ERROR, "Fel", "Kunde inte hyra ut."); }
                } else { showAlert(Alert.AlertType.WARNING, "Saknas info", "Välj alla fält."); }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(r -> loadActiveRentals());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setContentText(message); alert.show();
    }
}