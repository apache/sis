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
package org.apache.sis.process;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.lineage.Processing;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;


/**
 * Description of a {@linkplain Process process} algorithm and its input/output parameters.
 * This interface extends the ISO 19115 {@link Processing} interface, which provide human-readable
 * information about the process. In addition to the ISO/OGC interface, {@code ProcessDescriptor}
 * provides:
 * <ul>
 *   <li>A more machine-usable description of the input and output parameters
 *       (including source data and output data).</li>
 *   <li>A method for creating new {@link Process} instances which can be run in
 *       an {@link ExecutorService}.</li>
 * </ul>
 * <p>
 * {@code ProcessDescriptor} implementations are declared as services for {@link java.util.ServiceLoader}
 * and can be found by name with {@link ProcessFinder}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public interface ProcessDescriptor extends Processing {
    /**
     * Information to identify the processing package that run the process.
     * The identifier {@linkplain Identifier#getCode() code} is the name used by
     * {@link ProcessFinder} for finding this descriptor, and the identifier
     * {@linkplain Identifier#getCodeSpace() code space} or {@linkplain Identifier#getAuthority() authority}
     * identifies the processing package.
     *
     * @return Identifier of the processing package that run the process.
     */
    @Override
    Identifier getIdentifier();

    /**
     * Process name to display in user interfaces.
     * This name is not intended to identify the process;
     * identification shall use the process {@linkplain #getIdentifier() identifier} instead.
     *
     * @return The processing name to display in user interfaces, or {@code null} if none.
     */
    InternationalString getDisplayName();

    /**
     * Category where to show this process in a tree of processes, such as a toolbox.
     * Each {@linkplain GenericName#getParsedNames() component} of the returned name is a folder of the tree,
     * from the root ({@linkplain GenericName#head() head}) to the folder containing this process
     * ({@linkplain GenericName#tip() tip}). The {@code toString()} value of each component is a
     * locale-independent code identifying the folder, and {@code toInternationalString()} is the label
     * to show to users. Common categories are defined in {@link ProcessCategories}.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * @return category of this process, or {@code null} if uncategorized.
     *
     * @see ProcessCategories
     */
    default GenericName getCategory() {
        return null;
    }

    /**
     * Additional details about the processing procedures.
     *
     * @return The processing procedures, or {@code null} if none.
     */
    @Override
    InternationalString getProcedureDescription();

    /**
     * Returns a description of the input parameters. The {@linkplain ParameterValueGroup parameter
     * values} include both the source data (for example the source images) and the parameters that
     * configure the operation applied on those data.
     * <p>
     * This is the descriptor of the parameter values returned by {@link Process#getInput()}.
     *
     * @return Description of the input parameters.
     *
     * @see Process#getInput()
     * @see ProcessStep#getSources()
     */
    ParameterDescriptorGroup getInputDescriptor();

    /**
     * Returns a description of the output parameters.
     * This is the descriptor of the parameter values returned by {@link Process#call()}.
     * Those {@linkplain ParameterValueGroup parameter values} include output data
     * (for example the output images) and optionally some metadata related to those outputs
     * (for example statistics).
     *
     * @return Description of the output parameters.
     *
     * @see Process#call()
     * @see ProcessStep#getOutputs()
     */
    ParameterDescriptorGroup getOutputDescriptor();

    /**
     * Creates a new process initialized with the given input parameter values. The returned
     * process is not yet started. To start the process, callers must give the process to an
     * {@link ExecutorService} or invoke {@link Process#call()} explicitely.
     * <p>
     * The input parameters are typically created by calls to
     * <code>{@linkplain #getInputDescriptor()}.{@linkplain ParameterDescriptorGroup#createValue()
     * createValue()}</code>. Then the parameter values shall be set before to be given to this
     * {@code createProcess} method.
     *
     * @param  input The input parameters.
     * @return A new un-started process which will use the given input parameter values.
     *
     * @see ExecutorService#submit(Callable)
     */
    Process createProcess(ParameterValueGroup input);
}
