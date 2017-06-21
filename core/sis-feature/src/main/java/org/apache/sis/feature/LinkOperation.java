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
package org.apache.sis.feature;

import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.io.IOException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.util.ArgumentChecks;


/**
 * A link operation, which is like a redirection or an alias.
 * The operation acts like a reference to another property.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.6
 * @module
 */
final class LinkOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 765096861589501215L;

    /**
     * The parameter descriptor for the "Link" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = FeatureUtilities.parameters("Link");

    /**
     * The type of the result.
     */
    private final AbstractIdentifiedType result;

    /**
     * The name of the referenced attribute or feature association.
     */
    final String referentName;

    /**
     * Creates a new link to the given attribute or association.
     *
     * @param identification  the name of the link, together with optional information.
     * @param referent        the referenced attribute or feature association.
     */
    LinkOperation(final Map<String,?> identification, AbstractIdentifiedType referent) {
        super(identification);
        if (referent instanceof LinkOperation) {
            referent = ((LinkOperation) referent).result;
        }
        result = referent;
        referentName = referent.getName().toString();
    }

    /**
     * Returns a description of the input parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Returns the expected result type.
     */
    @Override
    public AbstractIdentifiedType getResult() {
        return result;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    public Set<String> getDependencies() {
        return Collections.singleton(referentName);
    }

    /**
     * Returns the property from the referenced attribute of feature association.
     *
     * @param  feature     the feature from which to get the property.
     * @param  parameters  ignored (can be {@code null}).
     * @return the linked property from the given feature.
     */
    @Override
    public Object apply(final AbstractFeature feature, final ParameterValueGroup parameters) {
        ArgumentChecks.ensureNonNull("feature", feature);
        return feature.getProperty(referentName);
    }

    /**
     * Computes a hash-code value for this operation.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + referentName.hashCode();
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        // 'this.result' is compared (indirectly) by the super class.
        return super.equals(obj) && referentName.equals(((LinkOperation) obj).referentName);
    }

    /**
     * Appends a string representation of the "formula" used for computing the result.
     *
     * @param  buffer  where to format the "formula".
     */
    @Override
    void formatResultFormula(final Appendable buffer) throws IOException {
        buffer.append(referentName);
    }
}
