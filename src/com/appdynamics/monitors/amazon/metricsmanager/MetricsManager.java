package com.appdynamics.monitors.amazon.metricsmanager;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.*;

public abstract class MetricsManager{

    private Logger logger = Logger.getLogger(this.getClass().getName());
    protected AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    protected AmazonCloudWatch awsCloudWatch;
    protected Map<String,HashSet<String>> disabledMetrics;

    public MetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
    }

    public void initialize() {
        this.awsCloudWatch = amazonCloudWatchMonitor.getAmazonCloudWatch();
        this.disabledMetrics = amazonCloudWatchMonitor.getDisabledMetrics();
    }

    public abstract Object gatherMetrics();
    public abstract void printMetrics(Object metrics);
    public abstract String getNamespacePrefix();

    protected GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                       String metricName,
                                                                       String statisticsType,
                                                                       List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000000000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

    protected List<Metric> getMetrics(String namespace, String... filterNames) {
        ListMetricsRequest request = new ListMetricsRequest();
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();

        for (String filterName : filterNames) {
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.withName(filterName);
            filters.add(dimensionFilter);
        }

        request.withNamespace(namespace);
        request.withDimensions(filters);

        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        return listMetricsResult.getMetrics();
    }

    protected Object gatherMetricsHelper(String namespace, String...filterNames) {
        HashMap<String, HashMap<String,List<Datapoint>>> metrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
        List<Metric> metricsList = getMetrics(namespace, filterNames);

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String key = dimensions.get(0).getValue();
            if (!metrics.containsKey(key)) {
                metrics.put(key, new HashMap<String, List<Datapoint>>());
            }
            if (!metrics.get(key).containsKey(metric.getMetricName())) {
                if (!amazonCloudWatchMonitor.isMetricDisabled(namespace, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(namespace, metric.getMetricName(), "Average", metric.getDimensions());
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    metrics.get(key).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return metrics;
    }
    protected void printMetricsHelper(Object metricsMap, String prefix) {
        HashMap<String, HashMap<String,List<Datapoint>>> metrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metricsMap;
        Iterator outerIterator = metrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String id = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = metrics.get(id);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(prefix, id + "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }
    protected String getNamespacePrefixHelper(String namespace, String id){
        StringBuilder sb = new StringBuilder(namespace.substring(4,namespace.length()));
        sb.append("|").append(id).append("|");
        return sb.toString();
    }
}
