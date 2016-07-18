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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.sis.internal.util.CollectionsExt.first;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.geotiff.LandsatReader;
import org.apache.sis.storage.geotiff.ModisReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;


/**
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Minh Chinh Vu (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class Record {
    
    Map<String, SummaryRecord> record;
    public Record() throws IOException, DataStoreException, Exception {
        ConfigurationReader path = new ConfigurationReader();
        File directory = new File(path.getPropValues());
        record = new HashMap();
        /**
         * Get all the files from a directory.
         *
         */
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
            String key = md.getFileIdentifier();
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
            record.put(key, summary);
        }
    }
    /**
     * Return all record of metadata
     *
     * @return
     * @throws Exception
     */
    public List<SummaryRecord> getAllRecord() { 
        return new ArrayList<SummaryRecord>(record.values()); 
    } 

    /**
     * Return All Record Paginated
     *
     * @param start
     * @param size
     * @return
     * @throws Exception
     */
    public List<SummaryRecord> getAllRecordPaginated(int start, int size) throws Exception {
        ArrayList<SummaryRecord> list = new ArrayList<SummaryRecord>(record.values()); 
        if (start + size > list.size()) 
            return new ArrayList<SummaryRecord>();
        
        return list.subList(start, start + size);
    }

    /**
     * Return record by id
     *
     * @param id
     * @return Record with id = identifier.
     * @throws Exception
     */
    public SummaryRecord getRecordById(String id) throws Exception {
        return record.get(id);
    }
}
