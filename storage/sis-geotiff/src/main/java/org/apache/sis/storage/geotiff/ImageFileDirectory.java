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
package org.apache.sis.storage.geotiff;

import java.util.Locale;
import java.io.IOException;
import static java.lang.Math.pow;
import java.net.MalformedURLException;
import java.text.ParseException;
import static java.util.Collections.singleton;
import java.util.Date;
import org.apache.sis.geometry.DirectPosition2D;
import org.opengis.metadata.citation.DateType;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * An Image File Directory (FID) in a TIFF image.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a>
 */
final class ImageFileDirectory {
    private String ORIGIN = "Image courtesy of the U.S.Geological Survey";
    private String TYPE= "L1 production";
    /**
     * {@code true} if this {@code ImageFileDirectory} has not yet read all deferred entries.
     * When this flag is {@code true}, the {@code ImageFileDirectory} is not yet ready for use.
     */
    boolean hasDeferredEntries;

    /**
     * The size of the image described by this FID, or -1 if the information has not been found.
     * The image may be much bigger than the memory capacity, in which case the image shall be tiled.
     */
    private long imageWidth = -1, imageHeight = -1;

    /**
     * The size of each tile, or -1 if the information has not be found.
     * Tiles should be small enough for fitting in memory.
     */
    private int tileWidth = -1, tileHeight = -1;

    private int samplesPerPixel = 1;

    /**
     * If {@code true}, the components are stored in separate “component planes”.
     * The default is {@code false}, which stands for the "chunky" format
     * (for example RGB data stored as RGBRGBRGB).
     */
    private boolean isPlanar;

    /**
     * The compression method, or {@code null} if unknown. If the compression method is unknown
     * or unsupported we can not read the image, but we still can read the metadata.
     */
    private Compression compression;
    private int[] ModelTiePoint;
    private int RowsPerStrip;
    private int bitsPerSample;
    private int minSampleValue;
    private int maxSampleValue;
    private double[] PixelScaleTag;
    private int[] Directory;

    /**
     * Creates a new image file directory.
     */
    ImageFileDirectory() {
    }

    /**
     * Adds the value read from the current position in the given stream
     * for the entry identified by the given GeoTIFF tag.
     *
     * @param  reader   the input stream to read, the metadata and the character encoding.
     * @param  tag      the GeoTIFF tag to decode.
     * @param  type     the GeoTIFF type of the value to read.
     * @param  count    the number of values to read.
     * @return {@code null} on success, or the unrecognized value otherwise.
     * @throws IOException if an error occurred while reading the stream.
     * @throws ParseException if the value need to be parsed as date and the parsing failed.
     * @throws NumberFormatException if the value need to be parsed as number and the parsing failed.
     * @throws ArithmeticException if the value can not be represented in the expected Java type.
     * @throws IllegalArgumentException if a value which was expected to be a singleton is not.
     * @throws UnsupportedOperationException if the given type is {@link Type#UNDEFINED}.
     */
    Object addEntry(final Reader reader, final int tag, final Type type, final long count) throws IOException, ParseException {
        switch (tag) {

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Essential information for being able to read the image at least as grayscale.       ////
            ////    In Java2D, following information are needed for building the SampleModel.           ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * How the components of each pixel are stored.
             * 1 = Chunky format. The component values for each pixel are stored contiguously (for example RGBRGBRGB).
             * 2 = Planar format. For example one plane of Red components, one plane of Green and one plane if Blue.
             */
            case Tags.PlanarConfiguration: {
                final long value = type.readLong(reader.input, count);
                if (value < 1 || value > 2) {
                    return value;
                }
                isPlanar = (value == 2);
                break;
            }
            /*
             * The number of columns in the image, i.e., the number of pixels per row.
             */
            case Tags.ImageWidth: {
                imageWidth = type.readUnsignedLong(reader.input, count);
                break;
            }
            /*
             * The number of rows of pixels in the image.
             */
            case Tags.ImageLength: {
                imageHeight = type.readUnsignedLong(reader.input, count);
                break;
            }
            /*
             * The number of rows per strip. RowsPerStrip and ImageLength together tell us the number of strips
             * in the entire image: StripsPerImage = floor((ImageLength + RowsPerStrip - 1) / RowsPerStrip).
             */
            case Tags.RowsPerStrip: {
                RowsPerStrip = (int) type.readUnsignedLong(reader.input, count); 
                break;
            }
            /*
             * For each strip, the number of bytes in the strip after compression.
             */
            case Tags.StripByteCounts: {
                // TODO
                break;
            }
            /*
             * For each strip, the byte offset of that strip relative to the beginning of the TIFF file.
             */
            case Tags.StripOffsets: {
                // TODO
                break;
            }
            /*
             * Compression scheme used on the image data.
             */
            case Tags.Compression: {
                final long value = type.readLong(reader.input, count);
                compression = Compression.valueOf(value);
                if (compression == null) {
                    return value;
                }
                break;
            }
            /*
             * The logical order of bits within a byte. If this value is 2, then bits order shall be reversed in every
             * bytes before decompression.
             */
            case Tags.FillOrder: {
                // TODO
                break;
            }
            /*
             * Number of bits per component. The array length should be the number of components in a
             * pixel (e.g. 3 for RGB values). Typically, all components have the same number of bits.
             * But the TIFF specification allows different values.
             */
            case Tags.BitsPerSample: {
                final long value = type.readLong(reader.input, count); 
                bitsPerSample = (int) value; 
                if (bitsPerSample == -1) { 
                    return value; 
                } 
                break;
            }
            /*
             * The number of components per pixel. Ssually 1 for bilevel, grayscale, and palette-color images,
             * and 3 for RGB images. Default value is 1.
             */
            case Tags.SamplesPerPixel: {
                final long value = type.readLong(reader.input, count); 
                samplesPerPixel = (int) value; 
                
                break;
            }
            /*
             * Specifies that each pixel has N extra components. When this field is used, the SamplesPerPixel field
             * has a value greater than the PhotometricInterpretation field suggests. For example, a full-color RGB
             * image normally has SamplesPerPixel=3. If SamplesPerPixel is greater than 3, then the ExtraSamples field
             * describes the meaning of the extra samples. It may be an alpha channel, but not necessarily.
             */
            case Tags.ExtraSamples: {
                double[] num = null; 
                int i = 0; 
                for (final double value : type.readDoubles(reader.input, count)) { 
                    num[i] = value; 
                    i++; 
                } 
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information related to the color palette or the meaning of sample values.           ////
            ////    In Java2D, following information are needed for building the ColorModel.            ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * The color space of the image data.
             * 0 = WhiteIsZero. For bilevel and grayscale images: 0 is imaged as white.
             * 1 = BlackIsZero. For bilevel and grayscale images: 0 is imaged as black.
             * 2 = RGB. RGB value of (0,0,0) represents black, and (255,255,255) represents white.
             * 3 = Palette color. The value of the component is used as an index into the RGB valuesthe ColorMap.
             * 4 = Transparency Mask the defines an irregularly shaped region of another image in the same TIFF file.
             */
            case Tags.PhotometricInterpretation: {
                // TODO
                break;
            }
            /*
             * The Red-Green-Blue lookup table map for palette-color images (IndexColorModel in Java2D).
             * All the Red values come first, followed by the Green values, then the Blue values.
             * The number of values for each color is (1 << BitsPerSample) and the type of each value is USHORT.
             * 0 represents the minimum intensity (black is 0,0,0) and 65535 represents the maximum intensity.
             * If the Indexed tag has value 1 and PhotometricInterpretation is different from PaletteColor,
             * then the color space may be different than RGB.
             */
            case Tags.ColorMap: {
                // TODO
                break;
            }
            /*
             * The minimum component value used. Default is 0.
             */
            case Tags.MinSampleValue: {
                for (final double value : type.readDoubles(reader.input, count)) { 
                    if (value != 0) { 
                        minSampleValue = (int) value; 
                    } 
                } 
                reader.metadata.addMinimumSampleValue(type.readDouble(reader.input, count));
                break;
            }
            /*
             * The maximum component value used. Default is {@code (1 << BitsPerSample) - 1}.
             * This field is for statistical purposes and should not to be used to affect the
             * visual appearance of an image, unless a map styling is applied.
             */
            case Tags.MaxSampleValue: {
                for (final double value : type.readDoubles(reader.input, count)) { 
                    if (value != 0) { 
                        maxSampleValue = (int) value; 
                    } 
                } 
                reader.metadata.addMaximumSampleValue(type.readDouble(reader.input, count));
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information useful for defining the image role in a multi-images context.           ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * A general indication of the kind of data contained in this subfile, mainly useful when there
             * are multiple subfiles in a single TIFF file. This field is made up of a set of 32 flag bits.
             *
             * Bit 0 is 1 if the image is a reduced-resolution version of another image in this TIFF file.
             * Bit 1 is 1 if the image is a single page of a multi-page image (see PageNumber).
             * Bit 2 is 1 if the image defines a transparency mask for another image in this TIFF file (see PhotometricInterpretation).
             * Bit 4 indicates MRC imaging model as described in ITU-T recommendation T.44 [T.44] (See ImageLayer tag) - RFC 2301.
             */
            case Tags.NewSubfileType: {
                // TODO
                break;
            }
            /*
             * Old version (now deprecated) of above NewSubfileType.
             * 1 = full-resolution image data
             * 2 = reduced-resolution image data
             * 3 = a single page of a multi-page image (see PageNumber).
             */
            case Tags.SubfileType: {
                // TODO
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Information related to the Coordinate Reference System and the bounding box.        ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * The orientation of the image with respect to the rows and columns.
             * This is an integer numeroted from 1 to 7 inclusive (see TIFF specification for meaning).
             */
            case Tags.Orientation: {
                // TODO
                break;
            }


            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Metadata for discovery purposes, conditions of use, etc.                            ////
            ////    Those metadata are not "critical" information for reading the image.                ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * A string that describes the subject of the image.
             * For example, a user may wish to attach a comment such as "1988 company picnic" to an image.
             */
            case Tags.ImageDescription: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addTitle(value);
                }
                break;
            }
            /*
             * Person who created the image. Some older TIFF files used this tag for storing
             * Copyright information, but Apache SIS does not support this legacy practice.
             */
            case Tags.Artist: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.addAuthor(value);
                }
                break;
            }
            /*
             * Copyright notice of the person or organization that claims the copyright to the image.
             * Example: “Copyright, John Smith, 1992. All rights reserved.”
             */
            case Tags.Copyright: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.parseLegalNotice(value);
                }
                break;
            }
            /*
             * Date and time of image creation. The format is: "YYYY:MM:DD HH:MM:SS" with 24-hour clock.
             */
            case Tags.DateTime: {
                for (final String value : type.readString(reader.input, count, reader.owner.encoding)) {
                    reader.metadata.add(reader.getDateFormat().parse(value), DateType.CREATION);
                }
                break;
            }
            /*
             * The computer and/or operating system in use at the time of image creation.
             */
            case Tags.HostComputer: {
                // TODO
                break;
            }
            /*
             * Name and version number of the software package(s) used to create the image.
             */
            case Tags.Software: {
                // TODO
                break;
            }
            /*
             * Manufacturer of the scanner, video digitizer, or other type of equipment used to generate the image.
             * Synthetic images should not include this field.
             */
            case Tags.Make: {
                // TODO
                break;
            }
            /*
             * The model name or number of the scanner, video digitizer, or other type of equipment used to
             * generate the image.
             */
            case Tags.Model: {
                // TODO
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageWidth direction.
             */
            case Tags.XResolution: {
                // TODO
                break;
            }
            /*
             * The number of pixels per ResolutionUnit in the ImageLength direction.
             */
            case Tags.YResolution: {
                // TODO
                break;
            }
            /*
             * The unit of measurement for XResolution and YResolution.
             * 1 = None, 2 = Inch, 3 = Centimeter.
             */
            case Tags.ResolutionUnit: {
                // TODO
                break;
            }
            /*
             * The technique used to convert from gray to black and white pixels (if applicable):
             * 1 = No dithering or halftoning has been applied to the image data.
             * 2 = An ordered dither or halftone technique has been applied to the image data.
             * 3 = A randomized process such as error diffusion has been applied to the image data.
             */
            case Tags.Threshholding: {
                // TODO
                break;
            }
            /*
             * The width of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellWidth: {
                // TODO
                break;
            }
            /*
             * The height of the dithering or halftoning matrix used to create a dithered or halftoned
             * bilevel file. Meaningful only if Threshholding = 2.
             */
            case Tags.CellLength: {
                // TODO
                break;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////
            ////                                                                                        ////
            ////    Defined by TIFF specification but currently ignored.                                ////
            ////                                                                                        ////
            ////////////////////////////////////////////////////////////////////////////////////////////////

            /*
             * For each string of contiguous unused bytes in a TIFF file, the number of bytes and the byte offset
             * in the string. Those tags are deprecated and do not need to be supported.
             */
            case Tags.FreeByteCounts:
            case Tags.FreeOffsets:
            /*
             * For grayscale data, the optical density of each possible pixel value, plus the precision of that
             * information. This is ignored by most TIFF readers.
             */
            case Tags.GrayResponseCurve:
            case Tags.GrayResponseUnit: {
                // TODO: log a warning saying that this tag is ignored.
                break;
            }
            case Tags.ModelTiePoint: {
                ModelTiePoint = type.readInts(reader.input, count);
            }
            break;
            case Tags.GeoKeyDirectoryTag: {
                Directory = type.readInts(reader.input, count) ;
                    

            }
            break;
            case Tags.GeoAsciiParamsTag: {
                
            }
            break;
            case Tags.ModelPixelScaleTag: {
                PixelScaleTag = type.readDoubles(reader.input, count);
            }
            break;
        }
        return null;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys, or {@code NaN} if none.
     *
     * @param key1 the key for which to get the first floating-point value.
     * @param key2 the key for which to get the second floating-point value.
     * @param  max   {@code true} for the maximal value, or {@code false} for the minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max} is true) floating-point value
     *         associated to the given keys, or {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to one of the given keys can not be parsed
     *         as a floating-point number.
     */
    private double getExtremumValue(double key1, double key2, boolean max) throws NumberFormatException {
        if (max ? (key2 > key1) : (key2 < key1)) {
            return key2;
        } else {
            return key1;
        }
    }
    /**
     * Return the value use for bounding box
     * @return the value use for bounding box
     */
    public double[] getBoundingBox() throws FactoryException, MismatchedDimensionException, TransformException  {
        CoordinateReferenceSystem sourceCRS = CRS.forCode("EPSG:"+Directory[27]);
        CoordinateReferenceSystem targetCRS = CRS.forCode("CRS:84");
        CoordinateOperation op = CRS.findOperation(sourceCRS, targetCRS, null);
        double xscale  = PixelScaleTag[0];
        double yscale  = PixelScaleTag[1];
        double UL_x = ModelTiePoint[3];
        double UL_y = ModelTiePoint[4];
        double UR_x = UL_x + (xscale * (imageWidth-1));
        double UR_y = UL_y;
        double LL_x = UL_x;
        double LL_y = UL_y- (yscale * (imageHeight-1));
        double LR_x = UL_x + (xscale * (imageWidth-1));
        double LR_y = UL_y- (yscale * (imageHeight-1));
        double[] x = {UL_x,UR_x,LL_x,LR_x}; 
        double[] y = {UL_y,UR_y,LL_y,LR_y}; 
        double[] lat =new double[4] ;
        double[] lon =new double[4] ;
        
        for(int i =0; i< x.length;i++){
            DirectPosition sourcePt = new DirectPosition2D(x[i], y[i]);
            DirectPosition targetPt = op.getMathTransform().transform(sourcePt, null);
            lon[i] = targetPt.getCoordinate()[0];
            lat[i] = targetPt.getCoordinate()[1];
        }
        double[] bbox = new double[4];
        
        bbox[0]= getExtremumValue(lon[0],lon[2],false);//westBoundLongitude
        bbox[1]= getExtremumValue(lon[1],lon[3],true);//eastBoundLongitude
        bbox[2]= getExtremumValue(lat[2],lat[3],false);//southBoundLatitude
        bbox[3]= getExtremumValue(lat[0],lat[1],true);//northBoundLatitude
        return bbox;
    }
    /**
     * Completes the metadata with the information stored in the field of this IFD.
     * This method is invoked only if the user requested the ISO 19115 metadata.
     */
    final void completeMetadata(final MetadataBuilder metadata,Reader reader, final Locale locale) throws MalformedURLException, IOException, FactoryException, MismatchedDimensionException, TransformException {
        if (compression != null) {
            metadata.addCompression(compression.name().toLowerCase(locale),reader.file.toURL().openConnection().getContentType());
        }
            metadata.add(new Date(reader.file.lastModified()), DateType.CREATION); 
            metadata.addTitle(reader.file.getName()); 
            final String[] b = reader.file.getName().split("_"); 
            metadata.addIdentifier(b[0]); 
            if (maxSampleValue == 0) { 
                maxSampleValue = (int) (pow(2, bitsPerSample) - 1); 
            }
            metadata.addAuthor(ORIGIN);
            metadata.addMaximumSampleValue(maxSampleValue); 
            metadata.addMinimumSampleValue(minSampleValue); 
            double[] box = getBoundingBox();
            metadata.addBoundingBox(box[0], box[1], box[2], box[3]);
            metadata.addLanguage(Locale.ENGLISH);
            metadata.addScopeCode(singleton(ScopeCode.DATASET));
            metadata.addStandard();
    }
    
}
