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
package org.apache.sis.metadata.iso.legacy;

import org.opengis.annotation.UML;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;
import org.apache.sis.util.iso.Types;


/**
 * Name of the medium as defined in legacy ISO 19115:2003.
 * In more recent specification, this code list has been replaced by a citation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-389">SIS-389</a>
 * @see <a href="https://github.com/opengeospatial/geoapi/issues/14">GeoAPI issue #14</a>
 */
@UML(identifier="MD_MediumNameCode", specification=ISO_19115, version=2003)
public final class MediumName extends CodeList<MediumName> implements Citation {
    /** Serial number for compatibility with different versions. */
    private static final long serialVersionUID = 7157038832444373933L;

    /*
     * We need to construct values with `valueOf(String)` instead of the constructor
     * because this package is not exported to GeoAPI. See `CodeList` class javadoc.
     */

    /** Read-only optical disk. */
    @UML(identifier="cdRom", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CD_ROM = valueOf("CD_ROM");

    /** Digital versatile disk. */
    @UML(identifier="dvd", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DVD = valueOf("DVD");

    /** Digital versatile disk digital versatile disk, read only. */
    @UML(identifier="dvdRom", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DVD_ROM = valueOf("DVD_ROM");

    /** 3½ inch magnetic disk. */
    @UML(identifier="3halfInchFloppy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName FLOPPY_3_HALF_INCH = valueOf("FLOPPY_3_HALF_INCH");

    /** 5¼ inch magnetic disk. */
    @UML(identifier="5quarterInchFloppy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName FLOPPY_5_QUARTER_INCH = valueOf("FLOPPY_5_QUARTER_INCH");

    /** 7 track magnetic tape. */
    @UML(identifier="7trackTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TAPE_7_TRACK = valueOf("TAPE_7_TRACK");

    /** 9 track magnetic tape. */
    @UML(identifier="9trackTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TAPE_9_TRACK = valueOf("TAPE_9_TRACK");

    /** 3480 cartridge tape drive. */
    @UML(identifier="3480Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3480 = valueOf("CARTRIDGE_3480");

    /** 3490 cartridge tape drive. */
    @UML(identifier="3490Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3490 = valueOf("CARTRIDGE_3490");

    /** 3580 cartridge tape drive. */
    @UML(identifier="3580Cartridge", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_3580 = valueOf("CARTRIDGE_3580");

    /** 4 millimetre magnetic tape. */
    @UML(identifier="4mmCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_4mm = valueOf("CARTRIDGE_TAPE_4mm");

    /** 8 millimetre magnetic tape. */
    @UML(identifier="8mmCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_8mm = valueOf("CARTRIDGE_TAPE_8mm");

    /** ¼ inch magnetic tape. */
    @UML(identifier="1quarterInchCartridgeTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName CARTRIDGE_TAPE_1_QUARTER_INCH = valueOf("CARTRIDGE_TAPE_1_QUARTER_INCH");

    /** Half inch cartridge streaming tape drive. */
    @UML(identifier="digitalLinearTape", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName DIGITAL_LINEAR_TAPE = valueOf("DIGITAL_LINEAR_TAPE");

    /** Direct computer linkage. */
    @UML(identifier="onLine", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName ON_LINE = valueOf("ON_LINE");

    /** Linkage through a satellite communication system. */
    @UML(identifier="satellite", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName SATELLITE = valueOf("SATELLITE");

    /** Communication through a telephone network. */
    @UML(identifier="telephoneLink", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName TELEPHONE_LINK = valueOf("TELEPHONE_LINK");

    /** Pamphlet or leaflet giving descriptive information. */
    @UML(identifier="hardcopy", obligation=CONDITIONAL, specification=ISO_19115)
    public static final MediumName HARDCOPY = valueOf("HARDCOPY");

    /** Constructs an element of the given name. */
    private MediumName(final String name) {
        super(name);
    }

    /**
     * Returns the list of {@code MediumName}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    @Override
    public MediumName[] family() {
        return values(MediumName.class);
    }

    /**
     * Returns the medium name that matches the given string, or a new one if none match it.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name, or {@code null}.
     */
    public static MediumName valueOf(final String code) {
        return valueOf(MediumName.class, code, MediumName::new).get();
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
                return Types.forCodeName(MediumName.class, title.toString(), null);
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
