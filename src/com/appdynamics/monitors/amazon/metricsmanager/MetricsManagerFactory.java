package com.appdynamics.monitors.amazon.metricsmanager;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl.*;

public final class MetricsManagerFactory {

    private static final String AWS_EC2_NAMESPACE = "AWS/EC2";
    private static final String AWS_AUTOSCALING_NAMESPACE = "AWS/AutoScaling";
    private static final String AWS_EBS_NAMESPACE = "AWS/EBS";
    private static final String AWS_ELB_NAMESPACE = "AWS/ELB";
    private static final String AWS_ELASTI_CACHE_NAMESPACE = "AWS/ElastiCache";
    private static final String AWS_REDSHIFT_NAMESPACE = "AWS/Redshift";
    private static final String AWS_DYNAMO_DB_NAMESPACE = "AWS/DynamoDB";
    private static final String AWS_RDS_NAMESPACE = "AWS/RDS";
    private static final String AWS_ROUTE53_NAMESPACE = "AWS/Route53";
    private static final String AWS_SQS_NAMESPACE = "AWS/SQS";
    private static final String AWS_ELASTIC_MAP_REDUCE_NAMESPACE = "AWS/ElasticMapReduce";
    private static final String AWS_STORAGE_GATEWAY_NAMESPACE = "AWS/StorageGateway";
    private static final String AWS_OPS_WORKS_NAMESPACE = "AWS/OpsWorks";
    private static final String AWS_SNS_NAMESPACE = "AWS/SNS";
    private static final String AWS_BILLING_NAMESPACE = "AWS/Billing";

    private MetricsManager metricsManager;
    private AmazonCloudWatchMonitor amazonCloudWatchMonitor;

    public MetricsManagerFactory(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
    }

    /**
     * Creates an instance of a MetricsManager specific to a namespace
     * @param   namespace       The name of the namespace
     * @return	MetricsManager  An instance of MetricsManager specific to the namespace
     */
    public MetricsManager createMetricsManager(String namespace) {
        if (namespace.equals(AWS_EC2_NAMESPACE)){
            metricsManager = new EC2MetricsManager();
        }
        else if (namespace.equals(AWS_AUTOSCALING_NAMESPACE)) {
            metricsManager = new AutoScalingMetricsManager();
        }
        else if (namespace.equals(AWS_EBS_NAMESPACE)) {
            metricsManager = new EBSMetricsManager();
        }
        else if (namespace.equals(AWS_ELB_NAMESPACE)) {
            metricsManager = new ELBMetricsManager();
        }
        else if (namespace.equals(AWS_ELASTI_CACHE_NAMESPACE)) {
            metricsManager = new ElastiCacheMetricsManager();
        }
        else if (namespace.equals(AWS_REDSHIFT_NAMESPACE)) {
            metricsManager = new RedshiftMetricsManager();
        }
        else if (namespace.equals(AWS_DYNAMO_DB_NAMESPACE)) {
            metricsManager = new DynamoDBMetricsManager();
        }
        else if (namespace.equals(AWS_RDS_NAMESPACE)) {
            metricsManager = new RDSMetricsManager();
        }
        else if (namespace.equals(AWS_ROUTE53_NAMESPACE)) {
            metricsManager = new Route53MetricsManager();
        }
        else if (namespace.equals(AWS_SQS_NAMESPACE)) {
            metricsManager = new SQSMetricsManager();
        }
        else if (namespace.equals(AWS_ELASTIC_MAP_REDUCE_NAMESPACE)) {
            metricsManager = new ElasticMapReduceMetricsManager();
        }
        else if (namespace.equals(AWS_STORAGE_GATEWAY_NAMESPACE)) {
            metricsManager = new StorageGatewayMetricsManager();
        }
        else if (namespace.equals(AWS_OPS_WORKS_NAMESPACE)) {
            metricsManager = new OpsWorksMetricsManager();
        }
        else if (namespace.equals(AWS_SNS_NAMESPACE)) {
            metricsManager = new SNSMetricsManager();
        }
        else if (namespace.equals(AWS_BILLING_NAMESPACE)) {
            metricsManager = new BillingMetricsManager();
        }
        else {
            throw new UnsupportedOperationException();
        }

        metricsManager.initialize(amazonCloudWatchMonitor);
        return metricsManager;
    }
}
