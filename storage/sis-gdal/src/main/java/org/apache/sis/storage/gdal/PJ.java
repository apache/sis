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
package org.apache.sis.storage.gdal;

import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.system.OS;


/**
 * Wraps the <a href="http://proj.osgeo.org/">{@literal Proj.4}</a> {@code PJ} native data structure.
 * Many methods defined in this class are native methods delegating their work to the Proj.4 library.
 * This class is the only place where such native methods are defined for Proj.4 support.
 *
 * <p>In the Proj.4 library, the {@code PJ} structure is an aggregation of {@link GeodeticDatum},
 * {@link Ellipsoid}, {@link PrimeMeridian}, {@link org.opengis.referencing.cs.CoordinateSystem},
 * {@link org.opengis.referencing.crs.CoordinateReferenceSystem} and their sub-interfaces.
 * The relationship with the GeoAPI methods is indicated in the "See" tags when appropriate.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class PJ implements ReferenceIdentifier {
    /**
     * The maximal number of dimension accepted by the {@link #transform(PJ, int, double[], int, int)} method.
     * This upper limit is actually somewhat arbitrary. This limit exists mostly as a safety against potential misuse.
     */
    static final int DIMENSION_MAX = 100;

    /**
     * Loads the {@literal Proj.4} library.
     * This static initializer may throw a {@link UnsatisfiedLinkError} if the static library can not be loaded.
     * In such case, any future attempt to use this {@code PJ} class will cause a {@link NoClassDefFoundError}
     * as per Java language specification.
     */
    static {
        OS.load(PJ.class, "libproj-binding");
    }

    /**
     * The pointer to {@code PJ} structure allocated in the C/C++ heap. This value has no
     * meaning in Java code. <strong>Do not modify</strong>, since this value is used by Proj.4.
     * Do not rename neither, unless you update accordingly the C code in JNI wrappers.
     */
    private final long ptr;

    /**
     * Creates a new {@code PJ} structure from the given {@literal Proj.4} definition string.
     *
     * @param  definition  the Proj.4 definition string.
     * @throws InvalidGeodeticParameterException if the PJ structure can not be created from the given string.
     */
    public PJ(final String definition) throws InvalidGeodeticParameterException {
        Objects.requireNonNull(definition);
        ptr = allocatePJ(definition);
        if (ptr == 0) {
            throw new InvalidGeodeticParameterException(Errors.format(Errors.Keys.UnparsableStringForClass_2,
                    CoordinateReferenceSystem.class, definition));
        }
    }

    /**
     * Creates a new {@code PJ} structure for the geographic part of the given {@code PJ} object.
     * This constructor is usually for getting the
     * {@linkplain org.opengis.referencing.crs.ProjectedCRS#getBaseCRS() base geographic CRS}
     * from a {@linkplain org.opengis.referencing.crs.ProjectedCRS projected CRS}.
     *
     * @param  crs   the CRS (usually projected) from which to derive a new CRS.
     * @throws FactoryException if the PJ structure can not be created.
     */
    public PJ(final PJ crs) throws FactoryException {
        Objects.requireNonNull(crs);
        ptr = allocateGeoPJ(crs);
        if (ptr == 0) {
            throw new FactoryException(crs.getLastError());
        }
    }

    /**
     * Allocates a PJ native data structure and returns the pointer to it. This method should be invoked by
     * the constructor only, and the return value <strong>must</strong> be assigned to the {@link #ptr} field.
     * The allocated structure is released by the {@link #finalize()} method.
     *
     * @param  definition  the Proj.4 definition string.
     * @return a pointer to the PJ native data structure, or 0 if the operation failed.
     */
    private static native long allocatePJ(String definition);

    /**
     * Allocates a PJ native data structure for the base geographic CRS of the given CRS, and returns the pointer to it.
     * This method should be invoked by the constructor only, and the return value <strong>must</strong> be assigned to
     * the {@link #ptr} field. The allocated structure is released by the {@link #finalize()} method.
     *
     * @param  projected  the CRS from which to derive the base geographic CRS.
     * @return a pointer to the PJ native data structure, or 0 if the operation failed.
     */
    private static native long allocateGeoPJ(PJ projected);

    /**
     * Returns the project responsible for maintenance of the namespace.
     *
     * @see #getCodeSpace()
     */
    @Override
    public Citation getAuthority() {
        return Citations.PROJ4;
    }

    /**
     * Returns the version identifier for the namespace, as specified by the code authority.
     * This method* parses the Proj.4 release string (for example <cite>"Rel. 4.9.3, 15 August 2016"</cite>)
     * for extracting the version number ("4.9.3" in above example).
     *
     * @see Proj4#version()
     */
    @Override
    public String getVersion() {
        String rel = getRelease();
        if (rel != null) {
            int start = -1;
            final int length = rel.length();
            for (int c, i=0; i < length; i += Character.charCount(c)) {
                c = rel.codePointAt(i);
                if (Character.isDigit(c)) {
                    if (start < 0) start = i;
                } else if (c != '.' && start >= 0) {
                    return rel.substring(start, i);
                }
            }
        }
        return rel;
    }

    /**
     * Returns the version number of the {@literal Proj.4} library.
     *
     * @return the Proj.4 release string.
     *
     * @see #getVersion()
     * @see Proj4#version()
     */
    static native String getRelease();

    /**
     * Returns the namespace in which the code is valid.
     *
     * @see #getAuthority()
     * @see #getCode()
     */
    @Override
    public String getCodeSpace() {
        return Constants.PROJ4;
    }

    /**
     * Returns the {@literal Proj.4} definition string. This is the string given to the constructor,
     * expanded with as much information as possible.
     *
     * <div class="note"><b>Example:</b> "+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0"</div>
     *
     * @return the Proj.4 definition string.
     */
    @Override
    public native String getCode();

    /**
     * Returns the string representation of the PJ structure.
     * Note that the string returned by Proj.4 contains <cite>End Of Line</cite> characters.
     *
     * <div class="note"><b>Example:</b> "Lat/long (Geodetic alias)"</div>
     */
    native String getName();

    /**
     * Returns the string representation of the PJ structure.
     *
     * @return the string representation, or {@code null} if none.
     */
    public InternationalString getDescription() {
        String name = getName();
        if (name != null) {
            final StringBuilder buffer = new StringBuilder(name.length());
            for (CharSequence line : CharSequences.splitOnEOL(getName())) {
                line = CharSequences.trimWhitespaces(line);
                if (buffer.length() != 0) buffer.append(' ');
                buffer.append(line);
            }
            name = buffer.toString();
            if (!name.isEmpty()) {
               return new SimpleInternationalString(name);
            }
        }
        return null;
    }

    /**
     * Returns the Coordinate Reference System type.
     *
     * @return the CRS type.
     */
    public native Type getType();

    /**
     * The coordinate reference system (CRS) type returned by {@link PJ#getType()}.
     * In the Proj.4 library, a CRS can only be geographic, geocentric or projected,
     * without distinction between 2D and 3D CRS.
     */
    enum Type {
        /*
         * IMPLEMENTATION NOTE: Do not rename those fields, unless you update the
         * native C code accordingly.
         */

        /**
         * The CRS is of type {@link org.opengis.referencing.crs.GeographicCRS}.
         * The CRS can be two-dimensional or three-dimensional.
         */
        GEOGRAPHIC,

        /**
         * The CRS is of type {@link org.opengis.referencing.crs.GeocentricCRS}.
         * The CRS can only be three-dimensional.
         */
        GEOCENTRIC,

        /**
         * The CRS is of type {@link org.opengis.referencing.crs.ProjectedCRS}.
         * The CRS can be two-dimensional or three-dimensional.
         */
        PROJECTED
    }

    /**
     * Returns the square of the ellipsoid eccentricity (ε²). The eccentricity is related to axis length
     * by ε=√(1-(<var>b</var>/<var>a</var>)²). The eccentricity of a sphere is zero.
     *
     * @return the eccentricity.
     *
     * @see Ellipsoid#isSphere()
     * @see Ellipsoid#getInverseFlattening()
     */
    public native double getEccentricitySquared();

    /**
     * Returns the inverse flattening, computed from the eccentricity.
     * The inverse flattening factor of a sphere is infinity.
     */
    public double getInverseFlattening() {
        return 1 / (1 - Math.sqrt(1 - getEccentricitySquared()));
    }

    /**
     * Returns the value stored in the {@code a_orig} PJ field.
     *
     * @return the axis length stored in {@code a_orig}.
     *
     * @see Ellipsoid#getSemiMajorAxis()
     */
    public native double getSemiMajorAxis();

    /**
     * Returns the value computed from PJ fields by {@code √((a_orig)² × (1 - es_orig))}.
     *
     * @return the axis length computed by {@code √((a_orig)² × (1 - es_orig))}.
     *
     * @see Ellipsoid#getSemiMinorAxis()
     */
    public native double getSemiMinorAxis();

    /**
     * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
     *
     * @return the prime meridian longitude, in degrees.
     *
     * @see PrimeMeridian#getGreenwichLongitude()
     */
    public native double getGreenwichLongitude();

    /**
     * Returns an array of character indicating the direction of each axis. Directions are
     * characters like {@code 'e'} for East, {@code 'n'} for North and {@code 'u'} for Up.
     *
     * @return the axis directions.
     *
     * @see org.opengis.referencing.cs.CoordinateSystemAxis#getDirection()
     */
    public native char[] getAxisDirections();

    /**
     * Returns the conversion factor from the linear units to metres.
     *
     * @param  vertical {@code false} for the conversion factor of horizontal axes,
     *         or {@code true} for the conversion factor of the vertical axis.
     * @return the conversion factor to metres for the given axis.
     */
    public native double getLinearUnitToMetre(boolean vertical);

    /**
     * Transforms in-place the coordinates in the given array.
     * The coordinates array shall contain (<var>x</var>,<var>y</var>,<var>z</var>,…) tuples,
     * where the <var>z</var> and any additional dimensions are optional.
     * Note that any dimension after the <var>z</var> value are ignored.
     *
     * <p>Input and output units:</p>
     * <ul>
     *   <li>Angular units (as in longitude and latitudes) are decimal degrees.</li>
     *   <li>Linear units are usually metres, but this is actually projection-dependent.</li>
     * </ul>
     *
     * @param  target       the target CRS.
     * @param  dimension    the dimension of each coordinate value. Must be in the [2-{@value #DIMENSION_MAX}] range.
     * @param  coordinates  the coordinates to transform, as a sequence of (<var>x</var>,<var>y</var>,&lt;<var>z</var>&gt;,…) tuples.
     * @param  offset       offset of the first coordinate in the given array.
     * @param  numPts       number of points to transform.
     * @throws NullPointerException if the {@code target} or {@code coordinates} argument is null.
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code numPts} arguments are invalid.
     * @throws TransformException if the operation failed for another reason (provided by Proj.4).
     *
     * @see org.opengis.referencing.operation.MathTransform#transform(double[], int, double[], int, int)
     */
    public native void transform(PJ target, int dimension, double[] coordinates, int offset, int numPts)
            throws TransformException;

    /**
     * Returns a description of the last error that occurred, or {@code null} if none.
     *
     * @return the last error that occurred, or {@code null}.
     *
     * @todo this method is not thread-safe. Proj.4 provides a better alternative using a context parameter.
     */
    native String getLastError();

    /**
     * Returns a hash code value for this {@literal Proj.4} object.
     */
    @Override
    public int hashCode() {
        return ~getCode().hashCode();
    }

    /**
     * Compares this identifier with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof PJ) && getCode().equals(((PJ) other).getCode());
    }

    /**
     * Returns the string representation of the PJ structure.
     *
     * @return the string representation.
     */
    @Override
    public String toString() {
        return IdentifiedObjects.toString(this);
    }

    /**
     * Deallocates the native PJ data structure.
     * It is okay if this method is invoked more than once.
     */
    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected final native void finalize();
}
