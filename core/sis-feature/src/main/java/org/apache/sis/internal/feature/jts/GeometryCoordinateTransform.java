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
package org.apache.sis.internal.feature.jts;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * This implementation use a MathTransform to transform each coordinate of
 * the geometry.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GeometryCoordinateTransform extends GeometryTransform {

    private final MathTransform transform;

    public GeometryCoordinateTransform(MathTransform transform) {
        this.transform = transform;
    }

    public GeometryCoordinateTransform(MathTransform transform, final CoordinateSequenceFactory csf) {
        super(csf);
        this.transform = transform;
    }

    public GeometryCoordinateTransform(MathTransform transform, final GeometryFactory gf) {
        super(gf);
        this.transform = transform;
    }

    @Override
    protected CoordinateSequence transform(CoordinateSequence in, int minpoints) throws TransformException {
        final int dim = in.getDimension();
        final int size = in.size();
        final CoordinateSequence out = csf.create(size, dim);

        final double[] val = new double[dim];
        for (int i = 0; i<size; i++) {
            switch (dim) {
                case 3 :
                    val[0] = in.getOrdinate(i, 0);
                    val[1] = in.getOrdinate(i, 1);
                    val[2] = in.getOrdinate(i, 2);
                    transform.transform(val, 0, val, 0, 1);
                    out.setOrdinate(i, 0, val[0]);
                    out.setOrdinate(i, 1, val[1]);
                    out.setOrdinate(i, 2, val[2]);
                    break;
                default :
                    val[0] = in.getOrdinate(i, 0);
                    val[1] = in.getOrdinate(i, 1);
                    transform.transform(val, 0, val, 0, 1);
                    out.setOrdinate(i, 0, val[0]);
                    out.setOrdinate(i, 1, val[1]);
                    break;
            }
        }
        return out;
    }

}
