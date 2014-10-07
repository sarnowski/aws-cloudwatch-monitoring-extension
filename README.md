AppDynamics AWS CloudWatch Monitoring Extension
===============================================

This extension works only with the standalone machine agent.

Use Case
-------- 

The AWS CloudWatch custom monitor captures statistics from Amazon CloudWatch and displays them in the AppDynamics Metric Browser.

Metrics are categorized under the AWS Product Namespaces:

 <table>
  <tr>
    <th align="left">AWS Product</th>
    <th align="left">Namespace</th>
  </tr>
  <tr>
    <td> Auto Scaling </td>
    <td> AWS/AutoScaling</td>
  </tr>
  <tr>
    <td> Billing </td>
    <td> AWS/Billng</td>
  </tr>
  <tr>
    <td> Amazon DynamoDB </td>
    <td> AWS/DynamoDB</td>
  </tr>
  <tr>
    <td> Amazon ElastiCache </td>
    <td> AWS/ElastiCache</td>
  </tr>
  <tr>
    <td> Amazon Elastic Block Store </td>
    <td> AWS/EBS</td>
  </tr>
  <tr>
    <td> Amazon Elastic Compute Cloud </td>
    <td> AWS/AutoScaling</td>
  </tr>
  <tr>
    <td> Elastic Load Balancing </td>
    <td> AWS/ELB</td>
  </tr>
  <tr>
    <td> Amazon Elastic MapReduce </td>
    <td> AWS/ElasticMapReduce</td>
  </tr>
  <tr>
    <td> AWS OpsWorks </td>
    <td> AWS/OpsWorks</td>
  </tr>
  <tr>
    <td> Amazon Redshift </td>
    <td> AWS/Redshift</td>
  </tr>
  <tr>
    <td> Amazon Relational Database Service </td>
    <td> AWS/RDS</td>
  </tr>
  <tr>
    <td> Amazon Route 53 </td>
    <td> AWS/Route53</td>
  </tr>
  <tr>
    <td> Amazon Simple Notification Service </td>
    <td> AWS/SNS</td>
  </tr>
  <tr>
    <td> Amazon Simple Queue Service </td>
    <td> AWS/SQS</td>
  </tr>
  <tr>
    <td> AWS Storage Gateway </td>
    <td> AWS/StorageGateway</td>
  </tr>
</table>

Specific metrics under each of these namespaces can be found at this link http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html

Installation
------------

 1. Clone aws-cloudwatch-monitoring-extension from GitHub https://github.com/Appdynamics
 2. Run 'mvn clean install' from the cloned aws-cloudwatch-monitoring-extension directory.
 3. Download the CloudWatchMonitor.zip found in the 'target' directory into <machineagent install dir>/monitors/
 4. Unzip the downloaded zip file.
 5. In the newly created "CloudWatchMonitor" directory, edit the "AWSConfigurations.xml" file configuring the parameters specified below.
 6. Restart the machine agent.
 7. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | Amazon Cloud Watch


Configuration
-------------

In the conf/AWSConfigurations.xml, there are a few things that can be configured:

1. The AWS account credentials (i.e. the access key and the secret key)
2. The supported AWS namespaces that you can retrieve metrics for (you can enable or disable metrics for specific namespaces). You can also add your custom namespace if required.
3. Regions (enable regions to monitor the running AWS Products in the corresponding region)
4. Use of instance name in Metrics. Default value is false. Note, this is only applicable for AWS/EC2 namespace.
5. The list of disabled metrics associated with their corresponding AWS namespaces
6. The list of metrics and associated metric type you wish to retrieve. Defaults to 'Ave' if not specified. Allowed metric types: **ave, max, min, sum, samplecount**

List of Amazon Cloudwatch Regions can be found at this link http://docs.aws.amazon.com/general/latest/gr/rande.html#cw_region
 
This is a sample AWSConfigurations.xml file: 

    <?xml version="1.0"?>
    <Configurations>
        <AWSCredentials>
            <AccessKey>AKIAJTB7DYHGUBXOS7BQ</AccessKey>
            <SecretKey>jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc</SecretKey>
        </AWSCredentials>
       <!--Individual namespaces can be disabled by simply commenting them out -->
        <SupportedNamespaces>
            <Namespace>AWS/EC2</Namespace>
            <Namespace>AWS/AutoScaling</Namespace>
            <Namespace>AWS/EBS</Namespace>
            <Namespace>AWS/ELB</Namespace>
            <Namespace>AWS/ElastiCache</Namespace>
            <Namespace>AWS/Redshift</Namespace>
            <Namespace>AWS/DynamoDB</Namespace>
            <Namespace>AWS/RDS</Namespace>
            <Namespace>AWS/Route53</Namespace>
            <Namespace>AWS/SQS</Namespace>
            <Namespace>AWS/ElasticMapReduce</Namespace>
            <Namespace>AWS/StorageGateway</Namespace>
            <Namespace>AWS/OpsWorks</Namespace>
            <Namespace>AWS/SNS</Namespace>
            <Namespace>AWS/Billing</Namespace>
            
            <!-- Custom Namespace -->
            <Namespace>MyCustomNamespace</Namespace>
        </SupportedNamespaces>
        
        <EC2InstanceName>
    		<UseNameInMetrics>true</UseNameInMetrics>
    		<TagFilterName>tag-key</TagFilterName>
    		<TagKey>Name</TagKey>
    	</EC2InstanceName>
        
		<Regions>
        	<Region>us-east-1</Region> 
	        <Region>us-west-2</Region>
    	    <Region>us-west-1</Region>
    	    <Region>eu-west-1</Region>
        	<Region>ap-southeast-1</Region>
    	    <Region>ap-southeast-2</Region>
    	    <Region>ap-northeast-1</Region>
    	    <Region>sa-east-1</Region>
	    </Regions>
	    
        <DisabledMetrics>
            <Metric namespace="AWS/EC2" metricName="CPUUtilization"/>
            <Metric namespace="AWS/EC2" metricName="Some Metric"/>
        </DisabledMetrics>
        
    	<MetricTypes>
    		<Metric namespace="AWS/ELB" metricName="RequestCount" metricType="sum"/>
		</MetricTypes>
		
    </Configurations> 
    
The monitor.xml contains one parameter, which is the path to the AWSConfigurations.xml file.

Here is the monitor.xml file:

    <monitor>
        <name>AmazonMonitor</name>
        <type>managed</type>
        <description>Amazon cloud watch monitor</description>
        <monitor-configuration></monitor-configuration>
        <monitor-run-task>
                <execution-style>periodic</execution-style>
                <execution-frequency-in-seconds>60</execution-frequency-in-seconds>
                <name>Amazon Cloud Watch Monitor Run Task</name>
                <display-name>Cloud Watch Monitor Task</display-name>
                <description>Cloud Watch Monitor Task</description>
                <type>java</type>
                <execution-timeout-in-secs>60</execution-timeout-in-secs>
                <java-task>
                    <classpath>CloudWatchMonitor.jar;lib/aws-java-sdk-1.6.8.jar;lib/httpclient-4.2.3.jar;lib/httpcore-4.2.2.jar;lib/commons-logging-1.1.1.jar;lib/joda-time-2.3.jar;lib/appd-exts-commons-1.0.3.jar;lib/slf4j-api-1.7.5.jar;lib/slf4j-log4j12-1.7.5.jar;lib/guava-11.0.2.jar;lib/jackson-annotations-2.1.1.jar;lib/jackson-core-2.1.1.jar;lib/jackson-core-asl-1.9.13.jar;lib/jackson-databind-2.1.1.jar;lib/jackson-mapper-asl-1.9.13.jar</classpath>
                        <impl-class>com.appdynamics.extensions.cloudwatch.AmazonCloudWatchMonitor</impl-class>
                </java-task>
                <task-arguments>
                    <argument name="configurations" is-required="true" default-value="/mnt/appdynamics/machineagent/monitors/AmazonMonitor/conf/AWSConfigurations.xml"/>
                </task-arguments>
        </monitor-run-task>
</monitor>
 
Directory Structure
-------------------

<table>
  <tr>
    <th align="left">File/Folder</th>
    <th align="left">Description</th>
  </tr>
  <tr>
    <td> src/main/resources/conf </td>
    <td> Contains monitor.xml and AWSConfigurations.xml</td>
  </tr>
  <tr>
    <td> src/main/java</td>
    <td> Contains the source code for aws-cloudwatch-monitoring-extension</td>
  </tr>
  <tr>
    <td> target </td>
    <td> The directory created when 'maven' is run. Run 'mvn clean install' to generate distributable .zip file.</td>
  </tr>
  <tr>
    <td> pom.xml </td>
    <td> Maven build script to package the project (required only if changing the Java code)</td>
  </tr>
</table>  

***Main Java File***: **src/com/appdynamics/extensions/cloudwatch/AmazonCloudWatchMonitor.java** 

Custom Dashboard
----------------

![alt tag](https://raw.github.com/Appdynamics/aws-cloudwatch-monitoring-extension/master/images/AWSCloudWatchDashboard.png)

Contributing
------------

Always feel free to fork and contribute any changes directly via <a href="https://github.com/Appdynamics/aws-cloudwatch-monitoring-extension">GitHub</a>.


Community
---------

Find out more in the <a href="http://appsphere.appdynamics.com/t5/eXchange/AWS-CloudWatch-Monitoring-Extension/idi-p/3541">AppSphere</a>.

Support
-------

For any questions or feature request, please contact <a href="mailto:ace-request@appdynamics.com">AppDynamics Center of Excellence</a>.
