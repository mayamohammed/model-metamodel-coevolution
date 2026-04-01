package com.coevolution.cli.commands;

import com.coevolution.predictor.PredictionService;
import com.coevolution.predictor.PredictionService.FeatureVector;
import com.coevolution.predictor.PredictionService.PredictionResult;

public class PredictCommand {

    private final PredictionService service;

    public PredictCommand(String apiUrl) {
        this.service = new PredictionService(apiUrl);
    }

    public void execute(String[] args) {
        System.out.println("[predict] Checking API health ...");

        if (!service.isApiHealthy()) {
            System.err.println("[ERROR] Flask API not reachable.");
            System.err.println("  Start API: cd python/api && python app.py");
            System.exit(1);
        }
        System.out.println("[predict] API OK");

        FeatureVector fv = parseArgs(args);
        System.out.println("[predict] Features: " + fv);

        PredictionResult result = service.predict(fv);

        if (result.isError()) {
            System.err.println("[ERROR] " + result.getError());
            System.exit(1);
        }

        printResult(result);
    }

    private FeatureVector parseArgs(String[] args) {
        FeatureVector fv = new FeatureVector();
        for (int i = 1; i < args.length - 1; i += 2) {
            String flag = args[i].toLowerCase();
            int    val  = 0;
            try { val = Integer.parseInt(args[i + 1]); }
            catch (NumberFormatException e) {
                System.err.println("[WARN] Invalid value for " + flag + ": " + args[i+1]);
                continue;
            }
            switch (flag) {
                case "--added-classes":       fv.setNbAddedClasses(val);       break;
                case "--removed-classes":     fv.setNbRemovedClasses(val);     break;
                case "--added-attributes":    fv.setNbAddedAttributes(val);    break;
                case "--removed-attributes":  fv.setNbRemovedAttributes(val);  break;
                case "--type-changes":        fv.setNbTypeChanges(val);        break;
                case "--added-references":    fv.setNbAddedReferences(val);    break;
                case "--removed-references":  fv.setNbRemovedReferences(val);  break;
                case "--multiplicity-changes":fv.setNbMultiplicityChanges(val);break;
                case "--containment-changes": fv.setNbContainmentChanges(val); break;
                case "--abstract-changes":    fv.setNbAbstractChanges(val);    break;
                case "--supertype-changes":   fv.setNbSupertypeChanges(val);   break;
                case "--nsuri-changed":       fv.setNsUriChanged(val);         break;
                default:
                    System.err.println("[WARN] Unknown flag: " + flag);
            }
        }
        return fv;
    }

    private void printResult(PredictionResult r) {
        System.out.println("=" .repeat(50));
        System.out.println("  PREDICTION RESULT");
        System.out.println("=" .repeat(50));
        System.out.printf ("  Label      : %s%n", r.getPrediction());
        System.out.printf ("  Confidence : %.2f%%%n", r.getConfidencePct());
        if (r.getProbabilities() != null) {
            System.out.println("  Top probs  :");
            r.getProbabilities().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .forEach(e -> System.out.printf(
                    "    %-40s : %.2f%%%n", e.getKey(), e.getValue() * 100));
        }
        System.out.println("=" .repeat(50));
    }
}