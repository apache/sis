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
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.OperationMethod;

import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.operation.DefaultOperationMethod;


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
     * The file which contains the axis orientations for each CRS code.
     */
    static final String AXIS_FILE = "axis-orientations.txt";

    /**
     * The file which contains parameter aliases.
     */
    static final String PARAMETERS_FILE = "parameter-names.txt";

    /**
     * The file which contains projection aliases.
     */
    static final String PROJECTIONS_FILE = "projection-names.txt";

    /**
     * The map of axis orientations for each CRS codes.
     * This map will be loaded from the {@value #AXIS_FILE} file when first needed.
     */
    private static Map<String,String> axisOrientations;

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
     * Pool of identifiers, filled when first needed.
     */
    private static final Map<String, List<GenericName>> aliases = new HashMap<>();

    /**
     * The set of all operation methods, filled when first needed.
     */
    private static final Set<OperationMethod> methods = new LinkedHashSet<>();

    /**
     * Do not allows instantiation of this class.
     */
    private ResourcesLoader() {
    }

    /**
     * Returns the axis orientation map. Callers shall not modify the returned map.
     * The file format is the one created by {@code SupportedCodes.write()} in the
     * test directory.
     *
     * @throws FactoryException if the resource file can not be loaded.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    static synchronized Map<String,String> getAxisOrientations() throws FactoryException {
        if (axisOrientations != null) {
            return axisOrientations;
        }
        IOException cause = null;
        final InputStream in = ResourcesLoader.class.getResourceAsStream(AXIS_FILE);
        if (in != null) try {
            final Map<String,String> map = new LinkedHashMap<>(5000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ((line = line.trim()).isEmpty()) {
                        continue;                                       // Skip empty lines.
                    }
                    switch (line.charAt(0)) {
                        case '#': {
                            break;                                      // A line of comment. Ignore.
                        }
                        case '[': {
                            // The authority. Actually we don't parse yet
                            // this element. Maybe a future version will do.
                            break;
                        }
                        default: {
                            int s = line.indexOf(':');
                            final String orientation = line.substring(0, s).trim();
                            do {
                                final int p = s+1;
                                s = line.indexOf(' ', p);
                                final String code = (s >= 0) ? line.substring(p,s) : line.substring(p);
                                map.put(code.trim(), orientation);
                            } while (s >= 0);
                            break;
                        }
                    }
                }
            }
            return axisOrientations = map;
        } catch (IOException e) {
            cause = e;
        }
        throw new FactoryException("Can not read the \"" + AXIS_FILE + "\" resource", cause);
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

    /**
     * Returns the list of aliases for the given name, or an empty list if none.
     */
    static List<GenericName> getAliases(String name, final boolean isParam) throws FactoryException {
        // Replace the name by the Proj.4 name, if we find it.
        final Map<String, String> map = getAliases(isParam);
        final String projName = map.get(name);
        if (projName != null) {
            name = projName;
        }
        return getAliases(name, map);
    }

    /**
     * Returns the list of aliases for the given Proj.4 name, or an empty list if none.
     */
    private static List<GenericName> getAliases(final String name, final Map<String,String> map) {
        List<GenericName> list;
        synchronized (aliases) {
            list = aliases.get(name);
            if (list == null) {
                list = new ArrayList<>();
                for (final Map.Entry<String,String> entry : map.entrySet()) {
                    if (name.equalsIgnoreCase(entry.getValue())) {
                        list.add(new NamedIdentifier(null, entry.getKey()));
                    }
                }
                if (list.isEmpty()) {
                    list = Collections.emptyList();
                } else {
                    ((ArrayList<?>) list).trimToSize();
                    list = Collections.unmodifiableList(list);
                }
            }
        }
        return list;
    }

    /**
     * Returns the set of all operation methods.
     */
    static Set<OperationMethod> getMethods() throws FactoryException {
        synchronized (methods) {
            if (methods.isEmpty()) {
                final Map<String,String> map = getAliases(false);
                final Map<String,Object> properties = new HashMap<>(4);
                for (final String name : new HashSet<>(map.values())) {
                    final NamedIdentifier id = new NamedIdentifier(Citations.PROJ4, name);
                    final List<GenericName> aliases = getAliases(name, map);
                    properties.put(DefaultOperationMethod.NAME_KEY, id);
                    properties.put(DefaultOperationMethod.ALIAS_KEY, aliases);
                    methods.add(new DefaultOperationMethod(properties, 2, 2, new ParameterGroup(id, aliases)));
                }
            }
        }
        return Collections.unmodifiableSet(methods);
    }
}
