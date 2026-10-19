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
package org.apache.sis.geometries.internal.shared.topology;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.sis.geometries.Orientable;
import org.apache.sis.geometries.topology.Complex;
import org.apache.sis.geometries.topology.DirectedTopo;
import org.apache.sis.geometries.topology.Expression;
import org.apache.sis.geometries.topology.Primitive;


/**
 * Base class of the views of the primitives of an {@link IndexedComplex}, in either orientation.
 * A view is only an index in a complex, created on demand.
 *
 * @author Johann Sorel (Geomatys)
 */
abstract class IndexedTopo implements DirectedTopo {

    /**
     * The complex containing the primitive.
     */
    final IndexedComplex complex;

    /**
     * Index of the primitive in the complex.
     */
    final int index;

    /**
     * Creates a view of the primitive at the given index.
     */
    IndexedTopo(final IndexedComplex complex, final int index) {
        this.complex = complex;
        this.index   = index;
    }

    /**
     * Returns the name of the primitive type, for {@link #toString()}.
     */
    abstract String typeName();

    /**
     * Returns {@code true} if this view is the negative orientation of its primitive.
     */
    final boolean isNegative() {
        return getOrientationSign() == Orientable.Sign.NEGATIVE;
    }

    /**
     * Returns {@code false}: a primitive of an indexed complex is never empty.
     */
    @Override
    public final boolean isEmpty() {
        return false;
    }

    /**
     * Returns the primitive alone, whatever the orientation of this view.
     */
    @Override
    public final Set<Primitive> getInterior() {
        return Set.of(getTopo());
    }

    /**
     * Returns the primitives of the complex which are not in the closure of this primitive.
     */
    @Override
    public final Set<Primitive> getExterior() {
        final Set<Primitive> closure = getClosure();
        final Set<Primitive> exterior = new LinkedHashSet<>();
        for (final Primitive element : complex.getElements()) {
            if (!closure.contains(element)) {
                exterior.add(element);
            }
        }
        return exterior;
    }

    @Override
    public final Complex getMaximalComplex() {
        return complex;
    }

    @Override
    public final Set<Complex> getComplexes() {
        return Set.of(complex);
    }

    /**
     * Returns the nodes isolated in a face, or the nodes and edges isolated in a solid.
     */
    @Override
    public final Set<Primitive> getIsolated() {
        return complex.isolated(getDimension(), index);
    }

    /**
     * Returns the face or solid in which a node is isolated, or the solid in which an edge is isolated.
     */
    @Override
    public final Primitive getContainer() {
        return complex.container(getDimension(), index);
    }

    @Override
    public final Expression getExpression() {
        return DefaultExpression.of(this);
    }

    /**
     * Returns {@code true} if the given object is a view of the same type and orientation
     * on the same primitive of the same complex.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final IndexedTopo other = (IndexedTopo) obj;
            return other.complex == complex && other.index == index;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return (31 * index + getClass().hashCode()) ^ System.identityHashCode(complex);
    }

    @Override
    public final String toString() {
        return (isNegative() ? "-" : "") + typeName() + '[' + index + ']';
    }
}
