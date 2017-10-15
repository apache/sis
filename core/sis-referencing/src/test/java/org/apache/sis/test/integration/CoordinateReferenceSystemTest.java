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
package org.apache.sis.test.integration;

import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.TestFactorySource;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;


/**
 * Advances CRS constructions requiring the EPSG geodetic dataset.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.CRSTest.class,
    org.apache.sis.referencing.CommonCRSTest.class,
    org.apache.sis.referencing.factory.sql.EPSGFactoryTest.class,
    org.apache.sis.referencing.factory.MultiAuthoritiesFactoryTest.class
})
public final strictfp class CoordinateReferenceSystemTest extends TestCase {
    /**
     * Tests creation from codes in the
     * {@code "urn:ogc:def:type, type₁:authority₁:version₁:code₁, type₂:authority₂:version₂:code₂"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromCombinedURN() throws FactoryException {
        assumeNotNull(TestFactorySource.getSharedFactory());
        CoordinateReferenceSystem crs = CRS.forCode("urn:ogc:def:crs, crs:EPSG::27700, crs:EPSG::5701");
        assertSame("OSGB 1936 / British National Grid + ODN height", CRS.forCode("EPSG:7405"), crs);
    }
}
