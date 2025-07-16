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
package org.apache.sis.storage.netcdf.base;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.Collection;
import java.util.Optional;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;

// Test dependencies
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.dataset.TestData;


/**
 * Tests the {@link FeatureSet} implementation. The default implementation uses the UCAR library,
 * which is is our reference implementation. Subclass overrides {@link #createDecoder(TestData)}
 * method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class FeatureSetTest extends TestCase {
    /**
     * Type of the features read from the netCDF file.
     */
    private FeatureType type;

    /**
     * Index of the feature to verify.
     */
    private int featureIndex;

    /**
     * Instant from which time are measured.
     */
    private final Instant timeOrigin;

    /**
     * Creates a new test case.
     */
    public FeatureSetTest() {
        timeOrigin = Instant.parse("2014-11-29T00:00:00Z");
    }

    /**
     * Forgets the data used by the current test. This method makes {@code this} instance
     * ready for another test method reusing the decoders that are already opened.
     */
    @Override
    @AfterEach
    public void reset() {
        super.reset();
        type = null;
        featureIndex = 0;
    }

    /**
     * Returns a dummy data store implementation for the sole purpose of providing a non-null lock.
     */
    private static DataStore lock() {
        return new DataStore() {
            @Override public Optional<ParameterValueGroup> getOpenParameters() {return Optional.empty();}
            @Override public Metadata getMetadata() {return null;}
            @Override public void close() {}
        };
    }

    /**
     * Tests {@link FeatureSet} with a moving features file.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testMovingFeatures() throws IOException, DataStoreException {
        final DataStore lock = lock();
        final FeatureSet[] features;
        synchronized (lock) {
            features = FeatureSet.create(selectDataset(TestData.MOVING_FEATURES), lock);
        }
        assertEquals(1, features.length);
        type = features[0].getType();
        verifyType(type.getProperties(false).iterator());
        features[0].features(false).forEach(this::verifyInstance);
    }

    /**
     * Verifies that given properties are the expected ones
     * for {@link TestData#MOVING_FEATURES} feature type.
     */
    private static void verifyType(final Iterator<? extends PropertyType> it) {
        assertEquals("sis:identifier", it.next().getName().toString());
        assertEquals("sis:envelope",   it.next().getName().toString());
        assertEquals("sis:geometry",   it.next().getName().toString());

        AttributeType<?> at = (AttributeType<?>) it.next();
        assertEquals("features", at.getName().toString());

        at = (AttributeType<?>) it.next();
        assertEquals("trajectory", at.getName().toString());
        assertEquals(Shape.class, at.getValueClass());

        at = (AttributeType<?>) it.next();
        assertEquals("stations", at.getName().toString());
        assertEquals(String.class, at.getValueClass());

        assertFalse(it.hasNext());
    }

    /**
     * Verifies the given feature instance.
     */
    private void verifyInstance(final Feature instance) {
        assertSame(type, instance.getType());
        final float[] longitudes, latitudes;
        final short[] times;                    // In minutes since 2014-11-29 00:00:00.
        final String[] stations;
        final String identifier;
        switch (featureIndex++) {
            case 0: {
                identifier = "a4078a16";
                longitudes = new float[] {139.622715f, 139.696899f, 139.740440f, 139.759640f, 139.763328f, 139.766084f};
                latitudes  = new float[] { 35.466188f,  35.531328f,  35.630152f,  35.665498f,  35.675069f,  35.681382f};
                times      = new short[] {       1068,        1077,        1087,        1094,        1096,        1098};
                stations   = new String[] {
                    "Yokohama", "Kawasaki", "Shinagawa", "Shinbashi", "Yurakucho", "Tokyo"
                };
                break;
            }
            case 1: {
                identifier = "1e146c16";
                longitudes = new float[] {139.700258f, 139.730667f, 139.763786f, 139.774219f};
                latitudes  = new float[] { 35.690921f,  35.686014f,  35.699855f,  35.698683f};
                times      = new short[] {       1075,        1079,        1087,        1090};
                stations   = new String[] {
                    "Shinjuku", "Yotsuya", "Ochanomizu", "Akihabara"
                };
                break;
            }
            case 2: {
                identifier = "f50ff004";
                longitudes = new float[] {139.649867f, 139.665652f, 139.700258f};
                latitudes  = new float[] { 35.705385f,  35.706032f,  35.690921f};
                times      = new short[] {       3480,        3482,        3486};
                stations   = new String[] {
                    "Koenji", "Nakano", "Shinjuku"
                };
                break;
            }
            default: {
                fail("Unexpected feature instance.");
                return;
            }
        }
        // Convert the time vector to an array of instants.
        final var instants = new Instant[times.length];
        for (int i=0; i<times.length; i++) {
            instants[i] = timeOrigin.plus(times[i], ChronoUnit.MINUTES);
        }
        /*
         * Verify property values and characteristics.
         */
        assertEquals(identifier, instance.getPropertyValue("features"));
        final var trajectory = (Attribute<?>) instance.getProperty("trajectory");
        asserLineStringEquals((Shape) trajectory.getValue(), longitudes, latitudes);
        assertArrayEquals(stations, ((Collection<?>) instance.getPropertyValue("stations")).toArray());
        assertArrayEquals(instants, trajectory.characteristics().get("datetimes").getValues().toArray());
        assertInstanceOf(GeographicCRS.class, AttributeConvention.getCRSCharacteristic(instance, "trajectory"));
    }

    /**
     * Asserts the given shape is a line string with the following coordinates.
     *
     * @param  trajectory  the shape to verify.
     * @param  x           expected X coordinates.
     * @param  y           expected Y coordinates.
     */
    private static void asserLineStringEquals(final Shape trajectory, final float[] x, final float[] y) {
        assertEquals(x.length, y.length);
        final PathIterator it = trajectory.getPathIterator(null);
        final float[] point = new float[2];
        for (int i=0; i < x.length; i++) {
            assertFalse(it.isDone());
            assertEquals(i == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO, it.currentSegment(point));
            assertEquals(x[i], point[0], "x");
            assertEquals(y[i], point[1], "y");
            it.next();
        }
        assertTrue(it.isDone());
    }
}
