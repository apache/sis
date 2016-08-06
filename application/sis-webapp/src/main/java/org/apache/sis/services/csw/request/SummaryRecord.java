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
package org.apache.sis.services.csw.request;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.Metadata;

/**
 *
 * @author haonguyen
 */
@XmlRootElement(name = "SummaryRecord")
@XmlType(name = "SummaryRecordType")
public class SummaryRecord extends AbstractRecord {

    public SummaryRecord() {
    }

    public SummaryRecord(Metadata object, String version, String service) {
        super(object, version, service);
    }
    
    public AbstractRecord SummaryRecord(AbstractRecord ab) {
        AbstractRecord a = new AbstractRecord();
        a.version = ab.getVersion();
        a.service = ab.getService();
        a.identifier = ab.getIdentifier();
        a.BoundingBox = ab.getBoundingBox();
        a.title=ab.getTitle();
        a.type=ab.getType();
        a.modified=ab.getModified();
        a.format=ab.getFormat();
        a.relation=ab.getRelation();
        return a;
    }
}
