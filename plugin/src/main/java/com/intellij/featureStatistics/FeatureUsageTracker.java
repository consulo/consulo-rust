package com.intellij.featureStatistics;
public final class FeatureUsageTracker {
    public static FeatureUsageTracker getInstance() { return new FeatureUsageTracker(); }
    public void triggerFeatureUsed(String featureId) {}
    public void triggerFeatureShown(String featureId) {}
}
