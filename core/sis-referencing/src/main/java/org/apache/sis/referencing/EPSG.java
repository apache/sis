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
package org.apache.sis.referencing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Documents the relationship between a SIS element and the corresponding EPSG element.
 * The EPSG geodetic parameter dataset is a structured database required to:
 *
 * <ul>
 *   <li>define {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem Coordinate Reference Systems} (CRS)
 *       such that coordinates describe positions unambiguously;</li>
 *   <li>define {@linkplain org.opengis.referencing.operation.CoordinateOperation Coordinate Operations}
 *       that allow coordinates to be changed from one CRS to another CRS.</li>
 * </ul>
 *
 * This {@code EPSG} annotation is associated to various elements defined by SIS in Java, including:
 *
 * <ul>
 *   <li>classes or methods implementing a specific coordinate operation method;</li>
 *   <li>enumeration constants representing some specific CRS;</li>
 *   <li>fields containing parameter values.</li>
 * </ul>
 *
 * This annotation has two members: the GeoAPI {@linkplain #type() type} and the EPSG {@linkplain #code() code}.
 * The <var>type</var> specifies which {@link org.opengis.referencing.AuthorityFactory} method to invoke, while
 * the <var>code</var> specifies the argument value to give to that method in order to get the EPSG object. For
 * example the {@link GeodeticObjects#WGS84} enumeration constants has the following annotation:
 *
 * {@preformat java
 *   &#64;EPSG(type = GeographicCRS.class, code = 4326)
 * }
 *
 * which means that the annotated constant is related to an EPSG object that could be obtained by the following
 * code, where {@code factory} is an instance of {@link org.opengis.referencing.crs.CRSAuthorityFactory}:
 *
 * {@preformat java
 *   GeographicCRS crs = factory.createGeographicCRS("4326");
 * }
 *
 * The EPSG objects can also be inspected online on the <a href="http://www.epsg-registry.org/">EPSG registry</a> web site.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.metadata.iso.citation.Citations#EPSG
 * @see org.apache.sis.io.wkt.Convention#EPSG
 * @see <a href="http://www.epsg.org">EPSG Geodetic Parameters</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface EPSG {
    /**
     * The type returned by the {@link org.opengis.referencing.AuthorityFactory} method
     * which create the EPSG object. There is a one-to-one relationship between this type
     * and the table to query in the EPSG database.
     *
     * @return The type of the object identified by the EPSG code.
     */
    Class<?> type();

    /**
     * The argument to give to the method identified by {@link #type()} in order to create the EPSG object.
     * Those codes are used as primary keys in the EPSG database.
     *
     * @return The code of the EPSG object.
     */
    int code();
}
