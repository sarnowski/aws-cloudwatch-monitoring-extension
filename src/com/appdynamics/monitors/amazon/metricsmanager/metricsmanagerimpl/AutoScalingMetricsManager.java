package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

public final class AutoScalingMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/AutoScaling";

    public AutoScalingMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "AutoScalingGroupName");
    }

    @Override
    public void printMetrics(Object metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "GroupId");
    }
}
