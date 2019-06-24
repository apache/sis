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
package org.apache.sis.internal.metadata.legacy;

import java.util.List;
import java.util.ArrayList;
import org.opengis.annotation.UML;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.CodeLists;
import org.apache.sis.util.iso.Types;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Name of the medium as defined in legacy ISO 19115:2003.
 * In more recent specification, this code list has been replaced by a citation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-389">SIS-389</a>
 * @see <a href="https://github.com/opengeospatial/geoapi/issues/14">GeoAPI issue #14</a>
 *
 * @since 2.0
 * @module
 */
@UML(identifier="MD_MediumNameCode", specification=ISO_19115, version=2003)
public final class MediumName extends CodeList<MediumName> implements Citation {
    /** Serial number for compatibility with different versions. */
    private static final long serialVersionUID = 7157038832444373933L;

    /** List of all enumerations of this type. */
    private static final List<MediumName> VALUES = new ArrayList<>(18);

    /** Read-only optical disk. */
    @UML(identifier="cdRom", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CD_ROM = new MediumName("CD_ROM");

    /** Digital versatile disk. */
    @UML(identifier="dvd", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DVD = new MediumName("DVD");

    /** Digital versatile disk digital versatile disk, read only. */
    @UML(identifier="dvdRom", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DVD_ROM = new MediumName("DVD_ROM");

    /** 3½ inch magnetic disk. */
    @UML(identifier="3halfInchFloppy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName FLOPPY_3_HALF_INCH = new MediumName("FLOPPY_3_HALF_INCH");

    /** 5¼ inch magnetic disk. */
    @UML(identifier="5quarterInchFloppy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName FLOPPY_5_QUARTER_INCH = new MediumName("FLOPPY_5_QUARTER_INCH");

    /** 7 track magnetic tape. */
    @UML(identifier="7trackTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TAPE_7_TRACK = new MediumName("TAPE_7_TRACK");

    /** 9 track magnetic tape. */
    @UML(identifier="9trackTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TAPE_9_TRACK = new MediumName("TAPE_9_TRACK");

    /** 3480 cartridge tape drive. */
    @UML(identifier="3480Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3480 = new MediumName("CARTRIDGE_3480");

    /** 3490 cartridge tape drive. */
    @UML(identifier="3490Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3490 = new MediumName("CARTRIDGE_3490");

    /** 3580 cartridge tape drive. */
    @UML(identifier="3580Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3580 = new MediumName("CARTRIDGE_3580");

    /** 4 millimetre magnetic tape. */
    @UML(identifier="4mmCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_4mm = new MediumName("CARTRIDGE_TAPE_4mm");

    /** 8 millimetre magnetic tape. */
    @UML(identifier="8mmCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_8mm = new MediumName("CARTRIDGE_TAPE_8mm");

    /** ¼ inch magnetic tape. */
    @UML(identifier="1quarterInchCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_1_QUARTER_INCH = new MediumName("CARTRIDGE_TAPE_1_QUARTER_INCH");

    /** Half inch cartridge streaming tape drive. */
    @UML(identifier="digitalLinearTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DIGITAL_LINEAR_TAPE = new MediumName("DIGITAL_LINEAR_TAPE");

    /** Direct computer linkage. */
    @UML(identifier="onLine", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName ON_LINE = new MediumName("ON_LINE");

    /** Linkage through a satellite communication system. */
    @UML(identifier="satellite", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName SATELLITE = new MediumName("SATELLITE");

    /** Communication through a telephone network. */
    @UML(identifier="telephoneLink", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TELEPHONE_LINK = new MediumName("TELEPHONE_LINK");

    /** Pamphlet or leaflet giving descriptive information. */
    @UML(identifier="hardcopy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName HARDCOPY = new MediumName("HARDCOPY");

    /** Constructs an element of the given name. */
    private MediumName(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code MediumName}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    public static MediumName[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new MediumName[VALUES.size()]);
        }
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public MediumName[] family() {
        return values();
    }

    /**
     * Returns the medium name that matches the given string, or {@code null} if none match it.
     * Contrarily to non-deprecated code list, this method does not create a new code if none match the given name.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name, or {@code null}.
     */
    public static MediumName valueOf(final String code) {
        return CodeLists.forName(MediumName.class, code, false);
    }

    /**
     * Returns the given citation as a medium name code, or {@code null} if none.
     *
     * @param  citation  the medium name to return as a citation, or {@code null}.
     * @return the code as a citation, or {@code null}.
     */
    public static MediumName castOrWrap(final Citation citation) {
        if (citation instanceof MediumName) {
            return (MediumName) citation;
        }
        if (citation != null) {
            final InternationalString title = citation.getTitle();
            if (title != null) {
                return valueOf(title.toString());
            }
        }
        return null;
    }

    /**
     * {@link Citation} methods provided for transition from legacy code list to new citation type.
     */
    @Override
    public InternationalString getTitle() {
        return Types.toInternationalString(name());
    }
}
