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
package org.apache.sis.metadata.iso.lineage;

import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.quality.DefaultScope;


/**
 * Information about the events or source data used in constructing the data specified by
 * the scope or lack of knowledge about lineage.
 *
 * Only one of {@linkplain #getStatement statement}, {@linkplain #getProcessSteps process steps}
 * and {@link #getSources sources} should be provided.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "LI_Lineage_Type", propOrder = {
    "statement",
    "processSteps",
    "sources"
})
@XmlRootElement(name = "LI_Lineage")
public class DefaultLineage extends ISOMetadata implements Lineage {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3351230301999744987L;

    /**
     * General explanation of the data producer's knowledge about the lineage of a dataset.
     * Should be provided only if {@linkplain DefaultScope#getLevel scope level} is
     * {@linkplain ScopeCode#DATASET dataset} or {@linkplain ScopeCode#SERIES series}.
     */
    private InternationalString statement;

    /**
     * Information about an event in the creation process for the data specified by the scope.
     */
    private Collection<ProcessStep> processSteps;

    /**
     * Information about the source data used in creating the data specified by the scope.
     */
    private Collection<Source> sources;

    /**
     * Constructs an initially empty lineage.
     */
    public DefaultLineage() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultLineage castOrCopy(final Lineage object) {
        if (object == null || object instanceof DefaultLineage) {
            return (DefaultLineage) object;
        }
        final DefaultLineage copy = new DefaultLineage();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the general explanation of the data producer's knowledge about the lineage of a dataset.
     * Can be provided only if {@linkplain DefaultScope#getLevel scope level}
     * is {@link ScopeCode#DATASET DATASET} or {@link ScopeCode#SERIES SERIES}.
     */
    @Override
    @XmlElement(name = "statement")
    public synchronized InternationalString getStatement() {
        return statement;
    }

    /**
     * Sets the general explanation of the data producers knowledge about the lineage of a dataset.
     *
     * @param newValue The new statement.
     */
    public synchronized void setStatement(final InternationalString newValue) {
        checkWritePermission();
        statement = newValue;
    }

    /**
     * Returns the information about an event in the creation process for the data specified by the scope.
     */
    @Override
    @XmlElement(name = "processStep")
    public synchronized Collection<ProcessStep> getProcessSteps() {
        return processSteps = nonNullCollection(processSteps, ProcessStep.class);
    }

    /**
     * Sets information about an event in the creation process for the data specified by the scope.
     *
     * @param newValues The new process steps.
     */
    public synchronized void setProcessSteps(final Collection<? extends ProcessStep> newValues)  {
        processSteps = writeCollection(newValues, processSteps, ProcessStep.class);
    }

    /**
     * Returns information about the source data used in creating the data specified by the scope.
     */
    @Override
    @XmlElement(name = "source")
    public synchronized Collection<Source> getSources() {
        return sources = nonNullCollection(sources, Source.class);
    }

    /**
     * Sets information about the source data used in creating the data specified by the scope.
     *
     * @param newValues The new sources.
     */
    public synchronized void setSources(final Collection<? extends Source> newValues) {
        sources = writeCollection(newValues, sources, Source.class);
    }
}
