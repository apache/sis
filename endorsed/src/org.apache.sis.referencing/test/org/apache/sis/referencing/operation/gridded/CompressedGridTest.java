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
package org.apache.sis.referencing.operation.gridded;

import javax.measure.quantity.Dimensionless;
import org.opengis.referencing.operation.NoninvertibleTransformException;

// Test dependencies
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link CompressedGrid}. This class creates a grid using values computed by an affine transform,
 * and compare values computed by the grid using the affine transform as a reference.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CompressedGridTest extends LoadedGridTest {
    /**
     * Creates a new test case.
     */
    public CompressedGridTest() {
    }

    /**
     * Creates a new grid using an affine transform as a reference.
     *
     * @param  rotation  ignored.
     */
    @Override
    void init(final double rotation) throws NoninvertibleTransformException {
        super.init(0);      // No rotation in order to have integer values.
        grid = CompressedGrid.compress((LoadedGrid.Float<Dimensionless,Dimensionless>) grid, null, 0.5);
        assertInstanceOf("grid", CompressedGrid.class, grid);
    }
}
