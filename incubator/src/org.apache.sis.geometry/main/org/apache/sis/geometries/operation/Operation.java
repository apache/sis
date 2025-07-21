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
package org.apache.sis.geometries.operation;

import org.apache.sis.geometries.Geometry;
import org.apache.sis.util.ArgumentChecks;


/**
 * A geometric operation which involve one or two geometries and produces one or many results.
 *
 * An operation class contains both the input and ouput results of the process.
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.3 Methods for testing spatial relations between geometric objects
 * @see OGC Simple Feature Access 1.2.1 - 6.1.2.4 Methods that support spatial analysis
 * @author Johann Sorel (Geomatys)
 */
public abstract class Operation<T extends Operation> {

    public final Geometry geometry;

    public Operation(Geometry geometry) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Evaluate operand using default geometry processors.
     * @return this
     */
    public T eval() {
        GeometryOperations.evaluate(this);
        return (T) this;
    }

    /**
     * An operand which involves two geometries.
     * @param <T>
     */
    public static abstract class Binary<T extends Operation.Binary> extends Operation<T> {

        public final Geometry other;

        public Binary(Geometry geometry, Geometry other) {
            super(geometry);
            this.other = other;
        }

        public Geometry getOtherGeometry() {
            return other;
        }
    }

    /**
     * An operand which involves two geometries and geometries order do not matter in the process.
     * @param <T>
     */
    public static abstract class ReversableBinary<T extends Operation.Binary> extends Binary<T> {

        public ReversableBinary(Geometry geometry, Geometry other) {
            super(geometry, other);
        }
    }
}
