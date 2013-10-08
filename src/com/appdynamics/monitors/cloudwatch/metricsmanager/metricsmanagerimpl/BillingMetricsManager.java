package com.appdynamics.monitors.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;

import java.util.Map;

public final class BillingMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/Billing";

    /**
     * Gather metrics for AWS/Billing
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "Currency", "ServiceName");
    }

    /**
     * Print metrics for AWS/Billing
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/Billing
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "ServiceName");
    }
}
