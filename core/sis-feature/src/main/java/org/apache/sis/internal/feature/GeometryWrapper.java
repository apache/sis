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
package org.apache.sis.internal.feature;

import java.util.Set;
import java.util.Objects;
import org.opengis.geometry.Boundary;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.Precision;
import org.opengis.geometry.TransfiniteSet;
import org.opengis.geometry.complex.Complex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * Wraps a JTS or ESRI geometry behind a {@code Geometry} interface.
 * This is a temporary class to be refactored later as a more complete geometry framework.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class GeometryWrapper implements Geometry {
    /**
     * The JTS or ESRI geometric object.
     */
    public final Object geometry;

    /**
     * Geometry bounding box, together with its coordinate reference system.
     */
    private final Envelope envelope;

    /**
     * Creates a new geometry object.
     *
     * @param  geometry  the JTS or ESRI geometric object.
     * @param  envelope  geometry bounding box, together with its coordinate reference system.
     */
    public GeometryWrapper(final Object geometry, final Envelope envelope) {
        this.geometry = geometry;
        this.envelope = envelope;
    }

    /**
     * Returns the geometry CRS, which is taken from the envelope CRS.
     *
     * @return the geometry CRS.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return envelope.getCoordinateReferenceSystem();
    }

    /**
     * Returns the envelope specified at construction time.
     */
    @Override public Envelope getEnvelope() {
        return envelope;
    }

    @Override public Precision      getPrecision()                            {throw new UnsupportedOperationException();}
    @Override public Geometry       getMbRegion()                             {throw new UnsupportedOperationException();}
    @Override public DirectPosition getRepresentativePoint()                  {throw new UnsupportedOperationException();}
    @Override public Boundary       getBoundary()                             {throw new UnsupportedOperationException();}
    @Override public Complex        getClosure()                              {throw new UnsupportedOperationException();}
    @Override public boolean        isSimple()                                {throw new UnsupportedOperationException();}
    @Override public boolean        isCycle()                                 {throw new UnsupportedOperationException();}
    @Override public double         distance(Geometry geometry)               {throw new UnsupportedOperationException();}
    @Override public int            getDimension(DirectPosition point)        {throw new UnsupportedOperationException();}
    @Override public int            getCoordinateDimension()                  {throw new UnsupportedOperationException();}
    @Override public Set<Complex>   getMaximalComplex()                       {throw new UnsupportedOperationException();}
    @Override public DirectPosition getCentroid()                             {throw new UnsupportedOperationException();}
    @Override public Geometry       getConvexHull()                           {throw new UnsupportedOperationException();}
    @Override public Geometry       getBuffer(double distance)                {throw new UnsupportedOperationException();}
    @Override public boolean        isMutable()                               {throw new UnsupportedOperationException();}
    @Override public Geometry       toImmutable()                             {throw new UnsupportedOperationException();}
    @Override public Geometry       clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}
    @Override public boolean        contains(TransfiniteSet pointSet)         {throw new UnsupportedOperationException();}
    @Override public boolean        contains(DirectPosition point)            {throw new UnsupportedOperationException();}
    @Override public boolean        intersects(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Override public boolean        equals(TransfiniteSet pointSet)           {throw new UnsupportedOperationException();}
    @Override public TransfiniteSet union(TransfiniteSet pointSet)            {throw new UnsupportedOperationException();}
    @Override public TransfiniteSet intersection(TransfiniteSet pointSet)     {throw new UnsupportedOperationException();}
    @Override public TransfiniteSet difference(TransfiniteSet pointSet)       {throw new UnsupportedOperationException();}
    @Override public TransfiniteSet symmetricDifference(TransfiniteSet ps)    {throw new UnsupportedOperationException();}
    @Override public Geometry       transform(CoordinateReferenceSystem crs)  {throw new UnsupportedOperationException();}
    @Override public Geometry       transform(CoordinateReferenceSystem crs, MathTransform tr) {throw new UnsupportedOperationException();}

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof GeometryWrapper) && Objects.equals(((GeometryWrapper) obj).geometry, geometry);
    }

    @Override
    public int hashCode() {
        return ~Objects.hashCode(geometry);
    }

    @Override
    public String toString() {
        return String.valueOf(geometry);
    }
}
