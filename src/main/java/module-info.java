module hr.algebra.hockey {
    requires javafx.controls;
    requires javafx.fxml;


    opens hr.algebra.hockey to javafx.fxml;
    exports hr.algebra.hockey;
}