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

import java.io.Serializable;

import org.apache.sis.util.ObjectConverters;

import org.apache.sis.util.UnconvertibleObjectException;
import org.opengis.filter.expression.Expression;

/**
 * Override evaluate(Object,Class) by using the converters system.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class AbstractExpression implements Expression,Serializable {

    /**
     * Use SIS object converters to convert the default result object
     * to the wished class.
     *
     * @param candidate to evaluate
     * @param target wanted class
     */
    @Override
    public <T> T evaluate(final Object candidate, final Class<T> target) {
        final Object value = evaluate(candidate);
        if (target == null) {
            return (T) value;
        }
        try{
            return ObjectConverters.convert(value, target);
        }catch (UnconvertibleObjectException ex){
            return null;
        }
    }

}
