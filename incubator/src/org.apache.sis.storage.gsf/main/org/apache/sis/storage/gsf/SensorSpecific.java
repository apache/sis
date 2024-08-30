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
import org.apache.sis.storage.gsf.specific.CmpSassSpecific;
import org.apache.sis.storage.gsf.specific.DeltaTSpecific;
import org.apache.sis.storage.gsf.specific.EM100Specific;
import org.apache.sis.storage.gsf.specific.EM121ASpecific;
import org.apache.sis.storage.gsf.specific.EM12Specific;
import org.apache.sis.storage.gsf.specific.EM3RawSpecific;
import org.apache.sis.storage.gsf.specific.EM3Specific;
import org.apache.sis.storage.gsf.specific.EM4Specific;
import org.apache.sis.storage.gsf.specific.EM950Specific;
import org.apache.sis.storage.gsf.specific.ElacMkIISpecific;
import org.apache.sis.storage.gsf.specific.GeoSwathPlusSpecific;
import org.apache.sis.storage.gsf.specific.KMALLSpecific;
import org.apache.sis.storage.gsf.specific.Klein5410BssSpecific;
import org.apache.sis.storage.gsf.specific.R2SonicSpecific;
import org.apache.sis.storage.gsf.specific.Reson7100Specific;
import org.apache.sis.storage.gsf.specific.Reson8100Specific;
import org.apache.sis.storage.gsf.specific.ResonTSeriesSpecific;
import org.apache.sis.storage.gsf.specific.SBAmpSpecific;
import org.apache.sis.storage.gsf.specific.SBBDBSpecific;
import org.apache.sis.storage.gsf.specific.SBEchotracSpecific;
import org.apache.sis.storage.gsf.specific.SBMGD77Specific;
import org.apache.sis.storage.gsf.specific.SBNOSHDBSpecific;
import org.apache.sis.storage.gsf.specific.SBNavisoundSpecific;
import org.apache.sis.storage.gsf.specific.SeaBat8101Specific;
import org.apache.sis.storage.gsf.specific.SeaBatIISpecific;
import org.apache.sis.storage.gsf.specific.SeaBatSpecific;
import org.apache.sis.storage.gsf.specific.SeaBeam2112Specific;
import org.apache.sis.storage.gsf.specific.SeabeamSpecific;
import org.apache.sis.storage.gsf.specific.SeamapSpecific;
import org.apache.sis.storage.gsf.specific.TypeIIISpecific;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SensorSpecific extends StructClass{

    public static final GroupLayout LAYOUT = MemoryLayout.unionLayout(
        SeabeamSpecific.LAYOUT.withName("gsfSeaBeamSpecific"),
        EM100Specific.LAYOUT.withName("gsfEM100Specific"),
        EM121ASpecific.LAYOUT.withName("gsfEM121ASpecific"),
        EM121ASpecific.LAYOUT.withName("gsfEM121Specific"),
        SeaBatSpecific.LAYOUT.withName("gsfSeaBatSpecific"),
        EM950Specific.LAYOUT.withName("gsfEM950Specific"),
        EM950Specific.LAYOUT.withName("gsfEM1000Specific"),
        SeamapSpecific.LAYOUT.withName("gsfSeamapSpecific"),
        TypeIIISpecific.LAYOUT.withName("gsfTypeIIISeaBeamSpecific"),
        TypeIIISpecific.LAYOUT.withName("gsfSASSSpecific"),
        CmpSassSpecific.LAYOUT.withName("gsfCmpSassSpecific"),
        SBAmpSpecific.LAYOUT.withName("gsfSBAmpSpecific"),
        SeaBatIISpecific.LAYOUT.withName("gsfSeaBatIISpecific"),
        SeaBat8101Specific.LAYOUT.withName("gsfSeaBat8101Specific"),
        SeaBeam2112Specific.LAYOUT.withName("gsfSeaBeam2112Specific"),
        ElacMkIISpecific.LAYOUT.withName("gsfElacMkIISpecific"),
        EM3Specific.LAYOUT.withName("gsfEM3Specific"),
        EM3RawSpecific.LAYOUT.withName("gsfEM3RawSpecific"),
        Reson8100Specific.LAYOUT.withName("gsfReson8100Specific"),
        Reson7100Specific.LAYOUT.withName("gsfReson7100Specific"),
        ResonTSeriesSpecific.LAYOUT.withName("gsfResonTSeriesSpecific"),
        EM4Specific.LAYOUT.withName("gsfEM4Specific"),
        GeoSwathPlusSpecific.LAYOUT.withName("gsfGeoSwathPlusSpecific"),
        Klein5410BssSpecific.LAYOUT.withName("gsfKlein5410BssSpecific"),
        DeltaTSpecific.LAYOUT.withName("gsfDeltaTSpecific"),
        EM12Specific.LAYOUT.withName("gsfEM12Specific"),
        R2SonicSpecific.LAYOUT.withName("gsfR2SonicSpecific"),
        KMALLSpecific.LAYOUT.withName("gsfKMALLSpecific"),
        SBEchotracSpecific.LAYOUT.withName("gsfSBEchotracSpecific"),
        SBEchotracSpecific.LAYOUT.withName("gsfSBBathy2000Specific"),
        SBMGD77Specific.LAYOUT.withName("gsfSBMGD77Specific"),
        SBBDBSpecific.LAYOUT.withName("gsfSBBDBSpecific"),
        SBNOSHDBSpecific.LAYOUT.withName("gsfSBNOSHDBSpecific"),
        SBEchotracSpecific.LAYOUT.withName("gsfSBPDDSpecific"),
        SBNavisoundSpecific.LAYOUT.withName("gsfSBNavisoundSpecific")
    ).withName("t_gsfSensorSpecific");

    public SensorSpecific(MemorySegment struct) {
        super(struct);
    }

    public SensorSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

}
