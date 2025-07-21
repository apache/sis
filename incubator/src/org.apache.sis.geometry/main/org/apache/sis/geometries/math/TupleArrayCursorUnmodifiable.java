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
package org.apache.sis.geometries.math;

import org.apache.sis.util.ArgumentChecks;


/**
 * Unmodifiable tuple array cursor.
 *
 * @author Johann Sorel (Geomatys)
 */
final class TupleArrayCursorUnmodifiable implements TupleArrayCursor {

    private final TupleArrayCursor parent;
    private Tuple previous;
    private Tuple t;

    public TupleArrayCursorUnmodifiable(TupleArrayCursor parent) {
        ArgumentChecks.ensureNonNull("parent", parent);
        this.parent = parent;
    }

    @Override
    public Tuple samples() {
        Tuple cdt = parent.samples();
        if (t == null || previous != cdt) {
            t = new TupleUnmodifiable(cdt);
            previous = cdt;
        }
        return t;
    }

    @Override
    public int coordinate() {
        return parent.coordinate();
    }

    @Override
    public void moveTo(int coordinate) {
        parent.moveTo(coordinate);
    }

    @Override
    public boolean next() {
        return parent.next();
    }

}
