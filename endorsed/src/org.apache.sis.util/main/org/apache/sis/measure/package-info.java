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

/**
 * Units of measurements, values related to measurement (like angles and ranges) and their formatters.
 * A key class in this package is {@link org.apache.sis.measure.Units}, which provides static constants
 * for about 50 units of measurement including all the SI base units
 * ({@linkplain org.apache.sis.measure.Units#METRE    metre},
 *  {@linkplain org.apache.sis.measure.Units#KILOGRAM kilogram}
 *  {@linkplain org.apache.sis.measure.Units#SECOND   second},
 *  {@linkplain org.apache.sis.measure.Units#AMPERE   ampere},
 *  {@linkplain org.apache.sis.measure.Units#KELVIN   kelvin},
 *  {@linkplain org.apache.sis.measure.Units#MOLE     mole} and
 *  {@linkplain org.apache.sis.measure.Units#CANDELA  candela})
 * together with some derived units
 * ({@linkplain org.apache.sis.measure.Units#SQUARE_METRE      square metre},
 *  {@linkplain org.apache.sis.measure.Units#CUBIC_METRE       cubic metre},
 *  {@linkplain org.apache.sis.measure.Units#METRES_PER_SECOND metres per second},
 *  {@linkplain org.apache.sis.measure.Units#HERTZ             hertz},
 *  {@linkplain org.apache.sis.measure.Units#PASCAL            pascal},
 *  {@linkplain org.apache.sis.measure.Units#NEWTON            newton},
 *  {@linkplain org.apache.sis.measure.Units#JOULE             joule},
 *  {@linkplain org.apache.sis.measure.Units#WATT              watt},
 *  {@linkplain org.apache.sis.measure.Units#TESLA             tesla},
 *  <i>etc.</i>)
 * and some dimensionless units
 * ({@linkplain org.apache.sis.measure.Units#RADIAN    radian},
 *  {@linkplain org.apache.sis.measure.Units#STERADIAN steradian},
 *  {@linkplain org.apache.sis.measure.Units#PIXEL     pixel},
 *  {@linkplain org.apache.sis.measure.Units#UNITY     unity}).
 *
 * In relation to units of measurement, this package also defines:
 *
 * <ul>
 *   <li>{@linkplain org.apache.sis.measure.Quantities}
 *       as a {@code double} value value associated to a {@code Unit} instance.</li>
 *   <li>{@link org.apache.sis.measure.Angle} and its subclasses
 *      ({@link org.apache.sis.measure.Longitude},
 *       {@link org.apache.sis.measure.Latitude},
 *       {@link org.apache.sis.measure.ElevationAngle})</li>
 *   <li>{@link org.apache.sis.measure.Range} and its subclasses
 *      ({@link org.apache.sis.measure.NumberRange},
 *       {@link org.apache.sis.measure.MeasurementRange}) or annotation
 *      ({@link org.apache.sis.measure.ValueRange})</li>
 *   <li>Parsers and formatters
 *      ({@link org.apache.sis.measure.AngleFormat},
 *       {@link org.apache.sis.measure.RangeFormat},
 *       {@link org.apache.sis.measure.UnitFormat})</li>
 * </ul>
 *
 * Apache SIS supports arithmetic operations on units and on quantities.
 * The unit (including SI prefix) and the quantity type resulting from
 * those arithmetic operations are automatically inferred.
 * For example, this line of code:
 *
 * {@snippet lang="java" :
 *   System.out.println( Units.PASCAL.multiply(1000) );
 *   }
 *
 * prints <q>kPa</q>, i.e. the kilo prefix has been automatically applied
 * (SI prefixes are applied on SI units only, not on other systems).
 * Other example:
 *
 * {@snippet lang="java" :
 *   Force  f = Quantities.create(4, Units.NEWTON);
 *   Length d = Quantities.create(6, Units.MILLIMETRE);
 *   Time   t = Quantities.create(3, Units.SECOND);
 *   Quantity<?> e = f.multiply(d).divide(t);
 *   System.out.println(e);
 *   System.out.println("Instance of Power: " + (e instanceof Power));
 *   }
 *
 * prints <q>8 mW</q> and <q>Instance of Power: true</q>,
 * i.e. Apache SIS detects that the result of N⋅m∕s is Watt,
 * inherits the milli prefix from millimetre and creates an instance
 * of {@link javax.measure.quantity.Power}, not just {@code Quantity<Power>} (the generic parent).
 *
 * <p>{@linkplain org.apache.sis.measure.Units#valueOf(String) Parsing} and formatting use Unicode symbols by default, as in µg/m².
 * Parenthesis are recognized at parsing time and used for denominators at formatting time, as in kg/(m²⋅s).
 * While uncommon, Apache SIS accepts fractional powers as in m^⅔.
 * Some sentences like <q>100 feet</q>, <q>square metre</q> and <q>degree Kelvin</q>
 * are also recognized at parsing time.</p>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.6
 * @since   0.3
 */
package org.apache.sis.measure;
