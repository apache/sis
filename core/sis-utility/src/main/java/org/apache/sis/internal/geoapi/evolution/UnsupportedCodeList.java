/*
 * Copyright 2014 desruisseaux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.geoapi.evolution;

import java.util.List;
import java.util.ArrayList;
import org.opengis.annotation.UML;
import org.opengis.util.CodeList;

import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Placeholder for code list not yet available in GeoAPI.
 * Currently only for {@code org.opengis.metadata.citation.TelephoneType}, but we use the
 * {@code UnsupportedCodeList} class name for making clear that this class will disappear
 * in a future Apache SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 * @module
 */
@UML(identifier="CI_TelephoneTypeCode", specification=ISO_19115)
public final class UnsupportedCodeList extends CodeList<UnsupportedCodeList> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7205015191869240829L;

    /**
     * The list of constants defined in this code list.
     */
    private static final List<UnsupportedCodeList> VALUES = new ArrayList<UnsupportedCodeList>(3);

    /**
     * A frequently used code list element.
     */
    @UML(identifier="voice", obligation=CONDITIONAL, specification=ISO_19115)
    public static final CodeList<?> VOICE = new UnsupportedCodeList("VOICE");

    /**
     * A frequently used code list element.
     */
    @UML(identifier="facsimile", obligation=CONDITIONAL, specification=ISO_19115)
    public static final CodeList<?> FACSIMILE = new UnsupportedCodeList("FACSIMILE");

    /**
     * Constructor for new code list element.
     *
     * @param name The code list name.
     */
    private UnsupportedCodeList(String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     *
     * @return All code values for this code list.
     */
    @Override
    public UnsupportedCodeList[] family() {
        synchronized (VALUES) {
            return VALUES.toArray(new UnsupportedCodeList[VALUES.size()]);
        }
    }

    /**
     * Returns the telephone type that matches the given string, or returns a new one if none match it.
     *
     * @param code The name of the code to fetch or to create.
     * @return A code matching the given name.
     */
    public static UnsupportedCodeList valueOf(String code) {
        return valueOf(UnsupportedCodeList.class, code);
    }
}
