package se.scooterrental.util;

import se.scooterrental.service.Inventory;
import se.scooterrental.service.MemberRegistry;
import se.scooterrental.service.RentalService;

/**
 * Separata tråden för Autosave (VG-krav).
 * Sparar data till JSON med jämna mellanrum i bakgrunden.
 */
public class AutosaveThread extends Thread {

    private volatile boolean running = true;
    private static final long AUTOSAVE_INTERVAL_MS = 30000; // Spara var 30:e sekund

    private final MemberRegistry memberRegistry;
    private final Inventory inventory;
    private final RentalService rentalService;

    // NYTT: Callback för att meddela UI när sparning skett
    private Runnable onSaveCallback;

    public AutosaveThread(MemberRegistry memberRegistry, Inventory inventory, RentalService rentalService) {
        this.memberRegistry = memberRegistry;
        this.inventory = inventory;
        this.rentalService = rentalService;
        this.setDaemon(true);
        this.setName("Autosave-Thread");
    }

    /**
     * Sätter en funktion som körs efter varje lyckad sparning.
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @Override
    public void run() {
        System.out.println("Autosave-tråden startad. Sparar var " + (AUTOSAVE_INTERVAL_MS / 1000) + ":e sekund.");
        while (running) {
            try {
                Thread.sleep(AUTOSAVE_INTERVAL_MS);
                if (running) {
                    performAutosave();
                }
            } catch (InterruptedException e) {
                System.out.println("Autosave-tråden avbruten och avslutas.");
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stopThread() {
        this.running = false;
        this.interrupt();
    }

    private void performAutosave() {
        if (memberRegistry != null && inventory != null && rentalService != null) {
            memberRegistry.saveData();
            inventory.saveData();
            rentalService.saveData();
            System.out.println(">>> Autosave utfört i bakgrunden.");

            // NYTT: Kör callbacken om den finns (signalerar till MainApp)
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
        }
    }
}