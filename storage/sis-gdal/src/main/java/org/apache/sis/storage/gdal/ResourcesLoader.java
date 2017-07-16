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

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;



/**
 * Loads the resources needed by the Proj.4 wrappers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class ResourcesLoader {
    /**
     * The file which contains parameter aliases.
     */
    private static final String PARAMETERS_FILE = "parameter-names.txt";

    /**
     * The file which contains projection aliases.
     */
    private static final String PROJECTIONS_FILE = "projection-names.txt";

    /**
     * The Proj.4 names for OGC, EPSG or GeoTIFF projection names.
     * Will be filled when first needed.
     */
    private static final Map<String,String> projectionNames = new HashMap<>();

    /**
     * The Proj.4 names for OGC, EPSG or GeoTIFF parameter names.
     * Will be filled when first needed.
     */
    private static final Map<String,String> parameterNames = new HashMap<>();

    /**
     * Do not allows instantiation of this class.
     */
    private ResourcesLoader() {
    }

    /**
     * Returns the mapping from projection/parameter names to Proj.4 names.
     *
     * @throws FactoryException if the resource file can not be loaded.
     */
    private static Map<String,String> getAliases(final boolean isParam) throws FactoryException {
        final Map<String,String> map = isParam ? parameterNames : projectionNames;
        synchronized (map) {
            if (!map.isEmpty()) {
                return map;
            }
            IOException cause = null;
            final String file = isParam ? PARAMETERS_FILE : PROJECTIONS_FILE;
            final InputStream in = ResourcesLoader.class.getResourceAsStream(file);
            if (in != null) try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                    String parameter = null;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if ((line = line.trim()).isEmpty()) {
                            continue;                                               // Skip empty lines.
                        }
                        switch (line.charAt(0)) {
                            case '#': /* A line of comment */   break;
                            case '+': parameter = line;         break;
                            default:  map.put(line, parameter); break;
                        }
                    }
                }
                return map;
            } catch (IOException e) {
                cause = e;
            }
            throw new FactoryException("Can not read the \"" + file + "\" resource", cause);
        }
    }

    /**
     * Returns the Proj.4 name for the given parameter or projection.
     * If no mapping is found, then the parameter name is returned unchanged.
     *
     * @param  param    the parameter value or group from which to get the name.
     * @param  isParam  {@code true} if we are looking for a parameter name rather than a projection name.
     * @return the Proj.4 name.
     */
    static String getProjName(final GeneralParameterValue param, final boolean isParam) throws FactoryException {
        return getProjName(param.getDescriptor(), getAliases(isParam));
    }

    /**
     * Returns the Proj.4 name for the given identified object, looking in the given map of aliases.
     * If no mapping is found, then the parameter name is returned unchanged.
     */
    private static String getProjName(final IdentifiedObject descriptor, final Map<String, String> map)
            throws NoSuchIdentifierException
    {
        final String name = descriptor.getName().getCode();
        String proj = map.get(name);
        if (proj == null) {
            // If the name is not recognized, try the alias (if any).
            // If no alias match, then return the name unchanged.
            for (final GenericName alias : descriptor.getAlias()) {
                proj = map.get(alias.tip().toString());
                if (proj != null) {
                    return proj;
                }
            }
            proj = name;
        }
        if (!proj.startsWith("+")) {
            throw new NoSuchIdentifierException("Unknown identifier: " + proj, proj);
        }
        return proj;
    }
}
