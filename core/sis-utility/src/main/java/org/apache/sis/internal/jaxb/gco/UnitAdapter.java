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

import javax.measure.Unit;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.gml.Measure;


/**
 * JAXB adapter for unit of measurement.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see Measure
 *
 * @since 0.3
 * @module
 */
public class UnitAdapter extends XmlAdapter<String, Unit<?>> {
    /**
     * Invoked by reflection by JAXB.
     */
    public UnitAdapter() {
    }

    /**
     * Returns a unit for the given string.
     *
     * @param  value  the unit symbol.
     * @return the unit for the given symbol.
     * @throws IllegalArgumentException if the given symbol is unknown.
     */
    @Override
    public final Unit<?> unmarshal(final String value) throws IllegalArgumentException {
        final Context context = Context.current();
        return Context.converter(context).toUnit(context, value);
    }

    /**
     * Returns the symbol of the given unit.
     *
     * @param  value  the unit.
     * @return the unit symbol.
     */
    @Override
    public String marshal(final Unit<?> value) {
        return (value != null) ? value.toString() : null;
    }

    /**
     * A variant of {@link UnitAdapter} which marshal units as a URN for Coordinate System (CS) axes.
     * Example: {@code "urn:ogc:def:uom:EPSG::9001"}.
     *
     * The difference between coordinate system axis and other uses (prime meridian, etc.) is in the choice of EPSG
     * code for the degrees. See {@link org.apache.sis.measure.Units#getEpsgCode(Unit, boolean)} for more information.
     */
    public static final class ForCS extends UnitAdapter {
        @Override
        public String marshal(final Unit<?> value) {
            return Measure.getUOM(value, false, true);
        }
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends UnitAdapter {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public String marshal(final Unit<?> value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
