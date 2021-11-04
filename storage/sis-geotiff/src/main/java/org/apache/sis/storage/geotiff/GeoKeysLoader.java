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

import java.util.Map;
import org.apache.sis.math.Vector;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.geotiff.Resources;


/**
 * Loads GeoTIFF keys in a hash map, but without performing any interpretation.
 * A {@code GeoKeysLoader} receives as inputs the values of the following TIFF tags:
 *
 * <ul>
 *   <li>{@link Tags#GeoKeyDirectory} — array of unsigned {@code short} values grouped into blocks of 4.</li>
 *   <li>{@link Tags#GeoDoubleParams} — array of {@double} values referenced by {@code GeoKeyDirectory} elements.</li>
 *   <li>{@link Tags#GeoAsciiParams}  — array of characters referenced by {@code GeoKeyDirectory} elements.</li>
 * </ul>
 *
 * For example, consider the following values for the above-cited tags:
 *
 * <table class="sis">
 *   <caption>GeoKeyDirectory(34735) values</caption>
 *   <tr><td>    1 </td><td>     1 </td><td>  2 </td><td>     6 </td></tr>
 *   <tr><td> 1024 </td><td>     0 </td><td>  1 </td><td>     2 </td></tr>
 *   <tr><td> 1026 </td><td> 34737 </td><td>  0 </td><td>    12 </td></tr>
 *   <tr><td> 2048 </td><td>     0 </td><td>  1 </td><td> 32767 </td></tr>
 *   <tr><td> 2049 </td><td> 34737 </td><td> 14 </td><td>    12 </td></tr>
 *   <tr><td> 2050 </td><td>     0 </td><td>  1 </td><td>     6 </td></tr>
 *   <tr><td> 2051 </td><td> 34736 </td><td>  1 </td><td>     0 </td></tr>
 * </table>
 *
 * {@preformattext
 *   GeoDoubleParams(34736) = {1.5}
 *   GeoAsciiParams(34737) = "Custom File|My Geographic|"
 * }
 *
 * <p>The first number in the {@code GeoKeyDirectory} table indicates that this is a version 1 GeoTIFF GeoKey directory.
 * This version will only change if the key structure is changed. The other numbers on the first line said that the file
 * uses revision 1.2 of the set of keys and that there is 6 key values.</p>
 *
 * <p>The next line indicates that the first key (1024 = {@code ModelType}) has the value 2 (Geographic),
 * explicitly placed in the entry list since the TIFF tag location is 0.
 * The next line indicates that the key 1026 ({@code Citation}) is listed in the {@code GeoAsciiParams(34737)} array,
 * starting at offset 0 (the first in array), and running for 12 bytes and so has the value "Custom File".
 * The "|" character is converted to a null delimiter at the end in C/C++ libraries.</p>
 *
 * <p>Going further down the list, the key 2051 ({@code GeogLinearUnitSize}) is located in {@code GeoDoubleParams(34736)}
 * at offset 0 and has the value 1.5; the value of key 2049 ({@code GeogCitation}) is "My Geographic".</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
class GeoKeysLoader {
    /**
     * Number of {@code short} values in each GeoKey entry.
     */
    private static final int ENTRY_LENGTH = 4;

    /**
     * The character used as a separator in {@link String} multi-values.
     */
    static final char SEPARATOR = '|';

    /**
     * References the {@link GeoKeys} needed for building the Coordinate Reference System.
     * Can not be null when invoking {@link #load(Map)}.
     *
     * @see Tags#GeoKeyDirectory
     */
    public Vector keyDirectory;

    /**
     * The numeric values referenced by the {@link #keyDirectory}.
     * Can be {@code null} if none.
     *
     * @see Tags#GeoDoubleParams
     */
    public Vector numericParameters;

    /**
     * The characters referenced by the {@link #keyDirectory}.
     * Can be {@code null} if none.
     *
     * @see Tags#GeoAsciiParams
     * @see #setAsciiParameters(String[])
     */
    public String asciiParameters;

    /**
     * Version of the set of keys declared in the {@code GeoKeyDirectory} header.
     */
    short majorRevision, minorRevision;

    /**
     * Where to send warnings, or {@code null} for ignoring warnings silently.
     * While {@code CRSBuilder} is a class doing complex work (CRS construction),
     * only the logging-related methods will be invoked by {@code GeoKeysLoader}.
     */
    CRSBuilder logger;

    /**
     * Creates a new GeoTIFF keys loader. The {@link #keyDirectory}, {@link #numericParameters}
     * {@link #asciiParameters} and {@link #logger} fields must be initialized by the caller.
     */
    GeoKeysLoader() {
    }

    /**
     * Sets the value of {@link #asciiParameters} from {@link Tags#GeoAsciiParams} value.
     */
    final void setAsciiParameters(final String[] values) {
        switch (values.length) {
            case 0:  break;
            case 1:  asciiParameters = values[0]; break;
            default: asciiParameters = String.join("\u0000", values).concat("\u0000"); break;
        }
    }

    /**
     * Loads GeoKeys and write values in the given map.
     *
     * @param  geoKeys  where to write GeoKeys.
     * @return whether the operation succeed.
     */
    final boolean load(final Map<Short,Object> geoKeys) {
        final int numberOfKeys;
        final int directoryLength = keyDirectory.size();
        if (directoryLength >= ENTRY_LENGTH) {
            final int version = keyDirectory.intValue(0);
            if (version != 1) {
                warning(Resources.Keys.UnsupportedGeoKeyDirectory_1, version);
                return false;
            }
            majorRevision = keyDirectory.shortValue(1);
            minorRevision = keyDirectory.shortValue(2);
            numberOfKeys  = keyDirectory.intValue(3);
        } else {
            numberOfKeys = 0;
        }
        /*
         * The key directory may be longer than needed for the amount of keys, but not shorter.
         * If shorter, report a warning and stop the parsing since we have no way to know if the
         * missing information were essentiel or not.
         *
         *     (number of key + head) * 4    ---    1 entry = 4 short values.
         */
        final int expectedLength = (numberOfKeys + 1) * ENTRY_LENGTH;
        if (directoryLength < expectedLength) {
            warning(Resources.Keys.ListTooShort_3, "GeoKeyDirectory", expectedLength, directoryLength);
            return false;
        }
        final int numberOfDoubles = (numericParameters != null) ? numericParameters.size() : 0;
        final int numberOfChars   =   (asciiParameters != null) ? asciiParameters.length() : 0;
        /*
         * Now iterate over all GeoKey values. The values are copied in a HashMap for convenience,
         * because the CRS creation may use them out of order.
         */
        for (int i=1; i <= numberOfKeys; i++) {
            final int p = i * ENTRY_LENGTH;
            final short key       = keyDirectory.shortValue(p);
            final int tagLocation = keyDirectory.intValue(p+1);
            final int count       = keyDirectory.intValue(p+2);
            final int valueOffset = keyDirectory.intValue(p+3);
            if (valueOffset < 0 || count < 0) {
                missingValue(key);
                continue;
            }
            final Object value;
            switch (tagLocation) {
                /*
                 * tagLocation == 0 means that `valueOffset` actually contains the value,
                 * thus avoiding the need to allocate a separated storage location for it.
                 * The count should be 1.
                 */
                case 0: {
                    switch (count) {
                        case 0:  continue;
                        case 1:  break;          // Expected value.
                        default: {
                            warning(Resources.Keys.UnexpectedListOfValues_2, GeoKeys.name(key), count);
                            break;
                        }
                    }
                    value = valueOffset;
                    break;
                }
                /*
                 * Values of type `short` are stored in the same vector than the key directory;
                 * the specification does not allocate a separated vector for them. We use the
                 * `int` type if needed for allowing storage of unsigned short values.
                 */
                case Tags.GeoKeyDirectory & 0xFFFF: {
                    if (valueOffset + count > keyDirectory.size()) {
                        missingValue(key);
                        continue;
                    }
                    switch (count) {
                        case 0:  continue;
                        case 1:  value = keyDirectory.get(valueOffset); break;
                        default: {
                            final int[] array = new int[count];
                            for (int j=0; j<count; j++) {
                                array[j] = keyDirectory.intValue(valueOffset + j);
                            }
                            value = array;
                            break;
                        }
                    }
                    break;
                }
                /*
                 * Values of type `double` are read from a separated vector, `numericParameters`.
                 * Result is stored in a Double wrapper or in an array of type 'double[]'.
                 */
                case Tags.GeoDoubleParams & 0xFFFF: {
                    if (valueOffset + count > numberOfDoubles) {
                        missingValue(key);
                        continue;
                    }
                    switch (count) {
                        case 0:  continue;
                        case 1:  value = numericParameters.get(valueOffset); break;
                        default: {
                            final double[] array = new double[count];
                            for (int j=0; j<count; j++) {
                                array[j] = numericParameters.doubleValue(valueOffset + j);
                            }
                            value = array;
                            break;
                        }
                    }
                    break;
                }
                /*
                 * ASCII encoding use the pipe ('|') character as a replacement for the NUL character
                 * used in C/C++ programming languages. We need to omit those trailing characters.
                 */
                case Tags.GeoAsciiParams & 0xFFFF: {
                    int upper = valueOffset + count;
                    if (upper > numberOfChars) {
                        missingValue(key);
                        continue;
                    }
                    upper = CharSequences.skipTrailingWhitespaces(asciiParameters, valueOffset, upper);
                    while (upper > valueOffset && asciiParameters.charAt(upper - 1) == SEPARATOR) {
                        upper--;    // Skip trailing pipe, interpreted as C/C++ NUL character.
                    }
                    // Use String.trim() for skipping C/C++ NUL character in addition of whitespaces.
                    final String s = asciiParameters.substring(valueOffset, upper).trim();
                    if (s.isEmpty()) continue;
                    value = s;
                    break;
                }
                /*
                 * GeoKeys are not expected to use other storage mechanism. If this happen anyway, report a warning
                 * and continue on the assumption that if the value that we are ignoring was critical information,
                 * it would have be stored in one of the standard GeoTIFF tags.
                 */
                default: {
                    warning(Resources.Keys.UnsupportedGeoKeyStorage_1, GeoKeys.name(key));
                    continue;
                }
            }
            geoKeys.put(key, value);
        }
        return true;
    }

    /**
     * Reports a warning with a message built from the given resource keys and arguments.
     *
     * @param  key   one of the {@link Resources.Keys} constants.
     * @param  args  arguments for the log message.
     */
    private void warning(final short key, final Object... args) {
        if (logger != null) {
            logger.warning(key, args);
        }
    }

    /**
     * Reports a warning about missing value for the given key.
     */
    private void missingValue(final short key) {
        if (logger != null) {
            logger.missingValue(key);
        }
    }
}
