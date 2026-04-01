package com.coevolution.dataset.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DatasetBalancer
 *
 * Balances the ML dataset by oversampling minority classes
 * or undersampling majority classes.
 *
 * Semaine 4 - Dataset ML
 */
public class DatasetBalancer {

    public enum Strategy {
        OVERSAMPLE,
        UNDERSAMPLE,
        HYBRID
    }

    private final Strategy strategy;
    private final Random   random;

    public DatasetBalancer(Strategy strategy) {
        this.strategy = strategy;
        this.random   = new Random(42);
    }

    public DatasetBalancer() {
        this(Strategy.OVERSAMPLE);
    }

    public List<DatasetBuilder.DatasetEntry> balance(
            List<DatasetBuilder.DatasetEntry> entries) {

        if (entries == null || entries.isEmpty()) return new ArrayList<>();

        Map<String, List<DatasetBuilder.DatasetEntry>> byLabel = groupByLabel(entries);

        switch (strategy) {
            case OVERSAMPLE:  return oversample(byLabel);
            case UNDERSAMPLE: return undersample(byLabel);
            case HYBRID:      return hybrid(byLabel);
            default:          return new ArrayList<>(entries);
        }
    }

    private List<DatasetBuilder.DatasetEntry> oversample(
            Map<String, List<DatasetBuilder.DatasetEntry>> byLabel) {

        int maxSize = byLabel.values().stream()
            .mapToInt(List::size).max().orElse(0);

        List<DatasetBuilder.DatasetEntry> result = new ArrayList<>();
        for (List<DatasetBuilder.DatasetEntry> classEntries : byLabel.values()) {
            result.addAll(classEntries);
            int toAdd = maxSize - classEntries.size();
            for (int i = 0; i < toAdd; i++) {
                result.add(classEntries.get(random.nextInt(classEntries.size())));
            }
        }
        Collections.shuffle(result, random);
        return result;
    }

    private List<DatasetBuilder.DatasetEntry> undersample(
            Map<String, List<DatasetBuilder.DatasetEntry>> byLabel) {

        int minSize = byLabel.values().stream()
            .mapToInt(List::size).min().orElse(0);

        List<DatasetBuilder.DatasetEntry> result = new ArrayList<>();
        for (List<DatasetBuilder.DatasetEntry> classEntries : byLabel.values()) {
            List<DatasetBuilder.DatasetEntry> shuffled = new ArrayList<>(classEntries);
            Collections.shuffle(shuffled, random);
            result.addAll(shuffled.subList(0, Math.min(minSize, shuffled.size())));
        }
        Collections.shuffle(result, random);
        return result;
    }

    private List<DatasetBuilder.DatasetEntry> hybrid(
            Map<String, List<DatasetBuilder.DatasetEntry>> byLabel) {

        List<Integer> sizes = new ArrayList<>();
        for (List<DatasetBuilder.DatasetEntry> v : byLabel.values())
            sizes.add(v.size());
        Collections.sort(sizes);
        int target = sizes.get(sizes.size() / 2);

        List<DatasetBuilder.DatasetEntry> result = new ArrayList<>();
        for (List<DatasetBuilder.DatasetEntry> classEntries : byLabel.values()) {
            List<DatasetBuilder.DatasetEntry> shuffled = new ArrayList<>(classEntries);
            Collections.shuffle(shuffled, random);

            if (shuffled.size() >= target) {
                result.addAll(shuffled.subList(0, target));
            } else {
                result.addAll(shuffled);
                int toAdd = target - shuffled.size();
                for (int i = 0; i < toAdd; i++) {
                    result.add(shuffled.get(random.nextInt(shuffled.size())));
                }
            }
        }
        Collections.shuffle(result, random);
        return result;
    }

    private Map<String, List<DatasetBuilder.DatasetEntry>> groupByLabel(
            List<DatasetBuilder.DatasetEntry> entries) {
        Map<String, List<DatasetBuilder.DatasetEntry>> map = new HashMap<>();
        for (DatasetBuilder.DatasetEntry e : entries) {
            map.computeIfAbsent(e.getLabel(), k -> new ArrayList<>()).add(e);
        }
        return map;
    }

    public Map<String, Integer> getDistribution(
            List<DatasetBuilder.DatasetEntry> entries) {
        Map<String, Integer> dist = new HashMap<>();
        for (DatasetBuilder.DatasetEntry e : entries) {
            dist.merge(e.getLabel(), 1, Integer::sum);
        }
        return dist;
    }

    public Strategy getStrategy() { return strategy; }
}