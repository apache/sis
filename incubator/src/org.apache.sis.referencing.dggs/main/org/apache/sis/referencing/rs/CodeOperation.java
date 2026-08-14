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
package org.apache.sis.referencing.rs;

import java.util.Collection;
import java.util.Collections;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;


/**
 * An operation on codes that transforms or converts code to another <abbr>RS</abbr>.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface CodeOperation extends IdentifiedObject {

    /**
     * Key for the <code>{@value}</code> property.
     * This is used for setting the value to be returned by {@link #getCoordinateOperationAccuracy()}.
     *
     * @see #getCoordinateOperationAccuracy()
     */
    String COORDINATE_OPERATION_ACCURACY_KEY = "coordinateOperationAccuracy";

    /**
     * Returns the <abbr>RS</abbr> from which codes are changed.
     *
     * @return the <abbr>RS</abbr> from which codes are changed.
     */
    ReferenceSystem getSourceRS();

    /**
     * Returns the <abbr>RS</abbr> to which codes are changed.
     *
     * @return the <abbr>RS</abbr> to which codes are changed.
     *
     * @see #getTargetEpoch()
     */
    ReferenceSystem getTargetRS();

    /**
     * Returns estimate(s) of the impact of this operation on point accuracy.
     * It gives position error estimates for target coordinates of this coordinate operation,
     * assuming no errors in source coordinates.
     *
     * @return the position error estimates, or an empty collection if not available.
     */
    default Collection<PositionalAccuracy> getCoordinateOperationAccuracy() {
        return Collections.emptyList();
    }

    /**
     * Get or create inverse transform.
     *
     * @return inverse code transform
     * @throws NoninvertibleTransformException if reverse transformation is not possible
     */
    CodeOperation inverse() throws NoninvertibleTransformException;

    /**
     * Transform a single code.
     *
     * @param source to transform, never null
     * @param target can be null
     * @return target code if provided, a new one if null.
     * @throws TransformException
     */
    default Code transform(Code source, Code target) throws TransformException {
        final Code[] sarray = new Code[]{source};
        final Code[] tarray = new Code[]{target};
        transform(sarray, 0, tarray, 0, 1);
        return tarray[0];
    }

    /**
     * Transform multiple codes at once.
     *
     * @param source to read from
     * @param soffset source array offset
     * @param target to write into
     * @param toffset target array offset
     * @param nb number of codes to transform
     */
    void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException;

}
