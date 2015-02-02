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
import org.apache.sis.internal.simple.SimpleCitation;


/**
 * Base class of factories provided in the Apache SIS library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class AbstractFactory implements Factory {
    /**
     * Creates a new factory.
     */
    protected AbstractFactory() {
    }

    /**
     * Returns the implementor of this factory, or {@code null} if unknown.
     * The default implementation tries to fetch this information from the
     * manifest associated to the package of {@code this.getClass()}.
     *
     * @return The vendor for this factory implementation, or {@code null} if unknown.
     *
     * @see Package#getImplementationVendor()
     */
    @Override
    public Citation getVendor() {
        final Package p = getClass().getPackage();
        if (p != null) {
            final String vendor = p.getImplementationVendor();
            if (vendor != null) {
                return new SimpleCitation(vendor);
            }
        }
        return null;
    }
}
