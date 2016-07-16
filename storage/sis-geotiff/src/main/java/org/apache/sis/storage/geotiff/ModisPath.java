/*
 * Copyright 2016 haonguyen.
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
package org.apache.sis.storage.geotiff;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author haonguyen
 */
public class ModisPath {

    /**
     * Do not allow instantiation of this class.
     */
    ModisPath() {
    }

    ////
    //// GROUP = METADATA_FILE_INFO
    ////
    static final String DataCenterId = "/GranuleMetaDataFile/DataCenterId";
    static final String ProductionDateTime = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/ProductionDateTime";
    static final String UL_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[1]/PointLongitude";
    static final String UL_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[1]/PointLatitude";
    static final String UR_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[2]/PointLongitude";
    static final String UR_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[2]/PointLatitude";
    static final String LL_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[3]/PointLongitude";
    static final String LL_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[3]/PointLatitude";
    static final String LR_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[4]/PointLongitude";
    static final String LR_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[4]/PointLatitude";
    static final String ReprocessingActual = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/ReprocessingActual";
    static final String ShortName = "/GranuleMetaDataFile/GranuleURMetaData/CollectionMetaData/ShortName";
    static final String ChecksumOrigin = "/GranuleMetaDataFile/GranuleURMetaData/DataFiles/DataFileContainer/ChecksumOrigin";
    static final String DistributedFileName = "/GranuleMetaDataFile/GranuleURMetaData/DataFiles/DataFileContainer/DistributedFileName";
    static final String LocalGranuleID = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/LocalGranuleID";
    static final String PlatformShortName = "/GranuleMetaDataFile/GranuleURMetaData/Platform/PlatformShortName";
    static final String InputGranule = "DataFileContainer";

    List<String> path() {
        List<String> a = new ArrayList<>();
        a.add(DataCenterId);
        a.add(ProductionDateTime);
        a.add(UL_LON);
        a.add(UL_LAT);
        a.add(UR_LON);
        a.add(UR_LAT);
        a.add(LL_LON);
        a.add(LL_LAT);
        a.add(LR_LON);
        a.add(LR_LAT);
        a.add(ShortName);
        a.add(ChecksumOrigin);
        a.add(DistributedFileName);
        a.add(LocalGranuleID);
        a.add(PlatformShortName);
        a.add(InputGranule);
        a.add(ReprocessingActual);
        

        return a;
    }

    public static void main(String[] args) {
        ModisPath path = new ModisPath();
        System.out.println(path.path());
    }
}
