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
package org.apache.sis.referencing.factory;

import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.test.referencing.ObjectFactoryTest;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.test.DependsOn;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Ignore;


/**
 * Tests {@link GeodeticObjectFactory} using the suite of tests provided in the GeoAPI project.
 * Note that this does not include authority factories tests or GIGS tests.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn({
    org.apache.sis.referencing.crs.DefaultGeocentricCRSTest.class,
    org.apache.sis.referencing.crs.DefaultGeographicCRSTest.class,
    org.apache.sis.referencing.crs.DefaultProjectedCRSTest.class
})
public final strictfp class GeodeticObjectFactoryTest extends ObjectFactoryTest {
    /**
     * Creates a new test suite using the singleton factory instance.
     */
    public GeodeticObjectFactoryTest() {
        super(DefaultFactories.forBuildin(DatumFactory.class),
              DefaultFactories.forBuildin(CSFactory   .class),
              DefaultFactories.forBuildin(CRSFactory  .class),
              new DefaultCoordinateOperationFactory());         // TODO: use DefaultFactories.
    }

    @Override
    @Deprecated
    @Ignore("Replaced by testProjectedWithGeoidalHeight()")
    public void testProjected3D() throws FactoryException {
    }

    @Override
    @Ignore("This tests need the Transverse Mercator projection, which is not yet implemented in SIS.")
    public void testProjectedWithGeoidalHeight() throws FactoryException {
    }
}
