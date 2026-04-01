package com.coevolution.analyzer.dataset;

import com.coevolution.analyzer.features.FeatureVector;
import java.util.*;

public class DatasetBalancer {

    private final int targetMin;

    public DatasetBalancer(int targetMin) {
        this.targetMin = targetMin;
    }

    public List<FeatureVector> balance(List<FeatureVector> dataset) {
        Map<String, List<FeatureVector>> byLabel = new LinkedHashMap<>();
        for (FeatureVector fv : dataset) {
            byLabel.computeIfAbsent(fv.getLabel(), k -> new ArrayList<>()).add(fv);
        }

        System.out.println("[DatasetBalancer] Distribution avant :");
        byLabel.forEach((label, list) ->
            System.out.println("  " + label + " : " + list.size()));

        List<FeatureVector> balanced = new ArrayList<>();
        Random rng = new Random(42);

        for (Map.Entry<String, List<FeatureVector>> entry : byLabel.entrySet()) {
            List<FeatureVector> list = entry.getValue();
            balanced.addAll(list);

            int needed = targetMin - list.size();
            if (needed > 0) {
                for (int i = 0; i < needed; i++) {
                    FeatureVector src = list.get(rng.nextInt(list.size()));
                    double[] vals = src.getValues().clone();
                    for (int j = 0; j < vals.length; j++) {
                        vals[j] = Math.max(0,
                            vals[j] + (rng.nextGaussian() * 0.3));
                    }
                    balanced.add(new FeatureVector(
                        "synth_" + entry.getKey() + "_" + i,
                        vals, entry.getKey()));
                }
            }
        }

        Collections.shuffle(balanced, new Random(42));
        System.out.println("[DatasetBalancer] Distribution apres : "
                + balanced.size() + " paires");
        return balanced;
    }

    public static void main(String[] args) {
        System.out.println("=== DatasetBalancer Test ===");
        DatasetBalancer balancer = new DatasetBalancer(10);
        List<FeatureVector> dataset = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            dataset.add(new FeatureVector("p" + i,
                new double[]{1,2,1,1,0,3,4,1,1,0,0,1,1,0,0,0,0,0,0,0,0},
                "ECLASS_ADDED"));
        for (int i = 0; i < 2; i++)
            dataset.add(new FeatureVector("q" + i,
                new double[]{2,1,-1,0,1,3,3,0,0,0,0,1,1,0,0,0,0,0,0,0,0},
                "ECLASS_REMOVED"));
        List<FeatureVector> result = balancer.balance(dataset);
        System.out.println("  Total apres balance : " + result.size());
    }
}

