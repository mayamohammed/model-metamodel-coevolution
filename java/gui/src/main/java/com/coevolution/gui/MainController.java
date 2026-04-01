package com.coevolution.gui;

import com.coevolution.analyzer.diff.EcoreDiff;
import com.coevolution.analyzer.diff.comparator.MetamodelComparator;
import com.coevolution.analyzer.diff.diff.DiffCategorizer;
import com.coevolution.analyzer.diff.diff.LevenshteinDetector;
import com.coevolution.analyzer.diff.model.DiffDelta;
import com.coevolution.migrator.ATLTransformationRunner;
import com.coevolution.migrator.MigrationReportExporter;
import com.coevolution.migrator.MigrationValidator;
import com.coevolution.migrator.TransformationGenerator;
import com.coevolution.migrator.TransformationGenerator.MigrationPlan;
import com.coevolution.gui.ReportDialogController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.eclipse.emf.ecore.EPackage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.File;

public class MainController implements Initializable {

    private static final String API_URL =
            System.getProperty("api.url", "http://localhost:5000");

    @FXML private Label lblApiStatus;
    @FXML private Label lblStatusBar;
    @FXML private Label lblM1Title;
    @FXML private Label lblM2Title;
    @FXML private TreeView<String> tvM1;
    @FXML private TreeView<String> tvM2;

    private File m1File;
    private File m2File;

    private String lastPredictionJson = "";
    private Map<String, Integer> lastFeatures = new LinkedHashMap<>();
    private DeltaReport lastReport = null;

    private List<String> currentDeltas = new ArrayList<>();

    private String lastOutputEcorePath = null;
    private String lastReportJsonPath  = null;
    private String lastReportCsvPath   = null;
    private String lastAtlFilePath = null;

    @FXML private TableView<DeltaRow>          tblDeltas;
    @FXML private TableColumn<DeltaRow,String> colDeltaType;
    @FXML private TableColumn<DeltaRow,String> colDeltaElement;
    @FXML private TableColumn<DeltaRow,String> colDeltaDetails;

    @FXML private TextArea taAiSummary;
    @FXML private Label    lblDeltaSize;
    @FXML private Label    lblComplexity;
    @FXML private Label    lblBreaking;

    @FXML private TextArea taMigrationResult;
    @FXML private Button btnOpenAtlFile;
    @FXML private Button btnRunMigration;
    @FXML private Label    lblMigStatus;
    @FXML private Label    lblMigValid;
    @FXML private Label    lblMigAtl;

    @FXML private Button btnOpenOutput;
    @FXML private Button btnOpenReportJson;
    @FXML private Button btnOpenReportCsv;
    @FXML private Button btnOpenOutputDir;

    @FXML private TabPane bottomTabPane;
    @FXML private ProgressIndicator piMigration;

    
    
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initDeltaTable();
        initTrees();
        checkApiHealthOnce();
        setStatus("Ready - API: " + API_URL);

        if (taAiSummary != null) {
            taAiSummary.setText("");
            taAiSummary.setPromptText("🖱  Cliquez ici pour voir le rapport complet...");
            taAiSummary.setOnMouseClicked(e -> openReportDialog());
        }
        if (taMigrationResult != null) {
            taMigrationResult.setText("");
            taMigrationResult.setPromptText("Lancez une migration pour voir les résultats ici...");
        }
    }

    private void initTrees() {
        if (tvM1 != null) { tvM1.setRoot(new TreeItem<>("M1 not loaded")); tvM1.setShowRoot(true); }
        if (tvM2 != null) { tvM2.setRoot(new TreeItem<>("M2 not loaded")); tvM2.setShowRoot(true); }
    }

    private void initDeltaTable() {
        if (tblDeltas == null) return;
        if (colDeltaType    != null) colDeltaType.setCellValueFactory(c -> c.getValue().typeProperty());
        if (colDeltaElement != null) colDeltaElement.setCellValueFactory(c -> c.getValue().elementProperty());
        if (colDeltaDetails != null) colDeltaDetails.setCellValueFactory(c -> c.getValue().detailsProperty());
        tblDeltas.getItems().clear();
        tblDeltas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    
    
    
    @FXML
    private void onLoadV1(ActionEvent e) {
        File f = chooseEcore("Charger Méta-modèle Source (V1)");
        if (f == null) return;
        m1File = f;
        if (lblM1Title != null) lblM1Title.setText(f.getName());
        setStatus("Loaded V1: " + f.getAbsolutePath());
        try {
            EcoreInfo info = parseEcore(f);
            if (tvM1 != null) {
                tvM1.setRoot(buildTree("EPackage: " + safe(info.nsURI, "root"), info));
                tvM1.getRoot().setExpanded(true);
            }
        } catch (Exception ex) {
            setStatus("Failed to load V1: " + ex.getMessage());
        }
    }

    @FXML
    private void onLoadV2(ActionEvent e) {
        File f = chooseEcore("Charger Méta-modèle Target (V2)");
        if (f == null) return;
        m2File = f;
        if (lblM2Title != null) lblM2Title.setText(f.getName());
        setStatus("Loaded V2: " + f.getAbsolutePath());
        try {
            EcoreInfo info = parseEcore(f);
            if (tvM2 != null) {
                tvM2.setRoot(buildTree("EPackage: " + safe(info.nsURI, "root"), info));
                tvM2.getRoot().setExpanded(true);
            }
        } catch (Exception ex) {
            setStatus("Failed to load V2: " + ex.getMessage());
        }
    }

    
    
    
    @FXML
    private void onConfigureAI(ActionEvent e) {
        
        boolean apiOnline = httpGet("/health").toLowerCase().contains("ok");
        String statusText = apiOnline ? "🟢  ONLINE" : "🔴  OFFLINE";

        String v1Name = (m1File != null) ? m1File.getName() : "—  (not loaded)";
        String v2Name = (m2File != null) ? m2File.getName() : "—  (not loaded)";

        String content = String.format(
            "📡  API Endpoint     :  %s\n" +
            "🔌  API Status       :  %s\n" +
            "\n" +
            "────────────────────────────────\n" +
            "\n" +
            "📂  Endpoints disponibles :\n" +
            "     GET   /health\n" +
            "     POST  /predict\n" +
            "     GET   /model/info\n" +
            "\n" +
            "────────────────────────────────\n" +
            "\n" +
            "📄  V1 Source        :  %s\n" +
            "📄  V2 Target        :  %s\n" +
            "\n" +
            "💡  Tip: démarrer l'API avec\n" +
            "     python app.py  (ou  flask run)",
            API_URL, statusText, v1Name, v2Name
        );

        showInfoAlert("AI Configuration", "⬡  Metamodel Coevolution — AI Config", content);
        setStatus("AI configuration displayed.");
    }

    
    
    

    
    private void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert, false);
        alert.showAndWait();
    }

    
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert, true);
        alert.showAndWait();
    }

    
    private void styleAlert(Alert alert, boolean isError) {
        DialogPane dp = alert.getDialogPane();

        
        dp.setStyle(
            "-fx-background-color: #161b22;" +
            "-fx-border-color: #30363d;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );

        
        Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle(
                "-fx-background-color: #0d1117;" +
                "-fx-border-color: transparent transparent #30363d transparent;" +
                "-fx-border-width: 0 0 1 0;" +
                "-fx-padding: 16 20 16 20;"
            );
        }

        
        Node headerLabel = dp.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle(
                "-fx-text-fill: #e6edf3;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif;"
            );
        }

        
        Node contentLabel = dp.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle(
                "-fx-text-fill: #8b949e;" +
                "-fx-font-size: 12px;" +
                "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                "-fx-padding: 16 20 16 20;" +
                "-fx-line-spacing: 3;"
            );
        }

        
        Node btnBar = dp.lookup(".button-bar");
        if (btnBar != null) {
            btnBar.setStyle(
                "-fx-background-color: #0d1117;" +
                "-fx-border-color: #30363d transparent transparent transparent;" +
                "-fx-border-width: 1 0 0 0;" +
                "-fx-padding: 10 16 10 16;"
            );
        }

        
        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            String btnColor   = isError ? "#da3633" : "#238636";
            String btnHover   = isError ? "#f85149" : "#2ea043";
            String btnBorder  = isError ? "#f8514955" : "#2ea04355";

            okBtn.setStyle(
                "-fx-background-color: " + btnColor + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-border-color: " + btnBorder + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 6 20 6 20;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
            );
            okBtn.setOnMouseEntered(ev -> okBtn.setStyle(
                okBtn.getStyle().replace(btnColor, btnHover)));
            okBtn.setOnMouseExited(ev -> okBtn.setStyle(
                okBtn.getStyle().replace(btnHover, btnColor)));
        }

        
        Node graphic = dp.lookup(".graphic-container");
        if (graphic != null) {
            graphic.setStyle("-fx-padding: 0; -fx-pref-width: 0; -fx-max-width: 0;");
        }

        
        dp.setMinWidth(500);
        dp.setMinHeight(320);

        
        try {
            URL cssUrl = getClass().getResource("style.css");
            if (cssUrl != null)
                dp.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}
    }

    
    
    
    @FXML
    private void onRunAnalysis(ActionEvent e) { onDetectDeltas(e); }

    @FXML
    private void onDetectDeltas(ActionEvent e) {
        if (m1File == null || m2File == null) {
            setStatus("Please load V1 and V2 first.");
            showErrorAlert("Erreur", "Fichiers manquants",
                "Veuillez charger V1 et V2 avant de lancer l'analyse.");
            return;
        }

        if (piMigration != null) piMigration.setVisible(true);
        setStatus("Running analysis (deltas + AI) ...");

        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                EPackage v1 = EcoreDiff.load(m1File);
                EPackage v2 = EcoreDiff.load(m2File);

                MetamodelComparator cmp = new MetamodelComparator();
                MetamodelComparator.Snapshot s1 = cmp.snapshot(v1);
                MetamodelComparator.Snapshot s2 = cmp.snapshot(v2);

                List<DiffDelta> deltas = new DiffCategorizer().categorize(s1, s2);
                LevenshteinDetector lev = new LevenshteinDetector();
                deltas = lev.detectAttributeRenames(deltas, s1, s2, 0.80);

                Map<String, Integer> features = deltasToFeatures(deltas);

                String predJson = allFeaturesZero(features)
                        ? "{\"prediction\":\"NO_CHANGE\",\"confidence_pct\":0,\"top3\":[]}"
                        : httpPost("/predict", toJson(features));

                DeltaReport report = new DeltaReport();
                report.m1File = m1File.getName();
                report.m2File = m2File.getName();
                report.lines.addAll(deltasToLines(deltas));
                applyFeatureCountersToReport(report, features);

                List<String> deltaTexts = buildMigrationActions(deltas);
                return new AnalysisResult(report, features, predJson, deltaTexts);
            }
        };

        task.setOnSucceeded(ev -> {
            if (piMigration != null) piMigration.setVisible(false);
            AnalysisResult r = task.getValue();
            lastPredictionJson = r.predictionJson;
            lastFeatures       = r.features;
            lastReport         = r.report;

            currentDeltas.clear();
            currentDeltas.addAll(r.deltaTexts);

            fillDeltaTable(r.report);

            String aiText = buildAiText(r.report, r.features, r.predictionJson);
            if (taAiSummary != null) taAiSummary.setText(aiText);
            updateStatsLabels(r.report, r.features);
            if (bottomTabPane != null) bottomTabPane.getSelectionModel().select(1);
            setStatus("Analysis done. " + currentDeltas.size() + " delta(s) ready for migration.");
            if (btnRunMigration != null) btnRunMigration.setDisable(false);
        });

        task.setOnFailed(ev -> {
            if (piMigration != null) piMigration.setVisible(false);
            String err = task.getException() != null
                    ? task.getException().getMessage() : "Unknown error";
            setStatus("Analysis error: " + err);
            showErrorAlert("Erreur Analyse", "Échec de l'analyse", err);
        });

        new Thread(task, "analysis").start();
    }

 
 
 
 @FXML
 private void onRunMigration(ActionEvent e) {
     if (m1File == null || m2File == null) {
         setStatus("Please load V1 and V2 first.");
         showErrorAlert("Erreur", "Fichiers manquants",
             "Veuillez charger V1 et V2 avant de lancer la migration.");
         return;
     }
     if (lastPredictionJson == null || lastPredictionJson.isBlank()) {
         setStatus("Please run analysis first (need AI prediction).");
         showErrorAlert("Erreur", "Analyse requise",
             "Lancez d'abord l'analyse pour obtenir la prédiction IA.");
         return;
     }
     if (currentDeltas.isEmpty()) {
         setStatus("No deltas available — please re-run analysis.");
         showErrorAlert("Erreur", "Aucun delta",
             "Aucun delta trouvé. Veuillez relancer l'analyse.");
         return;
     }

     if (piMigration != null) piMigration.setVisible(true);
     setStatus("Running migration ATL (" + currentDeltas.size() + " deltas) ...");
     disableMigrationButtons(true);

     final List<String> deltasSnapshot = new ArrayList<>(currentDeltas);

     Task<MigrationUiResult> task = new Task<>() {
         @Override
         protected MigrationUiResult call() throws Exception {
             String label   = extractJson(lastPredictionJson, "prediction");
             String confStr = extractJson(lastPredictionJson, "confidence_pct");
             double conf = 0.0;
             try { conf = Double.parseDouble(confStr) / 100.0; } catch (Exception ignore) {}

             
             File projectRoot        = detectProjectRoot();
             File transformationsDir = new File(projectRoot, "data/transformations");
             File outDir             = new File(projectRoot, "data/migrations-output");
             File reportsDir         = new File(projectRoot, "data/migration-reports");
             transformationsDir.mkdirs();
             outDir.mkdirs();
             reportsDir.mkdirs();

             System.out.println("[MIGRATION] projectRoot = " + projectRoot.getAbsolutePath());
             System.out.println("[MIGRATION] label       = " + label);
             System.out.println("[MIGRATION] deltas      = " + deltasSnapshot.size());

             
             TransformationGenerator tg =
                     new TransformationGenerator(transformationsDir.getAbsolutePath());
             MigrationPlan plan = tg.generatePlan(
                     m1File.getAbsolutePath(),
                     m2File.getAbsolutePath(),
                     label, conf, deltasSnapshot);

             System.out.println("[MIGRATION] ATL = " + plan.getAtlFile()
                     + " | actions = " + plan.getActions().size());

             
             ATLTransformationRunner runner =
                     new ATLTransformationRunner(outDir.getAbsolutePath());
             ATLTransformationRunner.MigrationResult result = runner.run(plan);

             
             MigrationValidator validator = new MigrationValidator();
             MigrationValidator.ValidationResult vr = validator.validate(result);

             
             MigrationReportExporter exporter =
                     new MigrationReportExporter(reportsDir.getAbsolutePath());
             String reportJsonPath = exporter.exportReport(List.of(result), List.of(vr));
             String reportCsvPath  = exporter.exportCsv(List.of(result));

             System.out.println("[MIGRATION] success=" + result.isSuccess()
                     + " | valid=" + vr.isValid());

             
             MigrationUiResult ui = new MigrationUiResult();
             ui.label              = label;
             ui.confidencePct      = confStr;
             ui.atlPath            = plan.getAtlPath();
             ui.outputEcorePath    = result.getOutputPath();
             ui.success            = result.isSuccess();
             ui.validationValid    = vr.isValid();
             ui.validationErrors   = new ArrayList<>(vr.getErrors());
             ui.validationWarnings = new ArrayList<>(vr.getWarnings());
             ui.reportJsonPath     = reportJsonPath;
             ui.reportCsvPath      = reportCsvPath;
             ui.deltasPreview      = new ArrayList<>(deltasSnapshot);
             return ui;
         }
     };

     task.setOnSucceeded(ev -> {
         if (piMigration != null) piMigration.setVisible(false);
         MigrationUiResult r = task.getValue();

         lastOutputEcorePath = r.outputEcorePath;
         lastReportJsonPath  = r.reportJsonPath;
         lastReportCsvPath   = r.reportCsvPath;
         lastAtlFilePath     = r.atlPath;

         if (btnOpenAtlFile != null)
             btnOpenAtlFile.setDisable(!new File(r.atlPath).exists());

         StringBuilder sb = new StringBuilder();
         sb.append("=== MIGRATION ATL ===\n\n");
         sb.append("Prediction : ").append(r.label).append("\n");
         sb.append("Confidence : ").append(r.confidencePct).append("%\n");
         sb.append("ATL used   : ").append(fileName(r.atlPath)).append("\n\n");
         sb.append("Output     : ").append(fileName(r.outputEcorePath)).append("\n");
         sb.append("Success    : ").append(r.success).append("\n");
         sb.append("Valid      : ").append(r.validationValid).append("\n\n");

         if (!r.validationErrors.isEmpty()) {
             sb.append("=== Validation Errors ===\n");
             for (String err : r.validationErrors) sb.append("- ").append(err).append("\n");
             sb.append("\n");
         }
         if (!r.validationWarnings.isEmpty()) {
             sb.append("=== Validation Warnings ===\n");
             for (String w : r.validationWarnings) sb.append("- ").append(w).append("\n");
             sb.append("\n");
         }
         sb.append("Report JSON : ").append(fileName(r.reportJsonPath)).append("\n");
         sb.append("Report CSV  : ").append(fileName(r.reportCsvPath)).append("\n\n");

         if (!r.deltasPreview.isEmpty()) {
             sb.append("=== Deltas appliqués (").append(r.deltasPreview.size()).append(") ===\n");
             for (String l : r.deltasPreview)
                 sb.append("- ").append(l).append("\n");
         }

         if (taMigrationResult != null) taMigrationResult.setText(sb.toString());
         updateMigrationLabels(r);
         disableMigrationButtons(false);
         setStatus("Migration terminée — " + (r.success ? "SUCCESS ✅" : "FAILED ❌"));
         if (bottomTabPane != null) bottomTabPane.getSelectionModel().select(2);
     });

     task.setOnFailed(ev -> {
         if (piMigration != null) piMigration.setVisible(false);
         Throwable ex = task.getException();
         StringWriter sw = new StringWriter();
         if (ex != null) ex.printStackTrace(new PrintWriter(sw));
         String err = sw.toString().isBlank()
                 ? (ex != null ? ex.toString() : "Unknown error") : sw.toString();
         setStatus("Migration error.");
         showErrorAlert("Erreur Migration", "Échec de la migration", err);
         if (taMigrationResult != null) taMigrationResult.setText("[MIGRATION ERROR]\n" + err);
         if (bottomTabPane != null) bottomTabPane.getSelectionModel().select(2);
         disableMigrationButtons(false);
     });

     new Thread(task, "migration").start();
 }

 
 
 
 private File detectProjectRoot() {
     File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
     for (int i = 0; i < 6; i++) {
         if (new File(current, "data").exists()) {
             System.out.println("[ROOT] detected = " + current.getAbsolutePath());
             return current;
         }
         File parent = current.getParentFile();
         if (parent == null) break;
         current = parent;
     }
     System.err.println("[ROOT] fallback = " + current.getAbsolutePath());
     return new File(System.getProperty("user.dir")).getAbsoluteFile();
 }

    
    
    
    @FXML private void onOpenOutputEcore(ActionEvent e)  { openFile(lastOutputEcorePath); }
    @FXML private void onOpenReportJson(ActionEvent e)   { openFile(lastReportJsonPath); }
    @FXML private void onOpenReportCsv(ActionEvent e)    { openFile(lastReportCsvPath); }
    @FXML
    private void onOpenAtlFile(ActionEvent e) {

        if (lastAtlFilePath == null || lastAtlFilePath.isBlank()) {
            setStatus("ATL file not available.");
            return;
        }

        File atlFile = new File(lastAtlFilePath);

        if (!atlFile.exists()) {
            System.out.println("[ATL] File not found: " + atlFile.getAbsolutePath());
            setStatus("ATL file not found.");
            return;
        }
        try {
            java.awt.Desktop.getDesktop().open(atlFile);
            setStatus("ATL opened: " + atlFile.getName());
        } catch (Exception ex) {
        }
    }   

    @FXML
    private void onOpenOutputFolder(ActionEvent e) {
        if (lastOutputEcorePath == null) return;
        openFile(new File(lastOutputEcorePath).getAbsoluteFile().getParent());
    }

    private void openFile(String path) {
        if (path == null || path.isBlank()) { setStatus("Chemin introuvable."); return; }
        File f = new File(path).getAbsoluteFile();
        if (!f.exists()) { setStatus("Fichier introuvable : " + f.getAbsolutePath()); return; }
        try {
            java.awt.Desktop.getDesktop().open(f);
            setStatus("Ouvert : " + f.getName());
        } catch (Exception ex) {
            setStatus("Impossible d'ouvrir : " + ex.getMessage());
        }
    }

    private void disableMigrationButtons(boolean disable) {
        if (btnOpenOutput     != null) btnOpenOutput.setDisable(disable);
        if (btnOpenReportJson != null) btnOpenReportJson.setDisable(disable);
        if (btnOpenReportCsv  != null) btnOpenReportCsv.setDisable(disable);
        if (btnOpenOutputDir  != null) btnOpenOutputDir.setDisable(disable);
        if (btnOpenAtlFile    != null) btnOpenAtlFile.setDisable(disable);     
    }

    private String fileName(String path) {
        if (path == null) return "---";
        return new File(path).getAbsoluteFile().getName();
    }

    
    
    
    private void openReportDialog() {
        if (taAiSummary == null || taAiSummary.getText().isBlank()) {
            setStatus("Aucun rapport à afficher. Lancez d'abord l'analyse.");
            return;
        }
        try {
            java.net.URL fxmlUrl = getClass().getResource("report_dialog.fxml");
            java.net.URL cssUrl  = getClass().getResource("style.css");
            if (fxmlUrl == null) { setStatus("ERREUR : report_dialog.fxml introuvable !"); return; }
            if (cssUrl  == null) { setStatus("ERREUR : style.css introuvable !"); return; }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();
            ReportDialogController ctrl = loader.getController();
            ctrl.setReport(taAiSummary.getText(),
                    lastPredictionJson != null ? lastPredictionJson : "");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(cssUrl.toExternalForm());
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Rapport IA — Metamodel Coevolution Tool");
            dialog.setScene(scene);
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setMinWidth(800);
            dialog.setMinHeight(600);
            dialog.show();
        } catch (Exception ex) {
            setStatus("Erreur ouverture rapport : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    
    
    
    private void updateStatsLabels(DeltaReport report, Map<String, Integer> features) {
        int total = features.values().stream().mapToInt(Integer::intValue).sum();
        boolean breaking = report.removedClasses > 0
                        || report.removedAttributes > 0
                        || report.removedReferences > 0;
        String complexity = total <= 3 ? "Low" : total <= 8 ? "Medium" : "High";

        if (lblDeltaSize  != null) lblDeltaSize.setText(String.valueOf(report.lines.size()));
        if (lblComplexity != null) lblComplexity.setText(complexity);
        if (lblBreaking   != null) {
            lblBreaking.setText(breaking ? "Yes" : "No");
            lblBreaking.setStyle(breaking
                    ? "-fx-text-fill:#f85149; -fx-font-weight:700; -fx-font-size:18px;"
                    : "-fx-text-fill:#238636; -fx-font-weight:700; -fx-font-size:18px;");
        }
    }

    private void updateMigrationLabels(MigrationUiResult r) {
        if (lblMigStatus != null) {
            lblMigStatus.setText(r.success ? "✔ SUCCESS" : "✘ FAILED");
            lblMigStatus.setStyle(r.success
                    ? "-fx-text-fill:#238636; -fx-font-weight:700; -fx-font-size:15px;"
                    : "-fx-text-fill:#f85149; -fx-font-weight:700; -fx-font-size:15px;");
        }
        if (lblMigValid != null) {
            lblMigValid.setText(r.validationValid ? "✔ Yes" : "✘ No");
            lblMigValid.setStyle(r.validationValid
                    ? "-fx-text-fill:#238636; -fx-font-weight:700; -fx-font-size:15px;"
                    : "-fx-text-fill:#f85149; -fx-font-weight:700; -fx-font-size:15px;");
        }
        if (lblMigAtl != null) {
            lblMigAtl.setText(fileName(r.atlPath));
            lblMigAtl.setStyle("-fx-text-fill:#58a6ff; -fx-font-size:11px; -fx-font-weight:600;");
        }
    }

    
    
    
    private void fillDeltaTable(DeltaReport report) {
        if (tblDeltas == null) return;
        tblDeltas.getItems().clear();
        for (String line : report.lines) tblDeltas.getItems().add(DeltaRow.fromLine(line));
        if (tblDeltas.getItems().isEmpty())
            tblDeltas.getItems().add(new DeltaRow("INFO", "-", "No deltas detected."));
    }

    private TreeItem<String> buildTree(String rootTitle, EcoreInfo info) {
        TreeItem<String> root = new TreeItem<>(rootTitle);

        TreeItem<String> classesNode = new TreeItem<>("EClasses");
        classesNode.setExpanded(true);
        for (Map.Entry<String, EClassInfo> e : info.classes.entrySet()) {
            EClassInfo c = e.getValue();
            TreeItem<String> classNode = new TreeItem<>(
                    "EClass: " + c.name + (c.isAbstract ? " (abstract)" : ""));
            if (!c.superTypes.isEmpty()) {
                TreeItem<String> st = new TreeItem<>("eSuperTypes");
                for (String s : c.superTypes) st.getChildren().add(new TreeItem<>(s));
                classNode.getChildren().add(st);
            }
            classesNode.getChildren().add(classNode);
        }

        TreeItem<String> attrsNode = new TreeItem<>("EAttributes");
        attrsNode.setExpanded(true);
        for (Map.Entry<String, String> a : info.attributes.entrySet())
            attrsNode.getChildren().add(new TreeItem<>(
                    "EAttribute: " + a.getKey() + " (" + safe(a.getValue(), "unknown") + ")"));

        TreeItem<String> refsNode = new TreeItem<>("EReferences");
        refsNode.setExpanded(true);
        for (Map.Entry<String, ERefInfo> r : info.references.entrySet()) {
            ERefInfo ref = r.getValue();
            refsNode.getChildren().add(new TreeItem<>(
                    "EReference: " + ref.name + " -> " + safe(ref.eType, "?") +
                            " [upper=" + safe(ref.upperBound, "?") +
                            ", containment=" + safe(ref.containment, "?") + "]"));
        }

        root.getChildren().addAll(classesNode, attrsNode, refsNode);
        return root;
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (lblStatusBar != null) lblStatusBar.setText(msg); });
    }

    private File chooseEcore(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ecore (*.ecore)", "*.ecore"));
        return fc.showOpenDialog(null);
    }

    
    
    
    private List<String> buildMigrationActions(List<DiffDelta> deltas) {
        List<String> actions = new ArrayList<>();

        Set<String> renamedOldKeys = new HashSet<>();
        Set<String> renamedNewKeys = new HashSet<>();

        for (DiffDelta d : deltas) {
            if (d.getKind() != DiffDelta.Kind.RENAME) continue;
            String details = d.getDetails() != null ? d.getDetails() : "";
            Pattern rp = Pattern.compile(
                "rename:\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)",
                Pattern.CASE_INSENSITIVE);
            Matcher rm = rp.matcher(details.isBlank() ? d.getElement() : details);
            if (rm.find()) {
                renamedOldKeys.add(rm.group(1) + "::" + rm.group(2));
                renamedNewKeys.add(rm.group(3) + "::" + rm.group(4));
            } else {
                renamedOldKeys.add(d.getElement());
            }
        }

        for (DiffDelta d : deltas) {
            String elem    = d.getElement()  != null ? d.getElement()  : "";
            String details = d.getDetails()  != null ? d.getDetails()  : "";
            DiffDelta.Kind        kind = d.getKind();
            DiffDelta.ElementType type = d.getElementType();

            switch (kind) {
                case ADD:
                    if      (type == DiffDelta.ElementType.CLASS)
                        actions.add("ADD CLASS " + elem + " | EClass added");
                    else if (type == DiffDelta.ElementType.ATTRIBUTE
                            && !renamedNewKeys.contains(elem))
                        actions.add("ADD ATTRIBUTE " + elem + " | EAttribute added");
                    else if (type == DiffDelta.ElementType.REFERENCE)
                        actions.add("ADD REFERENCE " + elem + " | EReference added");
                    break;

                case DELETE:
                    if      (type == DiffDelta.ElementType.CLASS)
                        actions.add("DELETE CLASS " + elem + " | EClass removed");
                    else if (type == DiffDelta.ElementType.ATTRIBUTE
                            && !renamedOldKeys.contains(elem))
                        actions.add("DELETE ATTRIBUTE " + elem + " | EAttribute removed");
                    else if (type == DiffDelta.ElementType.REFERENCE)
                        actions.add("DELETE REFERENCE " + elem + " | EReference removed");
                    break;

                case CHANGE:
                    if      (type == DiffDelta.ElementType.PACKAGE)
                        actions.add("CHANGE PACKAGE " + elem + " | " + details);
                    else if (type == DiffDelta.ElementType.CLASS)
                        actions.add("CHANGE CLASS " + elem + " | " + details);
                    else if (type == DiffDelta.ElementType.REFERENCE)
                        actions.add("CHANGE REFERENCE " + elem + " | " + details);
                    else if (type == DiffDelta.ElementType.ATTRIBUTE)
                        actions.add("CHANGE ATTRIBUTE " + elem + " | " + details);
                    break;

                case RENAME:
                    if (type == DiffDelta.ElementType.ATTRIBUTE) {
                        Pattern rp = Pattern.compile(
                            "rename:\\s*(\\w+::\\w+)\\s*->\\s*(\\w+::\\w+)",
                            Pattern.CASE_INSENSITIVE);
                        Matcher rm = rp.matcher(details);
                        if (rm.find()) {
                            actions.add("RENAME ATTRIBUTE "
                                    + rm.group(1) + " -> " + rm.group(2));
                        } else {
                            actions.add("RENAME ATTRIBUTE " + elem + " -> " + elem);
                        }
                    } else if (type == DiffDelta.ElementType.CLASS) {
                        Pattern rp = Pattern.compile(
                            "rename:\\s*(\\w+)\\s*->\\s*(\\w+)",
                            Pattern.CASE_INSENSITIVE);
                        Matcher rm = rp.matcher(details);
                        if (rm.find())
                            actions.add("RENAME CLASS " + rm.group(1) + " -> " + rm.group(2));
                    }
                    break;

                default:
                    actions.add(kind + " " + type + " " + elem + " | " + details);
            }
        }

        System.out.println("[MainController] buildMigrationActions: "
                + actions.size() + " action(s)");
        for (String a : actions) System.out.println("  >> " + a);
        return actions;
    }

    
    
    
    private void checkApiHealthOnce() {
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() {
                return httpGet("/health").toLowerCase().contains("ok");
            }
        };
        t.setOnSucceeded(e -> {
            boolean ok = t.getValue();
            if (lblApiStatus != null) {
                lblApiStatus.setText(ok ? "● API: ONLINE" : "● API: OFFLINE");
                lblApiStatus.setStyle(ok
                        ? "-fx-font-weight:700; -fx-text-fill:#238636;"
                        : "-fx-font-weight:700; -fx-text-fill:#f85149;");
            }
        });
        new Thread(t, "api-health").start();
    }

    private String httpGet(String path) {
        try {
            HttpURLConnection c = (HttpURLConnection)
                    URI.create(API_URL + path).toURL().openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            return read(c);
        } catch (Exception e) { return "{\"error\":\"" + e.getMessage() + "\"}"; }
    }

    private String httpPost(String path, String body) {
        try {
            HttpURLConnection c = (HttpURLConnection)
                    URI.create(API_URL + path).toURL().openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            c.setConnectTimeout(30000);
            c.setReadTimeout(30000);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            return read(c);
        } catch (Exception e) { return "{\"error\":\"" + e.getMessage() + "\"}"; }
    }

    private String read(HttpURLConnection c) throws IOException {
        InputStream is = (c.getResponseCode() >= 400)
                ? c.getErrorStream() : c.getInputStream();
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append("\n");
        }
        return sb.toString();
    }

    
    
    
    private String toJson(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
        }
        return sb.append("}").toString();
    }

    private String extractJson(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return "";
        i = json.indexOf(":", i) + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length()) return "";
        char c = json.charAt(i);
        if (c == '"') {
            int end = json.indexOf('"', i + 1);
            return end > 0 ? json.substring(i + 1, end) : "";
        }
        int end = i;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(i, end).trim();
    }

    private String extractJsonBlock(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return "";
        i = json.indexOf("[", i);
        if (i < 0) return "";
        int depth = 0, start = i;
        while (i < json.length()) {
            if      (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') { if (--depth == 0) { i++; break; } }
            i++;
        }
        return json.substring(start, i);
    }

    
    
    
    private String buildAiText(DeltaReport report,
                                Map<String, Integer> features,
                                String predictionJson) {
        String label = extractJson(predictionJson, "prediction");
        String conf  = extractJson(predictionJson, "confidence_pct");
        String top3  = extractJsonBlock(predictionJson, "top3");
        String atl   = labelToAtl(label);

        StringBuilder sb = new StringBuilder();
        sb.append("=== RESULTAT DE L'ANALYSE IA ===\n\n");
        sb.append("Prediction : ").append(label.isBlank() ? "ERROR" : label).append("\n");
        sb.append("Confiance  : ").append(conf.isBlank() ? "---" : conf + "%").append("\n\n");

        sb.append("=== Features extraites ===\n");
        for (Map.Entry<String, Integer> e : features.entrySet())
            sb.append("- ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");

        sb.append("\n=== Top 3 ===\n");
        sb.append(top3.isBlank() ? predictionJson : top3).append("\n");
        sb.append("\n=== Proposition de co-evolution ===\n");
        sb.append("ATL recommande : ").append(atl).append("\n\n");

        sb.append("=== Deltas (resume) ===\n");
        int shown = 0;
        for (String l : report.lines) {
            sb.append("- ").append(l).append("\n");
            if (++shown >= 12) {
                if (report.lines.size() > shown)
                    sb.append("... (").append(report.lines.size() - shown).append(" more)\n");
                break;
            }
        }
        return sb.toString();
    }

    private String labelToAtl(String label) {
        switch (label) {
            case "ECLASS_ADDED": case "EATTRIBUTE_ADDED":
            case "EREFERENCE_ADDED": case "ECLASS_SUPERTYPE_ADDED":
                return "add_class_migration.atl";
            case "ECLASS_REMOVED": case "EREFERENCE_REMOVED":
                return "remove_class_migration.atl";
            case "EATTRIBUTE_TYPE_CHANGED": case "EATTRIBUTE_REMOVED":
            case "EREFERENCE_MULTIPLICITY_CHANGED":
            case "EREFERENCE_CONTAINMENT_CHANGED":
            case "ECLASS_ABSTRACT_CHANGED":
                return "rename_attribute_migration.atl";
            default: return "mixed_changes_migration.atl";
        }
    }

    
    
    
    private Map<String, Integer> deltasToFeatures(List<DiffDelta> deltas) {
        Map<String, Integer> f = new LinkedHashMap<>();
        f.put("nb_added_classes", 0);       f.put("nb_removed_classes", 0);
        f.put("nb_added_attributes", 0);    f.put("nb_removed_attributes", 0);
        f.put("nb_type_changes", 0);        f.put("nb_added_references", 0);
        f.put("nb_removed_references", 0);  f.put("nb_multiplicity_changes", 0);
        f.put("nb_containment_changes", 0); f.put("nb_abstract_changes", 0);
        f.put("nb_supertype_changes", 0);   f.put("nsuri_changed", 0);

        for (DiffDelta d : deltas) {
            switch (d.getKind()) {
                case ADD:
                    if (d.getElementType() == DiffDelta.ElementType.CLASS)
                        inc(f, "nb_added_classes");
                    if (d.getElementType() == DiffDelta.ElementType.ATTRIBUTE)
                        inc(f, "nb_added_attributes");
                    if (d.getElementType() == DiffDelta.ElementType.REFERENCE)
                        inc(f, "nb_added_references");
                    break;
                case DELETE:
                    if (d.getElementType() == DiffDelta.ElementType.CLASS)
                        inc(f, "nb_removed_classes");
                    if (d.getElementType() == DiffDelta.ElementType.ATTRIBUTE)
                        inc(f, "nb_removed_attributes");
                    if (d.getElementType() == DiffDelta.ElementType.REFERENCE)
                        inc(f, "nb_removed_references");
                    break;
                case CHANGE:
                    if (d.getElementType() == DiffDelta.ElementType.PACKAGE
                            && d.getDetails().startsWith("nsURI:"))
                        inc(f, "nsuri_changed");
                    if (d.getElementType() == DiffDelta.ElementType.ATTRIBUTE
                            && d.getDetails().startsWith("type:"))
                        inc(f, "nb_type_changes");
                    if (d.getElementType() == DiffDelta.ElementType.REFERENCE
                            && d.getDetails().startsWith("multiplicity:"))
                        inc(f, "nb_multiplicity_changes");
                    if (d.getElementType() == DiffDelta.ElementType.REFERENCE
                            && d.getDetails().startsWith("containment:"))
                        inc(f, "nb_containment_changes");
                    if (d.getElementType() == DiffDelta.ElementType.CLASS
                            && d.getDetails().startsWith("abstract:"))
                        inc(f, "nb_abstract_changes");
                    if (d.getElementType() == DiffDelta.ElementType.CLASS
                            && d.getDetails().startsWith("supertypes:"))
                        inc(f, "nb_supertype_changes");
                    break;
                case RENAME: break;
            }
        }
        return f;
    }

    private void inc(Map<String, Integer> m, String k) {
        m.put(k, m.getOrDefault(k, 0) + 1);
    }

    private List<String> deltasToLines(List<DiffDelta> deltas) {
        List<String> lines = new ArrayList<>();
        for (DiffDelta d : deltas)
            lines.add(d.getKind() + " " + d.getElementType()
                    + " " + d.getElement() + " | " + d.getDetails());
        if (lines.isEmpty()) lines.add("INFO No deltas detected.");
        return lines;
    }

    private void applyFeatureCountersToReport(DeltaReport r, Map<String, Integer> f) {
        r.addedClasses        = f.getOrDefault("nb_added_classes", 0);
        r.removedClasses      = f.getOrDefault("nb_removed_classes", 0);
        r.addedAttributes     = f.getOrDefault("nb_added_attributes", 0);
        r.removedAttributes   = f.getOrDefault("nb_removed_attributes", 0);
        r.typeChanges         = f.getOrDefault("nb_type_changes", 0);
        r.addedReferences     = f.getOrDefault("nb_added_references", 0);
        r.removedReferences   = f.getOrDefault("nb_removed_references", 0);
        r.multiplicityChanges = f.getOrDefault("nb_multiplicity_changes", 0);
        r.containmentChanges  = f.getOrDefault("nb_containment_changes", 0);
        r.abstractChanges     = f.getOrDefault("nb_abstract_changes", 0);
        r.superTypeChanges    = f.getOrDefault("nb_supertype_changes", 0);
        r.nsUriChanged        = f.getOrDefault("nsuri_changed", 0) == 1;
    }

    private boolean allFeaturesZero(Map<String, Integer> feat) {
        for (Integer v : feat.values()) if (v != null && v != 0) return false;
        return true;
    }

    
    
    
    private static final Pattern P_NSURI       =
            Pattern.compile("\\bnsURI\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern P_ECLASS      =
            Pattern.compile("<eClassifiers\\b[^>]*xsi:type\\s*=\\s*\"ecore:EClass\"[^>]*\\bname\\s*=\\s*\"([^\"]+)\"[^>]*>");
    private static final Pattern P_ESTRUCT     =
            Pattern.compile("<eStructuralFeatures\\b[^>]*xsi:type\\s*=\\s*\"ecore:(EAttribute|EReference)\"[^>]*\\bname\\s*=\\s*\"([^\"]+)\"([^>]*)>");
    private static final Pattern P_ETYPE       =
            Pattern.compile("\\beType\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern P_UPPER       =
            Pattern.compile("\\bupperBound\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern P_CONTAINMENT =
            Pattern.compile("\\bcontainment\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern P_ABSTRACT    =
            Pattern.compile("\\babstract\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern P_ESUPER      =
            Pattern.compile("\\beSuperTypes\\s*=\\s*\"([^\"]+)\"");

    private EcoreInfo parseEcore(File f) throws IOException {
        String xml = new String(
                java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        EcoreInfo info = new EcoreInfo();
        info.fileName = f.getName();
        info.nsURI = group1(P_NSURI, xml);

        Matcher mc = P_ECLASS.matcher(xml);
        while (mc.find()) {
            String className = mc.group(1);
            EClassInfo ci = new EClassInfo();
            ci.name = className;
            ci.isAbstract = "true".equalsIgnoreCase(
                    findOnSameTagAttribute(xml, mc.start(), P_ABSTRACT));
            String superVal = findOnSameTagAttribute(xml, mc.start(), P_ESUPER);
            if (superVal != null && !superVal.isBlank())
                ci.superTypes.addAll(Arrays.asList(superVal.trim().split("\\s+")));
            info.classes.put(className, ci);
        }

        Matcher ms = P_ESTRUCT.matcher(xml);
        while (ms.find()) {
            String kind  = ms.group(1);
            String name  = ms.group(2);
            String attrs = ms.group(3);
            if ("EAttribute".equals(kind)) {
                info.attributes.put(name, safe(group1(P_ETYPE, attrs), ""));
            } else {
                ERefInfo r = new ERefInfo();
                r.name        = name;
                r.eType       = safe(group1(P_ETYPE, attrs), "");
                r.upperBound  = safe(group1(P_UPPER, attrs), "");
                r.containment = safe(group1(P_CONTAINMENT, attrs), "");
                info.references.put(name, r);
            }
        }
        return info;
    }

    private String group1(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private String findOnSameTagAttribute(String xml, int fromIndex, Pattern attrPattern) {
        int tagEnd = xml.indexOf(">", fromIndex);
        if (tagEnd < 0) return null;
        String tag = xml.substring(fromIndex, Math.min(tagEnd + 1, xml.length()));
        Matcher m = attrPattern.matcher(tag);
        return m.find() ? m.group(1) : null;
    }

    
    
    
    private static final class AnalysisResult {
        final DeltaReport          report;
        final Map<String, Integer> features;
        final String               predictionJson;
        final List<String>         deltaTexts;
        AnalysisResult(DeltaReport r, Map<String, Integer> f,
                       String p, List<String> dt) {
            report = r; features = f; predictionJson = p; deltaTexts = dt;
        }
    }

    private static final class MigrationUiResult {
        String label, confidencePct, atlPath, outputEcorePath,
               reportJsonPath, reportCsvPath;
        boolean success, validationValid;
        List<String> validationErrors   = new ArrayList<>();
        List<String> validationWarnings = new ArrayList<>();
        List<String> deltasPreview      = new ArrayList<>();
    }

    public static final class DeltaRow {
        private final SimpleStringProperty type, element, details;
        public DeltaRow(String t, String e, String d) {
            type    = new SimpleStringProperty(t);
            element = new SimpleStringProperty(e);
            details = new SimpleStringProperty(d);
        }
        public SimpleStringProperty typeProperty()    { return type; }
        public SimpleStringProperty elementProperty() { return element; }
        public SimpleStringProperty detailsProperty() { return details; }

        public static DeltaRow fromLine(String line) {
            if (line == null) return new DeltaRow("INFO", "-", "");
            String l = line.trim();
            if (l.startsWith("ADD "))    return new DeltaRow("ADD",    elementFromRest(l.substring(4)),  l.substring(4));
            if (l.startsWith("DELETE ")) return new DeltaRow("DELETE", elementFromRest(l.substring(7)),  l.substring(7));
            if (l.startsWith("CHANGE ")) return new DeltaRow("CHANGE", elementFromRest(l.substring(7)),  l.substring(7));
            if (l.startsWith("RENAME ")) return new DeltaRow("RENAME", elementFromRest(l.substring(7)),  l.substring(7));
            if (l.startsWith("INFO"))    return new DeltaRow("INFO",   "-", l);
            return new DeltaRow("DELTA", "-", l);
        }
        private static String elementFromRest(String rest) {
            String[] parts = rest.split("\\s+");
            return parts.length >= 2 ? parts[0] + " " + parts[1] : rest;
        }
    }

    private static final class EcoreInfo {
        String fileName, nsURI;
        final Map<String, EClassInfo> classes    = new TreeMap<>();
        final Map<String, String>     attributes = new TreeMap<>();
        final Map<String, ERefInfo>   references = new TreeMap<>();
    }

    private static final class EClassInfo {
        String name;
        boolean isAbstract = false;
        final Set<String> superTypes = new TreeSet<>();
    }

    private static final class ERefInfo {
        String name, eType, upperBound, containment;
    }

    private static final class DeltaReport {
        String m1File, m2File;
        boolean nsUriChanged;
        int addedClasses, removedClasses, addedAttributes, removedAttributes,
            typeChanges, addedReferences, removedReferences, multiplicityChanges,
            containmentChanges, abstractChanges, superTypeChanges;
        final List<String> lines = new ArrayList<>();
    }
}
