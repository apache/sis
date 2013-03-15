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
package org.apache.sis.internal.converter;

import java.io.File;
import java.net.URI;
import java.util.Date;
import org.opengis.metadata.spatial.PixelOrientation;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.internal.converter.HeuristicRegistry.SYSTEM;


/**
 * Tests the {@link HeuristicRegistry#SYSTEM} constant.
 * This class shall not perform any conversion tests; it shall only checks the registrations.
 * Conversion tests are the purpose of other test classes in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn(ConverterRegistryTest.class)
public final strictfp class HeuristicRegistryTest extends TestCase {
    /**
     * Tests the creation of {@link StringConverter}.
     */
    @Test
    public void testStringFile() {
        final ObjectConverter<String,File> c1 = SYSTEM.findExact(String.class, File.class);
        final ObjectConverter<File,String> c2 = SYSTEM.findExact(File.class, String.class);
        assertInstanceOf("File ← String", StringConverter.class, c1);
        assertInstanceOf("String ← File", ObjectToString.class,  c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of code list converter.
     */
    @Test
    public void testStringCodeList() {
        final ObjectConverter<String, PixelOrientation> c1 = SYSTEM.findExact(String.class, PixelOrientation.class);
        final ObjectConverter<PixelOrientation, String> c2 = SYSTEM.findExact(PixelOrientation.class, String.class);
        assertInstanceOf("PixelOrientation ← String", StringConverter.class, c1);
        assertInstanceOf("String ← PixelOrientation", ObjectToString.class,  c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link NumberConverter}.
     */
    @Test
    public void testFloatDouble() {
        final ObjectConverter<Float,Double> c1 = SYSTEM.findExact(Float.class, Double.class);
        final ObjectConverter<Double,Float> c2 = SYSTEM.findExact(Double.class, Float.class);
        assertInstanceOf("Double ← Float", NumberConverter.class, c1);
        assertInstanceOf("Float ← Double", NumberConverter.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link DateConverter}.
     */
    @Test
    public void testDateLong() {
        final ObjectConverter<Date,Long> c1 = SYSTEM.findExact(Date.class, Long.class);
        final ObjectConverter<Long,Date> c2 = SYSTEM.findExact(Long.class, Date.class);
        assertInstanceOf("Long ← Date", DateConverter.class,   c1);
        assertInstanceOf("Date ← Long", SystemConverter.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link DateConverter} from {@code java.util.Date}
     * to {@code java.sql.Date}. The inverse converter is an identity converter.
     */
    @Test
    public void testDateSQL() {
        final ObjectConverter<Date, java.sql.Date> c1 = SYSTEM.findExact(Date.class, java.sql.Date.class);
        final ObjectConverter<java.sql.Date, Date> c2 = SYSTEM.findExact(java.sql.Date.class, Date.class);
        assertInstanceOf("sql.Date ← Date", DateConverter.class,     c1);
        assertInstanceOf("Date ← sql.Date", IdentityConverter.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link PathConverter}.
     */
    @Test
    public void testFileURI() {
        final ObjectConverter<File,URI> c1 = SYSTEM.findExact(File.class, URI.class);
        final ObjectConverter<URI,File> c2 = SYSTEM.findExact(URI.class, File.class);
        assertInstanceOf("URI ← File", PathConverter.class, c1);
        assertInstanceOf("File ← URI", PathConverter.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }
}
