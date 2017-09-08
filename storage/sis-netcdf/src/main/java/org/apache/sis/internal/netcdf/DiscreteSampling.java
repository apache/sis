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
package org.apache.sis.internal.netcdf;

import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.feature.Geometries;

// Branch-dependent imports
import java.util.stream.Stream;
import org.opengis.feature.Feature;


/**
 * Returns the features encoded in the NetCDF files when they are encoded as discrete sampling.
 * The NetCDF attributes shall be conform to the "Discrete Sampling Geometries" chapter of
 * <a href="http://cfconventions.org/">CF conventions</a>. Some examples are trajectories
 * and profiles.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class DiscreteSampling {
    /**
     * The factory to use for creating geometries.
     */
    protected final Geometries<?> factory;

    /**
     * Creates a new discrete sampling parser.
     *
     * @param  library  the library for geometric objects, or {@code null} for the default.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    protected DiscreteSampling(final GeometryLibrary library) {
        factory = Geometries.implementation(library);
    }

    /**
     * Returns the stream of features.
     *
     * @return the stream of features.
     */
    public abstract Stream<Feature> features();
}
