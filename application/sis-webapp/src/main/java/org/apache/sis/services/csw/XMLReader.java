/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more 
 * contributor license agreements.  See the NOTICE file distributed with 
 * this work for additional information regarding copyright ownership. 
 * The ASF licenses this file to You under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with 
 * the License.  You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.apache.sis.services.csw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import static org.apache.sis.internal.util.CollectionsExt.first;
import org.apache.sis.storage.geotiff.LandsatReader;
import org.apache.sis.storage.geotiff.ModisReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;

/**
 * @author Thi Phuong Hao NGUYEN
 * @author Minh Chinh VU
 */
public class XMLReader {

    ConfigurationReader path = new ConfigurationReader();

    public List<SummaryRecord> Metadata() throws Exception {
        //get all the files from a directory
        ConfigurationReader path = new ConfigurationReader();
        List<SummaryRecord> record = new ArrayList<>();
        File directory = new File(path.getPropValues());
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            final Metadata md;
            if (file.isFile() && file.getName().endsWith(".txt")) {

                try (BufferedReader in = new BufferedReader(new FileReader(file.getPath()))) {
                    final LandsatReader reader = new LandsatReader(in);
                    md = reader.read();
                }
            } else if (file.isFile() && file.getName().endsWith(".xml")) {
                File xml = new File(file.getPath());
                final ModisReader read = new ModisReader(xml);
                md = read.read();
            } else {
                continue; 
            }
            Identification id = first(md.getIdentificationInfo());
            SummaryRecord summary = new SummaryRecord();
            summary.setIdentifier(md.getFileIdentifier()); 

            summary.setFormat(first(first(md.getDistributionInfo()).getDistributionFormats()).getName().toString());
            summary.setTitle(id.getCitation().getTitle().toString()); 
            summary.setType(first(md.getHierarchyLevels()).name()); 
            summary.setModified(md.getDateStamp());
            summary.setSubject(first(first(id.getDescriptiveKeywords()).getKeywords()).toString()); 
            List<Responsibility> responsibility = new ArrayList<>(first(md.getIdentificationInfo()).getPointOfContacts());
            summary.setCreator(first(responsibility.get(0).getParties()).getName().toString()); 
            summary.setPublisher(first(responsibility.get(1).getParties()).getName().toString()); 
            summary.setContributor(first(responsibility.get(2).getParties()).getName().toString()); 

            summary.setLanguage(md.getLanguage().toString()); 
            summary.setRelation(first(id.getAggregationInfo()).getAggregateDataSetName().getTitle().toString());
            Extent et = first(id.getExtents());
            GeographicBoundingBox gbd = (GeographicBoundingBox) first(et.getGeographicElements());
            summary.setBoundingBox(new BoundingBox(gbd)); 
            record.add(summary);
        }
        return record;
    }
    public static void main(String[] args) throws Exception {

        XMLReader test = new XMLReader();

        //      System.out.println(test.listGeotiff());
        System.out.println(test.Metadata());

    }
}
