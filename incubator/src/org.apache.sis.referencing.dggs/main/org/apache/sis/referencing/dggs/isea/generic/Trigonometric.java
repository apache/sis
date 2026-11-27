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
package org.apache.sis.referencing.dggs.isea.generic;

/**
 * Trigonometric functions with degree instead of radians
 *
 * @author Franz-Benjamin Mocnik
 */
public class Trigonometric {
    public static double sin(double x) {
        return Math.sin(Math.toRadians(x));
    }
    public static double cos(double x) {
        return Math.cos(Math.toRadians(x));
    }
    public static double tan(double x) {
        return Math.tan(Math.toRadians(x));
    }
    public static double cot(double x) {
        return 1 / Trigonometric.tan(x);
    }
    public static double asin(double x) {
        return Math.toDegrees(Math.asin(x));
    }
    public static double acos(double x) {
        return Math.toDegrees(Math.acos(x));
    }
    public static double atan(double x) {
        return Math.toDegrees(Math.atan(x));
    }
    public static double atan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }
}
