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
package org.apache.sis.feature;

import java.util.Map;
import java.util.HashMap;
import com.esri.core.geometry.Geometry;


/**
 * Simple Feature class.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultFeature {

    private Map<String, String> record;
    private Geometry geom;

    public Map<String, String> getRecord() {
        return record;
    }

    public void setRecord(HashMap<String, String> record) {
        this.record = record;
    }

    public Geometry getGeom() {
        return geom;
    }

    public void setGeom(Geometry geom) {
        this.geom = geom;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        for (String s : this.record.keySet()) {
            sb.append(s).append(": ").append(record.get(s)).append(lineSeparator);
        }
        return sb.toString();
    }
}
