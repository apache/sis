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
package org.apache.sis.internal.xml;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import org.apache.sis.storage.DataStore;


/**
 * Base class of XML data stores based on the STAX framework.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class StaxDataStore extends DataStore {
    /**
     * The STAX readers factory, created when first needed.
     *
     * @see #inputFactory()
     */
    private XMLInputFactory inputFactory;

    /**
     * The STAX writers factory, created when first needed.
     *
     * @see #outputFactory()
     */
    private XMLOutputFactory outputFactory;

    /**
     * Creates a new data store.
     */
    protected StaxDataStore() {
    }

    /**
     * Returns the short name (abbreviation) of the format being read or written.
     * This is used for error messages.
     *
     * @return short name of format being read or written.
     */
    public abstract String getFormatName();

    /**
     * Returns the factory for STAX readers.
     */
    final synchronized XMLInputFactory inputFactory() {
        if (inputFactory == null) {
            inputFactory = XMLInputFactory.newInstance();
            // TODO: register listeners here.
        }
        return inputFactory;
    }

    /**
     * Returns the factory for STAX writers.
     */
    final synchronized XMLOutputFactory outputFactory() {
        if (outputFactory == null) {
            outputFactory = XMLOutputFactory.newInstance();
            outputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        }
        return outputFactory;
    }
}
