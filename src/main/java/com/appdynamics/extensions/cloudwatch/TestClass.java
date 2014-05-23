/** 
* Copyright 2013 AppDynamics 
* 
* Licensed under the Apache License, Version 2.0 (the License);
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an AS IS BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.appdynamics.extensions.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.appdynamics.extensions.cloudwatch.configuration.Configuration;
import com.appdynamics.extensions.cloudwatch.configuration.ConfigurationUtil;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManagerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestClass {

    private static Configuration config;
    private static AmazonCloudWatch awsCloudWatch;
    private static AmazonCloudWatchMonitor monitor;
    private static MetricsManagerFactory metricsManagerFactory;

    // Initialization for local testing. Bit hacky since we are not using the monitor->execute()
    public static void init() {
        try {
            config = ConfigurationUtil.getConfigurations("conf/AWSConfigurations.xml");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        awsCloudWatch = new AmazonCloudWatchClient(config.awsCredentials);
        monitor = new AmazonCloudWatchMonitor();
        monitor.setAmazonCloudWatch(awsCloudWatch);
        monitor.setDisabledMetrics(config.disabledMetrics);
        metricsManagerFactory = new MetricsManagerFactory(monitor);
    }

    public static void main(String[] args) {
        init();
        // testMetrics("AWS/Billing");
        System.out.println("Finished execution");
    }

    public static void testDimensions() {
        ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
        Dimension d1 = new Dimension();
        d1.setName("ClusterIdentifier");
        d1.setValue("cluster1");
        dimensions.add(d1);
        GetMetricStatisticsRequest request = createGetMetricStatisticsRequest("AWS/Redshift", "CPUUtilization", "Average", dimensions);
        GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
        System.out.println("done");
    }

    public static GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                          String metricName,
                                                                          String statisticsType,
                                                                          List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 60000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

    private static void testMetrics(String namespace) {
        MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
        Map metrics = metricsManager.gatherMetrics();
        System.out.println("Finished testing metrics");
    }
}