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
package com.appdynamics.extensions.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OpsWorksMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/OpsWorks";

    /**
     * Gather metrics for AWS/OpsWorks
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics(AmazonCloudWatch awsCloudWatch, String region) {
        Map<String, Map<String, Map<String, List<Datapoint>>>> opsWorksMetrics = new ConcurrentHashMap<String, Map<String, Map<String, List<Datapoint>>>>();

        Map<String, Map<String, List<Datapoint>>> stackMetrics = gatherMetricsHelper(awsCloudWatch, NAMESPACE, region, "StackId");
        Map<String, Map<String, List<Datapoint>>> layerMetrics = gatherMetricsHelper(awsCloudWatch, NAMESPACE, region, "LayerId");
        Map<String, Map<String, List<Datapoint>>> instanceMetrics =  gatherMetricsHelper(awsCloudWatch, NAMESPACE, region, "InstanceId");

        opsWorksMetrics.put("StackId", stackMetrics);
        opsWorksMetrics.put("LayerId", layerMetrics);
        opsWorksMetrics.put("InstanceId", instanceMetrics);

        return opsWorksMetrics;
    }

    /**
     * Print metrics for AWS/OpsWorks
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map metrics) {
        Map<String, Map<String, Map<String, List<Datapoint>>>> opsWorksMetrics = (Map<String, Map<String,Map<String,List<Datapoint>>>>) metrics;
        Iterator dimensionFilterIterator = opsWorksMetrics.keySet().iterator();

        while (dimensionFilterIterator.hasNext()) {
            String metricType = dimensionFilterIterator.next().toString();
            Map<String, Map<String,List<Datapoint>>> dimensionMetrics = opsWorksMetrics.get(metricType);
            Iterator dimensionIterator = dimensionMetrics.keySet().iterator();
            while (dimensionIterator.hasNext()) {
                String dimensionId = dimensionIterator.next().toString();
                Map<String, List<Datapoint>> metricsMap = dimensionMetrics.get(dimensionId);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(region + "|", getNamespacePrefix() + metricType + "|" +  dimensionId + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }
    }

    /**
     * Construct namespace prefix for AWS/OpsWorks
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|";
    }
}
