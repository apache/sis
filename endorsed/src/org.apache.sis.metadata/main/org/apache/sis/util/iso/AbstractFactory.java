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
package org.apache.sis.util.iso;

import org.opengis.util.Factory;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.util.internal.shared.Strings;

// Specific to the geoapi-4.0 branch:
import org.opengis.util.FactoryException;


/**
 * Base class of factories provided in the Apache SIS library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
public abstract class AbstractFactory implements Factory {
    /**
     * Creates a new factory.
     */
    protected AbstractFactory() {
    }

    /**
     * Returns the implementer of this factory, or {@code null} if unknown.
     * The default implementation tries to fetch this information from the
     * manifest associated to the package of {@code this.getClass()}.
     *
     * @return the vendor for this factory implementation, or {@code null} if unknown.
     * @throws FactoryException if an error occurred while fetching the vendor information.
     *
     * @see Package#getImplementationVendor()
     */
    @Override
    public Citation getVendor() throws FactoryException {
        final Package p = getClass().getPackage();
        if (p != null) {
            final String vendor = p.getImplementationVendor();
            if (vendor != null) {
                return new SimpleCitation(vendor);
            }
        }
        return null;
    }

    /**
     * Returns a string representation of this factory for debugging purposes.
     * This string representation may change in any future version of Apache SIS.
     *
     * @return a string representation of this factory.
     *
     * @since 1.5
     */
    @Override
    public String toString() {
        final var args = new Object[2];
        try {
            Citation c = getVendor();
            if (c != null) {
                args[0] = "vendor";
                args[1] = c.getTitle();
            }
        } catch (FactoryException e) {
            args[0] = "exception";
            args[1] = e.toString();
        }
        return Strings.toString(getClass(), args);
    }
}
