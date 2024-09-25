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
package org.apache.sis.storage.gsf;

import java.lang.foreign.*;
import org.apache.sis.storage.gsf.specific.CmpSass;
import org.apache.sis.storage.gsf.specific.DeltaT;
import org.apache.sis.storage.gsf.specific.EM100;
import org.apache.sis.storage.gsf.specific.EM121A;
import org.apache.sis.storage.gsf.specific.EM12;
import org.apache.sis.storage.gsf.specific.EM3Raw;
import org.apache.sis.storage.gsf.specific.EM3;
import org.apache.sis.storage.gsf.specific.EM4;
import org.apache.sis.storage.gsf.specific.EM950;
import org.apache.sis.storage.gsf.specific.ElacMkII;
import org.apache.sis.storage.gsf.specific.GeoSwathPlus;
import org.apache.sis.storage.gsf.specific.KMALL;
import org.apache.sis.storage.gsf.specific.Klein5410Bss;
import org.apache.sis.storage.gsf.specific.R2Sonic;
import org.apache.sis.storage.gsf.specific.Reson7100;
import org.apache.sis.storage.gsf.specific.Reson8100;
import org.apache.sis.storage.gsf.specific.ResonTSeries;
import org.apache.sis.storage.gsf.specific.SBAmp;
import org.apache.sis.storage.gsf.specific.SBBDB;
import org.apache.sis.storage.gsf.specific.SBEchotrac;
import org.apache.sis.storage.gsf.specific.SBMGD77;
import org.apache.sis.storage.gsf.specific.SBNOSHDB;
import org.apache.sis.storage.gsf.specific.SBNavisound;
import org.apache.sis.storage.gsf.specific.SeaBat8101;
import org.apache.sis.storage.gsf.specific.SeaBatII;
import org.apache.sis.storage.gsf.specific.SeaBat;
import org.apache.sis.storage.gsf.specific.SeaBeam2112;
import org.apache.sis.storage.gsf.specific.Seabeam;
import org.apache.sis.storage.gsf.specific.Seamap;
import org.apache.sis.storage.gsf.specific.TypeIII;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SensorSpecific extends StructClass {

    static final GroupLayout LAYOUT = MemoryLayout.unionLayout(
        Seabeam.LAYOUT.withName("gsfSeaBeamSpecific"),
        EM100.LAYOUT.withName("gsfEM100Specific"),
        EM121A.LAYOUT.withName("gsfEM121ASpecific"),
        EM121A.LAYOUT.withName("gsfEM121Specific"),
        SeaBat.LAYOUT.withName("gsfSeaBatSpecific"),
        EM950.LAYOUT.withName("gsfEM950Specific"),
        EM950.LAYOUT.withName("gsfEM1000Specific"),
        Seamap.LAYOUT.withName("gsfSeamapSpecific"),
        TypeIII.LAYOUT.withName("gsfTypeIIISeaBeamSpecific"),
        TypeIII.LAYOUT.withName("gsfSASSSpecific"),
        CmpSass.LAYOUT.withName("gsfCmpSassSpecific"),
        SBAmp.LAYOUT.withName("gsfSBAmpSpecific"),
        SeaBatII.LAYOUT.withName("gsfSeaBatIISpecific"),
        SeaBat8101.LAYOUT.withName("gsfSeaBat8101Specific"),
        SeaBeam2112.LAYOUT.withName("gsfSeaBeam2112Specific"),
        ElacMkII.LAYOUT.withName("gsfElacMkIISpecific"),
        EM3.LAYOUT.withName("gsfEM3Specific"),
        EM3Raw.LAYOUT.withName("gsfEM3RawSpecific"),
        Reson8100.LAYOUT.withName("gsfReson8100Specific"),
        Reson7100.LAYOUT.withName("gsfReson7100Specific"),
        ResonTSeries.LAYOUT.withName("gsfResonTSeriesSpecific"),
        EM4.LAYOUT.withName("gsfEM4Specific"),
        GeoSwathPlus.LAYOUT.withName("gsfGeoSwathPlusSpecific"),
        Klein5410Bss.LAYOUT.withName("gsfKlein5410BssSpecific"),
        DeltaT.LAYOUT.withName("gsfDeltaTSpecific"),
        EM12.LAYOUT.withName("gsfEM12Specific"),
        R2Sonic.LAYOUT.withName("gsfR2SonicSpecific"),
        KMALL.LAYOUT.withName("gsfKMALLSpecific"),
        SBEchotrac.LAYOUT.withName("gsfSBEchotracSpecific"),
        SBEchotrac.LAYOUT.withName("gsfSBBathy2000Specific"),
        SBMGD77.LAYOUT.withName("gsfSBMGD77Specific"),
        SBBDB.LAYOUT.withName("gsfSBBDBSpecific"),
        SBNOSHDB.LAYOUT.withName("gsfSBNOSHDBSpecific"),
        SBEchotrac.LAYOUT.withName("gsfSBPDDSpecific"),
        SBNavisound.LAYOUT.withName("gsfSBNavisoundSpecific")
    ).withName("t_gsfSensorSpecific");

    SensorSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }
}
