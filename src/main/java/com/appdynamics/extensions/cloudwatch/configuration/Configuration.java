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
package com.appdynamics.extensions.cloudwatch.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricType;

import java.util.*;

public class Configuration {

	// This HashSet of disabled metrics is populated by reading the
	// DisabledMetrics.xml file
	public Map<String, Set<String>> disabledMetrics;
	// This HashSet of available namespaces is populated by reading the
	// AvailableNamespaces.xml file
	public Set<String> availableNamespaces;

	public Set<String> availableRegions;

	public AWSCredentials awsCredentials;
	
	public ClientConfiguration clientConfiguration;
	
	public boolean useNameInMetrics;
	
	public String tagFilterName;
	
	public String tagKey;
	
	public Map<String, Map<String, MetricType>> metricTypes;
	
	public Map<String, String> proxyParams;

	public Configuration() {
		disabledMetrics = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		metricTypes = Collections.synchronizedMap(new HashMap<String, Map<String, MetricType>>());
		availableNamespaces = new HashSet<String>();
		availableRegions = new HashSet<String>();
		clientConfiguration = new ClientConfiguration();
		proxyParams = new HashMap<String, String>();
	}
}
