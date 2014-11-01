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
package org.apache.sis.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.util.Collections.singletonMap;


/**
 * Compares {@link org.apache.sis.feature} memory usage with a plain {@link HashMap}.
 * This class simulates creation of features having the following properties:
 *
 * <ul>
 *   <li>{@code "city"}      : {@link String}</li>
 *   <li>{@code "longitude"} : {@link Float}</li>
 *   <li>{@code "latitude"}  : {@link Float}</li>
 * </ul>
 */
public final class FeatureMemoryBenchmark {
    /**
     * Features created by the benchmark. We need to keep reference to all of them
     * for preventing the garbage collector to free memory.
     */
    private final List<Object> features;

    /**
     * If we use SIS implementation, the feature type. Otherwise {@code null}.
     */
    private final DefaultFeatureType type;

    /**
     * In the case of non-SIS implementation, whether we use simple features or complex features.
     */
    private final boolean isSimple;

    /**
     * Random number generator for feature data.
     */
    private final Random random;

    /**
     * A buffer for generating random strings.
     */
    private final char[] buffer;

    /**
     * Creates a new benchmark.
     *
     * @param useSIS {@code true} for using SIS implementation, or {@code false} for {@link HashMap}.
     * @param isSimple In the case of non-SIS implementation, whether we use simple features or complex features.
     */
    private FeatureMemoryBenchmark(final boolean useSIS, final boolean isSimple) {
        features = new ArrayList<Object>(100000);
        this.isSimple = isSimple;
        if (useSIS) {
            type = new DefaultFeatureType           (singletonMap("name", "City"), false, null,
                    new DefaultAttributeType<String>(singletonMap("name", "city"),     String.class, 1, 1, null),
                    new DefaultAttributeType<Float> (singletonMap("name", "longitude"), Float.class, 1, 1, null),
                    new DefaultAttributeType<Float> (singletonMap("name", "latitude"),  Float.class, 1, 1, null));
        } else {
            type = null;
        }
        random = new Random();
        buffer = new char[8];
    }

    /**
     * The old feature implementation.
     */
    private static final class SimpleFeature {
        final HashMap<String,Object> attributes = new HashMap<String,Object>(8);
    }

    /**
     * A more complete feature implementation.
     */
    private static final class ComplexFeature {
        final HashMap<String, List<Property>> properties = new HashMap<String, List<Property>>(8);
    }

    /**
     * The property to be stored in {@link ComplexFeature}.
     */
    private static final class Property {
        final Object value;

        private Property(final Object value) {
            this.value = value;
        }

        static List<Property> asList(final Object value) {
            final List<Property> list = new ArrayList<Property>(2);
            list.add(new Property(value));
            return list;
        }
    }

    /**
     * Creates a new feature instance with random data.
     */
    private Object createFeature() {
        for (int i=0; i<buffer.length; i++) {
            buffer[i] = (char) ('A' + random.nextInt(26));
        }
        final String city      = new String(buffer);
        final Float  latitude  = random.nextFloat() * 180 -  90;
        final Float  longitude = random.nextFloat() * 360 - 180;
        if (type != null) {
            final AbstractFeature feature = type.newInstance();
            feature.setPropertyValue("city",      city);
            feature.setPropertyValue("latitude",  latitude);
            feature.setPropertyValue("longitude", longitude);
            return feature;
        } else if (isSimple) {
            final SimpleFeature feature = new SimpleFeature();
            feature.attributes.put("city",      city);
            feature.attributes.put("latitude",  latitude);
            feature.attributes.put("longitude", longitude);
            return feature;
        } else {
            final ComplexFeature feature = new ComplexFeature();
            feature.properties.put("city",      Property.asList(city));
            feature.properties.put("latitude",  Property.asList(latitude));
            feature.properties.put("longitude", Property.asList(longitude));
            return feature;
        }
    }

    /**
     * Creates a bunch of features until we get out of memory.
     */
    private void run() {
        for (int i=0; i<10000000; i++) {
            final Object feature;
            try {
                feature = createFeature();
            } catch (OutOfMemoryError e) {
                final int n = features.size();
                features.clear();
                System.gc();
                System.console().printf("Feature count: %d%n", n);
                return;
            }
            features.add(feature);
        }
    }

    /**
     * Runs from the command line. This method expect one argument, which is "sis", "simple" or "complex".
     *
     * @param arguments Command line arguments.
     */
    public static void main(final String[] arguments) {
        if (arguments.length == 1) {
            final String arg = arguments[0];
            final boolean useSIS = arg.equalsIgnoreCase("sis");
            boolean isSimple = false;
            if (useSIS || (isSimple = arg.equalsIgnoreCase("simple")) || arg.equalsIgnoreCase("complex")) {
                final FeatureMemoryBenchmark b = new FeatureMemoryBenchmark(useSIS, isSimple);
                long time = System.nanoTime();
                b.run();
                time = System.nanoTime() - time;
                System.console().printf("Ellapsed time: %f%n", time / 1E+9);
                return;
            }
        }
        System.console().printf("Expected argument: 'sis', 'simple' or 'complex'.%n");
    }
}
