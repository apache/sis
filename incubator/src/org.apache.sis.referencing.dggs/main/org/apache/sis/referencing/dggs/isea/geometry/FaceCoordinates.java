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
package org.apache.sis.referencing.dggs.isea.geometry;

/**
 * Cartesian Coordinates of a location on a face of a platonic solid.
 *
 * @author Franz-Benjamin Mocnik
 */
public class FaceCoordinates {
    private final Integer _face;
    private final Double _x;
    private final Double _y;

    public FaceCoordinates(Integer face, Double x, Double y) {
        this._face = face;
        this._x = x;
        this._y = y;
    }

    public Integer getFace() {
        return this._face;
    }

    public Double getX() {
        return this._x;
    }

    public Double getY() {
        return this._y;
    }

    public Double distanceTo(FaceCoordinates c) {
        return Math.sqrt(Math.pow(this._x - c.getX(), 2) + Math.pow(this._y - c.getY(), 2));
    }
    @Override
    public String toString() {
        return String.format("face %d x %f y %f", this._face, this._x, this._y);
    }
}
