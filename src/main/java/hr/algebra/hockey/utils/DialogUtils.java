package hr.algebra.hockey.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void showInformation(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}