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
package org.apache.sis.internal.filter.sqlmm;

import java.util.Arrays;
import java.util.Collection;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.feature.FunctionRegister;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.ArgumentChecks;


/**
 * A register of functions defined by the SQL/MM standard.
 * This standard is defined by <a href="https://www.iso.org/standard/60343.html">ISO/IEC 13249-3:2016
 * Information technology — Database languages — SQL multimedia and application packages — Part 3: Spatial</a>.
 *
 * @todo Implement all SQL/MM specification functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   1.1
 * @module
 */
public final class SQLMM implements FunctionRegister {

    /**
     * Creates the default register.
     */
    public SQLMM() {
    }

    /**
     * Returns a unique name for this factory.
     */
    @Override
    public String getIdentifier() {
        return "SQL/MM";
    }

    /**
     * Returns the names of all functions known to this register.
     */
    @Override
    public Collection<String> getNames() {
        return Arrays.asList(
                ST_Area.NAME,
                ST_AsBinary.NAME,
                ST_AsGML.NAME,
                ST_AsText.NAME,
                ST_Boundary.NAME,
                ST_Buffer.NAME,
                ST_Centroid.NAME,
                ST_Contains.NAME,
                ST_ConvexHull.NAME,
                ST_CoordDim.NAME,
                ST_Crosses.NAME,
                ST_Difference.NAME,
                ST_Dimension.NAME,
                ST_Disjoint.NAME,
                ST_Distance.NAME,
                ST_EndPoint.NAME,
                ST_Envelope.NAME,
                ST_Equals.NAME,
                ST_ExplicitPoint.NAME,
                ST_ExteriorRing.NAME,
                ST_GeomCollection.NAME,
                ST_GeometryN.NAME,
                ST_GeometryType.NAME,
                ST_InteriorRingN.NAME,
                ST_Intersection.NAME,
                ST_Intersects.NAME,
                ST_Is3D.NAME,
                ST_IsClosed.NAME,
                ST_IsEmpty.NAME,
                ST_IsRing.NAME,
                ST_IsSimple.NAME,
                ST_IsValid.NAME,
                ST_Length.NAME,
                ST_LineString.NAME,
                ST_MultiLineString.NAME,
                ST_MultiPoint.NAME,
                ST_MultiPolygon.NAME,
                ST_NumGeometries.NAME,
                ST_NumInteriorRings.NAME,
                ST_NumPoints.NAME,
                ST_Overlaps.NAME,
                ST_Perimeter.NAME,
                ST_Point.NAME,
                ST_PointN.NAME,
                ST_PointOnSurface.NAME,
                ST_Polygon.NAME,
                ST_Relate.NAME,
                ST_Simplify.NAME,
                ST_SimplifyPreserveTopology.NAME,
                ST_SRID.NAME,
                ST_StartPoint.NAME,
                ST_SymDifference.NAME,
                ST_ToGeomColl.NAME,
                ST_ToLineString.NAME,
                ST_ToMultiLine.NAME,
                ST_ToMultiPoint.NAME,
                ST_ToMultiPolygon.NAME,
                ST_ToPoint.NAME,
                ST_ToPolygon.NAME,
                ST_Touches.NAME,
                ST_Transform.NAME,
                ST_Union.NAME,
                ST_Within.NAME,
                ST_X.NAME,
                ST_XFromBinary.BdMPoly.NAME,
                ST_XFromBinary.BdPoly.NAME,
                ST_XFromBinary.GeomColl.NAME,
                ST_XFromBinary.Geom.NAME,
                ST_XFromBinary.Line.NAME,
                ST_XFromBinary.MLine.NAME,
                ST_XFromBinary.MPoint.NAME,
                ST_XFromBinary.MPoly.NAME,
                ST_XFromBinary.Point.NAME,
                ST_XFromBinary.Poly.NAME,
                ST_XFromGML.GeomColl.NAME,
                ST_XFromGML.Geom.NAME,
                ST_XFromGML.Line.NAME,
                ST_XFromGML.MLine.NAME,
                ST_XFromGML.MPoint.NAME,
                ST_XFromGML.MPoly.NAME,
                ST_XFromGML.Point.NAME,
                ST_XFromGML.Poly.NAME,
                ST_XFromText.BdMPoly.NAME,
                ST_XFromText.BdPoly.NAME,
                ST_XFromText.GeomColl.NAME,
                ST_XFromText.Geom.NAME,
                ST_XFromText.Line.NAME,
                ST_XFromText.MLine.NAME,
                ST_XFromText.MPoint.NAME,
                ST_XFromText.MPoly.NAME,
                ST_XFromText.Point.NAME,
                ST_XFromText.Poly.NAME,
                ST_Y.NAME,
                ST_Z.NAME
        );
    }

    /**
     * Create a new function of the given name with given parameters.
     *
     * @param  name        name of the function to create.
     * @param  parameters  function parameters.
     * @return function for the given name and parameters.
     * @throws IllegalArgumentException if function name is unknown or some parameters are illegal.
     */
    @Override
    public Function create(final String name, Expression[] parameters) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters[i]);
        }
        try {
            switch (name) {
                case ST_Area.NAME:                      return new ST_Area(parameters);
                case ST_AsBinary.NAME:                  return new ST_AsBinary(parameters);
                case ST_AsGML.NAME:                     return new ST_AsGML(parameters);
                case ST_AsText.NAME:                    return new ST_AsText(parameters);
                case ST_Boundary.NAME:                  return new ST_Boundary(parameters);
                case ST_Buffer.NAME:                    return new ST_Buffer(parameters);
                case ST_Centroid.NAME:                  return new ST_Centroid(parameters);
                case ST_Contains.NAME:                  return new ST_Contains(parameters);
                case ST_ConvexHull.NAME:                return new ST_ConvexHull(parameters);
                case ST_CoordDim.NAME:                  return new ST_CoordDim(parameters);
                case ST_Crosses.NAME:                   return new ST_Crosses(parameters);
                case ST_Difference.NAME:                return new ST_Difference(parameters);
                case ST_Dimension.NAME:                 return new ST_Dimension(parameters);
                case ST_Disjoint.NAME:                  return new ST_Disjoint(parameters);
                case ST_Distance.NAME:                  return new ST_Distance(parameters);
                case ST_EndPoint.NAME:                  return new ST_EndPoint(parameters);
                case ST_Envelope.NAME:                  return new ST_Envelope(parameters);
                case ST_Equals.NAME:                    return new ST_Equals(parameters);
                case ST_ExplicitPoint.NAME:             return new ST_ExplicitPoint(parameters);
                case ST_ExteriorRing.NAME:              return new ST_ExteriorRing(parameters);
                case ST_GeomCollection.NAME:            return new ST_GeomCollection(parameters);
                case ST_GeometryN.NAME:                 return new ST_GeometryN(parameters);
                case ST_GeometryType.NAME:              return new ST_GeometryType(parameters);
                case ST_InteriorRingN.NAME:             return new ST_InteriorRingN(parameters);
                case ST_Intersection.NAME:              return new ST_Intersection(parameters);
                case ST_Intersects.NAME:                return new ST_Intersects(parameters);
                case ST_Is3D.NAME:                      return new ST_Is3D(parameters);
                case ST_IsClosed.NAME:                  return new ST_IsClosed(parameters);
                case ST_IsEmpty.NAME:                   return new ST_IsEmpty(parameters);
                case ST_IsRing.NAME:                    return new ST_IsRing(parameters);
                case ST_IsSimple.NAME:                  return new ST_IsSimple(parameters);
                case ST_IsValid.NAME:                   return new ST_IsValid(parameters);
                case ST_Length.NAME:                    return new ST_Length(parameters);
                case ST_LineString.NAME:                return new ST_LineString(parameters);
                case ST_MultiLineString.NAME:           return new ST_MultiLineString(parameters);
                case ST_MultiPoint.NAME:                return new ST_MultiPoint(parameters);
                case ST_MultiPolygon.NAME:              return new ST_MultiPolygon(parameters);
                case ST_NumGeometries.NAME:             return new ST_NumGeometries(parameters);
                case ST_NumInteriorRings.NAME:          return new ST_NumInteriorRings(parameters);
                case ST_NumPoints.NAME:                 return new ST_NumPoints(parameters);
                case ST_Overlaps.NAME:                  return new ST_Overlaps(parameters);
                case ST_Perimeter.NAME:                 return new ST_Perimeter(parameters);
                case ST_Point.NAME:                     return new ST_Point(parameters);
                case ST_PointN.NAME:                    return new ST_PointN(parameters);
                case ST_PointOnSurface.NAME:            return new ST_PointOnSurface(parameters);
                case ST_Polygon.NAME:                   return new ST_Polygon(parameters);
                case ST_Relate.NAME:                    return new ST_Relate(parameters);
                case ST_Simplify.NAME:                  return new ST_Simplify(parameters);
                case ST_SimplifyPreserveTopology.NAME:  return new ST_SimplifyPreserveTopology(parameters);
                case ST_SRID.NAME:                      return new ST_SRID(parameters);
                case ST_StartPoint.NAME:                return new ST_StartPoint(parameters);
                case ST_SymDifference.NAME:             return new ST_SymDifference(parameters);
                case ST_ToGeomColl.NAME:                return new ST_ToGeomColl(parameters);
                case ST_ToLineString.NAME:              return new ST_ToLineString(parameters);
                case ST_ToMultiLine.NAME:               return new ST_ToMultiLine(parameters);
                case ST_ToMultiPoint.NAME:              return new ST_ToMultiPoint(parameters);
                case ST_ToMultiPolygon.NAME:            return new ST_ToMultiPolygon(parameters);
                case ST_ToPoint.NAME:                   return new ST_ToPoint(parameters);
                case ST_ToPolygon.NAME:                 return new ST_ToPolygon(parameters);
                case ST_Touches.NAME:                   return new ST_Touches(parameters);
                case ST_Transform.NAME:                 return new ST_Transform(parameters);
                case ST_Union.NAME:                     return new ST_Union(parameters);
                case ST_Within.NAME:                    return new ST_Within(parameters);
                case ST_X.NAME:                         return new ST_X(parameters);
                case ST_XFromBinary.BdMPoly.NAME:       return new ST_XFromBinary.BdMPoly(parameters);
                case ST_XFromBinary.BdPoly.NAME:        return new ST_XFromBinary.BdPoly(parameters);
                case ST_XFromBinary.GeomColl.NAME:      return new ST_XFromBinary.GeomColl(parameters);
                case ST_XFromBinary.Geom.NAME:          return new ST_XFromBinary.Geom(parameters);
                case ST_XFromBinary.Line.NAME:          return new ST_XFromBinary.Line(parameters);
                case ST_XFromBinary.MLine.NAME:         return new ST_XFromBinary.MLine(parameters);
                case ST_XFromBinary.MPoint.NAME:        return new ST_XFromBinary.MPoint(parameters);
                case ST_XFromBinary.MPoly.NAME:         return new ST_XFromBinary.MPoly(parameters);
                case ST_XFromBinary.Point.NAME:         return new ST_XFromBinary.Point(parameters);
                case ST_XFromBinary.Poly.NAME:          return new ST_XFromBinary.Poly(parameters);
                case ST_XFromGML.GeomColl.NAME:         return new ST_XFromGML.GeomColl(parameters);
                case ST_XFromGML.Geom.NAME:             return new ST_XFromGML.Geom(parameters);
                case ST_XFromGML.Line.NAME:             return new ST_XFromGML.Line(parameters);
                case ST_XFromGML.MLine.NAME:            return new ST_XFromGML.MLine(parameters);
                case ST_XFromGML.MPoint.NAME:           return new ST_XFromGML.MPoint(parameters);
                case ST_XFromGML.MPoly.NAME:            return new ST_XFromGML.MPoly(parameters);
                case ST_XFromGML.Point.NAME:            return new ST_XFromGML.Point(parameters);
                case ST_XFromGML.Poly.NAME:             return new ST_XFromGML.Poly(parameters);
                case ST_XFromText.BdMPoly.NAME:         return new ST_XFromText.BdMPoly(parameters);
                case ST_XFromText.BdPoly.NAME:          return new ST_XFromText.BdPoly(parameters);
                case ST_XFromText.GeomColl.NAME:        return new ST_XFromText.GeomColl(parameters);
                case ST_XFromText.Geom.NAME:            return new ST_XFromText.Geom(parameters);
                case ST_XFromText.Line.NAME:            return new ST_XFromText.Line(parameters);
                case ST_XFromText.MLine.NAME:           return new ST_XFromText.MLine(parameters);
                case ST_XFromText.MPoint.NAME:          return new ST_XFromText.MPoint(parameters);
                case ST_XFromText.MPoly.NAME:           return new ST_XFromText.MPoly(parameters);
                case ST_XFromText.Point.NAME:           return new ST_XFromText.Point(parameters);
                case ST_XFromText.Poly.NAME:            return new ST_XFromText.Poly(parameters);
                case ST_Y.NAME:                         return new ST_Y(parameters);
                case ST_Z.NAME:                         return new ST_Z(parameters);
                default: throw new IllegalArgumentException(Resources.format(Resources.Keys.UnknownFunction_1, name));
            }
        } catch (FactoryException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
