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
package org.apache.sis.referencing.operation;

import java.util.Map;
import java.util.HashMap;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.provider.AbstractProvider;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.Deprecable;


/**
 * Description of the inverse of another method. This class should be used only when no operation is defined
 * for the inverse, or when the inverse operation can not be represented by inverting the sign of parameters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
final class InverseOperationMethod extends DefaultOperationMethod {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6395008927817202180L;

    /**
     * The original operation method for which this {@code InverseOperationMethod} is the inverse.
     */
    private final OperationMethod inverse;

    /**
     * Creates the inverse of the given method.
     */
    private InverseOperationMethod(final Map<String,?> properties, final OperationMethod method) {
        super(properties, method.getTargetDimensions(), method.getSourceDimensions(), method.getParameters());
        inverse = method;
    }

    /**
     * Returns or create the inverse of the given operation method.
     */
    static OperationMethod create(final OperationMethod method) {
        if (method instanceof InverseOperationMethod) {
            return ((InverseOperationMethod) method).inverse;
        }
        if (method instanceof AbstractProvider && ((AbstractProvider) method).isInvertible()) {
            return method;
        }
        Identifier name = method.getName();
        name = new ImmutableIdentifier(null, name.getCodeSpace(), "Inverse " + name.getCode());
        final Map<String,Object> properties = new HashMap<>(6);
        properties.put(NAME_KEY,    name);
        properties.put(FORMULA_KEY, method.getFormula());
        properties.put(REMARKS_KEY, method.getRemarks());
        if (method instanceof Deprecable) {
            properties.put(DEPRECATED_KEY, ((Deprecable) method).isDeprecated());
        }
        return new InverseOperationMethod(properties, method);
    }
}
