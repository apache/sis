/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2011-2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.apache.sis.referencing.datum;

import java.lang.reflect.Field;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.referencing.VerticalDatumTypes;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Tests the {@link DefaultVerticalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.internal.referencing.VerticalDatumTypesTest.class)
public final strictfp class DefaultVerticalDatumTest extends TestCase {
    /**
     * Tests the {@link DefaultVerticalDatum#getVerticalDatumType()} method in a state
     * simulating unmarshalling of GML 3.2 document.
     *
     * @throws NoSuchFieldException   Should never happen.
     * @throws IllegalAccessException Should never happen.
     */
    @Test
    public void testAfterUnmarshal() throws NoSuchFieldException, IllegalAccessException {
        final Field typeField = DefaultVerticalDatum.class.getDeclaredField("type");
        typeField.setAccessible(true);
        assertEquals(VerticalDatumType .GEOIDAL,       typeForName(typeField, "Geoidal height"));
        assertEquals(VerticalDatumType .DEPTH,         typeForName(typeField, "Some depth measurement"));
        assertEquals(VerticalDatumTypes.ELLIPSOIDAL,   typeForName(typeField, "Ellipsoidal height"));
        assertEquals(VerticalDatumType .OTHER_SURFACE, typeForName(typeField, "NotADepth"));
    }

    /**
     * Returns the vertical datum type inferred by {@link DefaultVerticalDatum} for the given name.
     */
    private static VerticalDatumType typeForName(final Field typeField, final String name) throws IllegalAccessException {
        final DefaultVerticalDatum datum = new DefaultVerticalDatum(
                singletonMap(DefaultVerticalDatum.NAME_KEY, name), VerticalDatumType.OTHER_SURFACE);
        typeField.set(datum, null);
        return datum.getVerticalDatumType();
    }
}
