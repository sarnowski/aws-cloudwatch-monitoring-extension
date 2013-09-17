package main.java;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.management.MBeanAttributeInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

    private static final String AWS_ACCESS_KEY = "accessKey";
    private static final String AWS_SECRET_KEY = "secretKey";
    private Logger logger = Logger.getLogger(this.getClass().getName());

    // The AWS Cloud Watch client that retrieves instance metrics by executing requests
    private AmazonCloudWatch awsCloudWatch;
    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    private Set<String> disabledMetrics = new HashSet<String>();

    public AmazonCloudWatchMonitor() {
        setDisabledMetrics();
        initCloudWatchClient();
    }

    /**
     * Set list of disabled metrics from xml file
     * @return
     */
    private void setDisabledMetrics() {
        try {
            File fXmlFile = new File("conf/DisabledMetrics.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nList = doc.getElementsByTagName("MetricName");

            for (int i = 0; i < nList.getLength(); i++) {
                disabledMetrics.add(nList.item(i).getTextContent());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize Amazon Cloud Watch Client
     * @return
     */
    private void initCloudWatchClient() {
        Properties awsProperties = new Properties();
        try {
            awsProperties.load(new FileInputStream("conf/AwsCredentials.properties"));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        AWSCredentials awsCredentials = new BasicAWSCredentials(awsProperties.getProperty(AWS_ACCESS_KEY), awsProperties.getProperty(AWS_SECRET_KEY));
        awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> stringStringMap, TaskExecutionContext taskExecutionContext) {
        // gather metrics
        HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics = gatherInstanceMetrics();
        // print metrics to controller
        printMetrics(cloudWatchMetrics);

        return new TaskOutput("AWS Cloud Watch Metric Upload Complete");
    }

    /**
     * Gather metrics for every instance and group by instanceId. Populates global map called cloudWatchMetrics
     */
    private HashMap<String, HashMap<String, List<Datapoint>>> gatherInstanceMetrics() {
        // The outer hashmap has instanceIds as keys and inner hashmaps as the value.
        // The inner hashmaps have metric names as keys and lists of corresponding data points as the values
        HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter instanceIdFilter = new DimensionFilter();
        instanceIdFilter.setName("InstanceId");
        filter.add(instanceIdFilter);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = instanceMetricsResult.getMetrics();

        for (com.amazonaws.services.cloudwatch.model.Metric m : instanceMetrics) {
            List<Dimension> dimensions = m.getDimensions();
            for (Dimension dimension : dimensions) {
                if (!cloudWatchMetrics.containsKey(dimension.getValue())) {
                    cloudWatchMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }
                gatherInstanceMetricsHelper(m, dimension, cloudWatchMetrics);
            }
        }
        return cloudWatchMetrics;
    }

    private void gatherInstanceMetricsHelper(com.amazonaws.services.cloudwatch.model.Metric metric,
                                             Dimension dimension,
                                             HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics) {
        if (disabledMetrics.contains(metric.getMetricName())) {
            return;
        }
        GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(metric);
        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
        cloudWatchMetrics.get(dimension.getValue()).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
    }

    private GetMetricStatisticsRequest createGetMetricStatisticsRequest(com.amazonaws.services.cloudwatch.model.Metric m) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000000000))
                .withNamespace("AWS/EC2")
                .withPeriod(60 * 60)
                .withMetricName(m.getMetricName())
                .withStatistics("Average")
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    public void printMetric(String metricName, double metricValue, String aggregation, String timeRollup, String cluster)
    {
        try{
            MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName,
                    aggregation,
                    timeRollup,
                    cluster
            );

            metricWriter.printMetric(String.valueOf((long) metricValue));
        } catch (NullPointerException e){
            logger.info("NullPointerException: " + e.getMessage());
        }
    }

    /**
     * Metric Prefix
     * @return	String
     */
    private String getMetricPrefix()
    {
        return "Custom Metrics|Amazon Cloud Watch|Status|";
    }

    private void printMetrics(HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics) {
        Iterator outerIterator = cloudWatchMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String instanceId = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = cloudWatchMetrics.get(instanceId);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                Datapoint data = metricStatistics.get(metricName).get(0);
                printMetric(instanceId + "|" + metricName + "(" + data.getUnit() + ")", data.getAverage(),
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }
}
