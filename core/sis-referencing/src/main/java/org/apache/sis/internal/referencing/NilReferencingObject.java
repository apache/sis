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

import java.util.Set;
import java.util.Collection;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.NilObject;
import org.apache.sis.io.wkt.UnformattableObjectException;


/**
 * A referencing object for which every methods return {@code null} or a neutral value.
 * <strong>This is not a valid object</strong>. It is used only for initialization of
 * objects to be used by JAXB at unmarshalling time, as a way to simulate "no-argument"
 * constructor required by JAXB.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public final class NilReferencingObject implements NilObject, IdentifiedObject {
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
    @Override public ReferenceIdentifier      getName()        {return null;}
    @Override public Collection<GenericName>  getAlias()       {return null;}
    @Override public Set<ReferenceIdentifier> getIdentifiers() {return null;}
    @Override public InternationalString      getRemarks()     {return null;}

    /**
     * Throws the exception in all cases.
     *
     * @return Never return.
     * @throws UnformattableObjectException Always thrown.
     */
    @Override
    public String toWKT() throws UnformattableObjectException {
        throw new UnformattableObjectException();
    }

    /**
     * Returns the unique instance on deserialization.
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
