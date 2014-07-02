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

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;

import java.util.List;
import java.util.Map;

public final class RDSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/RDS";

    /**
     * Gather metrics for AWS/RDS
     * @return	Map     Map containing metrics
     */
    @Override
    public Map<String, Map<String,List<Datapoint>>> gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "DBInstanceIdentifier");
    }

    /**
     * Print metrics for AWS/RDS
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map<String, Map<String,List<Datapoint>>> metrics) {
        printMetricsHelper(region, NAMESPACE, metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/RDS
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "DBInstanceIdentifier");
    }
}
