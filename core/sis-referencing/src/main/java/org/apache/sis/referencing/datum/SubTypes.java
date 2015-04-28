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
package org.apache.sis.referencing.datum;

import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.ImageDatum;


/**
 * Implementation of {@link AbstractDatum} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all
 * datum implementations before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractDatum#castOrCopy(Datum)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
final class SubTypes {
    /**
     * Do not allow instantiation of this class.
     */
    private SubTypes() {
    }

    /**
     * Returns a SIS implementation for the given datum.
     *
     * @see AbstractDatum#castOrCopy(Datum)
     */
    static AbstractDatum castOrCopy(final Datum object) {
        if (object instanceof GeodeticDatum) {
            return DefaultGeodeticDatum.castOrCopy((GeodeticDatum) object);
        }
        if (object instanceof VerticalDatum) {
            return DefaultVerticalDatum.castOrCopy((VerticalDatum) object);
        }
        if (object instanceof TemporalDatum) {
            return DefaultTemporalDatum.castOrCopy((TemporalDatum) object);
        }
        if (object instanceof EngineeringDatum) {
            return DefaultEngineeringDatum.castOrCopy((EngineeringDatum) object);
        }
        if (object instanceof ImageDatum) {
            return DefaultImageDatum.castOrCopy((ImageDatum) object);
        }
        /*
         * Intentionally check for AbstractDatum after the interfaces because user may have defined his own
         * subclass implementing the interface. If we were checking for AbstractDatum before the interfaces,
         * the returned instance could have been a user subclass without the JAXB annotations required for
         * XML marshalling.
         */
        if (object == null || object instanceof AbstractDatum) {
            return (AbstractDatum) object;
        }
        return new AbstractDatum(object);
    }
}
