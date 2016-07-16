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
    /**
     * Path for Data Center Id.
     */
    static final String DataCenterId = "/GranuleMetaDataFile/DataCenterId";
    /**
     * Path for Production Date time.
     */
    static final String ProductionDateTime = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/ProductionDateTime";
    /**
     * Path for GRingPointLongtitude [1].
     */
    static final String UL_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[1]/PointLongitude";
    /**
     * Path for GRingPointLatitude [1].
     */
    static final String UL_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[1]/PointLatitude";
    /**
     * Path for GRingPointLongtitude [2].
     */
    static final String UR_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[2]/PointLongitude";
    /**
     * Path for GRingPointLatitude [2].
     */
    static final String UR_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[2]/PointLatitude";
    /**
     * Path for GRingPointLongtitude [3].
     */
    static final String LL_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[3]/PointLongitude";
    /**
     * Path for GRingPointLatitude [3].
     */
    static final String LL_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[3]/PointLatitude";
    /**
     * Path for GRingPointLongtitude [4].
     */
    static final String LR_LON = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[4]/PointLongitude";
    /**
     * Path for GRingPointLatitude [4].
     */
    static final String LR_LAT = "/GranuleMetaDataFile/GranuleURMetaData/SpatialDomainContainer/HorizontalSpatialDomainContainer/GPolygon/Boundary/Point[4]/PointLatitude";
    /**
     * Path for reprocessing Actual.
     */
    static final String ReprocessingActual = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/ReprocessingActual";
    /**
     * Path for Short Name for metadata.
     */
    static final String ShortName = "/GranuleMetaDataFile/GranuleURMetaData/CollectionMetaData/ShortName";
    /**
     * Path for Check sum origin.
     */
    static final String ChecksumOrigin = "/GranuleMetaDataFile/GranuleURMetaData/DataFiles/DataFileContainer/ChecksumOrigin";
    /**
     * Path for Distribute file name.
     */
    static final String DistributedFileName = "/GranuleMetaDataFile/GranuleURMetaData/DataFiles/DataFileContainer/DistributedFileName";
    /**
     * Path for Local GranuleId.
     */
    static final String LocalGranuleID = "/GranuleMetaDataFile/GranuleURMetaData/ECSDataGranule/LocalGranuleID";
    /**
     * Path for PlatformShortname.
     */
    static final String PlatformShortName = "/GranuleMetaDataFile/GranuleURMetaData/Platform/PlatformShortName";
    /**
     * Path for Input Granule.
     */
    static final String InputGranule = "DataFileContainer";

    /**
     * List all the path use to mapping metadata
     *
     * @return all the path use to mapping metadata
     */
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
