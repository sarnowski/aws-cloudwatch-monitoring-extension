package com.appdynamics.monitors.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;

import java.util.Map;

public final class StorageGatewayMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/StorageGateway";

    /**
     * Gather metrics for AWS/StorageGateway
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "GatewayId");
    }

    /**
     * Print metrics for AWS/StorageGateway
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/StorageGateway
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "GatewayId");
    }
}
