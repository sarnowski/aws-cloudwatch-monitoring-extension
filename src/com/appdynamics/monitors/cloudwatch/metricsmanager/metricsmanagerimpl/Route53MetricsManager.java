package com.appdynamics.monitors.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;

import java.util.Map;

public final class Route53MetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/Route53";

    /**
     * Gather metrics for AWS/Route53
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "HealthCheckId");
    }

    /**
     * Print metrics for AWS/Route53
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/Route53
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "HealthCheckId");
    }
}
