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
package org.apache.sis.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.apache.sis.util.collection.Containers;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.logging.Logging;


/**
 * Mapping between some legacy codes (e.g. ISO 19115:2003) and newer codes (e.g. ISO 19115:2014).
 * Provided in a separated class for loading only when first needed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class LegacyCodes {
    /**
     * Do not allow instantiation of this class.
     */
    private LegacyCodes() {
    }

    /**
     * Character set codes ({@code MD_CharacterSetCode}) mapping between ISO 19115:2003 and IANA names.
     * The ISO 19115:2014 revision uses IANA names.
     */
    static final Map<String,String> IANA_TO_LEGACY, LEGACY_TO_IANA;
    static {
        final Properties codes = new Properties();
        try (InputStream in = Metadata.class.getResourceAsStream("2003/charset-codes.properties")) {
            codes.load(in);
        } catch (IOException e) {
            Logging.unexpectedException(Context.LOGGER, ValueConverter.class, "toCharset[Code]", e);
        }
        final int capacity = Containers.hashMapCapacity(codes.size());
        IANA_TO_LEGACY = new HashMap<>(capacity);
        LEGACY_TO_IANA = new HashMap<>(capacity);
        for (final Map.Entry<Object, Object> entry : codes.entrySet()) {
            final String legacy = ((String) entry.getKey()).intern();
            final String name   = ((String) entry.getValue()).intern();
            IANA_TO_LEGACY.put(name  .toUpperCase(Locale.US), legacy);      // IANA names are restricted to US-ASCII.
            LEGACY_TO_IANA.put(legacy.toLowerCase(Locale.US), name);
            IANA_TO_LEGACY.put(name, legacy);
            LEGACY_TO_IANA.put(legacy, name);
        }
    }

    /**
     * Converts the given IANA name to its legacy ISO 19115:2003 character set code.
     * If the given name is unknown, then it is returned unchanged.
     */
    static String fromIANA(final String name) {
        String legacy = IANA_TO_LEGACY.get(name);
        if (legacy == null) {
            legacy = IANA_TO_LEGACY.get(name.toUpperCase(Locale.US));
            if (legacy == null) {
                return name;
            }
        }
        return legacy;
    }

    /**
     * Converts the given legacy ISO 19115:2003 character set code to its IANA name.
     * If the given legacy code is unknown, then it is returned unchanged.
     */
    static String toIANA(final String legacy) {
        String name = LEGACY_TO_IANA.get(legacy);
        if (name == null) {
            name = LEGACY_TO_IANA.get(legacy.toLowerCase(Locale.US));
            if (name == null) {
                return legacy;
            }
        }
        return name;
    }
}
