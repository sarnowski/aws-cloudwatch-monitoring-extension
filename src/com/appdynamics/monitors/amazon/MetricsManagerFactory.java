package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;

import java.util.List;
import java.util.Map;

public class MetricsManagerFactory {

    private MetricsManager metricsManager;
    private AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    private AmazonCloudWatch amazonCloudWatch;
    private Map<String,List<String>> disabledMetrics;

    public MetricsManagerFactory(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
    }

    public MetricsManager createMetricsManager(String namespace) {
        if (namespace.equals("AWS/EC2")){
            metricsManager = new EC2MetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/AutoScaling")) {
            metricsManager = new AutoScalingMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/EBS")) {
            metricsManager = new EBSMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/ELB")) {
            metricsManager = new ELBMetricsManager(amazonCloudWatchMonitor);
        }

        metricsManager.initialize();
        return metricsManager;
    }
}
