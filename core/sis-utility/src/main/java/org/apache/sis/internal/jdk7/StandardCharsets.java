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
package org.apache.sis.internal.jdk7;

import java.nio.charset.Charset;


/**
 * Place holder for {@link java.nio.charset.StandardCharsets}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI)
 * @version 0.5
 * @module
 */
public final class StandardCharsets {
    /**
     * Do not allow instantiation of this class.
     */
    private StandardCharsets() {
    }

    /**
     * Eight-bit UCS Transformation Format.
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Sixteen-bit UCS Transformation Format.
     */
    public static final Charset UTF_16 = Charset.forName("UTF-16");

    /**
     * ISO/IEC 8859-1, Information technology - 8-bit single byte coded graphic character sets - Part 1 : Latin alphabet No.1.
     */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     * Seven-bit ASCII, a.k.a. ISO646-US.
     */
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
}
