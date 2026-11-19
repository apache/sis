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
package org.apache.sis.storage.ubjson;

/**
 * Constants from UBJSON specification.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://ubjson.org/
 */
final class UBJson {

    public static final byte NULL = 'Z';
    public static final byte NO_OP = 'N';
    public static final byte TRUE = 'T';
    public static final byte FALSE = 'F';
    public static final byte INT8 = 'i';
    public static final byte UINT8 = 'U';
    public static final byte INT16 = 'I';
    public static final byte INT32 = 'l';
    public static final byte INT64 = 'L';
    public static final byte FLOAT32 = 'd';
    public static final byte FLOAT64 = 'D';
    public static final byte HIGH_PRECISION_NUMBER = 'H';
    public static final byte CHAR = 'C';
    public static final byte STRING = 'S';

    public static final byte ARRAY_START = '[';
    public static final byte ARRAY_END = ']';
    public static final byte OBJECT_START = '{';
    public static final byte OBJECT_END = '}';

    public static final byte OPTIMIZED_TYPE = '$';
    public static final byte OPTIMIZED_SIZE = '#';

    private UBJson(){}
}
