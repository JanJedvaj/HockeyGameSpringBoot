module hr.algebra.hockey {
    requires javafx.controls;
    requires javafx.fxml;

    opens hr.algebra.hockey to javafx.fxml;
    opens hr.algebra.hockey.controller to javafx.fxml;
    exports hr.algebra.hockey;
    exports hr.algebra.hockey.controller;
}