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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.amazonaws.auth.BasicAWSCredentials;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricType;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;

public class ConfigurationUtil {

	private static Logger logger = Logger.getLogger("com.singularity.extensions.ConfigurationUtil");

	/**
	 * Reads the config file in the conf/ directory and retrieves AWS
	 * credentials, disabled metrics, and available namespaces
	 * 
	 * @param filePath
	 *            Path to the configuration file
	 * @return Configuration Configuration object containing AWS credentials,
	 *         disabled metrics, and available namespaces
	 */
	public static Configuration getConfigurations(String filePath) throws Exception {
		Configuration awsConfiguration = new Configuration();
		BufferedInputStream configFile = null;

		try {
			logger.info("Reading config file::" + filePath);
			String fileName = getConfigFilename(filePath);
			configFile = new BufferedInputStream(new FileInputStream(fileName));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(configFile);

			// Initialize AmazonCloudWatch
			Element credentialsFromFile = (Element) doc.getElementsByTagName("AWSCredentials").item(0);
			String accessKey = credentialsFromFile.getElementsByTagName("AccessKey").item(0).getTextContent();
			String secretKey = credentialsFromFile.getElementsByTagName("SecretKey").item(0).getTextContent();
			awsConfiguration.awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

			// Initialize Namespaces
			Element namespacesElement = (Element) doc.getElementsByTagName("SupportedNamespaces").item(0);
			NodeList namespaces = namespacesElement.getElementsByTagName("SupportedNamespace");

			for (int i = 0; i < namespaces.getLength(); i++) {
				String namespace = namespaces.item(i).getTextContent();
				if (!awsConfiguration.availableNamespaces.contains(namespace)) {
					awsConfiguration.availableNamespaces.add(namespaces.item(i).getTextContent());
				}
			}

			Element regionElement = (Element) doc.getElementsByTagName("Regions").item(0);
			NodeList regions = regionElement.getElementsByTagName("Region");
			for (int i = 0; i < regions.getLength(); i++) {
				String region = regions.item(i).getTextContent();
				if (!awsConfiguration.availableRegions.contains(region)) {
					awsConfiguration.availableRegions.add(region);
				}
			}
			
			Element ec2InstanceNameElement = (Element) doc.getElementsByTagName("EC2InstanceName").item(0);
			String tagFilterName = ec2InstanceNameElement.getElementsByTagName("TagFilterName").item(0).getTextContent();
			awsConfiguration.tagFilterName = tagFilterName;
			String tagKey = ec2InstanceNameElement.getElementsByTagName("TagKey").item(0).getTextContent();
			awsConfiguration.tagKey = tagKey;
			String strUseNameInMetrics = ec2InstanceNameElement.getElementsByTagName("UseNameInMetrics").item(0).getTextContent();
			awsConfiguration.useNameInMetrics = Boolean.valueOf(strUseNameInMetrics);

			// Initialize Disabled Metrics
			Element disabledMetricsElement = (Element) doc.getElementsByTagName("DisabledMetrics").item(0);
			NodeList disabledMetricsList = disabledMetricsElement.getElementsByTagName("Metric");
			for (int i = 0; i < disabledMetricsList.getLength(); i++) {
				String namespaceKey = disabledMetricsList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
				String metricName = disabledMetricsList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
				if (!awsConfiguration.disabledMetrics.containsKey(namespaceKey)) {
					awsConfiguration.disabledMetrics.put(namespaceKey, new HashSet<String>());
				}
				(awsConfiguration.disabledMetrics.get(namespaceKey)).add(metricName);
			}
			
			Element metricTypesElement = (Element) doc.getElementsByTagName("MetricTypes").item(0);
			NodeList metricTypesList = metricTypesElement.getElementsByTagName("Metric");
			for (int i = 0; i < metricTypesList.getLength(); i++) {
				String namespaceKey = metricTypesList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
				String metricName = metricTypesList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
				String metricTypeName = metricTypesList.item(i).getAttributes().getNamedItem("metricType").getNodeValue();
				
				if (!awsConfiguration.metricTypes.containsKey(namespaceKey)) {
					awsConfiguration.metricTypes.put(namespaceKey, new ConcurrentHashMap<String, MetricType>());
				}
				
				(awsConfiguration.metricTypes.get(namespaceKey)).put(metricName, 
						convertToMetricType(namespaceKey, metricName, metricTypeName));
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("Enabled namespaces: " + awsConfiguration.availableNamespaces);
				logger.debug("Enabled regions: " + awsConfiguration.availableRegions);
			}
			
			return awsConfiguration;
		} catch (Exception e) {
			logger.error("Exception while reading configuration file", e);
			throw e;
		} finally {
			configFile.close();
		}
	}
	
	private static MetricType convertToMetricType(String namespace, String metricName, String metricTypeName) {
		MetricType metricType = null; 
		
		try {
			metricType = MetricType.fromString(metricTypeName);
			
		} catch (IllegalArgumentException ex) {
			logger.warn(String.format("Issue with selected metric type [%s] for %s - %s: %s.. Defaulting to [%s]!", 
					metricTypeName, namespace, metricName, ex.getMessage(), MetricType.AVE.getTypeName()));
			metricType = MetricType.AVE;
		}

		return metricType;
		
	}

	private static String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}
}