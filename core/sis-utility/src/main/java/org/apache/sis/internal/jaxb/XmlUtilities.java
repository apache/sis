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
package org.apache.sis.internal.jaxb;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;

/**
 * Utilities methods related to XML.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class XmlUtilities extends SystemListener {
    /**
     * The factory for creating {@link javax.xml.datatype} objects.
     */
    private static volatile DatatypeFactory factory;

    /**
     * Resets the {@link #factory} to {@code null} if the classpath changed.
     */
    static {
        SystemListener.add(new XmlUtilities());
    }

    /**
     * For internal usage only.
     */
    private XmlUtilities() {
        super(Modules.UTILITIES);
    }

    /**
     * Invoked when the classpath changed. This method resets the {@link #factory} to {@code null}
     * in order to force the search for a new instance.
     */
    @Override
    protected void classpathChanged() {
        synchronized (XmlUtilities.class) {
            factory = null;
        }
    }

    /**
     * Returns the factory for creating {@link javax.xml.datatype} objects.
     *
     * @return The factory (never {@code null}).
     * @throws DatatypeConfigurationException If the factory can not be created.
     */
    public static DatatypeFactory getDatatypeFactory() throws DatatypeConfigurationException {
        DatatypeFactory f = factory;
        if (f == null) {
            synchronized (XmlUtilities.class) {
                f = factory;
                if (f == null) {
                    factory = f = DatatypeFactory.newInstance();
                }
            }
        }
        return f;
    }
}
