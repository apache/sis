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
package org.apache.sis.converter;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Collection;
import java.lang.annotation.ElementType;
import org.opengis.metadata.citation.OnLineFunction;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.ObjectConverter;
import static org.apache.sis.converter.SystemRegistry.INSTANCE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link SystemRegistry#INSTANCE} constant.
 * This class shall not perform any conversion tests; it shall only checks the registrations.
 * Conversion tests are the purpose of other test classes in this package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SystemRegistryTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SystemRegistryTest() {
    }

    /**
     * Tests the creation of {@link StringConverter}.
     */
    @Test
    public void testStringAndInteger() {
        final ObjectConverter<String,Integer> c1 = INSTANCE.findExact(String.class, Integer.class);
        final ObjectConverter<Integer,String> c2 = INSTANCE.findExact(Integer.class, String.class);
        assertInstanceOf(StringConverter.Integer.class, c1, "Integer ← String");
        assertInstanceOf(ObjectToString.class,          c2, "String ← Integer");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link StringConverter}.
     */
    @Test
    public void testStringAndFile() {
        final ObjectConverter<String,File> c1 = INSTANCE.findExact(String.class, File.class);
        final ObjectConverter<File,String> c2 = INSTANCE.findExact(File.class, String.class);
        assertInstanceOf(StringConverter.File.class, c1, "File ← String");
        assertInstanceOf(ObjectToString.class,       c2, "String ← File");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of code list converter.
     */
    @Test
    public void testStringAndCodeList() {
        final ObjectConverter<String, OnLineFunction> c1 = INSTANCE.findExact(String.class, OnLineFunction.class);
        final ObjectConverter<OnLineFunction, String> c2 = INSTANCE.findExact(OnLineFunction.class, String.class);
        assertInstanceOf(StringConverter.CodeList.class, c1, "OnLineFunction ← String");
        assertInstanceOf(ObjectToString.CodeList.class,  c2, "String ← OnLineFunction");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of an enum converter.
     */
    @Test
    public void testStringAndEnum() {
        final ObjectConverter<String, ElementType> c1 = INSTANCE.findExact(String.class, ElementType.class);
        final ObjectConverter<ElementType, String> c2 = INSTANCE.findExact(ElementType.class, String.class);
        assertInstanceOf(StringConverter.Enum.class, c1, "ElementType ← String");
        assertInstanceOf(ObjectToString.Enum.class,  c2, "String ← ElementType");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link NumberConverter}.
     */
    @Test
    public void testFloatAndDouble() {
        final ObjectConverter<Float,Double> c1 = INSTANCE.findExact(Float.class, Double.class);
        final ObjectConverter<Double,Float> c2 = INSTANCE.findExact(Double.class, Float.class);
        assertInstanceOf(NumberConverter.class, c1, "Double ← Float");
        assertInstanceOf(NumberConverter.class, c2, "Float ← Double");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link DateConverter}.
     */
    @Test
    public void testDateAndLong() {
        final ObjectConverter<Date,Long> c1 = INSTANCE.findExact(Date.class, Long.class);
        final ObjectConverter<Long,Date> c2 = INSTANCE.findExact(Long.class, Date.class);
        assertInstanceOf(DateConverter.Long.class, c1, "Long ← Date");
        assertInstanceOf(SystemConverter.class,    c2, "Date ← Long");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link DateConverter} from {@code java.util.Date}
     * to {@code java.sql.Date}. The inverse converter is an identity converter.
     */
    @Test
    public void testDateAndSQL() {
        final ObjectConverter<Date, java.sql.Date> c1 = INSTANCE.findExact(Date.class, java.sql.Date.class);
        final ObjectConverter<java.sql.Date, Date> c2 = INSTANCE.findExact(java.sql.Date.class, Date.class);
        assertInstanceOf(DateConverter.SQL.class, c1, "sql.Date ← Date");
        assertInstanceOf(IdentityConverter.class, c2, "Date ← sql.Date");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
        assertSame(c1, assertSerializedEquals(c1));
        assertSame(c2, assertSerializedEquals(c2));
    }

    /**
     * Tests the creation of {@link PathConverter}.
     */
    @Test
    public void testFileAndURI() {
        final ObjectConverter<File,URI> c1 = INSTANCE.findExact(File.class, URI.class);
        final ObjectConverter<URI,File> c2 = INSTANCE.findExact(URI.class, File.class);
        assertInstanceOf(PathConverter.FileURI.class, c1, "URI ← File");
        assertInstanceOf(PathConverter.URIFile.class, c2, "File ← URI");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
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
        assertInstanceOf(AngleConverter.class,         c1, "Double ← Angle");
        assertInstanceOf(AngleConverter.Inverse.class, c2, "Angle ← Double");
        assertSame(c2, c1.inverse());
        assertSame(c1, c2.inverse());
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
        assertInstanceOf(CollectionConverter.class, c1, "List ← Collection");
        assertSame(c1, assertSerializedEquals(c1));
    }
}
