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
package org.apache.sis.referencing.operation;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.coordinate.DefaultCoordinateMetadata;
import org.apache.sis.coordinate.AbstractCoordinateSet;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coordinate.CoordinateSet;
import org.opengis.coordinate.CoordinateMetadata;


/**
 * The result of transforming coordinate tuples using the math transform of a given coordinate operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @todo The current implementation is inefficient.
 */
final class TransformedCoordinateSet extends AbstractCoordinateSet implements UnaryOperator<DirectPosition> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6533100977777070895L;

    /**
     * The data to transform.
     */
    @SuppressWarnings("serial")     // Apache SIS implementations of this interface are serializable.
    private final CoordinateSet data;

    /**
     * The transform to apply on coordinate tuples.
     *
     * @see #iterator()
     * @see #stream()
     */
    @SuppressWarnings("serial")     // Apache SIS implementations of this interface are serializable.
    private final MathTransform transform;

    /**
     * Creates a new transformed coordinate set.
     *
     * @param  op    the coordinate operation to apply.
     * @param  data  the coordinate tuples to transform.
     * @throws TransformException if the transform cannot be prepared.
     */
    TransformedCoordinateSet(final AbstractCoordinateOperation op, final CoordinateSet data) throws TransformException {
        super(new DefaultCoordinateMetadata(op.getTargetCRS(), op.getTargetEpoch().orElse(null)));
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        MathTransform transform = op.getMathTransform();
        if (transform == null) {
            throw new TransformException(Resources.format(Resources.Keys.OperationHasNoTransform_2, op.getClass(), op.getName()));
        }
        final CoordinateMetadata metadata = data.getCoordinateMetadata();
        if (metadata != null) try {
            final var context = CoordinateOperationContext.fromBoundingBox(CRS.getGeographicBoundingBox(op));
            final var step = new DefaultCoordinateMetadata(op.getSourceCRS(), op.getSourceEpoch().orElse(null));
            transform = MathTransforms.concatenate(CRS.findOperation(metadata, step, context).getMathTransform(), transform);
        } catch (FactoryException | MismatchedDimensionException e) {
            throw new TransformException(e.getMessage(), e);
        }
        this.transform = transform;
        this.data = data;
    }

    /**
     * Returns the number of dimension of output coordinate tuples.
     * This method is overridden in case that {@link #crs} is null.
     */
    @Override
    public int getDimension() {
        return transform.getTargetDimensions();
    }

    /**
     * Returns the transformed positions described by coordinate tuples.
     */
    @Override
    public Iterator<DirectPosition> iterator() {
        return stream().iterator();
    }

    /**
     * Returns the transformed positions described by coordinate tuples.
     */
    @Override
    public Spliterator<DirectPosition> spliterator() {
        return stream().spliterator();
    }

    /**
     * Returns a stream of transformed coordinate tuples.
     */
    @Override
    public Stream<DirectPosition> stream() {
        return data.stream().map(this);
    }

    /**
     * Performs an action for each coordinate tuple of this stream.
     *
     * @param action the action to perform.
     */
    @Override
    public void forEach(Consumer<? super DirectPosition> action) {
        stream().forEach(action);
    }

    /**
     * Transforms the given coordinate tuples.
     *
     * @param  source  coordinates in source CRS.
     * @return coordinates in target CRS.
     */
    @Override
    public DirectPosition apply(final DirectPosition source) {
        try {
            return transform.transform(source, null);
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }
    }
}
