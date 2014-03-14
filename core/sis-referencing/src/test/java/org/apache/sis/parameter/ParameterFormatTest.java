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
import java.util.Locale;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.OGC;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.OGP;


/**
 * Tests the {@link ParameterFormat} class.
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
    public static void createParameterDescriptor() {
        ParameterBuilder builder = new ParameterBuilder();
        builder.setCodeSpace(OGP, "EPSG").setRequired(true);
        ParameterDescriptor<?>[] parameters = {
            builder.addName("Latitude of natural origin")      .addName(OGC, "latitude_of_origin").createBounded( -80,  +84,  40, NonSI.DEGREE_ANGLE),
            builder.addName("Longitude of natural origin")     .addName(OGC, "central_meridian")  .createBounded(-180, +180, -60, NonSI.DEGREE_ANGLE),
            builder.addName("Scale factor at natural origin")  .addName(OGC, "scale_factor")      .createStrictlyPositive(1, Unit.ONE),
            builder.addName("False easting").setRequired(false).addName(OGC, "false_easting")     .create( 5000, SI.METRE),
            builder.addName("False northing")                  .addName(OGC, "false_northing")    .create(10000, SI.METRE)
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
    public static void clearParameterDescriptor() {
        descriptor = null;
    }

    /**
     * Creates parameter values with some arbitrary values different than the default values.
     * This method intentionally leaves {@code "central_meridian"} (a mandatory parameter) and
     * {@code "false_easting"} (an optional parameter) undefined, in order to test whether the
     * formatter fallback on default values.
     */
    private static ParameterValueGroup createParameterValues() {
        final ParameterValueGroup group = descriptor.createValue();
        group.parameter("latitude_of_origin").setValue(20);
        group.parameter("scale_factor").setValue(0.997);
        group.parameter("false_northing").setValue(20, SI.KILOMETRE);
        return group;
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} for descriptors with {@code ContentLevel.BRIEF}.
     * All parameter shall unconditionally be shown, even if optional. The table contains a column saying whether
     * the parameter is mandatory or optional.
     */
    @Test
    public void testFormatBriefDescriptors() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        final String text = format.format(descriptor);
        assertMultilinesEquals(
                "EPSG: Mercator (variant A)\n" +
                "┌────────────────────────────────┬────────┬────────────┬───────────────┬───────────────┐\n" +
                "│ Name (EPSG)                    │ Type   │ Obligation │ Value domain  │ Default value │\n" +
                "├────────────────────────────────┼────────┼────────────┼───────────────┼───────────────┤\n" +
                "│ Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │        40.0°  │\n" +
                "│ Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │       -60.0°  │\n" +
                "│ Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │         1.0   │\n" +
                "│ False easting                  │ Double │ Optional   │   (-∞ … ∞) m  │      5000.0 m │\n" +
                "│ False northing                 │ Double │ Optional   │   (-∞ … ∞) m  │     10000.0 m │\n" +
                "└────────────────────────────────┴────────┴────────────┴───────────────┴───────────────┘\n", text);
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} for values with {@code ContentLevel.BRIEF}.
     * Expected behavior:
     *
     * <ul>
     *   <li>{@code "Longitude of natural origin"} parameter, while not defined, shall be shown with its default
     *       value, because this parameter is defined as mandatory in this test suite.</li>
     *   <li>{@code "False easting"} parameter shall be omitted, because this parameter is defined as
     *       optional in this test suite and its value has not been defined.</li>
     *   <li>The obligation column is omitted, because not very useful in the case of parameter values..</li>
     * </ul>
     */
    @Test
    @DependsOnMethod("testFormatBriefDescriptors")
    public void testFormatBriefValues() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        final String text = format.format(createParameterValues());
        assertMultilinesEquals(
                "EPSG: Mercator (variant A)\n" +
                "┌────────────────────────────────┬────────┬───────────────┬──────────┐\n" +
                "│ Name (EPSG)                    │ Type   │ Value domain  │ Value    │\n" +
                "├────────────────────────────────┼────────┼───────────────┼──────────┤\n" +
                "│ Latitude of natural origin     │ Double │  [-80 … 84]°  │  20.0°   │\n" +
                "│ Longitude of natural origin    │ Double │ [-180 … 180]° │ -60.0°   │\n" +
                "│ Scale factor at natural origin │ Double │    (0 … ∞)    │ 0.997    │\n" +
                "│ False northing                 │ Double │   (-∞ … ∞) m  │  20.0 km │\n" +
                "└────────────────────────────────┴────────┴───────────────┴──────────┘\n", text);
    }

    /**
     * Tests formatting in a non-English locale.
     */
    @Test
    @DependsOnMethod("testFormatBriefValues")
    public void testFormatLocalized() {
        final ParameterFormat format = new ParameterFormat(Locale.FRANCE, null);
        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        final String text = format.format(createParameterValues());
        assertMultilinesEquals(
                "EPSG: Mercator (variant A)\n" +
                "┌────────────────────────────────┬────────┬─────────────────────┬──────────┐\n" +
                "│ Nom (EPSG)                     │ Type   │ Domaine des valeurs │ Valeur   │\n" +
                "├────────────────────────────────┼────────┼─────────────────────┼──────────┤\n" +
                "│ Latitude of natural origin     │ Double │  [-80 … 84]°        │    20°   │\n" +
                "│ Longitude of natural origin    │ Double │ [-180 … 180]°       │   -60°   │\n" +
                "│ Scale factor at natural origin │ Double │    (0 … ∞)          │ 0,997    │\n" +
                "│ False northing                 │ Double │   (-∞ … ∞) m        │    20 km │\n" +
                "└────────────────────────────────┴────────┴─────────────────────┴──────────┘\n", text);
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} for descriptors with {@code ContentLevel.DETAILED}.
     */
    @Test
    public void testFormatDetailedDescriptors() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.DETAILED);
        final String text = format.format(descriptor);
        assertMultilinesEquals(
                "EPSG: Mercator (variant A) (9804)\n" +
                "EPSG: Mercator (1SP)\n" +
                "OGC:  Mercator_1SP\n" +
                "╔══════════════════════════════════════╤════════╤════════════╤═══════════════╤═══════════════╗\n" +
                "║ Name                                 │ Type   │ Obligation │ Value domain  │ Default value ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │        40.0°  ║\n" +
                "║ OGC:  latitude_of_origin             │        │            │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │       -60.0°  ║\n" +
                "║ OGC:  central_meridian               │        │            │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │         1.0   ║\n" +
                "║ OGC:  scale_factor                   │        │            │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: False easting                  │ Double │ Optional   │   (-∞ … ∞) m  │      5000.0 m ║\n" +
                "║ OGC:  false_easting                  │        │            │               │               ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼───────────────╢\n" +
                "║ EPSG: False northing                 │ Double │ Optional   │   (-∞ … ∞) m  │     10000.0 m ║\n" +
                "║ OGC:  false_northing                 │        │            │               │               ║\n" +
                "╚══════════════════════════════════════╧════════╧════════════╧═══════════════╧═══════════════╝\n", text);
    }

    /**
     * Tests {@link ParameterFormat#format(Object, Appendable)} for values with {@code ContentLevel.DETAILED}.
     * The same comments than {@link #testFormatBriefValues()} apply, except that the column of obligation is
     * still shown.
     */
    @Test
    public void testFormatDetailedValues() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.DETAILED);
        final String text = format.format(createParameterValues());
        assertMultilinesEquals(
                "EPSG: Mercator (variant A) (9804)\n" +
                "EPSG: Mercator (1SP)\n" +
                "OGC:  Mercator_1SP\n" +
                "╔══════════════════════════════════════╤════════╤════════════╤═══════════════╤══════════╗\n" +
                "║ Name                                 │ Type   │ Obligation │ Value domain  │ Value    ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼──────────╢\n" +
                "║ EPSG: Latitude of natural origin     │ Double │ Mandatory  │  [-80 … 84]°  │  20.0°   ║\n" +
                "║ OGC:  latitude_of_origin             │        │            │               │          ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼──────────╢\n" +
                "║ EPSG: Longitude of natural origin    │ Double │ Mandatory  │ [-180 … 180]° │ -60.0°   ║\n" +
                "║ OGC:  central_meridian               │        │            │               │          ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼──────────╢\n" +
                "║ EPSG: Scale factor at natural origin │ Double │ Mandatory  │    (0 … ∞)    │ 0.997    ║\n" +
                "║ OGC:  scale_factor                   │        │            │               │          ║\n" +
                "╟──────────────────────────────────────┼────────┼────────────┼───────────────┼──────────╢\n" +
                "║ EPSG: False northing                 │ Double │ Optional   │   (-∞ … ∞) m  │  20.0 km ║\n" +
                "║ OGC:  false_northing                 │        │            │               │          ║\n" +
                "╚══════════════════════════════════════╧════════╧════════════╧═══════════════╧══════════╝\n", text);
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
                "│ False easting                  │ false_easting      │\n" +
                "│ False northing                 │ false_northing     │\n" +
                "└────────────────────────────────┴────────────────────┘\n", text);
    }

    /**
     * Tests the effect of {@link ParameterFormat#setPreferredCodespaces(String[])}.
     */
    @Test
    @DependsOnMethod({"testFormatNameSummary", "testFormatBriefValues"})
    public void testPreferredCodespaces() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.NAME_SUMMARY);
        assertNull(format.getPreferredCodespaces());
        format.setPreferredCodespaces("OGC", "EPSG");
        assertArrayEquals(new String[] {"OGC", "EPSG"}, format.getPreferredCodespaces());
        String text = format.format(new IdentifiedObject[] {descriptor});
        assertMultilinesEquals(
                "┌────────────┬──────────────┬──────────────────────┐\n" +
                "│ Identifier │ OGC          │ EPSG                 │\n" +
                "├────────────┼──────────────┼──────────────────────┤\n" +
                "│ EPSG:9804  │ Mercator_1SP │ Mercator (variant A) │\n" +
                "└────────────┴──────────────┴──────────────────────┘\n", text);

        format.setPreferredCodespaces("OGC");
        assertArrayEquals(new String[] {"OGC"}, format.getPreferredCodespaces());
        text = format.format(new IdentifiedObject[] {descriptor});
        assertMultilinesEquals(
                "┌────────────┬──────────────┐\n" +
                "│ Identifier │ OGC          │\n" +
                "├────────────┼──────────────┤\n" +
                "│ EPSG:9804  │ Mercator_1SP │\n" +
                "└────────────┴──────────────┘\n", text);

        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        text = format.format(createParameterValues());
        assertMultilinesEquals(
                "OGC: Mercator_1SP\n" +
                "┌────────────────────┬────────┬───────────────┬──────────┐\n" +
                "│ Name (OGC)         │ Type   │ Value domain  │ Value    │\n" +
                "├────────────────────┼────────┼───────────────┼──────────┤\n" +
                "│ latitude_of_origin │ Double │  [-80 … 84]°  │  20.0°   │\n" +
                "│ central_meridian   │ Double │ [-180 … 180]° │ -60.0°   │\n" +
                "│ scale_factor       │ Double │    (0 … ∞)    │ 0.997    │\n" +
                "│ false_northing     │ Double │   (-∞ … ∞) m  │  20.0 km │\n" +
                "└────────────────────┴────────┴───────────────┴──────────┘\n", text);
    }

    /**
     * Tests the formatting of a parameter value group with an invalid cardinality.
     * While not allowed by ISO 19111, the Apache SIS implementation shall be robust
     * to those cases.
     */
    @Test
    @DependsOnMethod("testFormatBriefDescriptors")
    public void testInvalidCardinality() {
        final ParameterFormat format = new ParameterFormat(null, null);
        format.setContentLevel(ParameterFormat.ContentLevel.BRIEF);
        final String text = format.format(DefaultParameterDescriptorGroupTest.M1_M1_O1_O2);
        assertMultilinesEquals(
                "Test group\n" +
                "┌─────────────┬─────────┬────────────┬──────────────┬───────────────┐\n" +
                "│ Name        │ Type    │ Obligation │ Value domain │ Default value │\n" +
                "├─────────────┼─────────┼────────────┼──────────────┼───────────────┤\n" +
                "│ Mandatory 1 │ Integer │ Mandatory  │              │            10 │\n" +
                "│ Mandatory 2 │ Integer │ Mandatory  │              │            10 │\n" +
                "│ Optional 3  │ Integer │ Optional   │              │            10 │\n" +
                "│ Optional 4  │ Integer │ 0 … 2      │              │            10 │\n" +
                "└─────────────┴─────────┴────────────┴──────────────┴───────────────┘\n", text);
    }
}
