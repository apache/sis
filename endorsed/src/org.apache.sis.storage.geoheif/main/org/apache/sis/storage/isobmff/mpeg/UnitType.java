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
package org.apache.sis.storage.isobmff.mpeg;


/**
 * Identifies which data are in a unit.
 *
 * In the current version, the ordinal of each enumeration value is the numerical code stored
 * in <abbr>HEIF</abbr> files, but this is not yet a requirement and may change in the future.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public enum UnitType {
    /**
     * The unit contains the full item.
     */
    FULL_ITEM,

    /**
     * The unit contains an image.
     */
    IMAGE,

    /**
     * The unit contains an image tile.
     */
    IMAGE_TILE,

    /**
     * The unit contains a row of an image time.
     */
    IMAGE_ROW,

    /**
     * The unit contains a pixel of a row.
     */
    IMAGE_PIXEL;

    /**
     * Returns the enumeration value for the given code, or {@code null} if unknown.
     *
     * @param  code  code stored in <abbr>HEIF</abbr> file.
     * @return enumeration value for the given code, or {@code null} if unknown.
     */
    public static UnitType valueOf(final int code) {
        switch (code) {
            case 0:  return FULL_ITEM;
            case 1:  return IMAGE;
            case 2:  return IMAGE_TILE;
            case 3:  return IMAGE_ROW;
            case 4:  return IMAGE_PIXEL;
            default: return null;
        }
    }
}
