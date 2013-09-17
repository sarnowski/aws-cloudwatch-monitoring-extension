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

import javax.crypto.Mac;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SignedRequestsHelper {
    private static final String UTF8_CHARSET = "UTF-8";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String REQUEST_URI = "/onca/xml";
    private static final String REQUEST_METHOD = "GET";
    private static final String ENDPOINT = "monitoring.amazonaws.com";
    private static final String AWS_ACCESS_KEYID = "AKIAJTB7DYHGUBXOS7BQ";
    private static final String AWS_SECRET_KEY = "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";

    private SecretKeySpec secretKeySpec = null;
    private Mac mac = null;

    public SignedRequestsHelper() {
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
        //SignedRequestsHelper requestsHelper = new SignedRequestsHelper();
        //String requestString = requestsHelper.sign(new HashMap<String, String>());
        //System.out.println(requestString);
        //AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider("conf");
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJTB7DYHGUBXOS7BQ", "jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc");
        AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
        //AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(credentialsProvider);
        /*Dimension instanceDimension = new Dimension();
        //instanceDimension.setName("instanceid");
        //instanceDimension.setValue("sasadad");
        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 100000))
                .withNamespace("AWS/EC2")
                .withPeriod(60 * 60)
                .withMetricName("CPUUtilization")
                .withStatistics("Average")
                .withDimensions(Arrays.asList(instanceDimension))
                .withEndTime(new Date());

        //print(awsCloudWatch.listMetrics());
        print(awsCloudWatch.getMetricStatistics(request));
        ListMetricsResult result = awsCloudWatch.listMetrics();
        List<Metric> results = result.getMetrics();*/
        HashMap<String, HashMap<String, List<Datapoint>>> uniqueInstanceIds = new HashMap<String, HashMap<String,List<Datapoint>>>();
        //HashMap<String, HashMap<String,Double>> uniqueInstanceIds = new HashMap<String, HashMap<String,Double>>();

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
                    //GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(m);
                    //GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    //uniqueInstanceIds.get(dimension.getValue()).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());
                //}
                //else if (dimension.getName().equals("InstanceId")) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(m);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    uniqueInstanceIds.get(dimension.getValue()).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());
                //}


            }
        }
        Iterator iterator = uniqueInstanceIds.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            String value = uniqueInstanceIds.get(key).toString();
            System.out.println(key);
        }
        //ListMetricsResult y = awsCloudWatch.

        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(awsCredentials);
        //CreateAutoScalingGroupRequest createAutoScalingGroupRequest = new CreateAutoScalingGroupRequest();
        //createAutoScalingGroupRequest.
        //amazonAutoScalingClient.createAutoScalingGroup();
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups();
        List<AutoScalingGroup> autoScalingGroupList = describeAutoScalingGroupsResult.getAutoScalingGroups();


        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();


        //autoScalingGroup.getInstances()

        Properties prop = new Properties();
        try {
            //load a properties file
            prop.load(new FileInputStream("conf/AwsCredentials.properties"));

            //get the property value and print it out
            //System.out.println(prop.getProperty("database"));
            //System.out.println(prop.getProperty("dbuser"));
            //System.out.println(prop.getProperty("accessKey"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        setDisabledMetrics();


    }
    private static void setDisabledMetrics() {
        try {
            Set<String> disabledMetrics = new HashSet<String>();

            File fXmlFile = new File("conf/DisabledMetrics.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nList = doc.getElementsByTagName("MetricName");
            for (int i = 0; i < nList.getLength(); i++) {
                disabledMetrics.add(nList.item(i).getTextContent());
            }
            printDisabledMetrics(disabledMetrics);

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

}

