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
package org.apache.sis.setup;


/**
 * Some libraries providing geometric objects or topological operations.
 * Apache SIS uses its own implementation for the most basic objects like
 * {@linkplain org.apache.sis.geometry.GeneralDirectPosition direct positions} and
 * {@linkplain org.apache.sis.geometry.GeneralEnvelope envelopes},
 * and can delegate to one of the enumerated libraries for more complex geometries.
 * All those libraries are optional.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see OptionKey#GEOMETRY_LIBRARY
 *
 * @since 0.8
 * @module
 */
public enum GeometryLibrary {
    /**
     * The Java Topology Suite (JTS) library. This open source library provides an object model
     * for Euclidean planar geometry together with a set of fundamental geometric functions.
     *
     * <ul>
     *   <li><b>Package name:</b> {@code com.vividsolutions.jts.geom}</li>
     *   <li><b>Web site:</b> <a href="http://locationtech.github.io/jts/">http://locationtech.github.io/jts/</a></li>
     *   <li><b>License:</b> LGPL prior JTS 1.14, Eclipse Distribution License afterward.</li>
     * </ul>
     */
//  JTS,

    /**
     * The ESRI geometry API library. This library can be used for spatial vector data processing.
     * It is used in ESRI GIS Tools for Hadoop and has an Android port.
     *
     * <ul>
     *   <li><b>Package name:</b> {@code com.esri.core.geometry}</li>
     *   <li><b>Web site:</b> <a href="https://github.com/Esri/geometry-api-java/wiki">https://github.com/Esri/geometry-api-java/wiki</a></li>
     *   <li><b>License:</b> Apache 2.</li>
     * </ul>
     */
    ESRI,

    /**
     * The Java 2D Graphics and Imaging library. This library does not provide as many topological operations
     * than other libraries, but is available on most standard Java environments and constitute a reliable
     * fallback when no other library is available.
     *
     * <ul>
     *   <li><b>Package name:</b> {@code java.awt.geom}</li>
     *   <li><b>Web site:</b> <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/2d/index.html">http://docs.oracle.com/javase/8/docs/technotes/guides/2d/index.html</a></li>
     *   <li><b>License:</b> GPL with classpath exception.</li>
     * </ul>
     */
    JAVA2D
}
