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


/**
 * A cursor allows to iterate over a tuple array without copying datas.
 * Any change made to the cursor samples are made in the tuple array.
 *
 * API copied from Unlicense.science
 */
public interface TupleArrayCursor {

    Tuple samples();

    /**
     * Get the current tuple coordinate.
     * @return Tuple, returns always the same container
     */
    int coordinate();

    /**
     * Move cursor to given coordinate.
     *
     * @param coordinate
     */
    void moveTo(int coordinate);

    /**
     * Move to next tuple.
     *
     * @return false if no more tuple
     */
    boolean next();

}
