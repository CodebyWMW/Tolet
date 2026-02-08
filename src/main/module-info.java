module com.tolet {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens com.tolet to javafx.fxml;
    exports com.tolet;
}

// module com.tolet {
//     requires javafx.controls;
//     requires javafx.fxml;
    
//     // Exports required for FXML to see your controllers
//     opens com.tolet to javafx.fxml;
//     exports com.tolet;
// }