package com.appdynamics.monitors.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;
import java.util.Map;

public final class EBSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/EBS";

    /**
     * Gather metrics for AWS/EBS
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "VolumeId");
    }

    /**
     * Print metrics for AWS/EBS
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/EBS
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "VolumeId");
    }
}
