AppDynamics AWS CloudWatch Monitoring Extension
===============================================

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
 2. Run 'ant package' from the cloned aws-cloudwatch-monitoring-extension directory.
 3. Download the CloudWatchMonitor.zip found in the 'dist' directory into <machineagent install dir>/monitors/
 4. Unzip the downloaded zip file.
 5. In the newly created "CloudWatchMonitor" directory, edit the "AWSConfigurations.xml" file configuring the parameters specified below.
 6. Restart the machine agent.
 7. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | Amazon Cloud Watch


Rebuilding the Project
----------------------

 1. At the command line, go to the root directory of this extension
 2. Run 'ant'. This will update the dist directory

Configuration
-------------

In the conf/AWSConfigurations.xml, there are three things that can be configured:

 1) The AWS account credentials (i.e. the access key and the secret key)
 2) The supported AWS namespaces that you can retrieve metrics for (you can enable or disable metrics for specific namespaces)
 3) The list of disabled metrics associated with their corresponding AWS namespaces
 
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
        </SupportedNamespaces>
        <DisabledMetrics>
            <Metric namespace="AWS/EC2" metricName="CPUUtilization"/>
            <Metric namespace="AWS/EC2" metricName="Some Metric"/>
        </DisabledMetrics>
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
                    <classpath>AmazonMonitor.jar;lib/aws-java-sdk-1.5.6.jar;lib/commons-codec-1.8.jar;lib/log4j-1.2.15.jar;lib/machineagent.jar;lib/httpclient-4.2.3.jar;lib/httpcore-4.2.jar;lib/commons-logging-1.1.1.jar</classpath>
                        <impl-class>com.appdynamics.monitors.cloudwatch.AmazonCloudWatchMonitor</impl-class>
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
    <td> conf </td>
    <td> Contains monitor.xml and AWSConfigurations.xml</td>
  </tr>
  <tr>
    <td> lib </td>
    <td> Contains third-party project references</td>
  </tr>
  <tr>
    <td> src</td>
    <td> Contains the source code for aws-cloudwatch-monitoring-extension</td>
  </tr>
  <tr>
    <td> dist </td>
    <td> The directory created when 'ant' is run. Run 'ant build' to generate the binaries. Run 'ant package' to generate distributable .zip file.</td>
  </tr>
  <tr>
    <td> build.xml </td>
    <td> Ant build script to package the project (required only if changing the Java code)</td>
  </tr>
</table>  

***Main Java File***: **src/com/appdynamics/monitors/cloudwatch/AmazonCloudWatchMonitor.java** 

Custom Dashboard
----------------

![alt tag](https://raw.github.com/Appdynamics/aws-cloudwatch-monitoring-extension/master/images/custom_dashboard.png?login=rvasanda&token=3cb0844b5b6f014a94b7f1e90fe3bbc2)

Contributing
------------

Always feel free to fork and contribute any changes directly via <a href="https://github.com/Appdynamics/aws-cloudwatch-monitoring-extension">GitHub</a>


Community
---------

Find out more in the <a href="http://appsphere.appdynamics.com/t5/Extensions/MongoDB-Monitoring-Extension/idi-p/831">AppSphere</a>

Support
-------

For any questions or feature request, please contact <a href="mailto:ace-request@appdynamics.com">AppDynamics Center of Excellence</a> 
