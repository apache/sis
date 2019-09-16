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

import java.util.Arrays;
import java.util.Collection;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.feature.FunctionRegister;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.ArgumentChecks;


/**
 * A register of functions defined by the SQL/MM standard.
 * This standard is defined by <a href="https://www.iso.org/standard/60343.html">ISO/IEC 13249-3:2016
 * Information technology — Database languages — SQL multimedia and application packages — Part 3: Spatial</a>.
 *
 * @todo Implement all SQL/MM specification functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class SQLMM implements FunctionRegister {
    /**
     * Creates the default register.
     */
    SQLMM() {
    }

    /**
     * Returns a unique name for this factory.
     */
    @Override
    public String getIdentifier() {
        return "SQL/MM";
    }

    /**
     * Returns the names of all functions known to this register.
     */
    @Override
    public Collection<String> getNames() {
        return Arrays.asList(ST_Transform.NAME, ST_Centroid.NAME, ST_Buffer.NAME);
    }

    /**
     * Create a new function of the given name with given parameters.
     *
     * @param  name        name of the function to create.
     * @param  parameters  function parameters.
     * @return function for the given name and parameters.
     * @throws IllegalArgumentException if function name is unknown or some parameters are illegal.
     */
    @Override
    public Function create(final String name, Expression[] parameters) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters[i]);
        }
        try {
            switch (name) {
                case ST_Transform.NAME: return new ST_Transform(parameters);
                case ST_Centroid.NAME:  return new ST_Centroid(parameters);
                case ST_Buffer.NAME:    return new ST_Buffer(parameters);
                default: throw new IllegalArgumentException(Resources.format(Resources.Keys.UnknownFunction_1, name));
            }
        } catch (FactoryException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
