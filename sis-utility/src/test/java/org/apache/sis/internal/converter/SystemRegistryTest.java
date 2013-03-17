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
import java.util.List;
import java.util.Collection;
import org.opengis.metadata.spatial.PixelOrientation;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.internal.converter.SystemRegistry.INSTANCE;


/**
 * Tests the {@link SystemRegistry#INSTANCE} constant.
 * This class shall not perform any conversion tests; it shall only checks the registrations.
 * Conversion tests are the purpose of other test classes in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@DependsOn(ConverterRegistryTest.class)
public final strictfp class SystemRegistryTest extends TestCase {
    /**
     * Tests the creation of {@link StringConverter}.
     */
    @Test
    public void testStringFile() {
        final ObjectConverter<String,File> c1 = INSTANCE.findExact(String.class, File.class);
        final ObjectConverter<File,String> c2 = INSTANCE.findExact(File.class, String.class);
        assertInstanceOf("File ← String", StringConverter.File.class, c1);
        assertInstanceOf("String ← File", ObjectToString.class, c2);
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
        final ObjectConverter<String, PixelOrientation> c1 = INSTANCE.findExact(String.class, PixelOrientation.class);
        final ObjectConverter<PixelOrientation, String> c2 = INSTANCE.findExact(PixelOrientation.class, String.class);
        assertInstanceOf("PixelOrientation ← String", StringConverter.CodeList.class, c1);
        assertInstanceOf("String ← PixelOrientation", ObjectToString.CodeList.class,  c2);
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
        final ObjectConverter<Float,Double> c1 = INSTANCE.findExact(Float.class, Double.class);
        final ObjectConverter<Double,Float> c2 = INSTANCE.findExact(Double.class, Float.class);
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
        final ObjectConverter<Date,Long> c1 = INSTANCE.findExact(Date.class, Long.class);
        final ObjectConverter<Long,Date> c2 = INSTANCE.findExact(Long.class, Date.class);
        assertInstanceOf("Long ← Date", DateConverter.Long.class, c1);
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
        final ObjectConverter<Date, java.sql.Date> c1 = INSTANCE.findExact(Date.class, java.sql.Date.class);
        final ObjectConverter<java.sql.Date, Date> c2 = INSTANCE.findExact(java.sql.Date.class, Date.class);
        assertInstanceOf("sql.Date ← Date", DateConverter.SQL.class, c1);
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
        final ObjectConverter<File,URI> c1 = INSTANCE.findExact(File.class, URI.class);
        final ObjectConverter<URI,File> c2 = INSTANCE.findExact(URI.class, File.class);
        assertInstanceOf("URI ← File", PathConverter.FileURI.class, c1);
        assertInstanceOf("File ← URI", PathConverter.URIFile.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link AngleConverter}.
     */
    @Test
    public void testAngle() {
        final ObjectConverter<Angle,Double> c1 = INSTANCE.findExact(Angle.class, Double.class);
        final ObjectConverter<Double,Angle> c2 = INSTANCE.findExact(Double.class, Angle.class);
        assertInstanceOf("Double ← Angle", AngleConverter.class, c1);
        assertInstanceOf("Angle ← Double", AngleConverter.Inverse.class, c2);
        assertSame("inverse()", c2, c1.inverse());
        assertSame("inverse()", c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link CollectionConverter}.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testCollection() {
        final ObjectConverter<Collection,List> c1 = INSTANCE.findExact(Collection.class, List.class);
        assertInstanceOf("List ← Collection", CollectionConverter.class, c1);
        assertSame(c1, assertSerializedEquals(c1));
    }
}
