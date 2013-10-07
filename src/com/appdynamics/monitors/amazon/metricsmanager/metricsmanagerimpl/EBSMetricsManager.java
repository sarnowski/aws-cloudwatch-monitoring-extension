package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;
import org.apache.log4j.Logger;

import java.util.Map;

public final class EBSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/EBS";
    private Logger logger = Logger.getLogger(this.getClass().getName());

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
