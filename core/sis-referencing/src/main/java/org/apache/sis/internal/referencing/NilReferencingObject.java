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
package org.apache.sis.internal.referencing;

import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.NilObject;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A referencing object for which every methods return {@code null} or a neutral value.
 * <strong>This is not a valid object</strong>. It is used only for initialization of
 * objects to be used by JAXB at unmarshalling time, as a way to simulate "no-argument"
 * constructor required by JAXB.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.6
 * @since   0.4
 * @module
 */
public final class NilReferencingObject implements NilObject, ReferenceSystem {
    /**
     * The default name of {@code NilReferencingObject} instances.
     * We use this value because {@link ReferenceSystem#getName()}
     * is a mandatory property and not all code is tolerant to null name.
     *
     * <div class="note"><b>Note:</b>
     * in theory we do not need a default name because it will be replaced by
     * the value of the {@code <gml:name>} element anyway at XML unmarshalling time.
     * But not all XML documents are valid, so the {@code <gml:name>} may be missing.</div>
     *
     * @since 0.6
     */
    public static final Identifier UNNAMED = new NamedIdentifier(null, Vocabulary.format(Vocabulary.Keys.Unnamed));

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

    /*
     * Simple properties. Not all of them are optional, but SIS is tolerant to null values.
     * Returning null for collection are okay in the particular case of SIS implementation,
     * because the constructor will replace empty collections by null references anyway.
     */
    @Override public Identifier          getName()  {return UNNAMED;}
    @Override public InternationalString getScope() {return null;}
}
