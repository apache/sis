package org.apache.sis.internal.storage;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.NullArgumentException;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.Metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ConcatenatedFeatureSetTest {

    @Test
    public void testSimple() throws DataStoreException {
        System.out.println(System.getenv("BASH"));
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.addAttribute(String.class).setName("label");
        builder.addAttribute(Integer.class).setName("amount");
        builder.setName("first");

        final FeatureType ft = builder.build();

        final FeatureSet fs1 = new MemoryFeatureSet(null, ft, Arrays.asList(ft.newInstance(), ft.newInstance()));
        final FeatureSet fs2 = new MemoryFeatureSet(null, ft, Arrays.asList(ft.newInstance()));

        final ConcatenatedFeatureSet cfs = new ConcatenatedFeatureSet(fs1, fs2);

        final FeatureType cType = cfs.getType();
        assertNotNull("Concatenation feature type", cType);
        assertEquals("Concatenation feature type", ft, cType);

        final Metadata md = cfs.getMetadata();
        assertNotNull("Concatenation metadata", md);

        final long count = cfs.features(false).count();
        assertEquals("Number of features", 3, count);
    }

    @Test
    public void testCommonSuperType() throws DataStoreException {
        /* First, we prepare two types sharing a common ancestor. We'll create two types using same properties, so we
         * can ensure that all data is exposed upon traversal, not only data defined in the super type.
         */
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.addAttribute(Integer.class).setName("value");
        builder.setName("parent");

        final FeatureType superType = builder.build();

        builder.clear();
        builder.setSuperTypes(superType);
        builder.addAttribute(String.class).setName("label");

        builder.setName("t1");
        final FeatureType t1 = builder.build();

        builder.setName("t2");
        final FeatureType t2 = builder.build();

        // Populate a feature set for first type.
        final Feature t1f1 = t1.newInstance();
        t1f1.setPropertyValue("value", 2);
        t1f1.setPropertyValue("label", "first-first");

        final Feature t1f2 = t1.newInstance();
        t1f2.setPropertyValue("value", 3);
        t1f2.setPropertyValue("label", "first-second");

        final FeatureSet t1fs = new MemoryFeatureSet(null, t1, Arrays.asList(t1f1, t1f2));

        // Populate a feature set for second type
        final Feature t2f1 = t2.newInstance();
        t2f1.setPropertyValue("value", 3);
        t2f1.setPropertyValue("label", "second-first");

        final Feature t2f2 = t2.newInstance();
        t2f2.setPropertyValue("value", 4);
        t2f2.setPropertyValue("label", "second-second");

        final FeatureSet t2fs = new MemoryFeatureSet(null, t1, Arrays.asList(t2f1, t2f2));

        /* First, we'll test that total sum of value property is coherent with initialized features. After that, we will
         * ensure that we can get back the right labels for each subtype.
         */
        final ConcatenatedFeatureSet set = new ConcatenatedFeatureSet(t1fs, t2fs);
        final int sum = set.features(true)
                .mapToInt(f -> (int) f.getPropertyValue("value"))
                .sum();
        assertEquals("Sum of feature \'value\' property", 12, sum);

        final Object[] t1labels = set.features(false)
                .filter(f -> t1.equals(f.getType()))
                .map(f -> f.getPropertyValue("label"))
                .toArray();
        assertArrayEquals("First type labels", new String[]{"first-first", "first-second"}, t1labels);


        final Object[] t2labels = set.features(false)
                .filter(f -> t2.equals(f.getType()))
                .map(f -> f.getPropertyValue("label"))
                .toArray();
        assertArrayEquals("First type labels", new String[]{"second-first", "second-second"}, t2labels);
    }

    @Test
    public void noCommonType() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("super");
        final FeatureType mockSuperType = builder.build();

        builder.setName("first");
        builder.setSuperTypes(mockSuperType);
        final FeatureType firstType = builder.build();

        builder.clear();
        builder.setName("second");
        final FeatureType secondType = builder.build();

        expectArgumentFailure(
                () -> new ConcatenatedFeatureSet(
                        new MemoryFeatureSet(null, firstType, Collections.EMPTY_LIST),
                        new MemoryFeatureSet(null, secondType, Collections.EMPTY_LIST)
                ),
                concatenation -> fail("Concatenation succeeded despite the lack of common type. Concatenation expose type: "+concatenation)
        );
    }

    @Test
    public void lackOfInput() {
        expectArgumentFailure(
                () -> new ConcatenatedFeatureSet(),
                concatenation -> fail("An empty concatenation has been created. Type: "+concatenation.getType())
        );

        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("mock");
        final FeatureType mockType = builder.build();

        expectArgumentFailure(
                () -> new ConcatenatedFeatureSet(new MemoryFeatureSet(null, mockType, Collections.EMPTY_LIST)),
                concatenation -> fail("A concatenation has been created from a single type: "+concatenation.getType())
        );
    }

    private static <T> void expectArgumentFailure(DataStoreComputing<T> input, Consumer<T> onUnexpectedSuccess) {
        final T computedValue;
        try {
            computedValue = input.compute();
        } catch (NullArgumentException|IllegalArgumentException e) {
            // expected behavior
            return;
        } catch (DataStoreException e) {
            fail("Concatenation failed with an error reserved for subset access. " +
                    "It should have used an null or illegal argument exception to denote the lack of input.");
            return; // Useless, but it allows compiler to understand that computed value can be final.
        }

        onUnexpectedSuccess.accept(computedValue);
    }

    @FunctionalInterface
    private static interface DataStoreComputing<T> {
        T compute() throws DataStoreException;
    }
}
