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
package org.apache.sis.referencing;

import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;


/**
 * Implementation of {@link AbstractIdentifiedObject} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all identified
 * object implementations before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractIdentifiedObject#castOrCopy(IdentifiedObject)}</li>
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
     * Returns a SIS implementation for the given object.
     *
     * @see AbstractIdentifiedObject#castOrCopy(IdentifiedObject)
     */
    static AbstractIdentifiedObject castOrCopy(final IdentifiedObject object) {
        if (object instanceof Datum) {
            return AbstractDatum.castOrCopy((Datum) object);
        }
        if (object instanceof Ellipsoid) {
            return DefaultEllipsoid.castOrCopy((Ellipsoid) object);
        }
        if (object instanceof PrimeMeridian) {
            return DefaultPrimeMeridian.castOrCopy((PrimeMeridian) object);
        }
        return new AbstractIdentifiedObject(object);
    }
}
