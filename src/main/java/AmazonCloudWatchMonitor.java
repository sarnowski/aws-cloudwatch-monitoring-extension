package main.java;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.controller.api.dto.Metric;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

    //private static final String AWS_ACCESS_KEYID = "AKIAJTB7DYHGUBXOS7BQ";
    //private static final String AWS_SECRET_KEY = "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";
    private static final String AWS_ACCESS_KEY = "accessKey";
    private static final String AWS_SECRET_KEY = "secretKey";
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private HashMap<String,Metric> cloudWatchMetrics = new HashMap<String, Metric>();
    private AmazonCloudWatch awsCloudWatch = null;
    private static Set<String> disabledMetrics = new HashSet<String>();

    public AmazonCloudWatchMonitor() {
        setDisabledMetrics();
        initCloudWatchClient();
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> stringStringMap, TaskExecutionContext taskExecutionContext) {
        //TODO: create map of params

        //TODO: create signed request using those params
            // String request = signedRequestHelper.sign(params);
        //TODO: fire request

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void populate() {
        //cloudWatchMetrics.put("CPUUtilization",)
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
}
