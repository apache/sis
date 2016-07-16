/*
 * Copyright 2016 haonguyen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.sis.util.collection.TreeTable.Node;
import org.w3c.dom.Document;

/**
 *
 * @author haonguyen
 */
public class Xpath {

    private DocumentBuilder builder;
    private Document document;

    /**
     * Constructor accepts XML string
     *
     * @param xmlData
     * @throws Exception
     */
    public Xpath(String xmlData) throws Exception {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ByteArrayInputStream stream = new ByteArrayInputStream(xmlData.getBytes());
        document = builder.parse(stream);
    }

    /**
     * Constructor allows you to pass an XML file for parsing
     *
     * @param file
     * @throws Exception
     */
    public Xpath(File file) throws Exception {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = builder.parse(new FileInputStream(file));
    }

    /**
     * Return a value by evaluating the string parameter using XPath
     *
     * @param xpathExpression
     * @return a value by evaluating the string parameter using XPath
     * @throws Exception
     */
    public String getValue(String xpathExpression) throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathExpression);
        return (String) expr.evaluate(document, XPathConstants.STRING);
    }

    /**
     * Return a Node by evaluating the string parameter using XPath
     *
     * @param xpathExpression
     * @return a Node by evaluating the string parameter using XPath
     * @throws Exception
     */
    public Node getNode(String xpathExpression) throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathExpression);
        return (Node) expr.evaluate(document, XPathConstants.NODE);
    }
}
