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
package org.apache.sis.referencing.factory;

import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.internal.Resources;


/**
 * Result of parsing a code in "OGC", "CRS", "AUTO" or "AUTO2" namespace.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class CommonAuthorityCode {
    /**
     * The parameter separator for codes in the {@code "AUTO(2)"} namespace.
     */
    static final char SEPARATOR = ',';

    /**
     * Local part of the code, without the authority part and without the parameters.
     */
    final String localCode;

    /**
     * The remaining part of the code after {@link #localCode}, or {@code null} if none.
     * This part may exist with codes in the {@code AUTO} or {@code AUTO2} namespace.
     */
    private String complement;

    /**
     * The result of parsing {@link #complement} as numerical parameters.
     * Computed by {@link #parameters()} when first requested.
     */
    private double[] parameters;

    /**
     * If the authority is {@code "AUTO"}, version of that authority (1 or 2). Otherwise 0.
     */
    private int versionOfAuto;

    /**
     * Whether the first character of {@link #localCode} is a decimal digit, the minus or the plus character.
     */
    final boolean isNumeric;

    /**
     * {@code true} if the "OGC" namespace was explicitly specified.
     */
    boolean isOGC;

    /**
     * Finds the index where the code begins, ignoring spaces and the {@code "OGC"}, {@code "CRS"}, {@code "AUTO"},
     * {@code "AUTO1"} or {@code "AUTO2"} namespaces if present. If a namespace is found and is a legacy one, then
     * the {@link #isLegacy} flag will be set.
     *
     * @param  code  authority, code and parameters to parse.
     * @throws NoSuchAuthorityCodeException if an authority is present but is not one of the recognized authorities.
     */
    CommonAuthorityCode(final String code) throws NoSuchAuthorityCodeException {
        int s = code.indexOf(Constants.DEFAULT_SEPARATOR);
        if (s >= 0) {
            final int end   = CharSequences.skipTrailingWhitespaces(code, 0, s);
            final int start = CharSequences.skipLeadingWhitespaces (code, 0, end);
            isOGC = GeodeticAuthorityFactory.regionMatches(Constants.OGC, code, start, end);
            if (!isOGC && !GeodeticAuthorityFactory.regionMatches(Constants.CRS, code, start, end)) {
                if (code.regionMatches(true, start, "AUTO", 0, 4)) {                    // 4 is the length of "AUTO".
                    switch (end - start) {
                        case 4: versionOfAuto = 1; break;                               // "AUTO".
                        case 5: versionOfAuto = (code.charAt(end-1) - '0'); break;      // "AUTO1" or "AUTO2".
                    }
                }
                if (!isAuto(false)) {
                    throw new NoSuchAuthorityCodeException(Resources.format(Resources.Keys.UnknownAuthority_1,
                            CharSequences.trimWhitespaces(code, 0, s)), Constants.OGC, code);
                }
            }
        }
        final int length = code.length();
        s = CharSequences.skipLeadingWhitespaces(code, s+1, length);
        /*
         * Above code removed the "CRS" part when it is used as a namespace, as in "CRS:84".
         * The code below removes the "CRS" prefix when it is concatenated within the code,
         * as in "CRS84". Together, those two checks handle redundant codes like "CRS:CRS84"
         * (malformed code, but seen in practice).
         */
        if (code.regionMatches(true, s, Constants.CRS, 0, Constants.CRS.length())) {
            s = CharSequences.skipLeadingWhitespaces(code, s + Constants.CRS.length(), length);
        }
        if (s >= length) {
            throw new NoSuchAuthorityCodeException(Errors.format(Errors.Keys.EmptyArgument_1, "code"), Constants.OGC, code);
        }
        /*
         * Check whether the code has parameters. It should happen only in code in "AUTO" or "AUTO2" namespace,
         * but we nevertheless check for all authorities.
         */
        int end = CharSequences.skipTrailingWhitespaces(code, s, length);
        final int startOfParameters = code.indexOf(SEPARATOR, s);
        if (startOfParameters >= 0) {
            complement = code.substring(CharSequences.skipLeadingWhitespaces(code, startOfParameters + 1, end), end);
            end = CharSequences.skipTrailingWhitespaces(code, s, startOfParameters);
        }
        localCode = code.substring(s, end);
        final char c = localCode.charAt(0);
        isNumeric = (c >= '0' && c <= '9') || (c == '-' || c == '+');
    }

    /**
     * Returns whether the authority is "AUTO", "AUTO1" or "AUTO2".
     *
     * @param  legacy  whether to return {@code true} only if the authority is "AUTO" or "AUTO1".
     * @return whether the authority is some "AUTO" namespace.
     */
    final boolean isAuto(final boolean legacy) {
        return legacy ? (versionOfAuto == 1) : (versionOfAuto >= 1 && versionOfAuto <= 2);
    }

    /**
     * Returns whether there is no parameters.
     */
    final boolean isParameterless() {
        return Strings.isNullOrEmpty(complement);
    }

    /**
     * Returns the result of parsing the comma-separated list of optional parameters after the code.
     * If there is no parameter, then this method returns an empty array.
     * Caller should not modify the returned array.
     *
     * @return the parameters after the code, or an empty array if none.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final double[] parameters() {
        if (parameters == null) {
            parameters = CharSequences.parseDoubles(complement, SEPARATOR);     // `parseDoubles(…)` is null-safe.
        }
        return parameters;
    }

    /**
     * Returns the error message for unexpected parameters after the code.
     */
    final String unexpectedParameters() {
        return Errors.format(Errors.Keys.UnexpectedCharactersAfter_2, localCode, complement);
    }
}
