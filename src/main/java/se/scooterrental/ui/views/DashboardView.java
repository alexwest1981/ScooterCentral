package se.scooterrental.ui.views;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import se.scooterrental.model.Item;
import se.scooterrental.model.Member;
import se.scooterrental.model.Rental;
import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.RentalService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardView extends BaseView {

    private final RentalService rentalService;
    private final Inventory inventory;
    private final MemberRegistry memberRegistry;

    private TableView<Rental> activeRentalsTable;
    private ObservableList<Rental> activeRentalsList;

    // UI Komponenter
    private Label totalRevenueLabel;
    private Label activeCountLabel;
    private Label totalMembersLabel;
    private Label vehicleStatsLabel;

    private LineChart<String, Number> revenueChart;
    private PieChart popularityChart;
    private ComboBox<String> periodSelector;

    private Timeline costTicker;
    private int tickerCounter = 0;

    public DashboardView(RentalService rentalService, Inventory inventory, MemberRegistry memberRegistry) {
        super("Översikt");
        this.rentalService = rentalService;
        this.inventory = inventory;
        this.memberRegistry = memberRegistry;
        this.activeRentalsList = FXCollections.observableArrayList(rentalService.getActiveRentals());

        setupUI();
        startTicker();
    }

    private void startTicker() {
        KeyFrame updateFrame = new KeyFrame(Duration.seconds(1), event -> {
            if (activeRentalsTable != null) {
                activeRentalsList.setAll(rentalService.getActiveRentals());
                activeRentalsTable.refresh();
                updateQuickStats();

                tickerCounter++;
                if (tickerCounter >= 5) {
                    updateRevenueChart();
                    updatePopularityChart();
                    tickerCounter = 0;
                }
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
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Container för att centrera innehållet
        StackPane centerContainer = new StackPane();
        centerContainer.setAlignment(Pos.TOP_CENTER);
        centerContainer.setPadding(new Insets(0)); // Lite luft runt hela

        // Huvudinnehållet med begränsad bredd
        VBox contentBox = new VBox(25); // Ökat avstånd mellan sektioner
        contentBox.setMaxWidth(1200); // MAXBREDD: Detta förhindrar att det blir för utdraget
        contentBox.setStyle("-fx-background-color: transparent;");

        // --- SEKTION 1: Scorecards ---
        HBox scorecards = new HBox(15);
        scorecards.setAlignment(Pos.CENTER_LEFT);

        // Skapa korten och låt dem växa jämnt
        VBox card1 = createScorecard("Kunder", totalMembersLabel = new Label("0"));
        VBox card2 = createScorecard("Aktiva", activeCountLabel = new Label("0"));
        VBox card3 = createScorecard("Fordon", vehicleStatsLabel = new Label("0 / 0"));
        VBox card4 = createScorecard("Kassa", totalRevenueLabel = new Label("0 kr"));

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        scorecards.getChildren().addAll(card1, card2, card3, card4);

        // --- SEKTION 2: Grafer (Widgets) ---
        HBox graphsBox = new HBox(20);
        graphsBox.setPrefHeight(280); // Något högre för balans

        VBox revenueGraphBox = createRevenueChartBox();
        HBox.setHgrow(revenueGraphBox, Priority.ALWAYS);

        VBox popularityGraphBox = createPopularityChartBox();
        HBox.setHgrow(popularityGraphBox, Priority.ALWAYS);

        graphsBox.getChildren().addAll(revenueGraphBox, popularityGraphBox);

        // --- SEKTION 3: Pågående Uthyrningar ---
        VBox tableBox = new VBox(10);
        Label tableTitle = new Label("Pågående Uthyrningar");
        // OBS: Denna titel har hårdkodad vit färg. Om du vill att den ska synas bra i Ljust läge
        // bör du ändra färgen här eller i CSS. För Mörkt läge är vit perfekt.
        // För nu låter jag den vara vit då den ligger på en mörk bakgrund i originaldesignen,
        // men tänk på att i ljust läge kan den bli osynlig om bakgrunden är vit.
        // En enkel fix är att använda en CSS-klass här med.
        tableTitle.getStyleClass().add("section-header");
        // tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        activeRentalsTable = createActiveRentalsTable();
        activeRentalsTable.setPrefHeight(350);

        activeRentalsTable.setOnMouseClicked(e -> {
            Rental selected = activeRentalsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showRentalDetailsDialog(selected);
                activeRentalsTable.getSelectionModel().clearSelection();
            }
        });

        tableBox.getChildren().addAll(tableTitle, activeRentalsTable);

        // Lägg till allt i contentBox
        contentBox.getChildren().addAll(scorecards, graphsBox, tableBox);

        // Lägg contentBox i centerContainer
        centerContainer.getChildren().add(contentBox);

        scrollPane.setContent(centerContainer);

        rootLayout.setPadding(new Insets(0));
        rootLayout.getChildren().add(scrollPane);

        updateRevenueChart();
        updatePopularityChart();
        updateQuickStats();
    }

    private VBox createScorecard(String title, Label valueLabel) {
        VBox card = new VBox(5);
        card.getStyleClass().add("summary-box");
        card.setPadding(new Insets(20)); // Mer padding
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        // FIX: Använd CSS-klass istället för hårdkodad stil
        titleLbl.getStyleClass().add("scorecard-title");
        // titleLbl.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-transform: uppercase;");

        // FIX: Använd CSS-klass istället för hårdkodad stil
        valueLabel.getStyleClass().add("scorecard-value");
        // valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        card.getChildren().addAll(titleLbl, valueLabel);
        return card;
    }

    private VBox createRevenueChartBox() {
        VBox box = new VBox(10);
        box.getStyleClass().add("center-panel");
        box.setPrefHeight(280);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Intäktstrend");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button expandBtn = new Button();
        FontIcon expandIcon = new FontIcon("mdi2a-arrow-expand-all");
        expandIcon.setIconSize(16);
        expandBtn.setGraphic(expandIcon);
        expandBtn.setTooltip(new Tooltip("Visa detaljerad analys"));
        expandBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        expandBtn.setOnAction(e -> showDetailedRevenueChart()); // Ändrat till diagram-metoden

        periodSelector = new ComboBox<>();
        periodSelector.getItems().addAll("1 Vecka", "1 Månad", "1 År");
        periodSelector.setValue("1 Vecka");
        periodSelector.setStyle("-fx-font-size: 11px;");
        periodSelector.setOnAction(e -> updateRevenueChart());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, periodSelector, expandBtn);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("");

        revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setLegendVisible(false);
        revenueChart.setAnimated(false);
        revenueChart.setCreateSymbols(true);
        revenueChart.setPadding(new Insets(0));

        box.getChildren().addAll(header, revenueChart);
        return box;
    }

    private VBox createPopularityChartBox() {
        VBox box = new VBox(10);
        box.getStyleClass().add("center-panel");
        box.setPrefHeight(280);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Lagerstatus");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button expandBtn = new Button();
        FontIcon expandIcon = new FontIcon("mdi2a-arrow-expand-all");
        expandIcon.setIconSize(16);
        expandBtn.setGraphic(expandIcon);
        expandBtn.setTooltip(new Tooltip("Visa detaljerad lageranalys"));
        expandBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        expandBtn.setOnAction(e -> showDetailedInventoryChart()); // Ändrat till diagram-metoden

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, expandBtn);

        popularityChart = new PieChart();
        popularityChart.setLabelsVisible(false);
        popularityChart.setLegendVisible(false);
        popularityChart.setPadding(new Insets(0));

        box.getChildren().addAll(header, popularityChart);
        return box;
    }

    // --- DATA UPPDATERING ---

    public void refreshData() {
        Platform.runLater(() -> {
            activeRentalsList.setAll(rentalService.getActiveRentals());
            updateRevenueChart();
            updatePopularityChart();
            updateQuickStats();
        });
    }

    private void updateQuickStats() {
        long totalMembers = memberRegistry.getMembers().size();
        totalMembersLabel.setText(String.valueOf(totalMembers));

        int activeRentals = activeRentalsList.size();
        activeCountLabel.setText(String.valueOf(activeRentals));

        long totalVehicles = inventory.getTotalCount();
        long rentedVehicles = inventory.getRentedCount();
        vehicleStatsLabel.setText(String.format("%d / %d", totalVehicles, rentedVehicles));

        double revenue = rentalService.getTotalRevenue();
        totalRevenueLabel.setText(String.format("%.0f kr", revenue));
    }

    private void updateRevenueChart() {
        String period = periodSelector.getValue();
        Map<LocalDate, Double> data = rentalService.getRevenueData(period);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<LocalDate, Double> sortedData = new TreeMap<>(data);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M");

        double maxRevenue = data.values().stream().mapToDouble(v -> v).max().orElse(0.0);

        for (Map.Entry<LocalDate, Double> entry : sortedData.entrySet()) {
            String label = entry.getKey().format(formatter);
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        revenueChart.getData().clear();
        revenueChart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                javafx.scene.Node node = d.getNode();
                if (node != null) {
                    double val = d.getYValue().doubleValue();
                    String tooltipText = String.format("%s: %.0f kr", d.getXValue(), val);

                    if (val > 0 && val == maxRevenue) {
                        node.setStyle("-fx-background-color: #F59E0B, white; -fx-padding: 5px;");
                    } else {
                        node.setStyle("-fx-background-color: #111827, white; -fx-padding: 3px;");
                    }
                    Tooltip.install(node, new Tooltip(tooltipText));
                }
            }
        });
    }

    private void updatePopularityChart() {
        Map<String, Long> stats = inventory.getModelPopularity();
        List<PieChart.Data> pieData = stats.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        if (popularityChart.getData().size() != pieData.size()) {
            popularityChart.getData().setAll(pieData);
            setupPieChartInteractions();
        } else {
            boolean needsReset = false;
            for (int i = 0; i < pieData.size(); i++) {
                PieChart.Data newData = pieData.get(i);
                PieChart.Data existing = popularityChart.getData().get(i);
                if (!existing.getName().equals(newData.getName())) {
                    needsReset = true; break;
                }
                existing.setPieValue(newData.getPieValue());
            }
            if (needsReset) {
                popularityChart.getData().setAll(pieData);
                setupPieChartInteractions();
            }
        }
    }

    private void setupPieChartInteractions() {
        for (PieChart.Data data : popularityChart.getData()) {
            javafx.scene.Node node = data.getNode();
            if (node != null) {
                Tooltip.install(node, new Tooltip(data.getName() + ": " + (int)data.getPieValue()));
                node.setStyle("-fx-cursor: hand;");
                node.setOnMouseClicked(e -> showDetailedInventoryChart());
            }
        }
    }

    // --- POPUPS (Detaljerade Charts) ---

    private void showDetailedRevenueChart() {
        Stage dialog = new Stage();
        dialog.setTitle("Intäktsanalys");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setPrefSize(900, 700); // Större fönster för grafer
        root.setStyle("-fx-background-color: white;");

        String period = periodSelector.getValue() != null ? periodSelector.getValue() : "1 Vecka";
        Label header = new Label("Intäktsanalys: " + period);
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // 1. Dataförberedelse
        List<Rental> allRentals = rentalService.getRentalsHistory();
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        boolean groupByMonth = "1 År".equals(period);

        switch (period) {
            case "1 Vecka": startDate = now.minusWeeks(1); break;
            case "1 Månad": startDate = now.minusMonths(1); break;
            case "1 År":    startDate = now.minusYears(1); break;
            default:        startDate = now.minusWeeks(1); break;
        }

        List<Rental> filteredRentals = allRentals.stream()
                .filter(r -> !r.isActive()) // Endast avslutade (som har genererat intäkt)
                .filter(r -> {
                    LocalDateTime dt = rentalService.parseDateTime(r.getStartTime());
                    return dt != null && !dt.toLocalDate().isBefore(startDate);
                })
                .collect(Collectors.toList());

        double totalRevenue = filteredRentals.stream().mapToDouble(Rental::getTotalCost).sum();

        // --- GRAF 1: Intäkt över tid (BarChart) ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(groupByMonth ? "Månad" : "Datum");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Intäkt (SEK)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Intäktsfördelning över tid");
        barChart.setLegendVisible(false);

        // Gruppera data
        Map<String, Double> revenueOverTime = filteredRentals.stream()
                .collect(Collectors.groupingBy(r -> {
                    LocalDateTime dt = rentalService.parseDateTime(r.getStartTime());
                    if (groupByMonth) return dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }, TreeMap::new, Collectors.summingDouble(Rental::getTotalCost)));

        XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
        revenueOverTime.forEach((date, rev) -> {
            XYChart.Data<String, Number> data = new XYChart.Data<>(date, rev);
            timeSeries.getData().add(data);
        });
        barChart.getData().add(timeSeries);

        // Lägg till Tooltips på staplarna
        for (XYChart.Data<String, Number> d : timeSeries.getData()) {
            if (d.getNode() != null) {
                Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + "\n" + String.format("%.0f kr", d.getYValue())));
                d.getNode().setStyle("-fx-bar-fill: #3B82F6;"); // Snygg blå färg
            }
        }

        // --- GRAF 2: Intäkt per Modell (PieChart) ---
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Intäkter per Modell (Top 5)");

        Map<String, Double> revenueByModel = filteredRentals.stream()
                .collect(Collectors.groupingBy(r -> {
                    Optional<Item> item = inventory.findItemById(r.getItemId());
                    return item.map(Item::getName).orElse("Okänd");
                }, Collectors.summingDouble(Rental::getTotalCost)));

        List<PieChart.Data> pieData = revenueByModel.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5) // Visa bara top 5 för att undvika kladd
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Lägg till "Övriga" om det finns fler
        double otherRevenue = revenueByModel.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .skip(5)
                .mapToDouble(Map.Entry::getValue).sum();
        if (otherRevenue > 0) {
            pieData.add(new PieChart.Data("Övriga", otherRevenue));
        }

        pieChart.getData().addAll(pieData);
        pieChart.setLegendSide(Side.RIGHT);

        // Tooltips för PieChart
        for (PieChart.Data d : pieChart.getData()) {
            if (d.getNode() != null) {
                Tooltip t = new Tooltip(d.getName() + "\n" + String.format("%.0f kr", d.getPieValue()) +
                        String.format(" (%.1f%%)", (d.getPieValue() / totalRevenue) * 100));
                Tooltip.install(d.getNode(), t);
            }
        }

        // Layout
        VBox.setVgrow(barChart, Priority.ALWAYS);
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        Label sumLabel = new Label("Total intäkt vald period: " + String.format("%.2f kr", totalRevenue));
        sumLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        Button closeBtn = new Button("Stäng");
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, barChart, pieChart, sumLabel, closeBtn);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showDetailedInventoryChart() {
        Stage dialog = new Stage();
        dialog.setTitle("Lageranalys");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setPrefSize(900, 700);
        root.setStyle("-fx-background-color: white;");

        Label header = new Label("Lagerstatus & Popularitet");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // --- GRAF 1: Stacked Bar Chart (Status: Ledig vs Uthyrd) ---
        CategoryAxis xAxisInv = new CategoryAxis();
        xAxisInv.setLabel("Modell");
        NumberAxis yAxisInv = new NumberAxis();
        yAxisInv.setLabel("Antal");

        StackedBarChart<String, Number> inventoryChart = new StackedBarChart<>(xAxisInv, yAxisInv);
        inventoryChart.setTitle("Nuvarande Lagerstatus");
        inventoryChart.setCategoryGap(20);

        XYChart.Series<String, Number> availableSeries = new XYChart.Series<>();
        availableSeries.setName("Lediga");
        XYChart.Series<String, Number> rentedSeries = new XYChart.Series<>();
        rentedSeries.setName("Uthyrda");

        // Hämta data
        Map<String, Long> totalCounts = inventory.getAllItems().stream()
                .collect(Collectors.groupingBy(Item::getName, Collectors.counting()));
        Map<String, Long> rentedCounts = inventory.getAllItems().stream()
                .filter(i -> !i.isAvailable())
                .collect(Collectors.groupingBy(Item::getName, Collectors.counting()));

        // Sortera modellerna alfabetiskt eller efter antal för snyggare graf
        new TreeMap<>(totalCounts).forEach((model, total) -> {
            long rented = rentedCounts.getOrDefault(model, 0L);
            long available = total - rented;

            availableSeries.getData().add(new XYChart.Data<>(model, available));
            rentedSeries.getData().add(new XYChart.Data<>(model, rented));
        });

        inventoryChart.getData().addAll(availableSeries, rentedSeries);

        // Styla färgerna: Ledig (Grön), Uthyrd (Röd/Orange)
        // OBS: JavaFX CSS styling av serier görs bäst via stylesheet, men vi kan fuska lite med lookup i Platform.runLater
        Platform.runLater(() -> {
            for (javafx.scene.Node n : availableSeries.getData().stream().map(XYChart.Data::getNode).collect(Collectors.toList())) {
                if (n != null) n.setStyle("-fx-bar-fill: #10B981;"); // Grön
            }
            for (javafx.scene.Node n : rentedSeries.getData().stream().map(XYChart.Data::getNode).collect(Collectors.toList())) {
                if (n != null) n.setStyle("-fx-bar-fill: #EF4444;"); // Röd
            }
        });

        // --- GRAF 2: Bar Chart (Historisk Popularitet - Top 10) ---
        CategoryAxis xAxisPop = new CategoryAxis();
        NumberAxis yAxisPop = new NumberAxis();
        yAxisPop.setLabel("Antal uthyrningar");

        BarChart<String, Number> historyChart = new BarChart<>(xAxisPop, yAxisPop);
        historyChart.setTitle("Mest populära modellerna (Genom tiderna)");
        historyChart.setLegendVisible(false);

        Map<String, Long> popularity = inventory.getModelPopularity();
        XYChart.Series<String, Number> popSeries = new XYChart.Series<>();

        popularity.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // Sortera fallande
                .limit(10) // Visa bara top 10
                .forEach(e -> popSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));

        historyChart.getData().add(popSeries);

        // Layout
        VBox.setVgrow(inventoryChart, Priority.ALWAYS);
        VBox.setVgrow(historyChart, Priority.ALWAYS);

        Button closeBtn = new Button("Stäng");
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, inventoryChart, historyChart, closeBtn);

        Scene scene = new Scene(root);
        // Lägg till CSS om huvudscenen har det, annars kör vi inline
        dialog.setScene(scene);
        dialog.show();
    }

    private void showRentalDetailsDialog(Rental rental) {
        Stage dialog = new Stage();
        dialog.setTitle("Detaljerad vy");
        dialog.initModality(Modality.APPLICATION_MODAL);
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setPrefWidth(400);
        root.setStyle("-fx-background-color: white;");

        Label header = new Label("Uthyrningsdetaljer");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Optional<Item> itemOpt = inventory.findItemById(rental.getItemId());
        String itemName = itemOpt.map(Item::getName).orElse("Okänd");
        double currentCost = rental.getCurrentCost(itemOpt.map(Item::getCurrentRentalPrice).orElse(0.0));

        root.getChildren().addAll(
                header,
                new Label("ID: " + rental.getRentalId()),
                new Label("Artikel: " + itemName),
                new Label("Start: " + rental.getStartTime()),
                new Label("Kostnad hittills: " + String.format("%.2f kr", currentCost)),
                new Separator()
        );

        Button closeBtn = new Button("Stäng");
        closeBtn.setOnAction(e -> dialog.close());
        root.getChildren().add(closeBtn);

        dialog.setScene(new Scene(root));
        dialog.show();
    }

    private TableView<Rental> createActiveRentalsTable() {
        TableView<Rental> table = new TableView<>();
        table.setItems(activeRentalsList);

        TableColumn<Rental, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("rentalId"));
        // Sätter en liten fast bredd på ID
        idCol.setPrefWidth(60);

        TableColumn<Rental, String> itemCol = new TableColumn<>("Artikel");
        itemCol.setCellValueFactory(cell -> {
            Optional<Item> i = inventory.findItemById(cell.getValue().getItemId());
            return new javafx.beans.property.SimpleStringProperty(i.map(Item::getName).orElse(cell.getValue().getItemId()));
        });

        TableColumn<Rental, String> startCol = new TableColumn<>("Starttid");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<Rental, String> costCol = new TableColumn<>("Kostnad");
        costCol.setCellValueFactory(cell -> {
            Optional<Item> i = inventory.findItemById(cell.getValue().getItemId());
            double price = i.map(Item::getCurrentRentalPrice).orElse(0.0);
            return new javafx.beans.property.SimpleStringProperty(String.format("%.2f kr", cell.getValue().getCurrentCost(price)));
        });

        table.getColumns().addAll(idCol, itemCol, startCol, costCol);

        // Dela utrymmet jämnt över kolumnerna förutom ID som är smalare
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }
}