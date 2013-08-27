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

package org.apache.sis.storage.shapefile;

import java.util.HashMap;
import com.esri.core.geometry.Geometry;

// 

/**
 * Provides a simple Feature class
 *
 * when dealing with codepages, should first check if there is a .cpg file 
 * 
 * @see <a href="http://www.clicketyclick.dk/databases/xbase/format/dbf.html#DBF_STRUCT">Xbase Data file</a>
 */

public class Feature {
	
	private HashMap<String, String> record;  
	private	Geometry geom;
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String lineSeparator = System.getProperty("line.separator", "\n");
		
		for (String s : this.record.keySet()) {
			sb.append(s).append(": ").append(record.get(s)).append(lineSeparator);
		}

		return sb.toString();
	}
	
	
	// getters and setters
	
	public HashMap<String, String> getRecord() {
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

}
