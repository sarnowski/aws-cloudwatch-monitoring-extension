package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

import java.util.Map;

public final class ElasticMapReduceMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/ElasticMapReduce";

    public ElasticMapReduceMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    /**
     * Gather metrics for AWS/ElasticMapReduce
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "JobFlowId");
    }

    /**
     * Print metrics for AWS/ElasticMapReduce
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/ElasticMapReduce
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "JobFlowId");
    }
}
