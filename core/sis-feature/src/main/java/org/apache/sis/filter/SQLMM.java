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
package org.apache.sis.filter;

import java.util.Set;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.feature.FunctionRegister;


/**
 * A register of functions defined by the SQL/MM standard.
 *
 * @todo Hide from public API.
 * @todo Implement all SQL/MM specification functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class SQLMM implements FunctionRegister {
    /**
     * Names of all functions known to this register.
     */
    private static final Set<String> FUNCTIONS = Collections.singleton(ST_Transform.NAME);

    /**
     * Creates the default register.
     */
    public SQLMM() {
    }

    @Override
    public String getIdentifier() {
        return "SQL/MM";
    }

    @Override
    public Set<String> getNames() {
        return FUNCTIONS;
    }

    @Override
    public Function create(final String name, Expression... parameters) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters[i]);
        }
        try {
            switch (name) {
                case ST_Transform.NAME: return new ST_Transform(parameters);
                default: throw new IllegalArgumentException("Unknown function " + name);
            }
        } catch (FactoryException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
