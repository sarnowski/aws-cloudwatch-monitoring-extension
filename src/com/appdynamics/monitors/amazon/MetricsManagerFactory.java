package com.appdynamics.monitors.amazon;

public class MetricsManagerFactory {

    private MetricsManager metricsManager;
    private AmazonCloudWatchMonitor amazonCloudWatchMonitor;

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
        else if (namespace.equals("AWS/ElastiCache")) {
            metricsManager = new ElastiCacheMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/Redshift")) {
            metricsManager = new RedshiftMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/DynamoDB")) {
            metricsManager = new DynamoDBMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/RDS")) {
            metricsManager = new RDSMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/Route53")) {
            metricsManager = new Route53MetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/SQS")) {
            metricsManager = new SQSMetricsManager(amazonCloudWatchMonitor);
        }
        else if (namespace.equals("AWS/ElasticMapReduce")) {
            metricsManager = new ElasticMapReduceMetricsManager(amazonCloudWatchMonitor);
        }
        else {
            throw new UnsupportedOperationException();
        }

        metricsManager.initialize();
        return metricsManager;
    }
}
