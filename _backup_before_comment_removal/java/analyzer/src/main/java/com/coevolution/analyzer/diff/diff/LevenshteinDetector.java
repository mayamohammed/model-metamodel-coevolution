package com.coevolution.analyzer.diff.diff;

import com.coevolution.analyzer.diff.comparator.MetamodelComparator;
import com.coevolution.analyzer.diff.model.DiffDelta;
import java.util.*;

public class LevenshteinDetector {
    public List<DiffDelta> detectAttributeRenames(List<DiffDelta> deltas,
            MetamodelComparator.Snapshot s1, MetamodelComparator.Snapshot s2, double threshold) {
        List<DiffDelta> result = new ArrayList<>(deltas);
        List<DiffDelta> deleted = new ArrayList<>(), added = new ArrayList<>();
        for (DiffDelta d : deltas) {
            if (d.getKind() == DiffDelta.Kind.DELETE && d.getElementType() == DiffDelta.ElementType.ATTRIBUTE) deleted.add(d);
            if (d.getKind() == DiffDelta.Kind.ADD    && d.getElementType() == DiffDelta.ElementType.ATTRIBUTE) added.add(d);
        }
        for (DiffDelta del : deleted) {
            for (DiffDelta add : added) {
                String n1 = del.getElement().contains("::") ? del.getElement().split("::")[1] : del.getElement();
                String n2 = add.getElement().contains("::") ? add.getElement().split("::")[1] : add.getElement();
                if (similarity(n1, n2) >= threshold) {
                    result.remove(del); result.remove(add);
                    result.add(new DiffDelta(DiffDelta.Kind.RENAME, DiffDelta.ElementType.ATTRIBUTE,
                        del.getElement(), "rename: " + del.getElement() + " -> " + add.getElement()));
                    break;
                }
            }
        }
        return result;
    }
    private double similarity(String a, String b) {
        int d = levenshtein(a, b);
        return 1.0 - (double) d / Math.max(a.length(), b.length());
    }
    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length()+1][b.length()+1];
        for (int i=0;i<=a.length();i++) dp[i][0]=i;
        for (int j=0;j<=b.length();j++) dp[0][j]=j;
        for (int i=1;i<=a.length();i++)
            for (int j=1;j<=b.length();j++)
                dp[i][j] = a.charAt(i-1)==b.charAt(j-1) ? dp[i-1][j-1]
                    : 1+Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        return dp[a.length()][b.length()];
    }
}