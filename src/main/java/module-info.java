module se.scooterrental {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Gson
    requires transitive com.google.gson;

    // Ikonli
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.antdesignicons;
    requires org.kordamp.ikonli.mapicons;
    requires org.kordamp.ikonli.maki2;

    // Exports
    // exports se.scooterrental;
    exports se.scooterrental.model;
    exports se.scooterrental.service;
    exports se.scooterrental.persistence;
    exports se.scooterrental.util;
    exports se.scooterrental.ui;
    exports se.scooterrental.ui.views;

    // Opens
    opens se.scooterrental.model to javafx.base, com.google.gson;
    opens se.scooterrental to javafx.fxml, javafx.graphics;
    opens se.scooterrental.ui to javafx.fxml, javafx.graphics;
    opens se.scooterrental.ui.views to javafx.fxml, javafx.graphics;
}