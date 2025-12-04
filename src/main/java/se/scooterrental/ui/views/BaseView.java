package se.scooterrental.ui.views;

import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

/**
 * Abstrakt basklass för vyer/flikar i applikationen.
 * FIXAT: setupUI() anropas inte längre från konstruktorn,
 * vilket förhindrar NullPointerException i subklasser.
 */
public abstract class BaseView {
    protected Tab tab;
    protected VBox rootLayout;

    public BaseView(String title) {
        rootLayout = new VBox(10); // 10 pixlars mellanrum
        rootLayout.setStyle("-fx-padding: 20;");

        tab = new Tab(title);
        tab.setContent(rootLayout);
        tab.setClosable(false);

        // BORTTAGET: setupUI(); <-- Detta orsakade felet
    }

    /**
     * Abstrakt metod som implementeras i subklasserna för att bygga UI.
     * Måste nu anropas manuellt i subklassens konstruktor.
     */
    protected abstract void setupUI();

    /**
     * Måste vara public för att MainApp ska kunna anropa den.
     */
    public Tab getTab() {
        return tab;
    }
}