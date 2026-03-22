package com.coevolution.analyzer.dataset;

import com.coevolution.analyzer.features.FeatureVector;
import java.util.ArrayList;
import java.util.List;

public class DatasetBuilder {

    private final List<FeatureVector> vectors = new ArrayList<>();

    public void add(FeatureVector fv) {
        if (fv != null && fv.getLabel() != null
                && !fv.getLabel().equals("UNKNOWN")) {
            vectors.add(fv);
        }
    }

    public void addAll(List<FeatureVector> list) {
        for (FeatureVector fv : list) add(fv);
    }

    public List<FeatureVector> build() {
        System.out.println("[DatasetBuilder] Dataset construit : "
                + vectors.size() + " paires valides");
        return new ArrayList<>(vectors);
    }

    public int size()     { return vectors.size(); }
    public void clear()   { vectors.clear(); }

    public static void main(String[] args) {
        System.out.println("=== DatasetBuilder Test ===");
        DatasetBuilder builder = new DatasetBuilder();
        builder.add(new FeatureVector("pair_1",
            new double[]{3,4,1,1,0,9,12,3,1,0,0,3,3,0,0,0,0,0,0,0,0},
            "ECLASS_ADDED"));
        builder.add(new FeatureVector("pair_2",
            new double[]{4,3,-1,0,1,9,9,0,0,0,0,3,3,0,0,0,0,0,0,0,0},
            "ECLASS_REMOVED"));
        builder.add(null);
        List<FeatureVector> dataset = builder.build();
        System.out.println("  Total : " + dataset.size());
    }
}
