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
package org.apache.sis.test;

import java.util.Set;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.lineage.Source;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Classes;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the main branch:
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Assertion methods used by the <abbr>SIS</abbr> project in addition of the JUnit and GeoAPI assertions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class Assertions {
    /**
     * Do not allow instantiation of this class.
     */
    private Assertions() {
    }

    /**
     * Returns the single element from the given array. If the given array is null or
     * does not contains exactly one element, then an {@link AssertionError} is thrown.
     *
     * @param  <E>    the type of array elements.
     * @param  array  the array from which to get the singleton.
     * @return the singleton element from the array.
     */
    public static <E> E assertSingleton(final E[] array) {
        assertNotNull(array, "Null array.");
        assertEquals(1, array.length, "Not a singleton array.");
        return array[0];
    }

    /**
     * Returns the single element from the given collection. If the given collection is null
     * or does not contains exactly one element, then an {@link AssertionError} is thrown.
     *
     * @param  <E>         the type of collection elements.
     * @param  collection  the collection from which to get the singleton.
     * @return the singleton element from the collection.
     */
    public static <E> E assertSingleton(final Iterable<? extends E> collection) {
        assertNotNull(collection, "Null collection.");
        final Iterator<? extends E> it = collection.iterator();
        assertTrue(it.hasNext(), "The collection is empty.");
        final E element = it.next();
        assertFalse(it.hasNext(), "The collection has more than one element.");
        return element;
    }

    /**
     * Asserts that the given metadata contains exactly one citation, and returns that citation.
     *
     * @param  metadata  the metadata from which to get the citation.
     * @return the singleton citation of the given metadata.
     */
    public static Citation assertSingletonCitation(final Metadata metadata) {
        Citation citation = assertSingleton(metadata.getIdentificationInfo()).getCitation();
        assertNotNull(citation, "citation");
        return citation;
    }

    /**
     * Asserts that the given metadata contains exactly one feature catalog, and returns its description.
     *
     * @param  metadata  the metadata from which to get the catalog description.
     * @return the singleton catalog description of the given metadata.
     */
    public static FeatureCatalogueDescription assertSingletonFeature(final Metadata metadata) {
        return assertInstanceOf(FeatureCatalogueDescription.class, assertSingleton(metadata.getContentInfo()));
    }

    /**
     * Asserts that the given metadata contains exactly one <abbr>CRS</abbr>,
     * and returns the identifier of that <abbr>CRS</abbr>.
     *
     * @param  metadata  the metadata from which to get the <abbr>CRS</abbr>.
     * @return the authority code of the singleton <abbr>CRS</abbr> of the given metadata.
     */
    public static Identifier assertSingletonReferenceSystem(final Metadata metadata) {
        return assertSingleton(metadata.getReferenceSystemInfo()).getName();
    }

    /**
     * Asserts that the given metadata contains exactly one extent, and returns that extent.
     *
     * @param  metadata  the metadata from which to get the extent.
     * @return the singleton extent of the given metadata.
     */
    public static Extent assertSingletonExtent(final Metadata metadata) {
        return assertSingleton(assertInstanceOf(DataIdentification.class, assertSingleton(metadata.getIdentificationInfo())).getExtents());
    }

    /**
     * Asserts that the given metadata contains exactly one bounding box, and returns that bounding box.
     * Vertical and temporal components are ignored.
     *
     * @param  metadata  the metadata from which to get the geographic bounding box.
     * @return the singleton geographic bounding box.
     */
    public static GeographicBoundingBox assertSingletonBBox(final Metadata metadata) {
        return assertSingletonBBox(assertSingletonExtent(metadata));
    }

    /**
     * Asserts that the given identification information contains exactly one bounding box,
     * and returns that bounding box. Vertical and temporal components are ignored.
     *
     * @param  identification  the identification information from which to get the geographic bounding box.
     * @return the singleton geographic bounding box.
     */
    public static GeographicBoundingBox assertSingletonBBox(final Identification identification) {
        return assertSingletonBBox(assertSingleton(assertInstanceOf(DataIdentification.class, identification).getExtents()));
    }

    /**
     * Asserts that the given extent contains exactly one bounding box, and returns that bounding box.
     * Vertical and temporal components are ignored.
     *
     * @param  extent  the extent from which to get the bounding box.
     * @return the singleton geographic bounding box.
     */
    public static GeographicBoundingBox assertSingletonBBox(final Extent extent) {
        return assertInstanceOf(GeographicBoundingBox.class, assertSingleton(extent.getGeographicElements()));
    }

    /**
     * Asserts that the given object contains exactly one identifier, then returns the code of that identifier.
     *
     * @param  object  the object for which to get the authority code.
     * @return the single authority code of the given object.
     */
    public static String assertSingletonAuthorityCode(final IdentifiedObject object) {
        return assertSingleton(object.getIdentifiers()).getCode();
    }

    /**
     * Asserts that the given object contains exactly one scope, then returns that scope in English.
     *
     * @param  object  the object for which to get the scope.
     * @return the single scope of the given object.
     */
    public static String assertSingletonScope(final IdentifiedObject object) {
        InternationalString scope;
        if (object instanceof CoordinateOperation op) {
            scope = op.getScope();
        } else if (object instanceof Datum datum) {
            scope = datum.getScope();
        } else {
            scope = assertInstanceOf(CoordinateReferenceSystem.class, object).getScope();
        }
        assertNotNull(scope, "Missing scope.");
        return scope.toString(Locale.US);
    }

    /**
     * Asserts that the given object as exactly one domain of validity, and returns that domain as a bounding box.
     *
     * @param  object  the object for which to get the domain of validity.
     * @return the single domain of validity of the given object.
     */
    public static GeographicBoundingBox assertSingletonDomainOfValidity(final IdentifiedObject object) {
        final Extent extent;
        if (object instanceof CoordinateOperation op) {
            extent = op.getDomainOfValidity();
        } else if (object instanceof Datum datum) {
            extent = datum.getDomainOfValidity();
        } else {
            extent = assertInstanceOf(CoordinateReferenceSystem.class, object).getDomainOfValidity();
        }
        assertNotNull(extent, "Missing extent.");
        return assertInstanceOf(GeographicBoundingBox.class, assertSingleton(extent.getGeographicElements()));
    }

    /**
     * Asserts that the English title of the given citation is equal to the expected string.
     *
     * @param expected  the expected English title.
     * @param citation  the citation to test.
     * @param message   the message to report in case of test failure.
     */
    public static void assertTitleEquals(final String expected, final Citation citation, final String message) {
        assertNotNull(citation, message);
        InternationalString title = citation.getTitle();
        assertNotNull(title, message);
        assertEquals(expected, title.toString(Locale.US), message);
    }

    /**
     * Asserts that the given citation has only one responsible party,
     * and its English name is equal to the expected string.
     *
     * @param expected  the expected English responsibly party name.
     * @param citation  the citation to test.
     * @param message   the message to report in case of test failure.
     */
    public static void assertPartyNameEquals(final String expected, final Citation citation, final String message) {
        assertNotNull(citation, message);
        assertSingleton(citation.getCitedResponsibleParties());
        // TODO: uncomment after merge of `util` with `metadata`:
        /*
        DefaultResponsibility r = assertInstanceOf(DefaultResponsibility.class, assertSingleton(citation.getCitedResponsibleParties()));
        InternationalString name = assertSingleton(r.getParties()).getName();
        assertNotNull(name, message);
        assertEquals(expected, name.toString(Locale.US), message);
         */
    }

    /**
     * Verifies that the given {@code ContentInfo} describes the given feature.
     * This method expects that the given catalog contains exactly one feature info.
     *
     * @param  name     expected feature type name (possibly null).
     * @param  count    expected feature instance count (possibly null).
     * @param  catalog  the content info to validate.
     */
    public static void assertContentInfoEquals(final String name, final Integer count, final FeatureCatalogueDescription catalog) {
        assertEquals(name, assertSingleton(catalog.getFeatureTypes()).tip().toString());
        // TODO: uncomment after merge of `util` with `metadata`:
        /*
        final DefaultFeatureTypeInfo info = assertInstanceOf(
                DefaultFeatureTypeInfo.class,
                assertSingleton(assertInstanceOf(DefaultFeatureCatalogueDescription.class, catalog).getFeatureTypeInfo()));
        assertEquals(name, String.valueOf(info.getFeatureTypeName()), "metadata.contentInfo.featureType");
        assertEquals(count, info.getFeatureInstanceCount(), "metadata.contentInfo.featureInstanceCount");
         */
    }

    /**
     * Verifies that the source contains the given feature type. This method expects that the given source contains
     * exactly one scope description and that the hierarchical level is {@link ScopeCode#FEATURE_TYPE}.
     *
     * @param  name      expected source identifier.
     * @param  features  expected names of feature type.
     * @param  source    the source to validate.
     */
    public static void assertFeatureSourceEquals(final String name, final String[] features, final Source source) {
        assertEquals(name, String.valueOf(source.getSourceCitation().getTitle()), "metadata.lineage.source.sourceCitation.title");
        // TODO: uncomment after merge of `util` with `metadata`:
        /*
        final DefaultScope scope = assertInstanceOf(DefaultScope.class, assertInstanceOf(DefaultSource.class, source).getScope());
        assertNotNull(scope, "metadata.lineage.source.scope");
        assertEquals(ScopeCode.FEATURE_TYPE, scope.getLevel(), "metadata.lineage.source.scope.level");
        final var actual = assertSingleton(scope.getLevelDescription()).getFeatures().toArray(CharSequence[]::new);
        for (int i=0; i<actual.length; i++) {
            actual[i] = actual[i].toString();
        }
        assertArrayEquals(features, actual, "metadata.lineage.source.scope.levelDescription.feature");
        */
    }

    /**
     * Asserts that the two given objects are not equal.
     * This method tests all {@link ComparisonMode} except {@code DEBUG}.
     *
     * @param  o1  the first object.
     * @param  o2  the second object.
     */
    public static void assertNotDeepEquals(final Object o1, final Object o2) {
        assertNotSame(o1, o2, "same");
        assertFalse(Objects  .equals    (o1, o2), "equals");
        assertFalse(Objects  .deepEquals(o1, o2), "deepEquals");
        assertFalse(Utilities.deepEquals(o1, o2, ComparisonMode.STRICT),          "deepEquals(STRICT)");
        assertFalse(Utilities.deepEquals(o1, o2, ComparisonMode.BY_CONTRACT),     "deepEquals(BY_CONTRACT)");
        assertFalse(Utilities.deepEquals(o1, o2, ComparisonMode.IGNORE_METADATA), "deepEquals(IGNORE_METADATA)");
        assertFalse(Utilities.deepEquals(o1, o2, ComparisonMode.COMPATIBILITY),   "deepEquals(COMPATIBILITY)");
        assertFalse(Utilities.deepEquals(o1, o2, ComparisonMode.APPROXIMATE),     "deepEquals(APPROXIMATE)");
    }

    /**
     * Asserts that the two given objects are approximately equal, while slightly different.
     * More specifically, this method asserts that the given objects are equal according the
     * {@link ComparisonMode#APPROXIMATE} criterion, but not equal according the
     * {@link ComparisonMode#COMPATIBILITY} criterion.
     *
     * @param  expected  the expected object.
     * @param  actual    the actual object.
     */
    public static void assertAlmostEquals(final Object expected, final Object actual) {
        assertFalse(Utilities.deepEquals(expected, actual, ComparisonMode.STRICT),          "Shall not be strictly equal.");
        assertFalse(Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA), "Shall be slightly different.");
        assertFalse(Utilities.deepEquals(expected, actual, ComparisonMode.COMPATIBILITY),   "Shall be slightly different.");
        assertTrue (Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG),           "Shall be approximately equal.");
        assertTrue (Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATE),     "DEBUG inconsistent with APPROXIMATE.");
    }

    /**
     * Asserts that the two given objects are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param  expected  the expected object.
     * @param  actual    the actual object.
     */
    public static void assertEqualsIgnoreMetadata(final Object expected, final Object actual) {
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG),           "Shall be approximately equal.");
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.APPROXIMATE),     "DEBUG inconsistent with APPROXIMATE.");
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.IGNORE_METADATA), "Shall be equal, ignoring metadata.");
    }

    /**
     * Asserts that the two given arrays contains objects that are equal ignoring metadata.
     * See {@link ComparisonMode#IGNORE_METADATA} for more information.
     *
     * @param  expected  the expected objects (array can be {@code null}).
     * @param  actual    the actual objects (array can be {@code null}).
     */
    public static void assertArrayEqualsIgnoreMetadata(final Object[] expected, final Object[] actual) {
        if (expected != actual) {
            if (expected == null) {
                assertNull(actual, "Expected null array.");
            } else {
                assertNotNull(actual, "Expected non-null array.");
                final int length = StrictMath.min(expected.length, actual.length);
                for (int i=0; i<length; i++) try {
                    assertEqualsIgnoreMetadata(expected[i], actual[i]);
                } catch (AssertionError e) {
                    throw new AssertionError("Comparison failure at index " + i
                            + " (a " + Classes.getShortClassName(actual[i]) + "): " + e, e);
                }
                assertEquals(expected.length, actual.length, "Unexpected array length.");
            }
        }
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons are performed on a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param  expected  the expected string.
     * @param  actual    the actual string.
     */
    public static void assertMultilinesEquals(final CharSequence expected, final CharSequence actual) {
        assertMultilinesEquals(expected, actual, null);
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, trailing
     * spaces (but not leading spaces) are ignored.
     *
     * @param  expected  the expected string.
     * @param  actual    the actual string.
     * @param  message   the message to print in case of failure, or {@code null} if none.
     */
    public static void assertMultilinesEquals(final CharSequence expected, final CharSequence actual, final String message) {
        final CharSequence[] expectedLines = CharSequences.splitOnEOL(expected);
        final CharSequence[] actualLines   = CharSequences.splitOnEOL(actual);
        final int length = StrictMath.min(expectedLines.length, actualLines.length);
        final var buffer = new StringBuilder(message != null ? message : "Line").append('[');
        final int base   = buffer.length();
        for (int i=0; i<length; i++) {
            buffer.append(i).append(']');
            CharSequence e = expectedLines[i];
            CharSequence a = actualLines[i];
            e = e.subSequence(0, CharSequences.skipTrailingWhitespaces(e, 0, e.length()));
            a = a.subSequence(0, CharSequences.skipTrailingWhitespaces(a, 0, a.length()));
            assertEquals(e, a, () -> buffer.toString());
            buffer.setLength(base);
        }
        if (expectedLines.length > actualLines.length) {
            fail(buffer.append(length).append("] missing line: ").append(expectedLines[length]).toString());
        }
        if (expectedLines.length < actualLines.length) {
            fail(buffer.append(length).append("] extraneous line: ").append(actualLines[length]).toString());
        }
    }

    /**
     * Asserts that the message of the given exception contains all the given keywords.
     * We do not test the exact exception message because it is locale-dependent.
     * If the list of keywords is empty, then this method only verifies that the message is non-null.
     *
     * @param  exception  the exception for which to validate the message.
     * @param  keywords   the keywords which should be present in the exception.
     */
    public static void assertMessageContains(final Throwable exception, final String... keywords) {
        final String message = exception.getMessage();
        assertNotNull(message, "Missing exception message.");
        for (final String keyword : keywords) {
            assertTrue(message.contains(keyword), () -> "Missing \"" + keyword + "\" in exception message: " + message);
        }
    }

    /**
     * Verifies that the given stream produces the same values as the given iterator, in same order.
     * This method assumes that the given stream is sequential.
     *
     * @param  <E>       the type of values to test.
     * @param  expected  the expected values.
     * @param  actual    the stream to compare with the expected values.
     */
    public static <E> void assertSequentialStreamEquals(final Iterator<E> expected, final Stream<E> actual) {
        actual.forEach(new Consumer<E>() {
            private int count;

            @Override
            public void accept(final Object value) {
                if (!expected.hasNext()) {
                    fail("Expected " + count + " elements, but the stream contains more.");
                }
                final Object ex = expected.next();
                if (!Objects.equals(ex, value)) {
                    fail("Expected " + ex + " at index " + count + " but got " + value);
                }
                count++;
            }
        });
        assertFalse(expected.hasNext(), "Unexpected end of stream.");
    }

    /**
     * Verifies that the given stream produces the same values as the given iterator, in any order.
     * This method is designed for use with parallel streams, but works with sequential streams too.
     *
     * @param  <E>       the type of values to test.
     * @param  expected  the expected values.
     * @param  actual    the stream to compare with the expected values.
     */
    public static <E> void assertParallelStreamEquals(final Iterator<E> expected, final Stream<E> actual) {
        final Integer ONE = 1;          // For doing autoboxing only once.
        final var count = new ConcurrentHashMap<E,Integer>();
        while (expected.hasNext()) {
            count.merge(expected.next(), ONE, (old, one) -> old + 1);
        }
        /*
         * Following may be parallelized in an arbitrary number of threads.
         */
        actual.forEach((value) -> {
            if (count.computeIfPresent(value, (key, old) -> old - 1) == null) {
                fail("Stream returned unexpected value: " + value);
            }
        });
        /*
         * Back to sequential order, verify that all elements have been traversed
         * by the stream and no more.
         */
        for (final Map.Entry<E,Integer> entry : count.entrySet()) {
            int n = entry.getValue();
            if (n != 0) {
                final String message;
                if (n < 0) {
                    message = "Stream returned too many occurrences of %s%n%d extraneous were found.";
                } else {
                    message = "Stream did not returned all expected occurrences of %s%n%d are missing.";
                }
                fail(String.format(message, entry.getKey(), StrictMath.abs(n)));
            }
        }
    }

    /**
     * Asserts that the given set contains the same elements, ignoring order.
     * In case of failure, this method lists the missing or unexpected elements.
     *
     * <p>The given collections are typically instances of {@link Set}, but this is not mandatory.</p>
     *
     * @param  expected  the expected set, or {@code null}.
     * @param  actual    the actual set, or {@code null}.
     */
    public static void assertSetEquals(final Collection<?> expected, final Collection<?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final var r = new LinkedHashSet<Object>(expected);
            assertTrue(r.removeAll(actual),   "The two sets are disjoint.");
            assertTrue(r.isEmpty(),           "The set is missing elements: " + r);
            assertTrue(r.addAll(actual),      "The set unexpectedly became empty.");
            assertTrue(r.removeAll(expected), "The two sets are disjoint.");
            assertTrue(r.isEmpty(),     () -> "The set contains unexpected elements: " + r);
        }
        if (expected instanceof Set<?> && actual instanceof Set<?>) {
            assertEquals(expected, actual, "Set.equals(Object) failed:");
            assertEquals(expected.hashCode(), actual.hashCode(), "Unexpected hash code value.");
        }
    }

    /**
     * Asserts that the given map contains the same entries.
     * In case of failure, this method lists the missing or unexpected entries.
     *
     * @param  expected  the expected map, or {@code null}.
     * @param  actual    the actual map, or {@code null}.
     */
    public static void assertMapEquals(final Map<?,?> expected, final Map<?,?> actual) {
        if (expected != null && actual != null && !expected.isEmpty()) {
            final var r = new LinkedHashMap<Object,Object>(expected);
            for (final Map.Entry<?,?> entry : actual.entrySet()) {
                final Object key = entry.getKey();
                if (!r.containsKey(key)) {
                    fail("Unexpected entry for key " + key);
                }
                final Object ve = r.remove(key);
                final Object va = entry.getValue();
                if (!Objects.equals(ve, va)) {
                    fail("Wrong value for key " + key + ": expected " + ve + " but got " + va);
                }
            }
            if (!r.isEmpty()) {
                fail("The map is missing entries: " + r);
            }
            r.putAll(actual);
            for (final Map.Entry<?,?> entry : expected.entrySet()) {
                final Object key = entry.getKey();
                if (!r.containsKey(key)) {
                    fail("Missing an entry for key " + key);
                }
                final Object ve = entry.getValue();
                final Object va = r.remove(key);
                if (!Objects.equals(ve, va)) {
                    fail("Wrong value for key " + key + ": expected " + ve + " but got " + va);
                }
            }
            if (!r.isEmpty()) {
                fail("The map contains unexpected elements:" + r);
            }
        }
        assertEquals(expected, actual, "Map.equals(Object) failed:");
    }

    /**
     * Serializes the given object in memory, deserializes it and ensures that the deserialized
     * object is equal to the original one. This method does not write anything to the disk.
     *
     * <p>If the serialization fails, then this method throws an {@link AssertionError}
     * as do the other JUnit assertion methods.</p>
     *
     * @param  <T>     the type of the object to serialize.
     * @param  object  the object to serialize.
     * @return the deserialized object.
     */
    public static <T> T assertSerializedEquals(final T object) {
        Objects.requireNonNull(object);
        final Object deserialized;
        try {
            final var buffer = new ByteArrayOutputStream();
            try (var out = new ObjectOutputStream(buffer)) {
                out.writeObject(object);
            }
            // Now reads the object we just serialized.
            final byte[] data = buffer.toByteArray();
            try (var in = new ObjectInputStream(new ByteArrayInputStream(data))) {
                try {
                    deserialized = in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e.toString(), e);
        }
        assertNotNull(deserialized, "Deserialized object shall not be null.");
        /*
         * Compare with the original object and return it.
         */
        @SuppressWarnings("unchecked")
        final Class<? extends T> type = (Class<? extends T>) object.getClass();
        assertEquals(object, deserialized, "Deserialized object not equal to the original one.");
        assertEquals(object.hashCode(), deserialized.hashCode(), "Deserialized object has a different hash code.");
        return type.cast(deserialized);
    }
}
