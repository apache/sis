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

import static org.apache.sis.test.Assert.*;
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
     * Tests {@link ParameterFormat#format(Object, Appendable)} with {@code ContentLevel.BRIEF}.
     */
    @Test
    public void testFormatBrief() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        String text = format.format(descriptor);
        assertMultilinesEquals(
                "EPSG: Mercator (variant A)\n" +
                "┌──────────────────────────────────────┬────────┬───────────────┬───────────────┐\n" +
                "│ Name                                 │ Type   │ Value domain  │ Default value │\n" +
                "├──────────────────────────────────────┼────────┼───────────────┼───────────────┤\n" +
                "│ EPSG: Latitude of natural origin     │ Double │  [-80 … 84]°  │        40.0°  │\n" +
                "│ EPSG: Longitude of natural origin    │ Double │ [-180 … 180]° │       -60.0°  │\n" +
                "│ EPSG: Scale factor at natural origin │ Double │    (0 … ∞)    │         1.0   │\n" +
                "│ EPSG: False easting                  │ Double │   (-∞ … ∞) m  │      5000.0 m │\n" +
                "│ EPSG: False northing                 │ Double │   (-∞ … ∞) m  │     10000.0 m │\n" +
                "└──────────────────────────────────────┴────────┴───────────────┴───────────────┘\n", text);
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} with {@code ContentLevel.DETAILED}.
     */
    @Test
    public void testFormatDetailed() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.DETAILED);
        String text = format.format(descriptor);
        assertMultilinesEquals(
                "EPSG: Mercator (variant A) (9804)\n" +
                "EPSG: Mercator (1SP)\n" +
                "OGC:  Mercator_1SP\n" +
                "╔══════════════════════════════════════╤════════╤═══════════════╤═══════════════╗\n" +
                "║ Name                                 │ Type   │ Value domain  │ Default value ║\n" +
                "╟──────────────────────────────────────┼────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Latitude of natural origin     │ Double │  [-80 … 84]°  │        40.0°  ║\n" +
                "║ OGC:  latitude_of_origin             │        │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Longitude of natural origin    │ Double │ [-180 … 180]° │       -60.0°  ║\n" +
                "║ OGC:  central_meridian               │        │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Scale factor at natural origin │ Double │    (0 … ∞)    │         1.0   ║\n" +
                "║ OGC:  scale_factor                   │        │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: False easting                  │ Double │   (-∞ … ∞) m  │      5000.0 m ║\n" +
                "║ OGC:  FalseEasting                   │        │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: False northing                 │ Double │   (-∞ … ∞) m  │     10000.0 m ║\n" +
                "║ OGC:  FalseNorthing                  │        │               │               ║\n" +
                "╚══════════════════════════════════════╧════════╧═══════════════╧═══════════════╝\n", text);
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} with {@code ContentLevel.NAME_SUMMARY}.
     */
    @Test
    public void testFormatNameSummary() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.NAME_SUMMARY);
        final List<GeneralParameterDescriptor> parameters = descriptor.descriptors();
        String text = format.format(new IdentifiedObject[] {descriptor});
        assertMultilinesEquals(
                "┌────────────┬──────────────────────┬──────────────┐\n" +
                "│ Identifier │ EPSG                 │ OGC          │\n" +
                "├────────────┼──────────────────────┼──────────────┤\n" +
                "│ EPSG:9804  │ Mercator (variant A) │ Mercator_1SP │\n" +
                "└────────────┴──────────────────────┴──────────────┘\n", text);

        text = format.format(parameters.toArray(new IdentifiedObject[parameters.size()]));
        assertMultilinesEquals(
                "┌────────────────────────────────┬────────────────────┐\n" +
                "│ EPSG                           │ OGC                │\n" +
                "├────────────────────────────────┼────────────────────┤\n" +
                "│ Latitude of natural origin     │ latitude_of_origin │\n" +
                "│ Longitude of natural origin    │ central_meridian   │\n" +
                "│ Scale factor at natural origin │ scale_factor       │\n" +
                "│ False easting                  │ FalseEasting       │\n" +
                "│ False northing                 │ FalseNorthing      │\n" +
                "└────────────────────────────────┴────────────────────┘\n", text);
    }
}
