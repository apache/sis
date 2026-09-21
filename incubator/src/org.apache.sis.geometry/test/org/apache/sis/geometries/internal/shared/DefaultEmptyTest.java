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
package org.apache.sis.geometries.internal.shared;

import org.apache.sis.geometries.Empty;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.SampleSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import org.apache.sis.geometries.EmptyTest;
import org.apache.sis.geometries.DataPointsType;


/**
 * Tests {@link DefaultEmpty}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultEmptyTest extends EmptyTest {
    /**
     * Creates a new test case.
     */
    public DefaultEmptyTest() {
    }

    /**
     * Creates an empty geometry whose positions use the given coordinate reference system.
     */
    @Override
    protected Empty createEmpty(final CoordinateReferenceSystem crs) {
        final DataPointsType.Template attType = new DataPointsType.Template();
        attType.addOrReplaceAttribute(DataPointsType.ATT_POSITION, SampleSystem.of(crs), DataType.DOUBLE);
        return new DefaultEmpty(attType);
    }
}
