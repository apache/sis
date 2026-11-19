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
package org.apache.sis.gml;


/**
 * The GML version that {@link GMLWriter} should target.
 * The {@link #V3} value covers GML 3.0, 3.1 and 3.2 when reading, and emits canonical GML 3.2 output
 * when writing. The {@link #V3_1} value emits the same GML 3 encoding in the unversioned namespace
 * of GML 3.0 and 3.1, as required by some standards such as OGC Filter Encoding 1.1.
 *
 * @author  Johann Sorel (Geomatys)
 */
public enum GMLVersion {
    /**
     * 2.0
     */
    V2,

    /**
     * GML 3 (3.0, 3.1 or 3.2). Writers emit GML 3.2.
     */
    V3,

    /**
     * GML 3.1, written in the unversioned {@code http://www.opengis.net/gml} namespace.
     * This is the GML version used by OGC Filter Encoding 1.1 and Symbology Encoding 1.1.
     * Readers report GML 3.0 and 3.1 documents as {@link #V3}.
     */
    V3_1
}
