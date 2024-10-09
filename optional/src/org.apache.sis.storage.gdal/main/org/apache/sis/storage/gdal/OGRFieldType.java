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
package org.apache.sis.storage.gdal;

/**
 *
 * @author Hilmi Bouallegue (Geomatys)
 */
enum OGRFieldType {
    /** Simple 32bit integer */
    OFTInteger(Integer.class),
    /** List of 32bit integers */
    OFTIntegerList(Object.class),
    /** Double Precision floating point */
    OFTReal(Double.class),
    /** List of doubles */
    OFTRealList(Object.class),
    /** String of ASCII chars */
    OFTString(String.class),
    /** Array of strings */
    OFTStringList(Object.class),
    /** deprecated */
    OFTWideString(String.class),
    /** deprecated */
    OFTWideStringList(Object.class),
    /** Raw Binary data */
    OFTBinary(Object.class),
    /** Date */
    OFTDate(Object.class),
    /** Time */
    OFTTime(Object.class),
    /** Date and Time */
    OFTDateTime(Object.class),
    /** Single 64bit integer */
    OFTInteger64(Long.class),
    /** List of 64bit integers */
    OFTInteger64List(Object.class),
    OFTMaxType(Object.class);

    private final Class javaClass;

    OGRFieldType(Class javaClass){
        this.javaClass =javaClass;
    }

    public Class getJavaClass(){
        return javaClass;
    }

    public static OGRFieldType valueOf(int value) {
        switch (value) {
            case 0 : return OFTInteger;
            case 1 : return OFTIntegerList;
            case 2 : return OFTReal;
            case 3 : return OFTRealList;
            case 4 : return OFTString;
            case 5 : return OFTStringList;
            case 6 : return OFTWideString;
            case 7 : return OFTWideStringList;
            case 8 : return OFTBinary;
            case 9 : return OFTDate;
            case 10 : return OFTTime;
            case 11 : return OFTDateTime;
            case 12 : return OFTInteger64;
            case 13 : return OFTInteger64List;
        }
        throw new IllegalArgumentException("Unknown type " + value);
    }

}
