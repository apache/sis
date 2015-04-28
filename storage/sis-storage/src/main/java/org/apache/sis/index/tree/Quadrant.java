/**
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

package org.apache.sis.index.tree;

/**
 * Enum to represent the 4 quadrants of a quad tree node.
 *
 */
enum Quadrant {

  NW(0), NE(1), SW(2), SE(3);
  private final int index;

  private Quadrant(int index) {
    this.index = index;
  }

  /**
   * Returns the index of the quadrant.
   *
   * @return index of the quadrant
   */
  public int index() {
    return index;
  }

  /**
   * Retrieves the quadrant matching specified index.
   *
   * @param index
   *          specified index
   * @return quadrant matching specified index
   */
  public static Quadrant getQuadrant(int index) {
    switch (index) {
    case 0:
      return NW;
    case 1:
      return NE;
    case 2:
      return SW;
    case 3:
      return SE;
    default:
      return null;
    }
  }
}
