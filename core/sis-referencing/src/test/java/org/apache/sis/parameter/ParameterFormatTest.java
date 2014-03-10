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
package org.apache.sis.parameter;

import java.util.List;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.HardCodedCitations.OGC;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.OGP;


/**
 * Tests the {@link ParameterBuilder} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(ParameterBuilderTest.class)
public final strictfp class ParameterFormatTest extends TestCase {
    /**
     * The parameter descriptors used for all tests in this class.
     */
    private static ParameterDescriptorGroup descriptor;

    /**
     * Creates the parameter descriptors to be used by all tests in this class. This method creates
     * a variant of the example documented in the {@link DefaultParameterDescriptorGroup} javadoc
     * with arbitrary non-zero default values.
     */
    @BeforeClass
    public static void createParameters() {
        ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(OGP, "EPSG").setRequired(true);
        ParameterDescriptor<?>[] parameters = {
            builder.addName("Latitude of natural origin")    .addName(OGC, "latitude_of_origin").createBounded( -80,  +84,  40, NonSI.DEGREE_ANGLE),
            builder.addName("Longitude of natural origin")   .addName(OGC, "central_meridian")  .createBounded(-180, +180, -60, NonSI.DEGREE_ANGLE),
            builder.addName("Scale factor at natural origin").addName(OGC, "scale_factor")      .createStrictlyPositive(1, Unit.ONE),
            builder.addName("False easting")                 .addName(OGC, "FalseEasting")      .create( 5000, SI.METRE),
            builder.addName("False northing")                .addName(OGC, "FalseNorthing")     .create(10000, SI.METRE)
        };
        builder.addIdentifier("9804")
               .addName("Mercator (variant A)")
               .addName("Mercator (1SP)")
               .addName(OGC, "Mercator_1SP");
        descriptor = builder.createGroup(parameters);
    }

    /**
     * Forgets the parameter descriptors after all tests are done.
     */
    @AfterClass
    public static void clearParameters() {
        descriptor = null;
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} for a {@link ParameterDescriptorGroup}.
     */
    @Test
    public void testFormatDescriptor() {
        final ParameterFormat format = new ParameterFormat(null, null);
        String text = format.format(descriptor);
        // TODO: verify output

        format.setContentLevel(ParameterFormat.ContentLevel.DETAILED);
        text = format.format(descriptor);
        // TODO: verify output
    }

    @Test
    public void testFormatIdentifiedObjects() throws Exception {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.NAME_SUMMARY);
        final List<GeneralParameterDescriptor> parameters = descriptor.descriptors();
        final String text = format.format(parameters.toArray(new IdentifiedObject[parameters.size()]));
        // TODO: verify output
    }
}
