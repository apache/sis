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

import org.opengis.metadata.acquisition.GeometryType;


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
 * @see org.apache.sis.feature.builder.FeatureTypeBuilder#addAttribute(GeometryType)
 *
 * @since 0.8
 * @module
 */
public enum GeometryLibrary {
    /**
     * The Java Topology Suite (JTS) library. This open source library provides an object model
     * for Euclidean planar geometry together with a set of fundamental geometric functions.
     * The library is licensed under Eclipse Distribution License.
     *
     * <table class="sis">
     *   <caption>Implementation classes</caption>
     *   <tr><th>Geometry type</th>               <th>Class name</th></tr>
     *   <tr><td>Root geometry class</td>         <td>{@code org.locationtech.jts.geom.Geometry}</td></tr>
     *   <tr><td>{@link GeometryType#POINT}</td>  <td>{@code org.locationtech.jts.geom.Point}</td></tr>
     *   <tr><td>{@link GeometryType#LINEAR}</td> <td>{@code org.locationtech.jts.geom.LineString}</td></tr>
     *   <tr><td>{@link GeometryType#AREAL}</td>  <td>{@code org.locationtech.jts.geom.Polygon}</td></tr>
     * </table>
     *
     * @see <a href="http://locationtech.github.io/jts/">JTS home page</a>
     */
//  JTS,

    /**
     * The ESRI geometry API library. This library can be used for spatial vector data processing.
     * It is used in ESRI GIS Tools for Hadoop and has an Android port.
     * The library is available under Apache 2 license.
     *
     * <table class="sis">
     *   <caption>Implementation classes</caption>
     *   <tr><th>Geometry type</th>               <th>Class name</th></tr>
     *   <tr><td>Root geometry class</td>         <td>{@code com.esri.core.geometry.Geometry}</td></tr>
     *   <tr><td>{@link GeometryType#POINT}</td>  <td>{@code com.esri.core.geometry.Point}</td></tr>
     *   <tr><td>{@link GeometryType#LINEAR}</td> <td>{@code com.esri.core.geometry.Polyline}</td></tr>
     *   <tr><td>{@link GeometryType#AREAL}</td>  <td>{@code com.esri.core.geometry.Polygon}</td></tr>
     * </table>
     *
     * @see <a href="https://github.com/Esri/geometry-api-java/wiki">API wiki page</a>
     */
    ESRI,

    /**
     * The Java 2D Graphics and Imaging library. This library does not provide as many topological operations
     * than other libraries, but is available on most standard Java environments and constitute a reliable
     * fallback when no other library is available.
     *
     * <table class="sis">
     *   <caption>Implementation classes</caption>
     *   <tr><th>Geometry type</th>               <th>Class name</th></tr>
     *   <tr><td>{@link GeometryType#POINT}</td>  <td>{@code java.awt.geom.Point2D}</td></tr>
     *   <tr><td>{@link GeometryType#LINEAR}</td> <td>{@code java.awt.Shape}</td></tr>
     *   <tr><td>{@link GeometryType#AREAL}</td>  <td>{@code java.awt.Shape}</td></tr>
     * </table>
     *
     * Note that contrarily to JTS and ESRI libraries,
     * a point does not extend any root geometry class in Java2D.
     *
     * @see <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/2d/index.html">Java2D home page</a>
     */
    JAVA2D
}
