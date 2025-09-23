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
package org.apache.sis.referencing.internal.shared;

import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.NilObject;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.InternationalString;


/**
 * A referencing object for which every methods return {@code null} or a neutral value.
 * <strong>This is not a valid object</strong>. It is used only for initialization of
 * objects to be used by JAXB at unmarshalling time, as a way to simulate "no-argument"
 * constructor required by JAXB.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class NilReferencingObject implements NilObject, ReferenceSystem {
    /**
     * The default name of {@code NilReferencingObject} instances.
     * We use this value because {@link ReferenceSystem#getName()}
     * is a mandatory property and not all code is tolerant to null name.
     *
     * <h4>Usage note</h4>
     * In theory we do not need a default name because it will be replaced by
     * the value of the {@code <gml:name>} element anyway at XML unmarshalling time.
     * But not all XML documents are valid, so the {@code <gml:name>} may be missing.
     */
    public static final ReferenceIdentifier UNNAMED = new NamedIdentifier(null, Vocabulary.format(Vocabulary.Keys.Unnamed));

    /**
     * The unique instance.
     */
    public static final NilReferencingObject INSTANCE = new NilReferencingObject();

    /**
     * Do not allow other instantiation of {@link #INSTANCE}.
     */
    private NilReferencingObject() {
    }

    /**
     * This object is empty because the value will be provided later.
     */
    @Override
    public NilReason getNilReason() {
        return NilReason.TEMPLATE;
    }

    /**
     * Returns the localized "unnamed" name because this property is mandatory.
     */
    @Override
    public ReferenceIdentifier getName() {
        return UNNAMED;
    }

    /**
     * For avoiding ambiguity.
     */
    @Override
    @Deprecated
    public InternationalString getScope() {
        return null;
    }
}
