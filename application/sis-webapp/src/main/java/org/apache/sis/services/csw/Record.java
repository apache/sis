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

import org.apache.sis.services.csw.request.SummaryRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.services.csw.reponse.GetRecordByIdReponse;
import org.apache.sis.services.csw.reponse.GetRecordsReponse;
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
public class Record {

    Map<String, SummaryRecord> record;

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
    public Record(String path,String version,String service) throws IOException, DataStoreException, Exception {
        Catalog catalog = new Catalog(path);
        record = new HashMap();
        /**
         * Get all the files from a directory.
         *
         */
        for (Metadata id : catalog.getAllMetadata()) {
            SummaryRecord summary = new SummaryRecord(id,version,service);
            String key = id.getFileIdentifier();
            record.put(key, summary);
        }
    }

    /**
     * Return all record of metadata
     *
     * @return all record of metadata
     * @throws Exception Exception checked exceptions. Checked exceptions need
     * to be declared in a method or constructor's {@code throws} clause if they
     * can be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public List<SummaryRecord> getAllRecord() {
        return new ArrayList<SummaryRecord>(record.values());
    }

    /**
     * Return All Record Paginated
     *
     * @param start start record
     * @param size size list record
     * @return All Record Paginated
     * @throws Exception checked exceptions. Checked exceptions need to be
     * declared in a method or constructor's {@code throws} clause if they can
     * be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public GetRecordsReponse getAllRecordPaginated(int start, int size) throws Exception {
        GetRecordsReponse reponse = new GetRecordsReponse();
        ArrayList<SummaryRecord> list = new ArrayList<SummaryRecord>(record.values());
        if (start + size > list.size()) {
            reponse.setRecord(new ArrayList<SummaryRecord>());
            return reponse; 
        }
        reponse.setRecord(list.subList(start, start + size));
        return reponse;
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
    public GetRecordByIdReponse getRecordById(String id) throws Exception {
        GetRecordByIdReponse reponse = new GetRecordByIdReponse();
        reponse.setRecordid(record.get(id));
        return reponse;
    }

}
