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
import javax.xml.XMLConstants;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;


/**
 * List some namespaces URLs used by JAXB when (un)marshalling.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Quentin Boileau (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class Namespaces extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Namespaces() {
    }

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GCO = "http://www.isotc211.org/2005/gco";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GFC = "http://www.isotc211.org/2005/gfc";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GMD = "http://www.isotc211.org/2005/gmd";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GMI = "http://www.isotc211.org/2005/gmi";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GMX = "http://www.isotc211.org/2005/gmx";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category OGC
     */
    public static final String GML = "http://www.opengis.net/gml";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category OGC
     */
    public static final String CSW = "http://www.opengis.net/cat/csw/2.0.2";

    /**
     * The <code>{@value}</code> URL.
     * This is also defined by {@link XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI}.
     *
     * @category W3C
     * @see XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI
     */
    public static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

    /**
     * The <code>{@value}</code> URL.
     *
     * @category W3C
     */
    public static final String XLINK = "http://www.w3.org/1999/xlink";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category Profiles
     */
    public static final String FRA = "http://www.cnig.gouv.fr/2005/fra";

    /**
     * URLs for which the prefix to use directly follows them.
     */
    private static final String[] GENERIC_URLS = {
        "http://www.isotc211.org/2005/",
        "http://www.opengis.net/",
        "http://www.w3.org/1999/",
        "http://www.cnig.gouv.fr/2005/",
        "http://purl.org/"
    };

    /**
     * A map of (<var>URLs</var>, <var>prefix</var>). Stores URLs for which
     * the prefix to use can not be easily inferred from the URL itself.
     */
    private static final Map<String,String> SPECIFIC_URLS;
    static {
        final Map<String,String> p = new HashMap<String,String>(40);
        p.put(XMLConstants.W3C_XML_SCHEMA_NS_URI,                         "xsd");
        p.put(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,                "xsi");
        p.put("http://www.w3.org/2004/02/skos/core#",                    "skos");
        p.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#",              "rdf");
        p.put("http://www.w3.org/1998/Math/MathML",                       "mml");
        p.put("http://www.opengis.net/sensorML/1.0",                     "sml1");
        p.put("http://www.opengis.net/sensorML/1.0.1",                    "sml");
        p.put("http://www.opengis.net/swe/1.0",                          "swe1");
        p.put("http://www.opengis.net/cat/csw/2.0.2",                     "csw");
        p.put("http://www.opengis.net/cat/wrs/1.0",                       "wrs");
        p.put("http://www.opengis.net/cat/wrs",                         "wrs09");
        p.put("http://www.opengis.net/ows-6/utds/0.3",                   "utds");
        p.put("http://www.opengis.net/citygml/1.0",                      "core");
        p.put("http://www.opengis.net/citygml/building/1.0",            "build");
        p.put("http://www.opengis.net/citygml/cityfurniture/1.0",   "furniture");
        p.put("http://www.opengis.net/citygml/transportation/1.0",         "tr");
        p.put("http://www.purl.org/dc/elements/1.1/",                     "dc2");
        p.put("http://www.purl.org/dc/terms/",                           "dct2");
        p.put("http://purl.org/dc/terms/",                                "dct");
        p.put("http://www.inspire.org",                                   "ins");
        p.put("http://inspira.europa.eu/networkservice/view/1.0",  "inspire_vs");
        p.put("urn:oasis:names:tc:ciq:xsdschema:xAL:2.0",                 "xal");
        p.put("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0",              "rim");
        p.put("urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5",            "rim25");
        p.put("urn:oasis:names:tc:xacml:2.0:context:schema:os", "xacml-context");
        p.put("urn:oasis:names:tc:xacml:2.0:policy:schema:os",   "xacml-policy");
        p.put("urn:us:gov:ic:ism:v2",                                   "icism");
        SPECIFIC_URLS = p;
    }

    /**
     * Returns the preferred prefix for the given namespace URI.
     *
     * @param  namespace    The namespace URI for which the prefix needs to be found.
     *                      Can not be {@code null}.
     * @param  defaultValue The default prefix to returned if the given {@code namespace}
     *                      is not recognized, or {@code null}.
     * @return The prefix inferred from the namespace URI, or {@code null} if the given namespace
     *         is unrecognized and the {@code defaultValue} is null.
     */
    public static String getPreferredPrefix(String namespace, final String defaultValue) {
        ArgumentChecks.ensureNonNull("namespace", namespace);
        String prefix = SPECIFIC_URLS.get(namespace);
        if (prefix != null) {
            return prefix;
        }
        namespace = namespace.toLowerCase(Locale.US);
        for (final String baseURL : GENERIC_URLS) {
            if (namespace.startsWith(baseURL)) {
                final int startAt = baseURL.length();
                final int endAt = namespace.indexOf('/', startAt);
                if (endAt >= 0) {
                    prefix = namespace.substring(startAt, endAt);
                } else {
                    prefix = namespace.substring(startAt);
                }
                return prefix;
            }
        }
        return defaultValue;
    }
}
