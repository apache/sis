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

import java.util.List;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.math.Vector2D;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SutherlandHogmanTest {

    @Test
    public void testRectangleClip(){

        final TupleArray subject = TupleArrays.of(2,
                0.0, 0.0,
                10.0, 0.0,
                10.0, 20.0,
                0.0, 20.0,
                0.0, 0.0);

        final TupleArray clip = TupleArrays.of(2,
                5.0, 8.0,
                15.0, 8.0,
                15.0, 24.0,
                5.0, 24.0,
                5.0, 8.0
            );

        final List<Tuple> result = SutherlandHodgman.clip(TupleArrays.asList(subject), TupleArrays.asList(clip));
        assertEquals(4,result.size());
        assertEquals(new Vector2D.Double( 5,  8),result.get(0));
        assertEquals(new Vector2D.Double(10,  8),result.get(1));
        assertEquals(new Vector2D.Double(10, 20),result.get(2));
        assertEquals(new Vector2D.Double( 5, 20),result.get(3));
    }
}
