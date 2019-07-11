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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.internal.feature.FunctionRegister;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

/**
 * SQL/MM function register.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SQLMM implements FunctionRegister {

    private static final Set<String> NAMES;
    static {
        Set<String> names = new HashSet<>();
        names.add(ST_Transform.NAME);
        NAMES = Collections.unmodifiableSet(names);
    }

    @Override
    public String getIdentifier() {
        return "SQL/MM";
    }

    @Override
    public Set<String> getNames() {
        return NAMES;
    }

    @Override
    public Function create(String name, Expression... parameters) {
        switch (name) {
            case ST_Transform.NAME : return new ST_Transform(parameters);
        }
        throw new IllegalArgumentException("Unknown function "+name);
    }

}
