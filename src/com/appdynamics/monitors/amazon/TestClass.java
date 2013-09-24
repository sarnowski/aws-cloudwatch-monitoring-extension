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
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
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
        byte[] secretyKeyBytes = new byte[0];
        try {
            secretyKeyBytes = AWS_SECRET_KEY.getBytes(UTF8_CHARSET);
            secretKeySpec =
                new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM);
            mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public String sign(Map<String, String> params) {
        params.put("AWSAccessKeyId", AWS_ACCESS_KEYID);
        params.put("Timestamp", timestamp());
        params.put("SignatureVersion", "2");
        params.put("Action", "GetMetricStatistics");
        params.put("Version", "2010-08-01");
        params.put("SignatureMethod", HMAC_SHA256_ALGORITHM);

        SortedMap<String, String> sortedParamMap =
                new TreeMap<String, String>(params);
        String canonicalQS = canonicalize(sortedParamMap);
        String toSign =
                REQUEST_METHOD + "\n"
                        + ENDPOINT + "\n"
                        + REQUEST_URI + "\n"
                        + canonicalQS;

        String hmac = hmac(toSign);
        String sig = percentEncodeRfc3986(hmac);
        String url = "http://" + ENDPOINT + REQUEST_URI + "?" +
                canonicalQS + "&Signature=" + sig;

        return url;
    }

    private String hmac(String stringToSign) {
        String signature = null;
        byte[] data;
        byte[] rawHmac;
        try {
            data = stringToSign.getBytes(UTF8_CHARSET);
            rawHmac = mac.doFinal(data);
            Base64 encoder = new Base64();
            signature = new String(encoder.encode(rawHmac));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
        }
        return signature;
    }

    private String timestamp() {
        String timestamp = null;
        Calendar cal = Calendar.getInstance();
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
        timestamp = dfm.format(cal.getTime());
        return timestamp;
    }

    private String canonicalize(SortedMap<String, String> sortedParamMap)
    {
        if (sortedParamMap.isEmpty()) {
            return "";
        }

        StringBuffer buffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter =
                sortedParamMap.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> kvpair = iter.next();
            buffer.append(percentEncodeRfc3986(kvpair.getKey()));
            buffer.append("=");
            buffer.append(percentEncodeRfc3986(kvpair.getValue()));
            if (iter.hasNext()) {
                buffer.append("&");
            }
        }
        String canonical = buffer.toString();
        return canonical;
    }

    private String percentEncodeRfc3986(String s) {
        String out;
        try {
            out = URLEncoder.encode(s, UTF8_CHARSET)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            out = s;
        }
        return out;
    }

    public static void main(String[] args) {
        setNamespaces();
        setDisabledMetrics();
        //TestClass requestsHelper = new TestClass();
        //String requestString = requestsHelper.sign(new HashMap<String, String>());
        //System.out.println(requestString);
        //AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider("conf");
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJTB7DYHGUBXOS7BQ", "jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc");
        AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
        //getEBSMetrics(awsCloudWatch);
        getELBMetrics(awsCloudWatch, awsCredentials);
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

        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(awsCredentials);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups();
        List<AutoScalingGroup> autoScalingGroupList = describeAutoScalingGroupsResult.getAutoScalingGroups();
        AutoScalingGroup group = autoScalingGroupList.get(0);
        EnableMetricsCollectionRequest request = new EnableMetricsCollectionRequest();
        request.setAutoScalingGroupName(group.getAutoScalingGroupName());
        request.setGranularity("1Minute");
        amazonAutoScalingClient.enableMetricsCollection(request);
        DescribeMetricCollectionTypesResult y = amazonAutoScalingClient.describeMetricCollectionTypes();
        List<EnabledMetric> enabledMetrics = group.getEnabledMetrics();

        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime( new Date( System.currentTimeMillis() - TimeUnit.MINUTES.toMillis( 2 ) ) )
                .withNamespace("AWS/AutoScaling")
                .withPeriod(60 * 60)
                .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(group.getAutoScalingGroupName()))
                .withMetricName("GroupMaxSize")
                .withStatistics("Average")
                .withEndTime(new Date());


        AmazonDynamoDBClient x = new AmazonDynamoDBClient();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter instanceIdFilter = new DimensionFilter();
        instanceIdFilter.setName("VolumeId");
        filter.add(instanceIdFilter);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = instanceMetricsResult.getMetrics();

        List<Dimension> dimensions= new ArrayList<Dimension>();
        dimensions.add(new Dimension().withName("Operation").withValue("PutItem"));
        dimensions.add(new Dimension().withName("TableName").withValue("TestTable"));

        GetMetricStatisticsRequest getDynamoDBStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime( new Date( System.currentTimeMillis() - 10000000))
                .withNamespace("AWS/EBS")
                .withPeriod(60 * 60)
                .withDimensions(new Dimension().withName("VolumeId").withValue("vol-b60fc1c1"))
                .withMetricName("VolumeIdleTime")
                .withStatistics("Average")
                .withEndTime(new Date());
        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getDynamoDBStatisticsRequest);

        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();




        //autoScalingGroup.getInstances()

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
            HashMap<String,HashSet<String>> disabledMetrics = new HashMap<String,HashSet<String>>();
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
                        .withDimensions(new Dimension().withName("VolumeId").withValue("vol-b60fc1c1"))
                        .withMetricName("VolumeIdleTime")
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
        filters.add(zoneFilter);

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
}


// gather instance metrics
//HashMap<String, HashMap<String, List<Datapoint>>> instanceMetrics = gatherInstanceMetrics();

// gather auto-scaling metrics
//HashMap<String, HashMap<String,List<Datapoint>>> autoscalingMetrics = gatherAutoScalingMetrics();

// print metrics to controller
//printInstanceMetrics(instanceMetrics);

// print auto-scaling metrics to controller
//printAutoScalingMetrics(autoscalingMetrics);


//    /**
//     * Gather metrics for every instance and group by instanceId. Populates global map called cloudWatchMetrics
//     */
//    private HashMap<String, HashMap<String, List<Datapoint>>> gatherInstanceMetrics() {
//        // The outer hashmap has instanceIds as keys and inner hashmaps as the value.
//        // The inner hashmaps have metric names as keys and lists of corresponding data points as the values
//        HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();
//
//        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
//        DimensionFilter instanceIdFilter = new DimensionFilter();
//        instanceIdFilter.setName("InstanceId");
//        filter.add(instanceIdFilter);
//        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
//        listMetricsRequest.setDimensions(filter);
//        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
//        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = instanceMetricsResult.getMetrics();
//
//        for (com.amazonaws.services.cloudwatch.model.Metric m : instanceMetrics) {
//            List<Dimension> dimensions = m.getDimensions();
//            for (Dimension dimension : dimensions) {
//                if (!cloudWatchMetrics.containsKey(dimension.getValue())) {
//                    cloudWatchMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
//                }
//                gatherInstanceMetricsHelper(m, dimension, cloudWatchMetrics);
//            }
//        }
//        return cloudWatchMetrics;
//    }
//
//    private void gatherInstanceMetricsHelper(com.amazonaws.services.cloudwatch.model.Metric metric,
//                                             Dimension dimension,
//                                             HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics) {
////        if (disabledMetrics.contains(metric.getMetricName())) {
////            return;
////        }
//        GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(metric);
//        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
//        cloudWatchMetrics.get(dimension.getValue()).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
//    }
//
//    private GetMetricStatisticsRequest createGetMetricStatisticsRequest(com.amazonaws.services.cloudwatch.model.Metric m) {
//        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
//                .withStartTime(new Date(new Date().getTime() - 1000000000))
//                .withNamespace("AWS/EC2")
//                .withPeriod(60 * 60)
//                .withMetricName(m.getMetricName())
//                .withStatistics("Average")
//                .withEndTime(new Date());
//        return getMetricStatisticsRequest;
//    }
//
//    private HashMap<String,HashMap<String,List<Datapoint>>> gatherAutoScalingMetrics() {
//        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
//        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJTB7DYHGUBXOS7BQ", "jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc");
//        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(awsCredentials);
//        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups();
//        List<AutoScalingGroup> autoScalingGroupList = describeAutoScalingGroupsResult.getAutoScalingGroups();
//        for (AutoScalingGroup autoScalingGroup : autoScalingGroupList) {
//            String groupName = autoScalingGroup.getAutoScalingGroupName();
//            if (!autoScalingMetrics.containsKey(groupName)) {
//                autoScalingMetrics.put(groupName, new HashMap<String,List<Datapoint>>());
//                //TODO: remove this. Ask Pranta
//                EnableMetricsCollectionRequest request = new EnableMetricsCollectionRequest();
//                request.setAutoScalingGroupName(groupName);
//                request.setGranularity("1Minute");
//                amazonAutoScalingClient.enableMetricsCollection(request);
//
//            }
//            gatherAutoScalingMetricsHelper(autoScalingGroup, autoScalingMetrics);
//        }
//
//
//        return autoScalingMetrics;
//    }
//
//    private void gatherAutoScalingMetricsHelper(AutoScalingGroup currentGroup, HashMap<String,HashMap<String,List<Datapoint>>> autoScalingMetrics) {
//        HashMap<String,List<Datapoint>> groupMetrics = autoScalingMetrics.get(currentGroup.getAutoScalingGroupName());
//        List<EnabledMetric> enabledMetrics = currentGroup.getEnabledMetrics();
//        for (EnabledMetric m : enabledMetrics) {
//            GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
//                    .withStartTime( new Date( System.currentTimeMillis() - TimeUnit.MINUTES.toMillis( 2 ) ) )
//                    .withNamespace("AWS/AutoScaling")
//                    .withPeriod(60 * 60)
//                    .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(currentGroup.getAutoScalingGroupName()))
//                    .withMetricName(m.getMetric())
//                    .withStatistics("Average")
//                    .withEndTime(new Date());
//            GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
//            List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
//            groupMetrics.put(m.getMetric(), datapoints);
//        }
//    }
//    private void printInstanceMetrics(HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics) {
//        Iterator outerIterator = cloudWatchMetrics.keySet().iterator();
//
//        while (outerIterator.hasNext()) {
//            String instanceId = outerIterator.next().toString();
//            HashMap<String, List<Datapoint>> metricStatistics = cloudWatchMetrics.get(instanceId);
//            Iterator innerIterator = metricStatistics.keySet().iterator();
//            while (innerIterator.hasNext()) {
//                String metricName = innerIterator.next().toString();
//                List<Datapoint> datapoints = metricStatistics.get(metricName);
//                if (datapoints != null && !datapoints.isEmpty()) {
//                    Datapoint data = datapoints.get(0);
//                    printMetric("EC2|", "InstanceId|" + instanceId + "|" + metricName + "(" + data.getUnit() + ")", data.getAverage(),
//                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
//                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
//                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
//                }
//            }
//        }
//    }
//
//    private void printAutoScalingMetrics(HashMap<String, HashMap<String,List<Datapoint>>> metrics) {
//        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
//        Iterator outerIterator = autoScalingMetrics.keySet().iterator();
//
//        while (outerIterator.hasNext()) {
//            String autoScalingGroupName = outerIterator.next().toString();
//            HashMap<String, List<Datapoint>> metricStatistics = autoScalingMetrics.get(autoScalingGroupName);
//            Iterator innerIterator = metricStatistics.keySet().iterator();
//            while (innerIterator.hasNext()) {
//                String metricName = innerIterator.next().toString();
//                List<Datapoint> datapoints = metricStatistics.get(metricName);
//                if (datapoints != null && !datapoints.isEmpty()) {
//                    Datapoint data = datapoints.get(0);
//                    printMetric("AutoScaling|", "GroupId|" + autoScalingGroupName + "|" + metricName, data.getAverage(),
//                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
//                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
//                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
//                }
//            }
//        }
//    }
