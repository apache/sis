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
package org.apache.sis.storage.isobmff.base;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ISO14496_12 implements BoxRegistry {

    private static final Set<String> BOXES = Set.of(
            FileType.FCC,
            ExtendedType.FCC,
            CombinaisonType.FCC,
            OriginalFileType.FCC,
            MediaData.FCC,
            FreeSpace.FCC,
            FreeSpace.FCC_FREE,
            ProgressiveDownloadInfo.FCC,
            IdentifiedMediaData.FCC,
            Movie.FCC,
            MovieHeader.FCC,
            Track.FCC,
            TrackHeader.FCC,
            Meta.FCC,
            HandlerReference.FCC,
            GroupList.FCC,
            PrimaryItem.FCC,
            ItemInfo.FCC,
            ItemInfoEntry.FCC,
            ItemProperties.FCC,
            ItemLocation.FCC,
            ItemData.FCC,
            ItemReference.FCC,
            FDItemInfoExtension.FCC,
            ItemPropertyContainer.FCC,
            ItemPropertyAssociation.FCC,
            ColourInformation.FCC
        );
    private static final Set<String> EXTENSIONS = Set.of();

    @Override
    public String getName() {
        return "ISO-14496-12";
    }

    @Override
    public Set<String> getBoxesFourCC() {
        return BOXES;
    }

    @Override
    public Set<String> getExtensionUUIDs() {
        return EXTENSIONS;
    }

    @Override
    public Box create(String fourCC) throws IllegalNameException {
        //TODO replace by String switch when SIS minimum java is updated
        if (FileType.FCC.equals(fourCC)) return new FileType();
        else if (ExtendedType.FCC.equals(fourCC)) return new ExtendedType();
        else if (CombinaisonType.FCC.equals(fourCC)) return new CombinaisonType();
        else if (OriginalFileType.FCC.equals(fourCC)) return new OriginalFileType();
        else if (MediaData.FCC.equals(fourCC)) return new MediaData();
        else if (FreeSpace.FCC_FREE.equals(fourCC)) return new FreeSpace();
        else if (FreeSpace.FCC.equals(fourCC)) return new FreeSpace();
        else if (ProgressiveDownloadInfo.FCC.equals(fourCC)) return new ProgressiveDownloadInfo();
        else if (IdentifiedMediaData.FCC.equals(fourCC)) return new IdentifiedMediaData();
        else if (Movie.FCC.equals(fourCC)) return new Movie();
        else if (MovieHeader.FCC.equals(fourCC)) return new MovieHeader();
        else if (Track.FCC.equals(fourCC)) return new Track();
        else if (TrackHeader.FCC.equals(fourCC)) return new TrackHeader();
        else if (Meta.FCC.equals(fourCC)) return new Meta();
        else if (HandlerReference.FCC.equals(fourCC)) return new HandlerReference();
        else if (GroupList.FCC.equals(fourCC)) return new GroupList();
        else if (PrimaryItem.FCC.equals(fourCC)) return new PrimaryItem();
        else if (ItemInfo.FCC.equals(fourCC)) return new ItemInfo();
        else if (ItemInfoEntry.FCC.equals(fourCC)) return new ItemInfoEntry();
        else if (ItemProperties.FCC.equals(fourCC)) return new ItemProperties();
        else if (ItemLocation.FCC.equals(fourCC)) return new ItemLocation();
        else if (ItemData.FCC.equals(fourCC)) return new ItemData();
        else if (ItemReference.FCC.equals(fourCC)) return new ItemReference();
        else if (FDItemInfoExtension.FCC.equals(fourCC)) return new FDItemInfoExtension();
        else if (ItemPropertyContainer.FCC.equals(fourCC)) return new ItemPropertyContainer();
        else if (ItemPropertyAssociation.FCC.equals(fourCC)) return new ItemPropertyAssociation();
        else if (ColourInformation.FCC.equals(fourCC)) return new ColourInformation();
        throw new IllegalNameException();
    }

    @Override
    public Box createExtension(String uuid) throws IllegalNameException {
        throw new IllegalNameException();
    }

}
