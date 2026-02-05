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
package org.apache.sis.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.GenericDeclaration;
import static org.apache.sis.util.Classes.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/*
 * Following imports are not used for actual code.
 * The are used only as various Class<?> arguments
 * given to the methods to test.
 */
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.io.File;
import java.io.Serializable;
import java.awt.geom.Point2D;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Tests the {@link Classes} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ClassesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ClassesTest() {
    }

    /**
     * Tests {@link Classes#changeArrayDimension(Class, int)}.
     */
    @Test
    public void testChangeArrayDimension() {
        assertEquals(float    .class, changeArrayDimension(float    .class,  0));
        assertEquals(Float    .class, changeArrayDimension(Float    .class,  0));
        assertEquals(float[]  .class, changeArrayDimension(float    .class,  1));
        assertEquals(Float[]  .class, changeArrayDimension(Float    .class,  1));
        assertEquals(float[][].class, changeArrayDimension(float    .class,  2));
        assertEquals(Float[][].class, changeArrayDimension(Float    .class,  2));
        assertEquals(float[][].class, changeArrayDimension(float[]  .class,  1));
        assertEquals(Float[][].class, changeArrayDimension(Float[]  .class,  1));
        assertEquals(float[]  .class, changeArrayDimension(float[][].class, -1));
        assertEquals(Float[]  .class, changeArrayDimension(Float[][].class, -1));
        assertEquals(float    .class, changeArrayDimension(float[][].class, -2));
        assertEquals(Float    .class, changeArrayDimension(Float[][].class, -2));
        assertNull  (                 changeArrayDimension(float[][].class, -3));
        assertNull  (                 changeArrayDimension(Float[][].class, -3));
        assertNull  (                 changeArrayDimension(Void.TYPE,       -1));
        assertEquals(Void.TYPE,       changeArrayDimension(Void.TYPE,        1));
    }

    /**
     * Tests {@link Classes#getAllInterfaces(Class)}.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testGetAllInterfaces() {
        assertArrayEquals(new Class[] {
            GeographicCRS.class,
            EllipsoidalCS.class,                // Shall be before parent types listed below.
            GeodeticCRS.class,
            SingleCRS.class,
            CoordinateReferenceSystem.class,
            ReferenceSystem.class,
            IdentifiedObject.class,
            CoordinateSystem.class
        }, getAllInterfaces(MixedImpl.class));
    }

    /**
     * A dummy class which implements two interfaces having a common parent.
     * The intent is to verify that explicitly declared interfaces are listed
     * before parent interfaces in {@link #testGetAllInterfaces()}.
     */
    private abstract static class MixedImpl implements GeographicCRS, EllipsoidalCS {
    }

    /**
     * Tests {@link Classes#getLeafInterfaces(Class, Class)}.
     */
    @Test
    public void testGetLeafInterfaces() {
        assertArrayEquals(new Class<?>[] {NavigableSet.class},
                getLeafInterfaces(TreeSet.class, Collection.class));

        assertArrayEquals(new Class<?>[] {GeographicCRS.class},
                getLeafInterfaces(T1.class, IdentifiedObject.class));

        assertArrayEquals(new Class<?>[] {GeographicCRS.class, CoordinateOperation.class},
                getLeafInterfaces(T2.class, IdentifiedObject.class));

        assertArrayEquals(new Class<?>[] {Transformation.class, GeographicCRS.class},
                getLeafInterfaces(T3.class, IdentifiedObject.class));
    }

    /**
     * Dummy class for {@link #testGetLeafInterfaces()}.
     */
    private abstract static class T1 implements GeographicCRS {}
    private abstract static class T2 extends T1 implements SingleCRS, CoordinateOperation {}
    private abstract static class T3 extends T2 implements Transformation {}

    /**
     * Tests {@link Classes#getStandardType(Class)}.
     */
    @Test
    public void testGetStandardType() {
        assertEquals(GeographicCRS.class,  Classes.getStandardType(T1.class));
        assertEquals(SingleCRS.class,      Classes.getStandardType(T2.class));
        assertEquals(Transformation.class, Classes.getStandardType(T3.class));
        assertEquals(String.class,         Classes.getStandardType(String.class));
        assertEquals(CharSequence.class,   Classes.getStandardType(CharSequence.class));
    }

    /**
     * Tests {@link Classes#findCommonInterfaces(Class, Class)}.
     */
    @Test
    public void testFindCommonInterfaces() {
        final Set<Class<?>> interfaces = findCommonInterfaces(ArrayList.class, HashSet.class);
        assertFalse(interfaces.contains(Set         .class));
        assertFalse(interfaces.contains(List        .class));
        assertTrue (interfaces.contains(Collection  .class));
        assertFalse(interfaces.contains(Iterable    .class));
        assertFalse(interfaces.contains(RandomAccess.class));
        assertTrue (interfaces.contains(Serializable.class));
        assertTrue (interfaces.contains(Cloneable   .class));
    }

    /**
     * Tests {@link Classes#implementSameInterfaces(Class, Class, Class)}.
     */
    @Test
    public void testImplementSameInterfaces() {
        assertTrue (implementSameInterfaces(StringBuilder.class, String.class, CharSequence.class));
        assertTrue (implementSameInterfaces(StringBuilder.class, String.class, Serializable.class));
        assertFalse(implementSameInterfaces(         File.class, String.class, CharSequence.class));
        assertTrue (implementSameInterfaces(         File.class, String.class, Serializable.class));

        // Tests more convolved cases
        assertTrue (implementSameInterfaces(T1.class, T3.class, CoordinateReferenceSystem.class));
        assertTrue (implementSameInterfaces(T3.class, T1.class, CoordinateReferenceSystem.class));
        assertFalse(implementSameInterfaces(T2.class, T3.class, CoordinateOperation.class));
        assertFalse(implementSameInterfaces(T3.class, T2.class, CoordinateOperation.class));
        assertFalse(implementSameInterfaces(T3.class, T1.class, CoordinateOperation.class));
    }

    /**
     * Tests the {@link Classes#boundOfParameterizedProperty(Field)} method.
     *
     * @throws NoSuchFieldException if there is an error in a field name.
     */
    @Test
    public void testBoundOfParameterizedField() throws NoSuchFieldException {
        final Class<Parameterized> c = Parameterized.class;
        assertNull(                boundOfParameterizedProperty(c.getField("attrib1")));
        assertEquals(Long  .class, boundOfParameterizedProperty(c.getField("attrib2")));
        assertEquals(String.class, boundOfParameterizedProperty(c.getField("attrib3")));
    }

    /**
     * Tests the {@link Classes#boundOfParameterizedProperty(Method)} method.
     *
     * @throws NoSuchMethodException if there is an error in a method name.
     */
    @Test
    public void testBoundOfParameterizedProperty() throws NoSuchMethodException {
        final Class<?>[] getter = null;
        final Class<?>[] setter = new Class<?>[] {Set.class};
        final Class<Parameterized> c = Parameterized.class;
        assertNull(                      boundOfParameterizedProperty(c.getMethod("getter0", getter)));
        assertNull(                      boundOfParameterizedProperty(c.getMethod("setter0", setter)));
        assertEquals(Integer     .class, boundOfParameterizedProperty(c.getMethod("getter1", getter)));
        assertEquals(Byte        .class, boundOfParameterizedProperty(c.getMethod("getter2", getter)));
        assertEquals(Object      .class, boundOfParameterizedProperty(c.getMethod("getter3", getter)));
        assertEquals(short[]     .class, boundOfParameterizedProperty(c.getMethod("getter4", getter)));
        assertEquals(Comparable  .class, boundOfParameterizedProperty(c.getMethod("getter5", getter)));
        assertEquals(Comparable[].class, boundOfParameterizedProperty(c.getMethod("getter6", getter)));
        assertEquals(String      .class, boundOfParameterizedProperty(c.getMethod("setter1", setter)));
        assertEquals(Short       .class, boundOfParameterizedProperty(c.getMethod("setter2", setter)));
        assertEquals(Object      .class, boundOfParameterizedProperty(c.getMethod("setter3", setter)));

        assertEquals(PrinterStateReason.class, boundOfParameterizedProperty(c.getMethod("getter7", getter)));
    }

    /**
     * Dummy class for {@link #testBoundOfParameterizedProperty()} usage only.
     */
    @SuppressWarnings("rawtypes")
    private static final class Parameterized {
        public Long                 attrib1;
        public Set<? extends Long>  attrib2;
        public Map<String,Integer>  attrib3;
        public Set                  getter0() {return null;}        // Intentionnaly unparameterized.
        public Set<       Integer>  getter1() {return null;}
        public Set<? extends Byte>  getter2() {return null;}
        public Set<? super  Float>  getter3() {return null;}
        public Set<       short[]>  getter4() {return null;}
        public Set<Comparable<?>>   getter5() {return null;}
        public Set<Comparable<?>[]> getter6() {return null;}
        public PrinterStateReasons  getter7() {return null;}

        public void setter0(Set                  dummy) {}         // Intentionnaly unparameterized.
        public void setter1(Set<         String> dummy) {}
        public void setter2(Set<? extends Short> dummy) {}
        public void setter3(Set<? super  Double> dummy) {}
    }

    /**
     * Tests the {@link Classes#boundOfParameterizedDeclaration(GenericDeclaration)} method.
     */
    @Test
    public void testBoundOfParameterizedDeclaration() {
        assertNull  (              boundOfParameterizedDeclaration(Long.class));
        assertEquals(Object.class, boundOfParameterizedDeclaration(List.class));
        assertEquals(Object.class, boundOfParameterizedDeclaration(Map.class));
        assertEquals(PrinterStateReason.class, boundOfParameterizedDeclaration(PrinterStateReasons.class));
    }

    /**
     * Tests the {@link Classes#getShortName(Class)}, in particular the example values given in the javadoc.
     */
    @Test
    public void testGetShortName() {
        assertEquals("java.lang.String", String.class.getName());
        assertEquals("String",           String.class.getSimpleName());
        assertEquals("java.lang.String", String.class.getCanonicalName());
        assertEquals("String",           getShortName(String.class));

        assertEquals("[D",       double[].class.getName());
        assertEquals("double[]", double[].class.getSimpleName());
        assertEquals("double[]", double[].class.getCanonicalName());
        assertEquals("double[]", getShortName(double[].class));

        assertEquals("java.awt.geom.Point2D$Double", Point2D.Double.class.getName());
        assertEquals("Double",                       Point2D.Double.class.getSimpleName());
        assertEquals("java.awt.geom.Point2D.Double", Point2D.Double.class.getCanonicalName());
        assertEquals("Point2D.Double",               getShortName(Point2D.Double.class));

        final Class<?> anonymous = new Comparable<Object>() {
            @Override public int compareTo(final Object o) {
                return 0; // Not the purpose of this test.
            }
        }.getClass();
        assertTrue(anonymous.getName().startsWith("org.apache.sis.util.ClassesTest$"));
        assertEquals("",       anonymous.getSimpleName());
        assertEquals(null,     anonymous.getCanonicalName());
        assertEquals("Object", getShortName(anonymous));
    }
}
