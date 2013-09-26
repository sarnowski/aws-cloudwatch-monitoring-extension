package com.appdynamics.monitors.amazon;

import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClient;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestClass {
    private static final String UTF8_CHARSET = "UTF-8";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String REQUEST_URI = "/onca/xml";
    private static final String REQUEST_METHOD = "GET";
    private static final String ENDPOINT = "monitoring.amazonaws.com";
    private static final String AWS_ACCESS_KEYID = "AKIAJTB7DYHGUBXOS7BQ";
    private static final String AWS_SECRET_KEY = "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";

    private SecretKeySpec secretKeySpec = null;
    private Mac mac = null;

    public TestClass() {
    }

    private static HashMap<String,HashSet<String>> disabledMetrics = new HashMap<String,HashSet<String>>();


    public static void main(String[] args) {
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJTB7DYHGUBXOS7BQ", "jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc");
        AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
        //setNamespaces();
        //setDisabledMetrics();
        //boolean result = isMetricDisabled("AWS/EC22", "CPUUtilization2");
        //getEBSMetrics(awsCloudWatch);
        //getELBMetrics(awsCloudWatch, awsCredentials);
        //getEC2InstanceMetrics(awsCloudWatch);
        //getElasticCacheClusterMetrics(awsCloudWatch, awsCredentials);
        //readConfigurations("conf/AWSConfigurations.xml");
        gatherRedshiftMetrics(awsCloudWatch);
    }
    private static void setNamespaces() {
        try {
            HashSet<String> namespaces = new HashSet<String>();
            File fXmlFile = new File("conf/AvailableNamespaces.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nList = doc.getElementsByTagName("Namespace");
            for (int i = 0; i < nList.getLength(); i++) {
//              String namespaceKey = nList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
//                String metricName = nList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
//                if (!disabledMetrics.containsKey(namespaceKey)) {
//                    disabledMetrics.put(namespaceKey, new HashSet<String>());
//                }
//                disabledMetrics.get(namespaceKey).add(metricName);

            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void setDisabledMetrics() {
        try {
            File fXmlFile = new File("conf/DisabledMetrics.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nList = doc.getElementsByTagName("Metric");
            for (int i = 0; i < nList.getLength(); i++) {
                String namespaceKey = nList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
                String metricName = nList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
                if (!disabledMetrics.containsKey(namespaceKey)) {
                    disabledMetrics.put(namespaceKey, new HashSet<String>());
                }
                disabledMetrics.get(namespaceKey).add(metricName);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private static void readConfigurations(String filePath) {
        try {
            File configFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            Element awsCredentials = (Element)doc.getElementsByTagName("AWSCredentials").item(0);
            Element namespaces = (Element)doc.getElementsByTagName("SupportedNamespaces").item(0);
            Element disabledMetrics = (Element)doc.getElementsByTagName("DisabledMetrics").item(0);

            System.out.println(awsCredentials.getElementsByTagName("AccessKey"));

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printDisabledMetrics(Set<String> disabledMetrics) {
        for (String s : disabledMetrics) {
            System.out.println(s);
        }
    }

    private static void print(Object s) {
        System.out.println(s);
    }

    private static GetMetricStatisticsRequest createGetMetricStatisticsRequest(Metric m) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000000000))
                .withNamespace("AWS/EC2")
                .withPeriod(60 * 60)
                .withMetricName(m.getMetricName())
                .withStatistics("Average")
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
        //GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
    }

    private static void getEBSMetrics(AmazonCloudWatch awsCloudWatch) {
        HashMap<String, HashMap<String, List<Datapoint>>> ebsMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter volumeIdFilter = new DimensionFilter();
        volumeIdFilter.setName("VolumeId");
        filter.add(volumeIdFilter);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult ebsMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> ebsMetricsResultMetrics = ebsMetricsResult.getMetrics();

        for (com.amazonaws.services.cloudwatch.model.Metric m : ebsMetricsResultMetrics) {
            List<Dimension> dimensions = m.getDimensions();
            for (Dimension dimension : dimensions) {
                if (!ebsMetrics.containsKey(dimension.getValue())) {
                    ebsMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }

                GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                        .withStartTime( new Date( System.currentTimeMillis() - 10000000))
                        .withNamespace("AWS/EBS")
                        .withPeriod(60 * 60)
                        .withDimensions()
                        //.withDimensions(new Dimension().withName("VolumeId").withValue(dimension.getValue()))
                        .withDimensions(m.getDimensions().get(0))
                        .withMetricName(m.getMetricName())
                        .withStatistics("Average")
                        .withEndTime(new Date());
                GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                ebsMetrics.get(dimension.getValue()).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());

                //gatherInstanceMetricsHelper(m, dimension, ebsMetrics);
            }
        }
    }

    private static void getELBMetrics(AmazonCloudWatch awsCloudWatch, AWSCredentials credentials) {
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
        DimensionFilter nameFilter = new DimensionFilter();
        nameFilter.setName("LoadBalancerName");
        DimensionFilter zoneFilter = new DimensionFilter();
        zoneFilter.setName("AvailabilityZone");
        filters.add(nameFilter);
        //filters.add(zoneFilter);

        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.withNamespace("AWS/ELB");
        listMetricsRequest.setDimensions(filters);
        ListMetricsResult elbMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> elbMetricsList = elbMetricsResult.getMetrics();

        //Top level     -- Key = LoadBalancerName,      Value = HashMap of availability zones
        //Mid level     -- Key = AvailabilityZoneName,  Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> elbMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();

        for (com.amazonaws.services.cloudwatch.model.Metric m : elbMetricsList) {
            List<Dimension> dimensions = m.getDimensions();
            String loadBalancerName = dimensions.get(0).getValue();
            String availabilityZone = dimensions.get(1).getValue();
            if (!elbMetrics.containsKey(loadBalancerName)) {
                elbMetrics.put(loadBalancerName, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            if (!elbMetrics.get(loadBalancerName).containsKey(availabilityZone)) {
                elbMetrics.get(loadBalancerName).put(availabilityZone, new HashMap<String, List<Datapoint>>());
            }
            if (!elbMetrics.get(loadBalancerName).get(availabilityZone).containsKey(m.getMetricName())) {
                GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                        .withStartTime( new Date( System.currentTimeMillis() - 10000000))
                        .withNamespace("AWS/ELB")
                        .withPeriod(60 * 60)
                        .withDimensions()
                        .withMetricName(m.getMetricName())
                        .withStatistics("Average")
                        .withEndTime(new Date());
                GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                elbMetrics.get(loadBalancerName).get(availabilityZone).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());
            }
        }

        Iterator loadBalancerIterator = elbMetrics.keySet().iterator();

        while (loadBalancerIterator.hasNext()) {
            String loadBalancerName = loadBalancerIterator.next().toString();
            HashMap<String, HashMap<String,List<Datapoint>>> availabilityZones = elbMetrics.get(loadBalancerName);
            Iterator zoneIterator = availabilityZones.keySet().iterator();
            while (zoneIterator.hasNext()) {
                String zoneName = zoneIterator.next().toString();
                HashMap<String, List<Datapoint>> metricsMap = availabilityZones.get(zoneName);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && !datapoints.isEmpty()) {
                        Datapoint data = datapoints.get(0);
//                        amazonCloudWatchMonitor.printMetric(getNamespacePrefix() + loadBalancerName + "|" + "Availability Zone|" +  availabilityZones + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
//                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
//                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
//                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }

        System.out.println("Done");

    }

    private static void getEC2InstanceMetrics(AmazonCloudWatch awsCloudWatch) {
        HashMap<String, HashMap<String, List<Datapoint>>> instanceMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter instanceIdFilter = new DimensionFilter();
        instanceIdFilter.setName("InstanceId");
        filter.add(instanceIdFilter);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetricResultList = instanceMetricsResult.getMetrics();

        for (com.amazonaws.services.cloudwatch.model.Metric m : instanceMetricResultList) {
            List<Dimension> dimensions = m.getDimensions();
            for (Dimension dimension : dimensions) {
                if (!instanceMetrics.containsKey(dimension.getValue())) {
                    instanceMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }

                GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                        .withStartTime( new Date( System.currentTimeMillis() - 10000000))
                        .withNamespace("AWS/EBS")
                        .withPeriod(60 * 60)
                        .withDimensions()
                                //.withDimensions(new Dimension().withName("VolumeId").withValue(dimension.getValue()))
                        .withDimensions(m.getDimensions().get(0))
                        .withMetricName(m.getMetricName())
                        .withStatistics("Average")
                        .withEndTime(new Date());
                GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                instanceMetrics.get(dimension.getValue()).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());

                //gatherInstanceMetricsHelper(m, dimension, ebsMetrics);
            }
        }
    }
    private static void getElasticCacheClusterMetrics(AmazonCloudWatch awsCloudWatch, AWSCredentials awsCredentials) {

        DimensionFilter clusterIdFilter = new DimensionFilter();
        clusterIdFilter.setName("CacheClusterId");
        DimensionFilter cacheNodeIdFilter = new DimensionFilter();
        cacheNodeIdFilter.setName("CacheNodeId");
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
        filters.add(clusterIdFilter);
        filters.add(cacheNodeIdFilter);

        ListMetricsRequest request = new ListMetricsRequest();
        //request.withNamespace()
        request.withDimensions(filters);
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        List<Metric> metricsList = listMetricsResult.getMetrics();

        //Top level     -- Key = CacheClusterIds,       Value = HashMap of cache nodes
        //Mid level     -- Key = CacheNodeIds,          Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> cacheClusterMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String loadBalancerName = dimensions.get(0).getValue();
            String availabilityZone = dimensions.get(1).getValue();
            if (!cacheClusterMetrics.containsKey(loadBalancerName)) {
                cacheClusterMetrics.put(loadBalancerName, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            if (!cacheClusterMetrics.get(loadBalancerName).containsKey(availabilityZone)) {
                cacheClusterMetrics.get(loadBalancerName).put(availabilityZone, new HashMap<String, List<Datapoint>>());
            }
            if (!cacheClusterMetrics.get(loadBalancerName).get(availabilityZone).containsKey(metric.getMetricName())) {

//                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
//                    GetMetricStatisticsRequest getMetricStatisticsRequest = amazonCloudWatchMonitor.createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
//                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
//                    elbMetrics.get(loadBalancerName).get(availabilityZone).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
//                }
            }
        }

    }

    private static void gatherRedshiftMetrics(AmazonCloudWatch awsCloudWatch) {
        DimensionFilter NodeIDFilter = new DimensionFilter();

        //NodeIDFilter.setName("NodeID");
        DimensionFilter filter1 = new DimensionFilter();
        filter1.setName("DBInstanceIdentifier");
        DimensionFilter filter2 = new DimensionFilter();
        filter2.setName("EngineName");
       // ClusterIdentifierFilter.setValue("TestTable");
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
        //filters.add(NodeIDFilter);
        filters.add(filter1);
        filters.add(filter2);

        ListMetricsRequest request = new ListMetricsRequest();
        request.withNamespace("AWS/RDS");
        request.withDimensions(filters);
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        List<Metric> metricsList = listMetricsResult.getMetrics();


        System.out.println("done");

    }

    public static boolean isMetricDisabled(String namespace, String metricName) {
        boolean result = false;
        if (disabledMetrics.get(namespace) != null) {
            if (disabledMetrics.get(namespace).contains(metricName)) {
                result = true;
            }
        }
        return result;
    }
}

            /*HashMap<String, HashMap<String, List<Datapoint>>> uniqueInstanceIds = new HashMap<String, HashMap<String,List<Datapoint>>>();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter x = new DimensionFilter();
        x.setName("InstanceId");
        filter.add(x);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        ListMetricsResult allMetricNames = awsCloudWatch.listMetrics();
        List<Metric> instanceMetrics = instanceMetricsResult.getMetrics();
        for (Metric m : instanceMetrics) {
            List<Dimension> dimensions = m.getDimensions();
            for (Dimension dimension : dimensions) {
                if (!uniqueInstanceIds.containsKey(dimension.getValue())) {
                    uniqueInstanceIds.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }
                GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(m);
                GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                uniqueInstanceIds.get(dimension.getValue()).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());


            }
        }
        Iterator iterator = uniqueInstanceIds.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            String value = uniqueInstanceIds.get(key).toString();
            System.out.println(key);
        }  */

//AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(awsCredentials);
//        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups();
//        List<AutoScalingGroup> autoScalingGroupList = describeAutoScalingGroupsResult.getAutoScalingGroups();
//        AutoScalingGroup group = autoScalingGroupList.get(0);
//        EnableMetricsCollectionRequest request = new EnableMetricsCollectionRequest();
//        request.setAutoScalingGroupName(group.getAutoScalingGroupName());
//        request.setGranularity("1Minute");
//        amazonAutoScalingClient.enableMetricsCollection(request);
//        DescribeMetricCollectionTypesResult y = amazonAutoScalingClient.describeMetricCollectionTypes();
//        List<EnabledMetric> enabledMetrics = group.getEnabledMetrics();
//
//        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
//                .withStartTime( new Date( System.currentTimeMillis() - TimeUnit.MINUTES.toMillis( 2 ) ) )
//                .withNamespace("AWS/AutoScaling")
//                .withPeriod(60 * 60)
//                .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(group.getAutoScalingGroupName()))
//                .withMetricName("GroupMaxSize")
//                .withStatistics("Average")
//                .withEndTime(new Date());
//
//
//        AmazonDynamoDBClient x = new AmazonDynamoDBClient();
//
//        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
//        DimensionFilter instanceIdFilter = new DimensionFilter();
//        instanceIdFilter.setName("VolumeId");
//        filter.add(instanceIdFilter);
//        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
//        listMetricsRequest.setDimensions(filter);
//        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
//        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = instanceMetricsResult.getMetrics();
//
//        List<Dimension> dimensions= new ArrayList<Dimension>();
//        dimensions.add(new Dimension().withName("Operation").withValue("PutItem"));
//        dimensions.add(new Dimension().withName("TableName").withValue("TestTable"));
//
//        GetMetricStatisticsRequest getDynamoDBStatisticsRequest = new GetMetricStatisticsRequest()
//                .withStartTime( new Date( System.currentTimeMillis() - 10000000))
//                .withNamespace("AWS/EBS")
//                .withPeriod(60 * 60)
//                .withDimensions(new Dimension().withName("VolumeId").withValue("vol-b60fc1c1"))
//                .withMetricName("VolumeIdleTime")
//                .withStatistics("Average")
//                .withEndTime(new Date());
//        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getDynamoDBStatisticsRequest);
//
//        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
//
//
//
//
//        //autoScalingGroup.getInstances()