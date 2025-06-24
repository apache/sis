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
package org.apache.sis.storage.gdal;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.LibraryLoader;
import org.apache.sis.storage.panama.LibraryStatus;
import org.apache.sis.storage.panama.NativeFunctions;
import org.apache.sis.storage.panama.Resources;


/**
 * Handlers to <abbr>GDAL</abbr> native functions needed by this package.
 * The <abbr>GDAL</abbr> library can be unloaded by invoking {@link #run()}.
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://gdal.org/en/latest/api/raster_c_api.html">GDAL Raster C API</a>
 */
final class GDAL extends NativeFunctions {
    /**
     * The global instance, created when first needed.
     * This field shall be read and updated in a synchronized block.
     * It may be reset to {@code null} if <abbr>GDAL</abbr> reported a fatal error.
     *
     * @see #global(boolean)
     */
    private static GDAL global;

    /**
     * Whether an error occurred during initialization of {@link #global}.
     * Shall be read and updated in the same synchronization block as {@link #global}.
     */
    private static LibraryStatus globalStatus;

    /**
     * <abbr>GDAL</abbr> {@code const char *GDALGetDriverLongName(GDALDriverH)}.
     * Returns the long name of a driver. For the GeoTIFF driver, this is "GeoTIFF"
     */
    final MethodHandle getName;

    /**
     * <abbr>GDAL</abbr> {@code const char *GDALGetDriverShortName(GDALDriverH)}.
     * Returns the short name of a driver. This is the string that can be passed to the
     * {@code GDALGetDriverByName()} function. For the GeoTIFF driver, this is "GTiff".
     */
    final MethodHandle getIdentifier;

    /**
     * <abbr>GDAL</abbr> {@code char **GDALGetMetadata(GDALMajorObjectH, const char*)}.
     * Fetches all metadata in a domain. This function can be invoked on various kinds of <abbr>GDAL</abbr> objects.
     */
    final MethodHandle getMetadata;

    /**
     * <abbr>GDAL</abbr> {@code const char *GDALGetMetadataItem(GDALMajorObjectH, const char*, const char*)}.
     * Fetches a single metadata item. This function can be invoked on various kinds of <abbr>GDAL</abbr> objects.
     *
     * @see Driver#getMetadataItem(String, String)
     */
    final MethodHandle getMetadataItem;

    /**
     * <abbr>GDAL</abbr> {@code GDALDriverH GDALIdentifyDriver(const char *pszFilename, CSLConstList papszFileList)}.
     * Identify the driver that can open a dataset.
     */
    final MethodHandle identifyDriver;

    /**
     * <abbr>GDAL</abbr> {@code GDALDatasetH GDALOpenEx(const char *pszFilename, …)}.
     * Opens a raster or vector file by invoking the open method of each driver in turn.
     * Requires <abbr>GDAL</abbr> 2.0.
     */
    final MethodHandle open;

    /**
     * <abbr>GDAL</abbr> {@code CPLErr GDALClose(GDALDatasetH)}.
     * For non-shared {@code GDALStore}s (the default), the {@code GDALStore} is closed and all resources are released.
     * For {@linkplain OpenFlag#SHARED shared} {@code GDALStore}s, the {@code GDALStore} is dereferenced and closed
     * only if the referenced count has dropped below 1.
     */
    final MethodHandle close;

    /**
     * <abbr>GDAL</abbr> {@code void VSIFree(void*)}, alias {@code CPLFree}.
     * Releases memory allocated by <abbr>GDAL</abbr>.
     */
    final MethodHandle free;

    /**
     * <abbr>GDAL</abbr> {@code void CSLDestroy(char **papszStrList)}.
     * Releases memory allocated by <abbr>GDAL</abbr> for a string list.
     * It is safe to pass {@code NULL}.
     */
    final MethodHandle destroy;

    /**
     * <abbr>GDAL</abbr> {@code char **GDALGetFileList(GDALDatasetH)}.
     * Fetch files forming dataset.
     */
    final MethodHandle getFileList;

    /**
     * <abbr>GDAL</abbr> {@code GDALDriverH GDALGetDatasetDriver(GDALDatasetH)}.
     * Fetches the driver to which a dataset relates.
     */
    final MethodHandle getDatasetDriver;

    /**
     * <abbr>GDAL</abbr> {@code OGRSpatialReferenceH GDALGetSpatialRef(GDALDatasetH)}.
     * Fetches the spatial reference for this dataset in <abbr>WKT</abbr> format.
     * Requires <abbr>GDAL</abbr> 3.0.
     */
    final MethodHandle getSpatialRef, getGCPSpatialRef;

    /**
     * <abbr>GDAL</abbr> {@code OGRErr OSRExportToWktEx(OGRSpatialReferenceH, char** ppszResult, char** papszOption}.
     * Convert a <abbr>SRS</abbr> into <abbr>WKT</abbr> format.
     * The returned WKT string shall be freed with {@link #free}.
     * Requires <abbr>GDAL</abbr> 3.0.
     */
    final MethodHandle exportToWkt;

    /**
     * <abbr>GDAL</abbr> {@code int* OSRGetDataAxisToSRSAxisMapping(OGRSpatialReferenceH hSRS, int *pnCount)}.
     * Return the data axis to SRS axis mapping.
     * Requires <abbr>GDAL</abbr> 3.0.
     */
    final MethodHandle getDataAxisToCRSAxis;

    /**
     * <abbr>GDAL</abbr> {@code CPLErr GDALGetGeoTransform(GDALDatasetH, double*)}.
     * Fetches the affine transformation coefficients in an array of length 6.
     */
    final MethodHandle getGeoTransform;

    /**
     * <abbr>GDAL</abbr> {@code int GDALGetRasterX/YSize(GDALDatasetH)}.
     * Fetches raster width and height in pixels.
     */
    final MethodHandle getRasterXSize, getRasterYSize;

    /**
     * <abbr>GDAL</abbr> {@code int int GDALGetRasterBandX/YSize(GDALRasterBandH)}.
     * Fetches band width and height in pixels. Should have the same size as the raster,
     * except if the band has lower resolution.
     */
    final MethodHandle getRasterBandXSize, getRasterBandYSize;

    /**
     * <abbr>GDAL</abbr> {@code int GDALGetRasterCount(GDALDatasetH)}.
     * Fetches the number of raster bands on this dataset.
     */
    final MethodHandle getRasterCount;

    /**
     * <abbr>GDAL</abbr> {@code GDALRasterBandH GDALGetRasterBand(GDALDatasetH, int)}.
     * Fetches a band object for a dataset. Band index starts at 1 (not zero).
     */
    final MethodHandle getRasterBand;

    /**
     * <abbr>GDAL</abbr> {@code int GDALGetBandNumber(GDALRasterBandH)}.
     * Fetches the band number, starting at 1. A value 0 means that the
     * band is not directly part of the data set.
     */
    final MethodHandle getBandNumber;

    /**
     * <abbr>GDAL</abbr> {@code GDALDataType GDALGetRasterDataType(GDALRasterBandH)}.
     * Fetches the pixel data type for a band.
     */
    final MethodHandle getRasterDataType;

    /**
     * <abbr>GDAL</abbr> {@code double GDALGetRasterMinimum/Maximum(GDALRasterBandH, int *pbSuccess)}.
     * Fetches the minimum or maximum value for a band in units of the sample data (not converted values).
     * If none, the value is determined from the data type. "No data" values are excluded from the range.
     */
    final MethodHandle getRasterMinimum, getRasterMaximum;

    /**
     * <abbr>GDAL</abbr> {@code double GDALGetRasterNoDataValue(GDALRasterBandH, int *pbSuccess)}.
     * Fetches the no data value for a band. The value is not converted by the scale and offset.
     */
    final MethodHandle getRasterNoDataValue;

    /**
     * <abbr>GDAL</abbr> {@code double GDALGetRasterScale/Offset(GDALRasterBandH, int *pbSuccess)}.
     * Fetches the raster value scale or offset. Those two values define the transfer function.
     * If unspecified in the data file, the default values are for the identity transform.
     */
    final MethodHandle getRasterScale, getRasterOffset;

    /**
     * <abbr>GDAL</abbr> {@code const char *GDALGetRasterUnitType(GDALRasterBandH)}.
     * Returns raster unit type, or an empty string if none.
     */
    final MethodHandle getRasterUnitType;

    /**
     * <abbr>GDAL</abbr> {@code char **GDALGetRasterCategoryNames(GDALRasterBandH)}.
     * etch the list of category names for this raster.
     */
    final MethodHandle getRasterCategoryNames;

    /**
     * <abbr>GDAL</abbr> {@code GDALColorTableH GDALGetRasterColorTable(GDALRasterBandH)}.
     * Fetches the color table associated with a band.
     */
    final MethodHandle getRasterColorTable;

    /**
     * <abbr>GDAL</abbr> {@code int GDALGetColorEntryCount(GDALColorTableH)}.
     * Gets number of color entries in table.
     */
    final MethodHandle getColorEntryCount;

    /**
     * <abbr>GDAL</abbr> {@code int GDALGetColorEntryAsRGB(GDALColorTableH, int, GDALColorEntry*)}.
     * Fetches a table entry in RGB format.
     */
    final MethodHandle getColorEntryAsRGB;

    /**
     * <abbr>GDAL</abbr> {@code GDALColorInterp GDALGetRasterColorInterpretation(GDALRasterBandH)}.
     * Specifies how a band should be interpreted as color.
     */
    final MethodHandle getColorInterpretation;

    /**
     * <abbr>GDAL</abbr> {@code void GDALGetBlockSize(GDALRasterBandH, int *pnXSize, int *pnYSize)}.
     * Fetches the "natural" block size of a band.
     */
    final MethodHandle getBlockSize;

    /**
     * <abbr>GDAL</abbr> {@code CPLErr GDALRasterIO(GDALRasterBandH hRBand, ...)}.
     * Read/write a region of image data for this band.
     */
    final MethodHandle rasterIO;

    /**
     * <abbr>GDAL</abbr> {@code CPLErr GDALRasterAdviseRead(GDALRasterBandH hRBand, ...)}.
     * Advise driver of upcoming read requests.
     */
    final MethodHandle adviseRead;

    /**
     * <abbr>GDAL</abbr> {@code int GDALDatasetGetLayerCount(GDALDatasetH)}.
     * Get the number of vector layers in the dataset.
     */
    final MethodHandle getLayerCount;

    /**
     * Pointers to native methods for the <abbr>OGR</abbr> part of <abbr>GDAL</abbr>.
     * Stores in a separated object for avoiding to load those symbols before needed.
     *
     * @see #ogr()
     */
    private OGR ogr;

    /**
     * Creates the handles for all <abbr>GDAL</abbr> functions which will be needed.
     *
     * @param  loader  the object used for loading the library.
     * @throws NoSuchElementException if a <abbr>GDAL</abbr> function has not been found in the library.
     */
    private GDAL(final LibraryLoader<GDAL> loader) {
        super(loader);

        // A few frequently-used function signatures.
        final var acceptPointerReturnPointer = FunctionDescriptor.of(ValueLayout.ADDRESS,     ValueLayout.ADDRESS);
        final var acceptPointerReturnInt     = FunctionDescriptor.of(ValueLayout.JAVA_INT,    ValueLayout.ADDRESS);
        final var acceptTwoPtrsReturnDouble  = FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
        final var acceptTwoPtrsReturnPointer = FunctionDescriptor.of(ValueLayout.ADDRESS,     ValueLayout.ADDRESS, ValueLayout.ADDRESS);

        // Memory management
        free    = lookup("VSIFree",    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        destroy = lookup("CSLDestroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

        // For Driver and/or all major objects
        identifyDriver  = lookup("GDALIdentifyDriver",     acceptTwoPtrsReturnPointer);
        getName         = lookup("GDALGetDriverLongName",  acceptPointerReturnPointer);
        getIdentifier   = lookup("GDALGetDriverShortName", acceptPointerReturnPointer);
        getMetadata     = lookup("GDALGetMetadata",        acceptTwoPtrsReturnPointer);
        getMetadataItem = lookup("GDALGetMetadataItem", FunctionDescriptor.of(
                ValueLayout.ADDRESS,    // const char* (return type)
                ValueLayout.ADDRESS,    // GDALMajorObject
                ValueLayout.ADDRESS,    // const char* name
                ValueLayout.ADDRESS));  // const char* domain

        // For Opener
        close = lookup("GDALClose",  acceptPointerReturnInt);
        open  = lookup("GDALOpenEx", FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,        // const char *pszFilename
                ValueLayout.JAVA_INT,       // unsigned int nOpenFlags
                ValueLayout.ADDRESS,        // const char *const *papszAllowedDrivers
                ValueLayout.ADDRESS,        // const char *const *papszOpenOptions
                ValueLayout.ADDRESS));      // const char *const *papszSiblingFiles

        // For all data sets
        getFileList      = lookup("GDALGetFileList",      acceptPointerReturnPointer);
        getDatasetDriver = lookup("GDALGetDatasetDriver", acceptPointerReturnPointer);
        getSpatialRef    = lookup("GDALGetSpatialRef",    acceptPointerReturnPointer);
        getGCPSpatialRef = lookup("GDALGetGCPSpatialRef", acceptPointerReturnPointer);
        exportToWkt      = lookup("OSRExportToWktEx",     FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // CPLErr error code (return value)
                ValueLayout.ADDRESS,        // OGRSpatialReferenceH
                ValueLayout.ADDRESS,        // char **ppszWKT
                ValueLayout.ADDRESS));      // const char *const *papszOptions

        getDataAxisToCRSAxis = lookup("OSRGetDataAxisToSRSAxisMapping", acceptTwoPtrsReturnPointer);

        // For TiledResource (GDAL Raster)
        getGeoTransform = lookup("GDALGetGeoTransform", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // CPLErr error code (return value)
                ValueLayout.ADDRESS,        // GDALDataSetH dataset
                ValueLayout.ADDRESS));      // double *padfTransform

        getRasterXSize     = lookup("GDALGetRasterXSize",     acceptPointerReturnInt);
        getRasterYSize     = lookup("GDALGetRasterYSize",     acceptPointerReturnInt);
        getRasterCount     = lookup("GDALGetRasterCount",     acceptPointerReturnInt);
        getRasterBandXSize = lookup("GDALGetRasterBandXSize", acceptPointerReturnInt);
        getRasterBandYSize = lookup("GDALGetRasterBandYSize", acceptPointerReturnInt);
        getRasterBand      = lookup("GDALGetRasterBand", FunctionDescriptor.of(
                ValueLayout.ADDRESS,        // GDALRasterBandH (return value)
                ValueLayout.ADDRESS,        // GDALDataSetH dataset
                ValueLayout.JAVA_INT));     // Band index, starting with 1.

        // For Band
        getBandNumber          = lookup("GDALGetBandNumber",                acceptPointerReturnInt);
        getColorInterpretation = lookup("GDALGetRasterColorInterpretation", acceptPointerReturnInt);
        getRasterDataType      = lookup("GDALGetRasterDataType",            acceptPointerReturnInt);
        getRasterMinimum       = lookup("GDALGetRasterMinimum",             acceptTwoPtrsReturnDouble);
        getRasterMaximum       = lookup("GDALGetRasterMaximum",             acceptTwoPtrsReturnDouble);
        getRasterNoDataValue   = lookup("GDALGetRasterNoDataValue",         acceptTwoPtrsReturnDouble);
        getRasterScale         = lookup("GDALGetRasterScale",               acceptTwoPtrsReturnDouble);
        getRasterOffset        = lookup("GDALGetRasterOffset",              acceptTwoPtrsReturnDouble);
        getRasterUnitType      = lookup("GDALGetRasterUnitType",            acceptPointerReturnPointer);
        getRasterCategoryNames = lookup("GDALGetRasterCategoryNames",       acceptPointerReturnPointer);
        getRasterColorTable    = lookup("GDALGetRasterColorTable",          acceptPointerReturnPointer);
        getColorEntryCount     = lookup("GDALGetColorEntryCount",           acceptPointerReturnInt);
        getColorEntryAsRGB     = lookup("GDALGetColorEntryAsRGB", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS));

        // Band I/O
        getBlockSize = lookup("GDALGetBlockSize", FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,        // GDALRasterBandH
                ValueLayout.ADDRESS,        // int *pnXSize
                ValueLayout.ADDRESS));      // int *pnYSize

        rasterIO = lookup("GDALRasterIO", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // CPLErr error code (return value)
                ValueLayout.ADDRESS,        // GDALRasterBandH hRBand
                ValueLayout.JAVA_INT,       // GDALRWFlag eRWFlag
                ValueLayout.JAVA_INT,       // int nDSXOff
                ValueLayout.JAVA_INT,       // int nDSYOff
                ValueLayout.JAVA_INT,       // int nDSXSize
                ValueLayout.JAVA_INT,       // int nDSYSize
                ValueLayout.ADDRESS,        // void *pBuffer
                ValueLayout.JAVA_INT,       // int nBXSize
                ValueLayout.JAVA_INT,       // int nBYSize
                ValueLayout.JAVA_INT,       // GDALDataType eBDataType
                ValueLayout.JAVA_INT,       // int nPixelSpace
                ValueLayout.JAVA_INT));     // int nLineSpace

        adviseRead = lookup("GDALRasterAdviseRead", FunctionDescriptor.of(
                ValueLayout.JAVA_INT,       // CPLErr error code (return value)
                ValueLayout.ADDRESS,        // GDALRasterBandH hRBand
                ValueLayout.JAVA_INT,       // int nDSXOff
                ValueLayout.JAVA_INT,       // int nDSYOff
                ValueLayout.JAVA_INT,       // int nDSXSize
                ValueLayout.JAVA_INT,       // int nDSYSize
                ValueLayout.JAVA_INT,       // int nBXSize
                ValueLayout.JAVA_INT,       // int nBYSize
                ValueLayout.JAVA_INT,       // GDALDataType eBDataType
                ValueLayout.ADDRESS));      // CSLConstList papszOptions

        // Dataset layer API
        getLayerCount = lookup("GDALDatasetGetLayerCount", acceptPointerReturnInt);

        // Set error handling first in order to redirect initialization warnings.
        setErrorHandler(null);

        // Initialize GDAL after we found all functions.
        if (!invoke("GDALAllRegister")) {
            log(GDAL.class, "<init>", Resources.forLocale(null)
                    .createLogRecord(Level.WARNING, Resources.Keys.CannotInitialize_1, Constants.GDAL));
        }
    }

    /**
     * Returns the helper class for loading the <abbr>GDAL</abbr> library.
     * If {@code def} is true, then this method tries to load the library
     * now and stores the result in {@link #global} and {@link #globalStatus}.
     *
     * @param  now  whether this method is invoked for the default (global) library.
     *         In such case, the caller must be synchronized and {@link #global} must be initially null.
     * @return the library loader for <abbr>GDAL</abbr>.
     */
    private static LibraryLoader<GDAL> load(final boolean now) {
        final var loader = new LibraryLoader<>(GDAL::new);
        if (now) {
            try {
                global = loader.global("gdal");
            } finally {
                globalStatus = loader.status();
            }
            if (global != null) {
                if (GDALStoreProvider.LOGGER.isLoggable(Level.CONFIG)) {
                    global.version("--version").ifPresent((version) -> {
                        log(GDAL.class, "<init>", new LogRecord(Level.CONFIG, version));
                    });
                }
            }
        }
        return loader;
    }

    /**
     * Loads the <abbr>GDAL</abbr> library from the given file.
     * Callers should register the returned instance in a {@link java.lang.ref.Cleaner}.
     *
     * @param  library  the library to load.
     * @return handles to native functions needed by this module.
     * @throws IllegalArgumentException if the GDAL library has not been found.
     * @throws NoSuchElementException if a <abbr>GDAL</abbr> function has not been found in the library.
     * @throws IllegalCallerException if this Apache SIS module is not authorized to call native methods.
     */
    static GDAL load(final Path library) {
        return load(false).load(library);
    }

    /**
     * Returns an instance using the <abbr>GDAL</abbr> library loaded from the default library path.
     * The handles are valid for the Java Virtual Machine lifetime, i.e. it uses the global arena.
     * If this method has already been invoked, this method returns the previously created instance.
     *
     * <p>If the <abbr>GDAL</abbr> library is not found, the current default is {@link SymbolLookup#loaderLookup()}
     * for allowing users to invoke {@link System#loadLibrary(String)} as a fallback. This policy may be revised in
     * any future version of Apache <abbr>SIS</abbr>.</p>
     *
     * @return handles to native functions needed by this module.
     * @throws DataStoreException if the native library has not been found or if SIS is not allowed to call
     *         native functions, and {@code onError} is null.
     */
    static synchronized GDAL global() throws DataStoreException {
        if (globalStatus == null) {
            load(true).validate(Constants.GDAL);
        }
        globalStatus.report(Constants.GDAL, null);
        return global;
    }

    /**
     * Same as {@link #global}, but logs a warning instead of throwing an exception in case of error.
     *
     * @param  classe  the class which is invoking this method (for logging purpose).
     * @param  method  the name of the method which is invoking this method (for logging purpose).
     * @return handles to native functions needed by this module, or empty if not available.
     */
    static synchronized Optional<GDAL> tryGlobal(final Class<?> classe, final String method) {
        if (globalStatus == null) {
            load(true).getError(Constants.GDAL).ifPresent((record) -> log(classe, method, record));
        }
        return Optional.ofNullable(global);
    }

    /**
     * Installs a function for redirecting <abbr>GDAL</abbr> errors to Apache <abbr>SIS</abbr> loggers.
     * The handler is set by a call to {@code CPLErrorHandler CPLSetErrorHandler(CPLErrorHandler)} where
     * {@code CPLErrorHandler} is {@code void (*CPLErrorHandler)(CPLErr, CPLErrorNum, const char*)}.
     *
     * <p><b>The error handler is valid only during the lifetime of the {@linkplain #arena() arena}.</b>
     * The error handle shall be uninstalled before the arena is closed.</p>
     *
     * @param  target  the function to set as an error handler, or {@link MemorySegment#NULL} for the GDAL default.
     *                 If {@code null}, the function handle will be created by this method.
     * @return the previous error handler, or {@link MemorySegment#NULL} it it was the <abbr>GDAL</abbr> default.
     *
     * @see #run()
     * @see GDALStoreProvider#fatalError()
     */
    @SuppressWarnings("restricted")
    private MemorySegment setErrorHandler(MemorySegment target) {
        final MemorySegment setter = symbols.find("CPLSetErrorHandler").orElse(null);
        if (setter == null) {
            return MemorySegment.NULL;
        }
        if (target == null) {
            target = linker.upcallStub(ErrorHandler.getMethod(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS), arena());
        }
        final MethodHandle handle = linker.downcallHandle(setter,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        try {
            return (MemorySegment) handle.invokeExact(target);
        } catch (Throwable e) {
            throw propagate(e);
        }
    }

    /**
     * Logs the given record as if was produced by the {@link GDALStoreProvider}, which is the public class.
     *
     * @param  classe  the class which is invoking this method (for logging purpose).
     * @param  method  the name of the method which is invoking this method (for logging purpose).
     * @param  record  the error to log.
     */
    private static void log(final Class<?> classe, final String method, final LogRecord record) {
        Logging.completeAndLog(GDALStoreProvider.LOGGER, classe, method, record);
    }

    /**
     * Returns the <abbr>GDAL</abbr> version. The request can be:
     *
     * <ul>
     *   <li>{@code "LICENSE"}:      content of the {@code LICENSE.TXT} file from the {@code GDAL_DATA} directory.</li>
     *   <li>{@code "VERSION_NUM"}:  version packed in a string. Example: "30603000" for GDAL 3.6.3.0.</li>
     *   <li>{@code "RELEASE_DATE"}: release packed in a string. Example: "20230312".</li>
     *   <li>{@code "--version"}:    </li>
     * </ul>
     *
     * @param  request  the desired version information.
     * @return the <abbr>GDAL</abbr> version, or a message saying that the library was not found.
     */
    final Optional<String> version(final String request) {
        return invokeGetString("GDALVersionInfo", request);
    }

    /**
     * Returns a {@code NULL}-terminated array as a modifiable list of strings.
     * This way to encode arrays of strings is specific to <abbr>GDAL</abbr>.
     * The list cannot contain any value (not possible with null-terminated arrays).
     * Instead, <abbr>GDAL</abbr> sometime represents missing values by empty strings.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return the results as strings, or {@code null} if the result was null.
     */
    @SuppressWarnings("restricted")
    static List<String> fromNullTerminatedStrings(MemorySegment result) {
        if (isNull(result)) {
            return null;
        }
        result = result.reinterpret(Integer.MAX_VALUE);
        final var  items  = new ArrayList<String>();
        final long stride = ValueLayout.ADDRESS.byteSize();
        String item;
        for (long offset = 0; (item = toString(result.get(ValueLayout.ADDRESS, offset))) != null; offset += stride) {
            items.add(item);
        }
        return items;
    }

    /**
     * Returns a Java array of strings as a {@code NULL}-terminated array.
     * This way to encode arrays of strings is specific to <abbr>GDAL</abbr>.
     *
     * @param  arena  the arena to use for memory allocation.
     * @param  items  the Java strings to copy, or {@code null}.
     * @return the {@code NULL}-terminated array of string.
     */
    static MemorySegment toNullTerminatedStrings(final Arena arena, final String... items) {
        if (items == null) {
            return MemorySegment.NULL;
        }
        final var layout = ValueLayout.ADDRESS;
        final MemorySegment array = arena.allocate(layout, items.length + 1);
        for (int i=0; i<items.length; i++) {
            array.setAtIndex(layout, i, arena.allocateFrom(items[i]));
        }
        array.setAtIndex(layout, items.length, MemorySegment.NULL);
        return array;
    }

    /**
     * Returns the pointers to native methods for the <abbr>OGR</abbr> part of <abbr>GDAL</abbr>.
     */
    final synchronized OGR ogr() {
        if (ogr == null) {
            ogr = new OGR(this);
        }
        return ogr;
    }

    /**
     * Unloads the <abbr>GDAL</abbr> library. If the arena is global,
     * then this method should not be invoked before <abbr>JVM</abbr> shutdown.
     * Otherwise, this method is invoked when {@link GDALStoreProvider} is garbage-collected.
     */
    @Override
    public void run() {
        try {
            // Clear the error handler because the arena will be closed.
            setErrorHandler(MemorySegment.NULL);
            invoke("GDALDestroy");
        } finally {
            super.run();
        }
    }
}
