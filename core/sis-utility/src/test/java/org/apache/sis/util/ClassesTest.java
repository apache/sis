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
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;
import static org.apache.sis.util.Classes.*;

/*
 * Following imports are not used for actual code.
 * The are used only as various Class<?> arguments
 * given to the methods to test.
 */
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.awt.geom.Point2D;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Tests the {@link Classes} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final strictfp class ClassesTest extends TestCase {
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
    public void testGetAllInterfaces() {
        final Set<Class<?>> interfaces = getInterfaceSet(ArrayList.class);
        assertTrue(interfaces.contains(List        .class));
        assertTrue(interfaces.contains(Collection  .class));
        assertTrue(interfaces.contains(Iterable    .class));
        assertTrue(interfaces.contains(RandomAccess.class));
        assertTrue(interfaces.contains(Serializable.class));
        assertTrue(interfaces.contains(Cloneable   .class));
    }

    /**
     * Tests {@link Classes#getLeafInterfaces(Class, Class)}.
     */
    @Test
    public void testGetLeafInterfaces() {
        assertArrayEquals("TreeSet class", new Class<?>[] {NavigableSet.class},
                getLeafInterfaces(TreeSet.class, Collection.class));

        assertArrayEquals("GeographicCRS", new Class<?>[] {GeographicCRS.class},
                getLeafInterfaces(T1.class, IdentifiedObject.class));

        assertArrayEquals("Mixed types",   new Class<?>[] {GeographicCRS.class, CoordinateOperation.class},
                getLeafInterfaces(T2.class, IdentifiedObject.class));

        assertArrayEquals("Mixed types",   new Class<?>[] {Transformation.class, GeographicCRS.class},
                getLeafInterfaces(T3.class, IdentifiedObject.class));
    }

    /**
     * Dummy class for {@link #testGetLeafInterfaces()}.
     */
    private static abstract class T1 implements GeographicCRS {}
    private static abstract class T2 extends T1 implements SingleCRS, CoordinateOperation {}
    private static abstract class T3 extends T2 implements Transformation {}

    /**
     * Tests {@link Classes#findCommonClass(Iterable)}
     * and {@link Classes#findSpecializedClass(Iterable)}.
     */
    @Test
    public void testFindCommonParent() {
        final Set<Object> types = new HashSet<Object>();

        assertTrue(types.add(new NotSerializableException()));
        assertEquals(NotSerializableException.class, findCommonClass     (types));
        assertEquals(NotSerializableException.class, findSpecializedClass(types));

        assertTrue(types.add(new InvalidObjectException(null)));
        assertEquals(ObjectStreamException.class, findCommonClass     (types));
        assertEquals(ObjectStreamException.class, findSpecializedClass(types));

        assertTrue(types.add(new FileNotFoundException()));
        assertEquals(IOException.class, findCommonClass     (types));
        assertEquals(IOException.class, findSpecializedClass(types));

        assertTrue(types.add(new IOException()));
        assertEquals(IOException.class, findCommonClass     (types));
        assertEquals(IOException.class, findSpecializedClass(types));

        assertTrue(types.add(new Exception()));
        assertEquals(  Exception.class, findCommonClass     (types));
        assertEquals(IOException.class, findSpecializedClass(types));
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
     * @throws NoSuchFieldException  Should never occur.
     * @throws NoSuchMethodException Should never occur.
     */
    @Test
    public void testBoundOfParameterizedProperty() throws NoSuchFieldException, NoSuchMethodException {
        final Class<?>[] g = null;
        final Class<?>[] s = new Class<?>[] {Set.class};
        final Class<Parameterized> c = Parameterized.class;
        assertNull(                 boundOfParameterizedProperty(c.getMethod("getter0", g)));
        assertNull(                 boundOfParameterizedProperty(c.getMethod("setter0", s)));
        assertEquals(Long      .class, boundOfParameterizedProperty(c.getField ("attrib2"   )));
        assertEquals(Integer   .class, boundOfParameterizedProperty(c.getMethod("getter1", g)));
        assertEquals(Byte      .class, boundOfParameterizedProperty(c.getMethod("getter2", g)));
        assertEquals(Object    .class, boundOfParameterizedProperty(c.getMethod("getter3", g)));
        assertEquals(short[]   .class, boundOfParameterizedProperty(c.getMethod("getter4", g)));
        assertEquals(Comparable.class, boundOfParameterizedProperty(c.getMethod("getter5", g)));
        assertEquals(String    .class, boundOfParameterizedProperty(c.getMethod("setter1", s)));
        assertEquals(Short     .class, boundOfParameterizedProperty(c.getMethod("setter2", s)));
        assertEquals(Object    .class, boundOfParameterizedProperty(c.getMethod("setter3", s)));
    }

    /**
     * Dummy class for {@link #testBoundOfParameterizedProperty()} usage only.
     */
    @SuppressWarnings("rawtypes")
    private static final class Parameterized {
        public Set<? extends Long> attrib2 = null;
        public Set                 getter0() {return null;} // Intentionnaly unparameterized.
        public Set<       Integer> getter1() {return null;}
        public Set<? extends Byte> getter2() {return null;}
        public Set<? super  Float> getter3() {return null;}
        public Set<       short[]> getter4() {return null;}
        public Set<Comparable<?>>  getter5() {return null;}

        public void setter0(Set                  dummy) {}  // Intentionnaly unparameterized.
        public void setter1(Set<         String> dummy) {}
        public void setter2(Set<? extends Short> dummy) {}
        public void setter3(Set<? super  Double> dummy) {}
    }

    /**
     * Tests the {@link Classes#getShortName(Class)}, in particular the example values
     * given in the javadoc.
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
