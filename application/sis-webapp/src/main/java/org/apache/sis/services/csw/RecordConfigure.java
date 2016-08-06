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

import org.apache.sis.services.csw.request.AbstractRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.services.csw.reponse.GetRecordByIdReponse;
import org.apache.sis.services.csw.request.BriefRecord;
import org.apache.sis.services.csw.request.Record;
import org.apache.sis.services.csw.request.SummaryRecord;
import org.apache.sis.storage.DataStoreException;
import org.opengis.metadata.Metadata;

/**
 *
 * @author Thi Phuong Hao Nguyen (VNSC)
 * @author Minh Chinh Vu (VNSC)
 * @since 0.8
 * @version 0.8
 * @module
 */
public class RecordConfigure {

    Map<String, AbstractRecord> record;

    public RecordConfigure() {
    }

    /**
     * Contructor's Record
     *
     * @throws IOException the general class of exceptions produced by failed or
     * interrupted I/O operations
     * @throws DataStoreException if an error occurred while reading a metadata
     * file.
     * @throws Exception Exception checked exceptions. Checked exceptions need
     * to be declared in a method or constructor's {@code throws} clause if they
     * can be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public RecordConfigure(String path, String version, String service) throws IOException, DataStoreException, Exception {
        Catalog catalog = new Catalog(path);
        record = new HashMap();
        /**
         * Get all the files from a directory.
         *
         */
        for (Metadata id : catalog.getAllMetadata()) {
            AbstractRecord records = new AbstractRecord(id, version, service);
            String key = id.getFileIdentifier();
            record.put(key, records);
        }
    }

    /**
     * Return all record tree
     *
     * @return all record tree
     */
    public List<AbstractRecord> getAllRecord() {
        return new ArrayList<AbstractRecord>(record.values());
    }

    public List<AbstractRecord> getAllRecordPaginated(int start, int size) throws Exception {
        List<AbstractRecord> list = getAllRecord();
        if (start + size > list.size()) {
            return new ArrayList<AbstractRecord>();
        }
        return list.subList(start, start + size);
    }

    public RecordResult getRecords(int start, int size, String setelement) throws Exception {
        List<AbstractRecord> list = getAllRecordPaginated(start, size);
        RecordResult result = new RecordResult();
        result.setElementset(setelement);
        result.setNextrecord(size*2);
        result.setNumberofrecordsmatched(getAllRecord().size());
        result.setNumberofrecordsreturned(size);
        if (("summary").equals(setelement)) {
            List<AbstractRecord> a = new ArrayList<>();
            for (AbstractRecord b : list) {
                AbstractRecord c = new SummaryRecord().SummaryRecord(b);
                a.add(c);
            }
            System.out.println(a);
            result.setSummary(a);
            return result;
        } else if (("full").equals(setelement)) {
            List<AbstractRecord> a = new ArrayList<>();
            for (AbstractRecord b : list) {
                AbstractRecord c = new Record().Record(b);
                a.add(c);
            }
            result.setRecord(a);
            return result;
        } else if (("brief").equals(setelement)) {
            List<AbstractRecord> a = new ArrayList<>();
            for (AbstractRecord b : list) {
                AbstractRecord c = new BriefRecord().BriefRecord(b);
                a.add(c);
            }
            result.setBrif(a);
            return result;
        }
        return result;
    }

    /**
     * Return record by id
     *
     * @param id identifier for record .
     * @return Record with id = identifier.
     * @return Record with id = identifier.
     * @throws Exception checked exceptions. Checked exceptions need to be
     * declared in a method or constructor's {@code throws} clause if they can
     * be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public GetRecordByIdReponse getRecordById(String id, String setelement) throws Exception {
        GetRecordByIdReponse reponse = new GetRecordByIdReponse();
        if (setelement.equals("summary")) {
            AbstractRecord summary = new SummaryRecord().SummaryRecord(record.get(id));
            reponse.setSummary(summary);
            return reponse;
        }
        if (setelement.equals("full")) {
            AbstractRecord full = new Record().Record(record.get(id));
            reponse.setRecord(full);
            return reponse;
        } else if (setelement.equals("brief")) {
            AbstractRecord brief = new BriefRecord().BriefRecord(record.get(id));
            reponse.setBrief(brief);
            return reponse;
        }
        return reponse;
    }

    public static void main(String[] args) throws Exception {
        RecordConfigure a = new RecordConfigure("/home/haonguyen/data", "2.0.2", "CSW");

        //BriefRecord brief = b.getBrief();
        System.out.println(a.getRecordById("LC81230522014071LGN00", "full"));
        System.out.println(a.getRecords(1, 6, "summary"));
    }
}
