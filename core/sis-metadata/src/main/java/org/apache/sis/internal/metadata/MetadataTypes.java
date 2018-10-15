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
package org.apache.sis.internal.metadata;

import java.util.Collection;
import java.util.function.UnaryOperator;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.lineage.Source;
import org.apache.sis.internal.jaxb.TypeRegistration;
import org.apache.sis.internal.jaxb.gco.Multiplicity;
import org.apache.sis.internal.jaxb.gmi.LE_ProcessStep;
import org.apache.sis.internal.jaxb.gmi.LE_Source;
import org.apache.sis.internal.jaxb.gmi.MI_Band;
import org.apache.sis.internal.jaxb.gmi.MI_CoverageDescription;
import org.apache.sis.internal.jaxb.gmi.MI_Georectified;
import org.apache.sis.internal.jaxb.gmi.MI_Georeferenceable;
import org.apache.sis.internal.jaxb.gmi.MI_ImageDescription;
import org.apache.sis.internal.jaxb.gmi.MI_Metadata;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.Georeferenceable;


/**
 * Declares the classes of objects to be marshalled using a default {@code MarshallerPool}.
 * This class is declared in the {@code META-INF/services/org.apache.sis.internal.jaxb.TypeRegistration} file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class MetadataTypes extends TypeRegistration implements UnaryOperator<Object> {
    /**
     * Adds to the given collection the metadata types that should be given to the initial JAXB context.
     */
    @Override
    protected void getTypes(final Collection<Class<?>> addTo) {
        addTo.add(DefaultMetadata.class);
        addTo.add(Multiplicity.class);          // Not used directly by ISO 19115 metadata, but used by Feature Catalog.
    }

    /**
     * Returns the converter to apply before marshalling objects.
     *
     * @return {@code this}.
     */
    @Override
    protected UnaryOperator<Object> beforeMarshal() {
        return this;
    }

    /**
     * Ensures that the given value is an instance of a class that can be marshalled, or returns {@code null}
     * if the type of the given value is not handled by this method. Current implementation handles all types
     * that may need to be put in the ISO 19115-3 namespace; we have to do that ourself because those classes
     * are not public. Other types may be added if needed, but we do not want to handle too many of them (for
     * performance reasons). However the list or recognized types shall contain at least {@link Metadata}.
     *
     * @param  value  the value to marshal.
     * @return the given value as a type that can be marshalled, or {@code null}.
     */
    @Override
    public Object apply(final Object value) {
        /*
         * Classes that are most likely to be used should be checked first.  If a type is a specialization
         * of another type (e.g. ImageDescription extends CoverageDescription), the specialized type shall
         * be before the more generic type.
         */
        if (value instanceof Metadata)            return MI_Metadata           .castOrCopy((Metadata)            value);
        if (value instanceof ImageDescription)    return MI_ImageDescription   .castOrCopy((ImageDescription)    value);
        if (value instanceof CoverageDescription) return MI_CoverageDescription.castOrCopy((CoverageDescription) value);
        if (value instanceof Georectified)        return MI_Georectified       .castOrCopy((Georectified)        value);
        if (value instanceof Georeferenceable)    return MI_Georeferenceable   .castOrCopy((Georeferenceable)    value);
        if (value instanceof Band)                return MI_Band               .castOrCopy((Band)                value);
        if (value instanceof ProcessStep)         return LE_ProcessStep        .castOrCopy((ProcessStep)         value);
        if (value instanceof Source)              return LE_Source             .castOrCopy((Source)              value);
        return null;
    }
}
