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
package org.apache.sis.geometries;

import java.util.List;
import javax.measure.quantity.Length;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometries.math.Vector;


/**
 * A Curve is a 1-dimensional geometric object usually stored as a sequence of Points, with the subtype of Curve
 * specifying the form of the interpolation between Points.
 * This standard defines only one subclass of Curve, LineString, which uses linear interpolation between Points.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Curve", specification=ISO_19107) // section 6.4.18
public interface Curve extends Orientable {

    /**
     * The length of this Curve in its associated spatial reference.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @return length of the curve.
     */
    @UML(identifier="length", specification=ISO_19107) // section 6.4.18.9
    default double getLength() {
        throw new UnsupportedOperationException();
    }

    /**
     * The start Point of this Curve.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @return start Point of this Curve.
     */
    @UML(identifier="startPoint", specification=ISO_19107) // section 6.4.18.6
    default Point getStartPoint() {
        throw new UnsupportedOperationException();
    }

    /**
     * The end Point of this Curve.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @return end Point of this Curve.
     */
    @UML(identifier="endPoint", specification=ISO_19107) // section 6.4.18.7
    default Point getEndPoint() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this Curve is closed [StartPoint() = EndPoint()].
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @return true if curve is closed.
     */
    default boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this Curve is closed [StartPoint () = EndPoint ()]
     * and this Curve is simple (does not pass through the same Point more than once).
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     * @return true if curve is a ring.
     */
    @UML(identifier="isRing", specification=ISO_19107) // section 6.4.18.8
    default boolean isRing() {
        throw new UnsupportedOperationException();
    }

    @UML(identifier="controlPoint", specification=ISO_19107) // section 6.4.18.2
    default List<DirectPosition> getControlPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="dataPoint", specification=ISO_19107) // section 6.4.18.3
    default List<DirectPosition> getDataPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="knot", specification=ISO_19107) // section 6.4.18.4
    default List<Knot> getKnots() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="interpolation", specification=ISO_19107) // section 6.4.18.5
    default CurveInterpolation getInterpolation() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="startConstrParam,", specification=ISO_19107) // section 6.4.18.15
    default double getStartConstrParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="endConstrParam", specification=ISO_19107) // section 6.4.18.15
    default double getEndConstrParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="startParam,", specification=ISO_19107) // section 6.4.18.13
    default Length getStartParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="endParam", specification=ISO_19107) // section 6.4.18.13
    default Length getEndParam() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="numDerivativesInterior", specification=ISO_19107) // section 6.4.18.10
    default Integer getNumDerivativeInterior() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="numDerivativesStart", specification=ISO_19107) // section 6.4.18.11
    default Integer getNumDerivativesStart() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="numDerivativesEnd", specification=ISO_19107) // section 6.4.18.12
    default Integer getNumDerivativesEnd() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="reverse", specification=ISO_19107) // section 6.4.18.14
    @Override
    default Curve getReverse() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="asLine", specification=ISO_19107) // section 6.4.18.17
    default LineString asLine(Length spacing, Length offset) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="constrParam", specification=ISO_19107) // section 6.4.18.18
    default DirectPosition constrParam(double cp) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="length", specification=ISO_19107) // section 6.4.18.19
    default Length getLength(DirectPosition point1, DirectPosition point2) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="length", specification=ISO_19107) // section 6.4.18.19
    default Length getLength(double cparam1, double cparam2) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="param", specification=ISO_19107) // section 6.4.18.20
    default DirectPosition param(Length s) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="paramForPoint", specification=ISO_19107) // section 6.4.18.21
    default List<Length> paramForPoint(List<DirectPosition> p) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="tangent", specification=ISO_19107) // section 6.4.18.22
    default Vector tangent(Length s) {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="tangent", specification=ISO_19107) // section 6.4.18.22
    default Vector tangent(double knotParameter) {
        //TODO
        throw new UnsupportedOperationException();
    }
}
