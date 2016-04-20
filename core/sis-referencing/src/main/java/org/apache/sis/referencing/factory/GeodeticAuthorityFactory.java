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
package org.apache.sis.referencing.factory;

import java.util.Set;
import java.util.Collections;
import javax.measure.unit.Unit;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.iso.AbstractFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.crs.DefaultParametricCRS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;


/**
 * Creates geodetic objects from codes defined by an authority.
 * An <cite>authority</cite> is an organization that maintains definitions of authority codes.
 * An <cite>authority code</cite> is a compact string defined by an authority to reference a particular spatial reference object.
 * A frequently used set of authority codes is the <a href="http://www.epsg.org">EPSG geodetic dataset</a>,
 * a database of coordinate systems and other spatial referencing objects where each object has a code number ID.
 *
 * <div class="note"><b>Example:</b>
 * the EPSG code for a <cite>World Geodetic System 1984</cite> (WGS84) coordinate reference system
 * with latitude and longitude axes is {@code "4326"}.</div>
 *
 * <p>This class defines a default implementation for most methods defined in the {@link DatumAuthorityFactory},
 * {@link CSAuthorityFactory} and {@link CRSAuthorityFactory} interfaces. However, those interfaces do not appear
 * in the {@code implements} clause of this class declaration. This is up to subclasses to decide which interfaces
 * they declare to implement.</p>
 *
 * <p>The default implementation for all {@code createFoo(String)} methods ultimately invokes
 * {@link #createObject(String)}, which may be the only method that a subclass need to override.
 * However, other methods may be overridden as well for better performances.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class GeodeticAuthorityFactory extends AbstractFactory implements AuthorityFactory {
    /**
     * Creates a new authority factory for geodetic objects.
     */
    protected GeodeticAuthorityFactory() {
    }

    /**
     * Returns the database or specification that defines the codes recognized by this factory.
     * This method may return {@code null} if it can not obtain this information, for example because
     * the connection to a database is not available.
     *
     * <div class="note"><b>Example:</b>
     * a factory that create coordinate reference system objects from EPSG codes could return
     * a citation like below:
     *
     * {@preformat text
     *   Citation
     *   ├─ Title ……………………………………………………… EPSG Geodetic Parameter Dataset
     *   ├─ Identifier ………………………………………… EPSG
     *   ├─ Online resource (1 of 2)
     *   │  ├─ Linkage ………………………………………… http://epsg-registry.org/
     *   │  └─ Function ……………………………………… Browse
     *   └─ Online resource (2 of 2)
     *      ├─ Linkage ………………………………………… jdbc:derby:/my/path/to/SIS_DATA/Databases/SpatialMetadata
     *      ├─ Description ……………………………… EPSG dataset version 8.9 on “Apache Derby Embedded JDBC Driver” version 10.12.
     *      └─ Function ……………………………………… Connection
     * }
     *
     * The online resource description with a “Connection” function is a SIS extension.</div>
     *
     * @return The organization responsible for definition of the database, or {@code null} if unknown.
     *
     * @see #getVendor()
     */
    @Override
    public abstract Citation getAuthority();

    /**
     * Returns all namespaces recognized by this factory. Those namespaces can appear before codes in
     * calls to {@code createFoo(String)} methods, for example {@code "EPSG"} in {@code "EPSG:4326"}.
     * Namespaces are case-insensitive.
     *
     * <p>The namespaces are closely related to the {@linkplain #getAuthority() authority}. Often the namespace is
     * the authority {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getIdentifiers() identifier},
     * but not always.</p>
     *
     * <div class="note"><b>Examples:</b>
     * <ul class="verbose">
     *   <li>The {@link org.apache.sis.referencing.factory.sql.EPSGFactory} authority identifier is {@code "EPSG"}
     *       and its {@code getCodeSpaces()} method returns a set containing {@code "EPSG"}. So in this example,
     *       authority and namespace match. That namespace value means that {@code EPSGFactory.createFoo(String)}
     *       methods accept both {@code "EPSG:4326"} (case-insensitive) and {@code "4326"} codes as argument.</li>
     *
     *   <li>The {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess} authority identifier is {@code "EPSG"}
     *       but its {@code getCodeSpaces()} method returns an empty set. This means that despite the EPSG authority,
     *       {@code EPSGDataAccess.createFoo(String)} methods accept only codes like {@code "4326"} without
     *       {@code "EPSG:"} prefix (the reason is that {@code EPSGDataAccess} is not expected to be used directly).</li>
     *
     *   <li>The {@link CommonAuthorityFactory} authority identifiers are ISO 19128 and OGC 06-042 (the WMS specification)
     *       but its {@code getCodeSpaces()} method returns a set containing {@code "CRS"}, {@code "AUTO"} and {@code "AUTO2"}.
     *       While ISO 19128 is defined as the first authority, the namespace is actually defined by OGC.</li>
     * </ul></div>
     *
     * The default implementation infers the namespace from the {@linkplain #getAuthority() authority}.
     * Subclasses can override this method, but the set should always contain the same elements during
     * all factory lifetime.
     *
     * @return The namespaces recognized by this factory, or an empty set if none.
     */
    public Set<String> getCodeSpaces() {
        final String authority = Citations.getCodeSpace(getAuthority());
        return (authority != null) ? Collections.singleton(authority) : Collections.<String>emptySet();
    }

    /**
     * Returns a description of the object corresponding to a code.
     * The description can be used for example in a combo box in a graphical user interface.
     *
     * <div class="section">Default implementation</div>
     * The default implementation invokes {@link #createObject(String)} for the given code
     * and returns the {@linkplain AbstractIdentifiedObject#getName() object name}.
     * This may be costly since it involves a full object creation.
     * Subclasses are encouraged to provide a more efficient implementation if they can.
     *
     * @param  code Value allocated by authority.
     * @return A description of the object, or {@code null} if the object
     *         corresponding to the specified {@code code} has no description.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if an error occurred while fetching the description.
     */
    @Override
    public InternationalString getDescriptionText(final String code) throws FactoryException {
        return new SimpleInternationalString(createObject(code).getName().getCode());
    }

    /**
     * Returns an arbitrary object from a code. The returned object will typically be an instance of {@link Datum},
     * {@link CoordinateSystem}, {@link CoordinateReferenceSystem} or {@link CoordinateOperation}.
     * This method may be used when the type of the object to create is unknown.
     * But it is recommended to invoke the most specific {@code createFoo(String)} method when
     * the desired type is known, both for performance reason and for avoiding ambiguity.
     *
     * <div class="section">Note for subclasses</div>
     * In default {@code GeodeticAuthorityFactory} implementation, all {@code createFoo(String)} methods ultimately
     * delegate to this {@code createObject(String)} method and verify if the created object is of the desired type.
     * Overriding this method is sufficient for supporting the more specific {@code createFoo(String)} methods,
     * but subclasses are encouraged to override the later for efficiency.
     *
     * @param  code Value allocated by authority.
     * @return The object for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.AbstractIdentifiedObject
     */
    @Override
    public abstract IdentifiedObject createObject(String code) throws NoSuchAuthorityCodeException, FactoryException;

    /**
     * Creates an arbitrary coordinate reference system from a code.
     * The returned object will typically be an instance of {@link GeographicCRS}, {@link ProjectedCRS},
     * {@link VerticalCRS} or {@link CompoundCRS}.
     * If the coordinate reference system type is known at compile time,
     * it is recommended to invoke the most precise method instead of this one (for example
     * {@link #createGeographicCRS createGeographicCRS(String)} instead of
     * <code>createCoordinateReferenceSystem(code)</code> if the caller know he is asking for a
     * {@linkplain org.apache.sis.referencing.crs.DefaultGeographicCRS geographic coordinate reference system}).
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * Two frequently used authorities are "CRS" and "EPSG", which include the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>       <th>Type</th>         <th>Description</th></tr>
     *   <tr><td>CRS:84</td>     <td>Geographic</td>   <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>EPSG:4326</td> <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4979</td> <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4978</td> <td>Geocentric</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:3395</td> <td>Projected</td>     <td>WGS 84 / World Mercator</td></tr>
     *   <tr><td>EPSG:5714</td> <td>Vertical</td>      <td>Mean Sea Level height</td></tr>
     *   <tr><td>EPSG:6349</td> <td>Compound</td>      <td>NAD83(2011) + NAVD88 height</td></tr>
     *   <tr><td>EPSG:5800</td> <td>Engineering</td>   <td>Astra Minas Grid</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.AbstractCRS
     * @see org.apache.sis.referencing.CRS#forCode(String)
     */
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateReferenceSystem.class, createObject(code), code);
    }

    /**
     * Creates a 2- or 3-dimensional coordinate reference system based on an ellipsoidal approximation of the geoid.
     * This provides an accurate representation of the geometry of geographic features
     * for a large portion of the earth's surface.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * Two frequently used authorities are "CRS" and "EPSG", which include the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Type</th>          <th>Description</th></tr>
     *   <tr><td>CRS:27</td>    <td>Geographic</td>    <td>Like EPSG:4267 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:83</td>    <td>Geographic</td>    <td>Like EPSG:4269 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:84</td>    <td>Geographic</td>    <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>EPSG:4322</td> <td>Geographic</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4985</td> <td>Geographic 3D</td> <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4326</td> <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4979</td> <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4267</td> <td>Geographic</td>    <td>North American Datum 1927</td></tr>
     *   <tr><td>EPSG:4269</td> <td>Geographic</td>    <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:4230</td> <td>Geographic</td>    <td>European Datum 1950</td></tr>
     *   <tr><td>EPSG:4258</td> <td>Geographic</td>    <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4937</td> <td>Geographic 3D</td> <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4047</td> <td>Geographic</td>    <td>GRS 1980 Authalic Sphere</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultGeographicCRS
     * @see org.apache.sis.referencing.CommonCRS#geographic()
     * @see org.apache.sis.referencing.CommonCRS#geographic3D()
     */
    public GeographicCRS createGeographicCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeographicCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 3-dimensional coordinate reference system with the origin at the approximate centre of mass of the earth.
     * A geocentric CRS deals with the earth's curvature by taking a 3-dimensional spatial view, which obviates
     * the need to model the earth's curvature.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:4936</td> <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4978</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4984</td> <td>World Geodetic System 1972</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.crs.DefaultGeocentricCRS
     * @see org.apache.sis.referencing.CommonCRS#geocentric()
     */
    public GeocentricCRS createGeocentricCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeocentricCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 2-dimensional coordinate reference system used to approximate the shape of the earth on a planar surface.
     * It is done in such a way that the distortion that is inherent to the approximation is carefully controlled and known.
     * Distortion correction is commonly applied to calculated bearings and distances to produce values
     * that are a close match to actual field values.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which contains more than 4000 codes for projected CRS.
     * Some of them are:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:3034</td> <td>ETRS89 / Lambert Conic Conformal Europe</td></tr>
     *   <tr><td>EPSG:3395</td> <td>WGS 84 / World Mercator</td></tr>
     *   <tr><td>EPSG:6350</td> <td>NAD83(2011) / Conus Albers Equal Area</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultProjectedCRS
     */
    public ProjectedCRS createProjectedCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ProjectedCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 1-dimensional coordinate reference system used for recording heights or depths.
     * Vertical CRSs make use of the direction of gravity to define the concept of height or depth,
     * but the relationship with gravity may not be straightforward.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:5715</td> <td>Mean Sea Level depth</td></tr>
     *   <tr><td>EPSG:5714</td> <td>Mean Sea Level height</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
     * @see org.apache.sis.referencing.CommonCRS.Vertical#crs()
     */
    public VerticalCRS createVerticalCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 1-dimensional coordinate reference system used for the recording of time.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultTemporalCRS
     * @see org.apache.sis.referencing.CommonCRS.Temporal#crs()
     */
    public TemporalCRS createTemporalCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TemporalCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 1-dimensional parametric coordinate reference system.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.crs.ParametricCRS}. This change is pending GeoAPI revision.</div>
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultParametricCRS
     */
    public DefaultParametricCRS createParametricCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(DefaultParametricCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a CRS describing the position of points through two or more independent coordinate reference systems.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:6349</td> <td>NAD83(2011) + NAVD88 height</td></tr>
     *   <tr><td>EPSG:7423</td> <td>ETRS89 + EVRF2007 height</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultCompoundCRS
     */
    public CompoundCRS createCompoundCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CompoundCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a CRS that is defined by its coordinate conversion from another CRS (not by a datum).
     * {@code DerivedCRS} can not be {@code ProjectedCRS} themselves, but may be derived from a projected CRS.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultDerivedCRS
     */
    public DerivedCRS createDerivedCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(DerivedCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 1-, 2- or 3-dimensional contextually local coordinate reference system.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:5800</td> <td>Astra Minas Grid</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultEngineeringCRS
     */
    public EngineeringCRS createEngineeringCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EngineeringCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates a 2-dimensional engineering coordinate reference system applied to locations in images.
     * Image coordinate reference systems are treated as a separate sub-type because a separate
     * user community exists for images with its own terms of reference.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateReferenceSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate reference system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.crs.DefaultImageCRS
     */
    public ImageCRS createImageCRS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ImageCRS.class, createCoordinateReferenceSystem(code), code);
    }

    /**
     * Creates an arbitrary datum from a code. The returned object will typically be an
     * instance of {@link GeodeticDatum}, {@link VerticalDatum} or {@link TemporalDatum}.
     * If the datum is known at compile time, it is recommended to invoke the most precise method instead of this one.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which contains hundred of datum. Some of them are:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Type</th>        <th>Description</th></tr>
     *   <tr><td>EPSG:6326</td> <td>Geodetic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:6322</td> <td>Geodetic</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:1027</td> <td>Vertical</td>    <td>EGM2008 geoid</td></tr>
     *   <tr><td>EPSG:5100</td> <td>Vertical</td>    <td>Mean Sea Level</td></tr>
     *   <tr><td>EPSG:9315</td> <td>Engineering</td> <td>Seismic bin grid datum</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.AbstractDatum
     */
    public Datum createDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Datum.class, createObject(code), code);
    }

    /**
     * Creates a datum defining the location and orientation of an ellipsoid that approximates the shape of the earth.
     * Geodetic datum are used together with ellipsoidal coordinate system, and also with Cartesian coordinate system
     * centered in the ellipsoid (or sphere).
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which contains hundred of datum. Some of them are:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:6326</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:6322</td> <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:6269</td> <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:6258</td> <td>European Terrestrial Reference System 1989</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultGeodeticDatum
     * @see org.apache.sis.referencing.CommonCRS#datum()
     */
    public GeodeticDatum createGeodeticDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(GeodeticDatum.class, createDatum(code), code);
    }

    /**
     * Creates a datum identifying a particular reference level surface used as a zero-height surface.
     * There are several types of vertical datums, and each may place constraints on the axis with which
     * it is combined to create a vertical CRS.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:5100</td> <td>Mean Sea Level</td></tr>
     *   <tr><td>EPSG:1027</td> <td>EGM2008 geoid</td></tr>
     *   <tr><td>EPSG:1131</td> <td>Japanese Geodetic Datum 2011 (vertical)</td></tr>
     *   <tr><td>EPSG:5215</td> <td>European Vertical Reference Frame 2007</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
     * @see org.apache.sis.referencing.CommonCRS.Vertical#datum()
     */
    public VerticalDatum createVerticalDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalDatum.class, createDatum(code), code);
    }

    /**
     * Creates a datum defining the origin of a temporal coordinate reference system.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultTemporalDatum
     * @see org.apache.sis.referencing.CommonCRS.Temporal#datum()
     */
    public TemporalDatum createTemporalDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TemporalDatum.class, createDatum(code), code);
    }

    /**
     * Creates a datum defining the origin of a parametric coordinate reference system.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.datum.ParametricDatum}. This change is pending GeoAPI revision.</div>
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultParametricDatum
     */
    public DefaultParametricDatum createParametricDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(DefaultParametricDatum.class, createDatum(code), code);
    }

    /**
     * Creates a datum defining the origin of an engineering coordinate reference system.
     * An engineering datum is used in a region around that origin.
     * This origin can be fixed with respect to the earth or be a defined point on a moving vehicle.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:9315</td> <td>Seismic bin grid datum</td></tr>
     *   <tr><td>EPSG:9300</td> <td>Astra Minas</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultEngineeringDatum
     */
    public EngineeringDatum createEngineeringDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EngineeringDatum.class, createDatum(code), code);
    }

    /**
     * Creates a datum defining the origin of an image coordinate reference system.
     * An image datum is used in a local context only.
     * For an image datum, the anchor point is usually either the centre of the image or the corner of the image.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createDatum(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The datum for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultImageDatum
     */
    public ImageDatum createImageDatum(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(ImageDatum.class, createDatum(code), code);
    }

    /**
     * Creates a geometric figure that can be used to describe the approximate shape of the earth.
     * In mathematical terms, it is a surface formed by the rotation of an ellipse about its minor axis.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:7030</td> <td>WGS 84</td></tr>
     *   <tr><td>EPSG:7034</td> <td>Clarke 1880</td></tr>
     *   <tr><td>EPSG:7048</td> <td>GRS 1980 Authalic Sphere</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The ellipsoid for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultEllipsoid
     * @see org.apache.sis.referencing.CommonCRS#ellipsoid()
     */
    public Ellipsoid createEllipsoid(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Ellipsoid.class, createObject(code), code);
    }

    /**
     * Creates a prime meridian defining the origin from which longitude values are determined.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:8901</td> <td>Greenwich</td></tr>
     *   <tr><td>EPSG:8903</td> <td>Paris</td></tr>
     *   <tr><td>EPSG:8904</td> <td>Bogota</td></tr>
     *   <tr><td>EPSG:8905</td> <td>Madrid</td></tr>
     *   <tr><td>EPSG:8906</td> <td>Rome</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The prime meridian for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.datum.DefaultPrimeMeridian
     * @see org.apache.sis.referencing.CommonCRS#primeMeridian()
     */
    public PrimeMeridian createPrimeMeridian(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(PrimeMeridian.class, createObject(code), code);
    }

    /**
     * Creates information about spatial, vertical, and temporal extent (usually a domain of validity) from a code.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:1262</td> <td>World</td></tr>
     *   <tr><td>EPSG:3391</td> <td>World - between 80°S and 84°N</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The extent for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultExtent
     */
    public Extent createExtent(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Extent.class, createObject(code), code);
    }

    /**
     * Creates an arbitrary coordinate system from a code. The returned object will typically be an
     * instance of {@link EllipsoidalCS}, {@link CartesianCS}, {@link VerticalCS} or {@link TimeCS}.
     * If the coordinate system is known at compile time, it is recommended to invoke the most precise
     * method instead of this one.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Type</th>              <th>Axes</th>                                    <th>Orientations</th>    <th>Unit</th></tr>
     *   <tr><td>EPSG:4496</td> <td>Cartesian 2D CS</td>   <td>easting, northing (E,N)</td>                 <td>east, north</td>     <td>metre</td></tr>
     *   <tr><td>EPSG:6422</td> <td>Ellipsoidal 2D CS</td> <td>latitude, longitude</td>                     <td>north, east</td>     <td>degree</td></tr>
     *   <tr><td>EPSG:6423</td> <td>Ellipsoidal 3D CS</td> <td>latitude, longitude, ellipsoidal height</td> <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     *   <tr><td>EPSG:6404</td> <td>Spherical 3D CS</td>   <td>latitude, longitude, radius</td>             <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     *   <tr><td>EPSG:6499</td> <td>Vertical CS</td>       <td>height (H)</td>                              <td>up</td>              <td>metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.AbstractCS
     */
    public CoordinateSystem createCoordinateSystem(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CoordinateSystem.class, createObject(code), code);
    }

    /**
     * Creates a 2- or 3-dimensional coordinate system for geodetic latitude and longitude,
     * sometime with ellipsoidal height.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Axes</th>                                    <th>Orientations</th>    <th>Unit</th></tr>
     *   <tr><td>EPSG:6422</td> <td>latitude, longitude</td>                     <td>north, east</td>     <td>degree</td></tr>
     *   <tr><td>EPSG:6424</td> <td>longitude, latitude</td>                     <td>east, north</td>     <td>degree</td></tr>
     *   <tr><td>EPSG:6429</td> <td>longitude, latitude</td>                     <td>east, north</td>     <td>radian</td></tr>
     *   <tr><td>EPSG:6423</td> <td>latitude, longitude, ellipsoidal height</td> <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultEllipsoidalCS
     */
    public EllipsoidalCS createEllipsoidalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(EllipsoidalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 1-dimensional coordinate system for heights or depths of points.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Axes</th>       <th>Orientations</th> <th>Unit</th></tr>
     *   <tr><td>EPSG:6498</td> <td>depth (D)</td>  <td>down</td>         <td>metre</td></tr>
     *   <tr><td>EPSG:6499</td> <td>height (H)</td> <td>up</td>           <td>metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultVerticalCS
     */
    public VerticalCS createVerticalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(VerticalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 1-dimensional coordinate system for time elapsed in the specified time units
     * from a specified time origin.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultTimeCS
     */
    public TimeCS createTimeCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(TimeCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 1-dimensional parametric coordinate system.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may be changed
     * to {@code org.opengis.referencing.cs.ParametricCS}. This change is pending GeoAPI revision.</div>
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultParametricCS
     */
    public DefaultParametricCS createParametricCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(DefaultParametricCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 2- or 3-dimensional Cartesian coordinate system made of straight orthogonal axes.
     * All axes shall have the same linear unit of measure.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Axes</th>                    <th>Orientations</th> <th>Unit</th></tr>
     *   <tr><td>EPSG:4406</td> <td>easting, northing (E,N)</td> <td>east, north</td>  <td>kilometre</td></tr>
     *   <tr><td>EPSG:4496</td> <td>easting, northing (E,N)</td> <td>east, north</td>  <td>metre</td></tr>
     *   <tr><td>EPSG:4500</td> <td>northing, easting (N,E)</td> <td>north, east</td>  <td>metre</td></tr>
     *   <tr><td>EPSG:4491</td> <td>westing, northing (W,N)</td> <td>west, north</td>  <td>metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultCartesianCS
     */
    public CartesianCS createCartesianCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CartesianCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 3-dimensional coordinate system with one distance measured from the origin and two angular coordinates.
     * Not to be confused with an ellipsoidal coordinate system based on an ellipsoid "degenerated" into a sphere.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Axes</th>                        <th>Orientations</th>    <th>Unit</th></tr>
     *   <tr><td>EPSG:6404</td> <td>latitude, longitude, radius</td> <td>north, east, up</td> <td>degree, degree, metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultSphericalCS
     */
    public SphericalCS createSphericalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(SphericalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 3-dimensional coordinate system made of a polar coordinate system
     * extended by a straight perpendicular axis.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultCylindricalCS
     */
    public CylindricalCS createCylindricalCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(CylindricalCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a 2-dimensional coordinate system for coordinates represented by a distance from the origin
     * and an angle from a fixed direction.
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createCoordinateSystem(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultPolarCS
     */
    public PolarCS createPolarCS(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(PolarCS.class, createCoordinateSystem(code), code);
    }

    /**
     * Creates a coordinate system axis with name, direction, unit and range of values.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th>   <th>Unit</th></tr>
     *   <tr><td>EPSG:106</td>  <td>Latitude (φ)</td>  <td>degree</td></tr>
     *   <tr><td>EPSG:107</td>  <td>Longitude (λ)</td> <td>degree</td></tr>
     *   <tr><td>EPSG:1</td>    <td>Easting (E)</td>   <td>metre</td></tr>
     *   <tr><td>EPSG:2</td>    <td>Northing (N)</td>  <td>metre</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The axis for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis
     */
    public CoordinateSystemAxis createCoordinateSystemAxis(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateSystemAxis.class, createObject(code), code);
    }

    /**
     * Creates an unit of measurement from a code.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="Authority codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:9002</td> <td>decimal degree</td></tr>
     *   <tr><td>EPSG:9001</td> <td>metre</td></tr>
     *   <tr><td>EPSG:9030</td> <td>kilometre</td></tr>
     *   <tr><td>EPSG:1040</td> <td>second</td></tr>
     *   <tr><td>EPSG:1029</td> <td>year</td></tr>
     * </table>
     *
     * See {@link org.apache.sis.measure.Units#valueOfEPSG(int)} for a more complete list of codes.</div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The unit of measurement for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.measure.Units#valueOfEPSG(int)
     */
    public Unit<?> createUnit(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(Unit.class, createObject(code), code);
    }

    /**
     * Creates a definition of a single parameter used by an operation method.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:8801</td> <td>Latitude of natural origin</td></tr>
     *   <tr><td>EPSG:8802</td> <td>Longitude of natural origin</td></tr>
     *   <tr><td>EPSG:8805</td> <td>Scale factor at natural origin</td></tr>
     *   <tr><td>EPSG:8806</td> <td>False easting</td></tr>
     *   <tr><td>EPSG:8807</td> <td>False northing</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The parameter descriptor for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.parameter.DefaultParameterDescriptor
     * @see <a href="http://sis.apache.org/book/tables/CoordinateOperationMethods.html">Apache SIS™ Coordinate Operation Methods</a>
     */
    public ParameterDescriptor<?> createParameterDescriptor(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(ParameterDescriptor.class, createObject(code), code);
    }

    /**
     * Creates a description of the algorithm and parameters used to perform a coordinate operation.
     * An {@code OperationMethod} is a kind of metadata: it does not perform any coordinate operation
     * (e.g. map projection) by itself, but tells us what is needed in order to perform such operation.
     *
     * <p>Available methods depend both on the {@linkplain #getAuthorityCodes(Class) set declared by the authority} and on the
     * <a href="http://sis.apache.org/book/tables/CoordinateOperationMethods.html">list of methods implemented in Apache SIS</a>.
     * In order to be supported, an operation method must have its formulas coded in the Java programming language.
     * See {@link org.apache.sis.referencing.operation.transform.MathTransformProvider} for more information.</p>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The operation method for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.operation.DefaultOperationMethod
     * @see <a href="http://sis.apache.org/book/tables/CoordinateOperationMethods.html">Apache SIS™ Coordinate Operation Methods</a>
     */
    public OperationMethod createOperationMethod(final String code) throws NoSuchAuthorityCodeException, FactoryException {
        return cast(OperationMethod.class, createObject(code), code);
    }

    /**
     * Creates an operation for transforming coordinates in the source CRS to coordinates in the target CRS.
     * Coordinate operations contain a {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform
     * math transform}, which does the actual work of transforming coordinates.
     *
     * <div class="note"><b>Example:</b>
     * the {@linkplain #getAuthorityCodes(Class) set of available codes} depends on the defining
     * {@linkplain #getAuthority() authority} and the {@code GeodeticAuthorityFactory} subclass in use.
     * A frequently used authority is "EPSG", which includes the following codes:
     *
     * <table class="sis" summary="EPSG codes examples">
     *   <tr><th>Code</th>      <th>Description</th></tr>
     *   <tr><td>EPSG:1133</td> <td>ED50 to WGS 84 (1)</td></tr>
     *   <tr><td>EPSG:1241</td> <td>NAD27 to NAD83 (1)</td></tr>
     *   <tr><td>EPSG:1173</td> <td>NAD27 to WGS 84 (4)</td></tr>
     *   <tr><td>EPSG:6326</td> <td>NAD83(2011) to NAVD88 height (1)</td></tr>
     * </table></div>
     *
     * <div class="section">Default implementation</div>
     * The default implementation delegates to {@link #createObject(String)} and casts the result.
     * If the result can not be casted, then a {@link NoSuchAuthorityCodeException} is thrown.
     *
     * @param  code Value allocated by authority.
     * @return The operation for the given code.
     * @throws NoSuchAuthorityCodeException if the specified {@code code} was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     *
     * @see org.apache.sis.referencing.operation.AbstractCoordinateOperation
     */
    public CoordinateOperation createCoordinateOperation(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return cast(CoordinateOperation.class, createObject(code), code);
    }

    /**
     * Creates operations from source and target coordinate reference system codes.
     * This method should only extract the information explicitely declared in a database like EPSG.
     * This method should not attempt to infer by itself operations that are not explicitely recorded in the database.
     *
     * <div class="section">Default implementation</div>
     * The default implementation returns an empty set.
     *
     * @param  sourceCRS  Coded value of source coordinate reference system.
     * @param  targetCRS  Coded value of target coordinate reference system.
     * @return The operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws NoSuchAuthorityCodeException if a specified code was not found.
     * @throws FactoryException if the object creation failed for some other reason.
     */
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(String sourceCRS, String targetCRS)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        return Collections.emptySet();
    }

    /**
     * Creates a finder which can be used for looking up unidentified objects.
     * The finder tries to fetch a fully {@linkplain AbstractIdentifiedObject identified object}
     * from an incomplete one, for example from an object without "{@code ID[…]}" or
     * "{@code AUTHORITY[…]}" element in <cite>Well Known Text</cite>.
     *
     * @return A finder to use for looking up unidentified objects.
     * @throws FactoryException if the finder can not be created.
     *
     * @see org.apache.sis.referencing.IdentifiedObjects#newFinder(String)
     */
    public IdentifiedObjectFinder newIdentifiedObjectFinder() throws FactoryException {
        return new IdentifiedObjectFinder(this);
    }

    /**
     * Returns {@code true} if the given portion of the code is equal, ignoring case, to the given namespace.
     */
    static boolean regionMatches(final String namespace, final String code, final int start, final int end) {
        return (namespace.length() == end - start) && code.regionMatches(true, start, namespace, 0, namespace.length());
    }

    /**
     * Trims the namespace, if present. For example if this factory is an EPSG authority factory
     * and the specified code start with the {@code "EPSG:"} prefix, then the prefix is removed.
     * Otherwise, the string is returned unchanged (except for leading and trailing spaces).
     *
     * @param  code The code to trim.
     * @return The code with the namespace part removed if that part matched one of the values given by
     *         {@link #getCodeSpaces()}.
     */
    final String trimNamespace(final String code) {
        int s = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
        if (s >= 0) {
            final int end   = CharSequences.skipTrailingWhitespaces(code, 0, s);
            final int start = CharSequences.skipLeadingWhitespaces (code, 0, end);
            for (final String codespace : getCodeSpaces()) {
                if (regionMatches(codespace, code, start, end)) {
                    final int n = code.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR, s + 1);
                    if (n >= 0) {
                        /*
                         * The separator sometime appears twice, as in "EPSG::4326" or "EPSG:8.9:4326".
                         * The part between the two separators is the verion number, which we ignore in
                         * this simple version.
                         */
                        s = n;
                    }
                    final int length = code.length();
                    return CharSequences.trimWhitespaces(code, s+1, length).toString();
                }
            }
        }
        return CharSequences.trimWhitespaces(code);
    }

    /**
     * Casts the given object to the given type, or throws an exception if the object can not be casted.
     * This convenience method is provided for implementation of {@code createXXX} methods.
     *
     * @param  type   The type to return (e.g. {@code CoordinateReferenceSystem.class}).
     * @param  object The object to cast.
     * @param  code   The authority code, used only for formatting an error message.
     * @return The object casted to the given type.
     * @throws NoSuchAuthorityCodeException if the given object is not an instance of the given type.
     */
    @SuppressWarnings("unchecked")
    private <T> T cast(final Class<T> type, final IdentifiedObject object, final String code)
            throws NoSuchAuthorityCodeException
    {
        if (type.isInstance(object)) {
            return (T) object;
        }
        /*
         * Get the actual type of the object. Returns the GeoAPI type if possible,
         * or fallback on the implementation class otherwise.
         */
        final Class<?> actual;
        if (object instanceof AbstractIdentifiedObject) {
            actual = ((AbstractIdentifiedObject) object).getInterface();
        } else {
            actual = object.getClass();
        }
        /*
         * Get the authority from the object if possible, in order to avoid a call
         * to the potentially costly (for EPSGDataAccess) getAuthority() method.
         */
        final Identifier id = object.getName();
        final Citation authority = (id != null) ? id.getAuthority() : getAuthority();
        throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.UnexpectedTypeForReference_3, code, type, actual),
                Citations.getIdentifier(authority, false), trimNamespace(code), code);
    }

    /**
     * Returns a string representation of this factory for debugging purpose only.
     * The string returned by this method may change in any future SIS version.
     *
     * @return A string representation for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this))
                .append("[“").append(Citations.getIdentifier(getAuthority(), false)).append('”');
        toString(buffer);
        return buffer.append(']').toString();
    }

    /**
     * Hook for subclasses.
     */
    void toString(final StringBuilder buffer) {
    }
}
