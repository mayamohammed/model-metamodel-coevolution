package com.coevolution.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ReportDialogController {

    @FXML private TextArea taFullReport;
    @FXML private Label    lblDownloadStatus;

    private String reportText = "";
    private String reportJson = "";

    
    public void setReport(String text, String json) {
        this.reportText = (text != null) ? text : "";
        this.reportJson = (json != null) ? json : "";
        if (taFullReport != null) {
            taFullReport.setText(this.reportText);
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) taFullReport.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onDownloadTxt() {
        saveFile("rapport_ia.txt", reportText, "Fichier texte (*.txt)", "*.txt");
    }

    @FXML
    private void onDownloadJson() {
        String content = (reportJson == null || reportJson.isBlank())
                ? "{ \"report\": \"" + reportText.replace("\"", "'") + "\" }"
                : reportJson;
        saveFile("rapport_ia.json", content, "Fichier JSON (*.json)", "*.json");
    }

    private void saveFile(String defaultName, String content,
                          String description, String extension) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport");
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, extension));

        File f = fc.showSaveDialog(taFullReport.getScene().getWindow());
        if (f == null) return;

        try {
            Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
            if (lblDownloadStatus != null) {
                lblDownloadStatus.setText("✔ Enregistré : " + f.getName());
                lblDownloadStatus.setStyle(
                        "-fx-text-fill: #238636; -fx-font-weight: 700;");
            }
        } catch (IOException e) {
            if (lblDownloadStatus != null) {
                lblDownloadStatus.setText("✘ Erreur : " + e.getMessage());
                lblDownloadStatus.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-weight: 700;");
            }
        }
    }
}
