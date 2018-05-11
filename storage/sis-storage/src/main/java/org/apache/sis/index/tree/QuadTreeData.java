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
package org.apache.sis.index.tree;

//SIS imports
import org.apache.sis.geometry.DirectPosition2D;

/**
 * Interface representing data stored in quad tree. All data to be stored in
 * quad tree must implement this interface, so that quad tree can access
 * location and store name of file in which data is saved.
 *
 * <div class="warning"><b>Note on future work:</b> this interface may change in incompatible way
 * in a future Apache SIS release, or may be replaced by new API.</div>
 */
public interface QuadTreeData {
  /**
   * Returns the Java 2D x-coordinate for the longitude.
   *
   * @return the Java 2D x-coordinate
   */
  public double getX();

  /**
   * Returns the Java 2D y-coordinate for the latitude.
   *
   * @return the Java 2D y-coordinate
   */
  public double getY();

  /**
   * Returns the latitude/longitude pair.
   *
   * @return the latitude/longitude pair.
   */
  public DirectPosition2D getLatLon();

  /**
   * Returns the name of the file where the entry's info is saved.
   *
   * @return the name of the file where the entry's info is saved
   */
  public String getFileName();
}
