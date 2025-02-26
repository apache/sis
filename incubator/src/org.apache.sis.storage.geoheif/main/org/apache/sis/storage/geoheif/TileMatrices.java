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
package org.apache.sis.storage.geoheif;

import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
class TileMatrices {

    private TileMatrices(){}


    /**
     * Create a stream of point in the GridExtent.
     *
     * TODO : make a more efficient implementation.
     */
    public static Stream<long[]> pointStream(GridExtent extent) {
        final int dimension = extent.getDimension();
        final long[] low = new long[dimension];
        final long[] high = new long[dimension];
        for (int i=0; i<dimension; i++) {
            low[i] = extent.getLow(i);
            high[i] = extent.getHigh(i);
        }

        Stream<long[]> stream = LongStream.range(low[0], high[0]+1)
                .mapToObj((long value) -> {
                    final long[] array = new long[dimension];
                    array[0] = value;
                    return array;
        });
        for (int i = 1; i <dimension; i++) {
            final int idx = i;
            stream = stream.flatMap((long[] t) -> LongStream.range(low[idx], high[idx]+1)
                    .mapToObj((long value) -> {
                        final long[] array = t.clone();
                        array[idx] = value;
                        return array;
                    }));
        }
        return stream;
    }

}
