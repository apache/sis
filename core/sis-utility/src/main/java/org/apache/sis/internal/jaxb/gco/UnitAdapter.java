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
package org.apache.sis.internal.jaxb.gco;

import javax.measure.unit.Unit;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.Context;


/**
 * JAXB adapter for unit of measurement.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class UnitAdapter extends XmlAdapter<String, Unit<?>> {
    /**
     * Returns a unit for the given string.
     *
     * @param  value The unit symbol.
     * @return The unit for the given symbol.
     * @throws IllegalArgumentException if the given symbol is unknown.
     */
    @Override
    public Unit<?> unmarshal(String value) throws IllegalArgumentException {
        if (value != null) {
            final Context context = Context.current();
            return Context.converter(context).toUnit(context, value);
        }
        return null;
    }

    /**
     * Returns the symbol of the given unit.
     *
     * @param  value The unit.
     * @return The unit symbol.
     */
    @Override
    public String marshal(final Unit<?> value) {
        return (value != null) ? value.toString() : null;
    }
}
