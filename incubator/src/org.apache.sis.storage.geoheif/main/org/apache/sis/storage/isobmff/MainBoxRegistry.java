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
package org.apache.sis.storage.isobmff;

import java.util.UUID;
import java.util.ServiceLoader;
import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.isobmff.base.*;
import org.apache.sis.storage.isobmff.video.*;
import org.apache.sis.storage.isobmff.image.*;
import org.apache.sis.storage.isobmff.mpeg.*;
import org.apache.sis.storage.isobmff.gimi.*;


/**
 * The box registry used as a starting point.
 * This registry handles all boxes of types known to this module,
 * and dispatches unknown types to user-provided registries.
 *
 * <p><b>Note:</b> we do not make this registry accessible to users, neither as a static method
 * or through service loader, for avoiding never-ending recursive calls when a {@code create(…)}
 * method delegates to a user-specified registry.</p>
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class MainBoxRegistry extends BoxRegistry {
    /**
     * The unique instance.
     */
    static final MainBoxRegistry INSTANCE = new MainBoxRegistry();

    /**
     * User-specified registries.
     */
    private final Iterable<BoxRegistry> registries;

    /**
     * Creates the unique instance.
     */
    private MainBoxRegistry() {
        registries = ServiceLoader.load(BoxRegistry.class);
    }

    /**
     * Creates a new box for the given box type.
     * Unknown types are ignored.
     *
     * @param  reader  the reader from which to read bytes.
     * @param  fourCC  four-character code identifying the box type.
     * @return box, or {@code null} if the given type is not recognized by this registry.
     * @throws IOException if an error occurred while reading the box content.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws ArrayIndexOutOfBoundsException if a box does not have the expected number of elements.
     * @throws UnsupportedVersionException if the box declare an unsupported version number.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the box creation failed for another reason.
     */
    @Override
    public Box create(final Reader reader, final int fourCC) throws IOException, DataStoreException {
        switch (fourCC) {
            case ChromaLocation.BOXTYPE:                return new ChromaLocation(reader);
            case ColourInformation.BOXTYPE:             return new ColourInformation(reader);
            case CombinaisonType.BOXTYPE:               return new CombinaisonType(reader);
            case ComponentDefinition.BOXTYPE:           return reader.componentDefinition = new ComponentDefinition(reader);
            case ComponentPalette.BOXTYPE:              return new ComponentPalette(reader, reader.componentDefinition);
            case ComponentPatternDefinition.BOXTYPE:    return new ComponentPatternDefinition(reader, reader.componentDefinition);
            case ComponentReferenceLevel.BOXTYPE:       return new ComponentReferenceLevel(reader);
            case ContentDescribes.BOXTYPE:              return new ContentDescribes(reader);
            case Copyright.BOXTYPE:                     return new Copyright(reader);
            case CreationTime.BOXTYPE:                  return new CreationTime(reader);
            case DepthMappingInformation.BOXTYPE:       return new DepthMappingInformation(reader);
            case DerivedImageReference.BOXTYPE:         return new DerivedImageReference(reader);
            case DisparityInformation.BOXTYPE:          return new DisparityInformation(reader);
            case ExtendedType.BOXTYPE:                  return new ExtendedType(reader);
            case FDItemInfoExtension.BOXTYPE:           return new FDItemInfoExtension(reader);
            case FieldInterlace.BOXTYPE:                return new FieldInterlace(reader);
            case FieldInterlaceType.BOXTYPE:            return new FieldInterlaceType(reader);
            case FileType.BOXTYPE:                      return new FileType(reader);
            case FramePackingInformation.BOXTYPE:       return new FramePackingInformation(reader);
            case FreeSpace.SKIP:                        // Fall through
            case FreeSpace.BOXTYPE:                     return new FreeSpace();
            case GroupList.BOXTYPE:                     return new GroupList(reader);
            case HandlerReference.BOXTYPE:              return new HandlerReference(reader);
            case IdentifiedMediaData.BOXTYPE:           return new IdentifiedMediaData(reader);
            case ImagePyramid.BOXTYPE:                  return new ImagePyramid(reader);
            case ImageSpatialExtents.BOXTYPE:           return new ImageSpatialExtents(reader);
            case ItemData.BOXTYPE:                      return new ItemData(reader);
            case ItemInfo.BOXTYPE:                      return new ItemInfo(reader);
            case ItemInfoEntry.BOXTYPE:                 return new ItemInfoEntry(reader);
            case ItemLocation.BOXTYPE:                  return new ItemLocation(reader);
            case ItemProperties.BOXTYPE:                return new ItemProperties(reader);
            case ItemPropertyAssociation.BOXTYPE:       return new ItemPropertyAssociation(reader);
            case ItemPropertyContainer.BOXTYPE:         return new ItemPropertyContainer(reader);
            case ItemReference.BOXTYPE:                 return new ItemReference(reader);
            case MediaData.BOXTYPE:                     return new MediaData(reader);
            case Meta.BOXTYPE:                          return new Meta(reader);
            case ModelCRS.BOXTYPE:                      return new ModelCRS(reader);
            case ModelTiePoint.BOXTYPE:                 return new ModelTiePoint(reader);
            case ModelTransformation.BOXTYPE:           return new ModelTransformation(reader);
            case ModificationTime.BOXTYPE:              return new ModificationTime(reader);
            case Movie.BOXTYPE:                         return new Movie(reader);
            case MovieHeader.BOXTYPE:                   return reader.movieHeader = new MovieHeader(reader);
            case OriginalFileType.BOXTYPE:              return new OriginalFileType(reader);
            case PixelInformation.BOXTYPE:              return new PixelInformation(reader);
            case PolarizationPatternDefinition.BOXTYPE: return new PolarizationPatternDefinition(reader);
            case PrimaryItem.BOXTYPE:                   return new PrimaryItem(reader);
            case ProgressiveDownloadInfo.BOXTYPE:       return new ProgressiveDownloadInfo(reader);
            case SensorBadPixelsMap.BOXTYPE:            return new SensorBadPixelsMap(reader);
            case SensorNonUniformityCorrection.BOXTYPE: return new SensorNonUniformityCorrection(reader);
            case TAIClockInfo.BOXTYPE:                  return new TAIClockInfo(reader);
            case TAITimeStamp.BOXTYPE:                  return new TAITimeStamp(reader);
            case TiledImageConfiguration.BOXTYPE:       return new TiledImageConfiguration(reader);
            case Track.BOXTYPE:                         return new Track(reader);
            case TrackHeader.BOXTYPE:                   return new TrackHeader(reader, reader.movieHeader);
            case UncompressedFrameConfig.BOXTYPE:       return new UncompressedFrameConfig(reader, reader.componentDefinition);
            case UserData.BOXTYPE:                      return new UserData(reader);
            case UserDescription.BOXTYPE:               return new UserDescription(reader);
            case Extension.BOXTYPE: {
                final ChannelDataInput input = reader.input;
                return create(reader, new UUID(input.readLong(), input.readLong()));
            }
        }
        // Search in user-specified registries.
        for (BoxRegistry registry : registries) {
            Box candidate = registry.create(reader, fourCC);
            if (candidate != null) {
                return candidate;
            }
        }
        return super.create(reader, fourCC);
    }

    /**
     * Creates a new box for the given extension identifier.
     * This method is invoked when the box type is {@link Extension#BOXTYPE}.
     * Unknown identifiers are ignored.
     *
     * @param  reader     the reader from which to read bytes.
     * @param  extension  identifier of user-defined box.
     * @return box, or {@code null} if the given identifier is not recognized by this registry.
     * @throws IOException if an error occurred while reading the box content.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws ArrayIndexOutOfBoundsException if a box does not have the expected number of elements.
     * @throws UnsupportedVersionException if the box declare an unsupported version number.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the box creation failed for another reason.
     */
    @Override
    public Box create(final Reader reader, final UUID extension) throws IOException, DataStoreException {
        switch ((int) extension.getMostSignificantBits()) {
            case (int) UnknownProperty.UUID_HIGH_BITS: {
                if (extension.equals(UnknownProperty.EXTENDED_TYPE)) {
                    return new UnknownProperty(reader);
                }
                break;
            }
        }
        // Search in user-specified registries.
        for (BoxRegistry registry : registries) {
            Box candidate = registry.create(reader, extension);
            if (candidate != null) {
                return candidate;
            }
        }
        return super.create(reader, extension);
    }
}
