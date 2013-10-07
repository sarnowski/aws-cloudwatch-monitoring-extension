package com.appdynamics.monitors.amazon;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.amazon.configuration.Configuration;
import com.appdynamics.monitors.amazon.configuration.ConfigurationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class TestClass {

    public static void main(String[] args) {
        Configuration config = ConfigurationUtil.getConfigurations("conf/AWSConfigurations.xml");

        AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(config.awsCredentials);
        System.out.println("done");
    }
}