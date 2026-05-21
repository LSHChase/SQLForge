package com.company.sqloptimization.domain.rewrite.cost;

import java.util.LinkedHashMap;
import java.util.Map;

public class RewriteCostVector {

    private final double scanCost;
    private final double shuffleCost;
    private final double computeCost;
    private final double memoryCost;
    private final double weightedCost;

    public RewriteCostVector(double scanCost,
                             double shuffleCost,
                             double computeCost,
                             double memoryCost,
                             double weightedCost) {
        this.scanCost = scanCost;
        this.shuffleCost = shuffleCost;
        this.computeCost = computeCost;
        this.memoryCost = memoryCost;
        this.weightedCost = weightedCost;
    }

    public boolean dominates(RewriteCostVector other) {
        if (other == null) {
            return false;
        }
        boolean noWorse = scanCost <= other.scanCost
            && shuffleCost <= other.shuffleCost
            && computeCost <= other.computeCost
            && memoryCost <= other.memoryCost;
        boolean strictlyBetter = scanCost < other.scanCost
            || shuffleCost < other.shuffleCost
            || computeCost < other.computeCost
            || memoryCost < other.memoryCost;
        return noWorse && strictlyBetter;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scanCost", Double.valueOf(scanCost));
        result.put("shuffleCost", Double.valueOf(shuffleCost));
        result.put("computeCost", Double.valueOf(computeCost));
        result.put("memoryCost", Double.valueOf(memoryCost));
        result.put("weightedCost", Double.valueOf(weightedCost));
        return result;
    }

    public double getScanCost() {
        return scanCost;
    }

    public double getShuffleCost() {
        return shuffleCost;
    }

    public double getComputeCost() {
        return computeCost;
    }

    public double getMemoryCost() {
        return memoryCost;
    }

    public double getWeightedCost() {
        return weightedCost;
    }
}
