module hr.algebra.hockey {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.naming;
    requires java.rmi;
    requires java.logging;
    requires static lombok;

    opens hr.algebra.hockey to javafx.fxml;
    opens hr.algebra.hockey.controller to javafx.fxml;
    exports hr.algebra.hockey;
    exports hr.algebra.hockey.controller;
    exports hr.algebra.hockey.rmi;
}
