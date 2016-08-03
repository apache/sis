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
package org.apache.sis.internal.storage;

import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.nio.charset.Charset;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultSampleDimension;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import java.time.LocalDate;
import org.apache.sis.metadata.iso.DefaultIdentifier;

/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 * This class creates the metadata objects only when first needed.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Rémi Marechal (Geomatys)
 * @since 0.8
 * @version 0.8
 * @module
 */
public class MetadataBuilder {

    /**
     * The metadata created by this reader, or {@code null} if none.
     */
    private DefaultMetadata metadata;

    /**
     * The identifier for metadatathat are part of {@linkplain #metadata},
     * or {@code null} if none.
     */
    private DefaultIdentifier identifier;
    /**
     * The identification information that are part of {@linkplain #metadata},
     * or {@code null} if none.
     */
    private DefaultDataIdentification identification;

    /**
     * The citation of data {@linkplain #identification}, or {@code null} if
     * none.
     */
    private DefaultCitation citation;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or
     * {@code null} if none.
     */
    private DefaultResponsibility responsibility;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or
     * {@code null} if none.
     */
    private AbstractParty party;

    /**
     * Copyright information, or {@code null} if none.
     */
    private DefaultLegalConstraints constraints;

    /**
     * The extent information that are part of {@linkplain #identification}, or
     * {@code null} if none.
     */
    private DefaultExtent extent;

    /**
     * Information about the content of a grid data cell, or {@code null} if
     * none.
     */
    private DefaultCoverageDescription coverageDescription;

    /**
     * Information about content type for groups of attributes for a specific
     * range dimension, or {@code null} if none.
     */
    private DefaultAttributeGroup attributGroup;

    /**
     * The characteristic of each dimension (layer) included in the resource, or
     * {@code null} if none.
     */
    private DefaultSampleDimension sampleDimension;

    /**
     * The distribution format, or {@code null} if none.
     */
    private DefaultFormat format;

    /**
     * Information about distribution (including the {@linkplain #format}), or
     * {@code null} if none.
     */
    private DefaultDistribution distribution;

    /**
     * Creates a new metadata reader.
     */
    public MetadataBuilder() {
    }

    /**
     * Returns the metadata as an unmodifiable object, or {
     *
     * @cod null} if none. After this method has been invoked, the metadata can
     * not be modified anymore.
     *
     * @return the metadata, or {@code null} if none.
     */
    public final DefaultMetadata result() {
        commit();
        if (metadata != null) {
            metadata.freeze();
        }
        return metadata;
    }

    /**
     * Performs the links between the objects created in this builder. After
     * this method has been invoked, all metadata objects should be reachable
     * from the root {@link DefaultMetadata} object.
     */
    private void commit() {
        /*
         * Construction shall be ordered from children to parents.
         */
        if (extent != null) {
            identification().getExtents().add(extent);
            extent = null;
        }
        if (party != null) {
            responsibility().getParties().add(party);
            party = null;
        }
        if (responsibility != null) {
            citation().getCitedResponsibleParties().add(responsibility);
            responsibility = null;
        }
        if (citation != null) {
            identification().setCitation(citation);
            citation = null;
        }
        if (constraints != null) {
            identification().getResourceConstraints().add(constraints);
            constraints = null;
        }
        if (sampleDimension != null) {
            attributGroup().getAttributes().add(sampleDimension);
            sampleDimension = null;
        }
        if (attributGroup != null) {
            coverageDescription().getAttributeGroups().add(attributGroup);
            attributGroup = null;
        }
        if (coverageDescription != null) {
            metadata().getContentInfo().add(coverageDescription);
            coverageDescription = null;
        }
        if (identification != null) {
            metadata().getIdentificationInfo().add(identification);
            identification = null;
        }
        if (format != null) {
            distribution().getDistributionFormats().add(format);
            format = null;
        }
        if (distribution != null) {
            metadata().getDistributionInfo().add(distribution);
            distribution = null;
        }
        if (identifier != null) {
            metadata().setMetadataIdentifier(identifier);
            identifier = null;
        }
    }

    /**
     * Creates the metadata object if it does not already exists, then return
     * it.
     *
     * @return the metadata (never {@code null}).
     */
    private DefaultMetadata metadata() {
        if (metadata == null) {
            metadata = new DefaultMetadata();
        }
        return metadata;
    }

    /**
     * Creates the identification information object if it does not already
     * exists, then return it.
     *
     * @return the identification information (never {@code null}).
     */
    private DefaultDataIdentification identification() {
        if (identification == null) {
            identification = new DefaultDataIdentification();
        }
        return identification;
    }

    /**
     * Creates the citation object if it does not already exists, then return
     * it.
     *
     * @return the citation information (never {@code null}).
     */
    private DefaultCitation citation() {
        if (citation == null) {
            citation = new DefaultCitation();
        }
        return citation;
    }

    /**
     * Creates the responsibility object if it does not already exists, then
     * return it.
     *
     * @return the responsibility party (never {@code null}).
     */
    private DefaultResponsibility responsibility() {
        if (responsibility == null) {
            responsibility = new DefaultResponsibility();
        }
        return responsibility;
    }

    /**
     * Creates the person or organization information object if it does not
     * already exists, then return it.
     *
     * <p>
     * <b>Limitation:</b> current implementation creates an
     * {@code AbstractParty} instead than one of the subtypes. This is not
     * valid, but we currently have no way to know if the party is an individual
     * or an organization.</p>
     *
     * @return the person or organization information (never {@code null}).
     */
    private AbstractParty party() {
        if (party == null) {
            party = new AbstractParty();        // See limitation in above javadoc.
        }
        return party;
    }

    /**
     * Creates the constraints information object if it does not already exists,
     * then return it.
     *
     * @return the constraints information (never {@code null}).
     */
    private DefaultLegalConstraints constraints() {
        if (constraints == null) {
            constraints = new DefaultLegalConstraints();
        }
        return constraints;
    }

    /**
     * Creates the extent information object if it does not already exists, then
     * return it.
     *
     * @return the extent information (never {@code null}).
     */
    private DefaultExtent extent() {
        if (extent == null) {
            extent = new DefaultExtent();
        }
        return extent;
    }

    /**
     * Creates the sample dimension object if it does not already exists, then
     * return it.
     *
     * @return the sample dimension (never {@code null}).
     */
    private DefaultSampleDimension sampleDimension() {
        if (sampleDimension == null) {
            sampleDimension = new DefaultSampleDimension();
        }
        return sampleDimension;
    }

    /**
     * Creates the attribut group object if it does not already exists, then
     * return it.
     *
     * @return the attribut group (never {@code null}).
     */
    private DefaultAttributeGroup attributGroup() {
        if (attributGroup == null) {
            attributGroup = new DefaultAttributeGroup();
        }
        return attributGroup;
    }

    /**
     * Creates the coverage description object if it does not already exists,
     * then return it.
     *
     * @return the coverage description (never {@code null}).
     */
    private DefaultCoverageDescription coverageDescription() {
        if (coverageDescription == null) {
            coverageDescription = new DefaultCoverageDescription();
        }
        return coverageDescription;
    }

    /**
     * Creates the distribution format object if it does not already exists,
     * then return it.
     *
     * @return the distribution format (never {@code null}).
     */
    private DefaultFormat format() {
        if (format == null) {
            format = new DefaultFormat();
        }
        return format;
    }

    /**
     * Creates the distribution information object if it does not already
     * exists, then return it.
     *
     * @return the distribution information (never {@code null}).
     */
    private DefaultDistribution distribution() {
        if (distribution == null) {
            distribution = new DefaultDistribution();
        }
        return distribution;
    }

    /**
     * Creates the identifier object if it does not already
     * exists, then return it.
     * @return the identifier (never {@code null}).
     */
    private DefaultIdentifier identifier() {
        if (identifier == null) {
            identifier = new DefaultIdentifier();
        }
        return identifier;
    }

    /**
     * Adds the given character encoding to the metadata.
     *
     * @param encoding the character encoding to add.
     */
    public final void add(final Charset encoding) {
        if (encoding != null) {
            metadata().getCharacterSets().add(encoding);
        }
    }

    /**
     * Adds the given coordinate reference system to metadata, if it does not
     * already exists. This method ensures that there is no duplicated values.
     * Comparisons ignore metadata.
     *
     * @param crs the coordinate reference system to add to the metadata, or
     * {@code null} if none.
     */
    public final void add(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final Collection<ReferenceSystem> systems = metadata().getReferenceSystemInfo();
            for (final ReferenceSystem existing : systems) {
                if (Utilities.equalsIgnoreMetadata(crs, existing)) {
                    return;
                }
            }
            systems.add(crs);
        }
    }

    /**
     * Adds the given envelope, including its CRS, to the metadata. If the
     * metadata already contains a geographic bounding box, then a new bounding
     * box is added; this method does not compute the union of the two boxes.
     *
     * @param envelope the extent to add in the metadata, or {@code null} if
     * none.
     * @throws TransformException if an error occurred while converting the
     * given envelope to extents.
     */
    public final void add(final AbstractEnvelope envelope) throws TransformException {
        if (envelope != null) {
            add(envelope.getCoordinateReferenceSystem());
            if (!envelope.isAllNaN()) {
                extent().addElements(envelope);
            }
        }
    }

    /**
     * Adds a date of the given type.
     *
     * @param date the date to add, or {@code null}.
     * @param type the type of the date to add, or {@code null}.
     */
    public final void add(final Date date, final DateType type) {
        if (date != null) {
            citation().getDates().add(new DefaultCitationDate(date, type));
        }
    }

    /**
     * Returns the given character sequence as a non-empty character string with
     * leading and trailing spaces removed. If the given character sequence is
     * null, empty or blank, then this method returns {@code null}.
     */
    private static InternationalString trim(CharSequence string) {
        string = CharSequences.trimWhitespaces(string);
        if (string != null && string.length() != 0) {
            return Types.toInternationalString(string);
        } else {
            return null;
        }
    }

    /**
     * Returns the concatenation of the given string. The previous string may be
     * {@code null}. This method does nothing if the previous string already
     * contains the one to append.
     */
    private static InternationalString append(final InternationalString previous, final InternationalString toAdd) {
        if (previous == null) {
            return toAdd;
        }
        final String p = previous.toString();
        final String a = toAdd.toString();
        if (p.contains(a)) {
            return previous;
        }
        return Types.toInternationalString(p + System.lineSeparator() + a);
    }

    /**
     * Adds a title of the resource.
     *
     * @param title the resource title, or {@code null} if none.
     */
    public final void addTitle(final CharSequence title) {
        final InternationalString i18n = trim(title);
        if (i18n != null) {
            final DefaultCitation citation = citation();
            if (citation.getTitle() == null) {
                citation.setTitle(i18n);
            } else {
                citation.getAlternateTitles().add(i18n);
            }
        }
    }

    /**
     * Adds a brief narrative summary of the resource(s). If a summary already
     * existed, the new one will be appended after a new line.
     *
     * @param description the summary of resource(s), or {@code null} if none.
     */
    public final void addAbstract(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultDataIdentification identification = identification();
            identification.setAbstract(append(identification.getAbstract(), i18n));
        }
    }

    /**
     * Adds an author name.
     *
     * @param author the name of the author or publisher, or {@code null} if
     * none.
     */
    public final void addAuthor(final CharSequence author) {
        final InternationalString i18n = trim(author);
        if (i18n != null) {
            if (party != null) {
                responsibility().getParties().add(party);       // Save the previous party before to create a new one.
                party = null;
            }
            party().setName(i18n);
        }
    }

    /**
     * Elements to omit in the legal notice to be parsed by
     * {@link MetadataBuilder#parseLegalNotice(String)}. Some of those elements
     * are implied by the metadata were the legal notice will be stored.
     */
    private static final class LegalSymbols {

        /**
         * Symbols associated to restrictions.
         */
        private static final LegalSymbols[] VALUES = {
            new LegalSymbols(Restriction.COPYRIGHT, "COPYRIGHT", "(C)", "©", "All rights reserved"),
            new LegalSymbols(Restriction.TRADEMARK, "TRADEMARK", "(TM)", "™", "(R)", "®")
        };

        /**
         * The restriction to use if an item in the {@linkplain #symbols} list
         * is found.
         */
        private final Restriction restriction;

        /**
         * Symbols to use as an indication that the {@linkplain #restriction}
         * applies.
         */
        private final String[] symbols;

        /**
         * Creates a new enumeration value for the given symbol.
         */
        private LegalSymbols(final Restriction restriction, final String... symbols) {
            this.restriction = restriction;
            this.symbols = symbols;
        }

        /**
         * Returns {@code true} if the given character is a space or a
         * punctuation of category "other". The punctuation characters include
         * coma, dot, semi-colon, <i>etc.</i> but do not include parenthesis or
         * connecting punctuation.
         *
         * @param c the Unicode code point of the character to test.
         */
        private static boolean isSpaceOrPunctuation(final int c) {
            switch (Character.getType(c)) {
                case Character.LINE_SEPARATOR:
                case Character.SPACE_SEPARATOR:
                case Character.PARAGRAPH_SEPARATOR:
                case Character.OTHER_PUNCTUATION:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Implementation of {@link MetadataBuilder#parseLegalNotice(String)},
         * provided here for reducing the amount of class loading in the common
         * case where there is no legal notice to parse.
         */
        static void parse(final String notice, final DefaultLegalConstraints constraints) {
            final int length = notice.length();
            final StringBuilder buffer = new StringBuilder(length);
            int year = 0;         // The copyright year, or 0 if none.
            int quoteLevel = 0;         // Incremented on ( [ « characters, decremented on ) ] » characters.
            boolean isCopyright = false;     // Whether the word parsed by previous iteration was "Copyright" or "(C)".
            boolean wasSeparator = true;      // Whether the caracter parsed by the previous iteration was a word separator.
            boolean wasPunctuation = true;      // Whether the previous character was a punctuation of Unicode category "other".
            boolean skipNextChars = true;      // Whether the next spaces and some punction characters should be ignored.
            parse:
            for (int i = 0; i < length;) {
                final int c = notice.codePointAt(i);
                final int n = Character.charCount(c);
                int quoteChange = 0;
                boolean isSeparator = false;
                boolean isPunctuation;
                switch (Character.getType(c)) {
                    case Character.INITIAL_QUOTE_PUNCTUATION:
                    case Character.START_PUNCTUATION: {
                        quoteChange = +1;                     //  ( [ «  etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    case Character.FINAL_QUOTE_PUNCTUATION:
                    case Character.END_PUNCTUATION: {
                        quoteChange = -1;                     //  ) ] »  etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    default: {                                  // Letter, digit, hyphen, etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    case Character.OTHER_PUNCTUATION: {         //  , . : ; / " etc. but not -.
                        isPunctuation = true;
                        isSeparator = true;
                        break;
                    }
                    case Character.LINE_SEPARATOR:
                    case Character.SPACE_SEPARATOR:
                    case Character.PARAGRAPH_SEPARATOR: {
                        isPunctuation = wasPunctuation;
                        isSeparator = true;
                        break;
                    }
                }
                if (wasSeparator && !isSeparator && quoteLevel == 0) {
                    /*
                     * Found the beginning of a new word. Ignore textes like "(C)" or "All rights reserved".
                     * Some of those textes are implied by the metadata where the legal notice will be stored.
                     */
                    for (final LegalSymbols r : VALUES) {
                        for (final String symbol : r.symbols) {
                            if (notice.regionMatches(true, i, symbol, 0, symbol.length())) {
                                final int after = i + symbol.length();
                                if (after >= length || isSpaceOrPunctuation(notice.codePointAt(after))) {
                                    isCopyright |= Restriction.COPYRIGHT.equals(r.restriction);
                                    constraints.getUseConstraints().add(r.restriction);
                                    wasPunctuation = true;      // Pretend that "Copyright" was followed by a coma.
                                    skipNextChars = true;      // Ignore spaces and punctuations until the next word.
                                    i = after;                  // Skip the "Copyright" (or other) word.
                                    continue parse;
                                }
                            }
                        }
                    }
                    /*
                     * If a copyright notice is followed by digits, assume that those digits are the copyright year.
                     * We require the year to be surrounded by punctuations in order to reduce the risk of confusion
                     * with postal addresses. So this block should accept "John, 1992." but not "1992 Nowhere road".
                     */
                    if (isCopyright && wasPunctuation && year == 0 && c >= '0' && c <= '9') {
                        int endOfDigits = i + n;            // After the last digit in sequence.
                        while (endOfDigits < length) {
                            final int d = notice.codePointAt(endOfDigits);
                            if (d < '0' || d > '9') {
                                break;
                            }
                            endOfDigits++;              // No need to use Character.charCount(s) here.
                        }
                        // Verify if the digits are followed by a punctuation.
                        final int endOfToken = CharSequences.skipLeadingWhitespaces(notice, endOfDigits, length);
                        if (endOfToken >= length || isSpaceOrPunctuation(notice.codePointAt(endOfToken))) {
                            try {
                                year = Integer.parseInt(notice.substring(i, endOfDigits));
                                if (year >= 1800 && year <= 9999) {                     // Those limits are arbitrary.
                                    skipNextChars = true;
                                    i = endOfToken;
                                    continue;
                                }
                                year = 0;                                               // Reject as not a copyright year.
                            } catch (NumberFormatException e) {
                                // Not an integer - ignore, will be handled as text.
                            }
                        }
                    }
                }
                /*
                 * End of the block that was executed at the beginning of each new word.
                 * Following is executed for every characters, except if the above block
                 * skipped a portion of the input string.
                 */
                wasPunctuation = isPunctuation;
                wasSeparator = isSeparator;
                quoteLevel += quoteChange;
                if (!skipNextChars && !Character.isIdentifierIgnorable(c)) {
                    buffer.appendCodePoint(c);
                }
                i += n;
            }
            /*
             * End of parsing. Omit trailing spaces and some punctuations if any, then store the result.
             */
            int i = buffer.length();
            while (i > 0) {
                final int c = buffer.codePointBefore(i);
                if (!isSpaceOrPunctuation(c)) {
                    break;
                }
                i -= Character.charCount(c);
            }
            final DefaultCitation c = new DefaultCitation(notice);
            if (year != 0) {
                final Date date = new Date(LocalDate.of(year, 1, 1).toEpochDay() * (24 * 60 * 60 * 1000L));
                c.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.IN_FORCE)));
            }
            if (i != 0) {
                buffer.setLength(i);
                c.setCitedResponsibleParties(Collections.singleton(new DefaultResponsibility(Role.OWNER, null,
                        new AbstractParty(buffer, null))));     // Same limitation than MetadataBuilder.party().
            }
            constraints.getReferences().add(c);
        }
    }

    /**
     * Parses the legal notice. The method expects a string of the form
     * “Copyright, John Smith, 1992. All rights reserved.” The result of above
     * example will be:
     *
     * {
     *
     * @preformat text Metadata   └─Identification info       └─Resource
     * constraints           ├─Use constraints……………………………… Copyright
     *           └─Reference               ├─Title……………………………………………… Copyright
     * (C), John Smith, 1992. All rights reserved.               ├─Date
     *               │   ├─Date……………………………………… 1992-01-01
     *               │   └─Date type………………………… In force               └─Cited
     * responsible party                   ├─Party
     *                   │   └─Name…………………………… John Smith
     *                   └─Role……………………………………… Owner }
     *
     * @param notice the legal notice, or {@code null} if none.
     */
    public final void parseLegalNotice(final String notice) {
        if (notice != null) {
            LegalSymbols.parse(notice, constraints());
        }
    }

    /**
     * Adds a minimal value for the current sample dimension. If a minimal value
     * was already defined, then the new value will set only if it is smaller
     * than the existing one. {@code NaN} values are ignored.
     *
     * @param value the minimal value to add to the existing range of sample
     * values, or {@code NaN}.
     */
    public final void addMinimumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMinValue();
            if (current == null || value < current) {
                sampleDimension.setMinValue(value);
            }
        }
    }


    /**
     * Adds a maximal value for the current sample dimension. If a maximal value
     * was already defined, then the new value will set only if it is greater
     * than the existing one. {@code NaN} values are ignored.
     *
     * @param value the maximal value to add to the existing range of sample
     * values, or {@code NaN}.
     */
    public final void addMaximumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMaxValue();
            if (current == null || value > current) {
                sampleDimension.setMaxValue(value);
            }
        }
    }

    /**
     * Adds a Identifier value for metadata. 
     * @param value the identifier value , or {@code null}.
     */

    public final void addIdentifier(final CharSequence value) {
        final String i18n = new String(value.toString());
        if (i18n != null) {
            final DefaultIdentifier identifier = identifier();
            identifier.setCode(i18n);
        }
    }
    /**
     * Adds a compression name.
     *
     * @param value the compression name, or {@code null}.
     * @param value the format name, or {@code null}.
     */
    public final void addCompression(final CharSequence value, final CharSequence format) {
        final InternationalString i18n = trim(value);
        final String i = format.toString();
        if (i != null || i18n != null) {
            final DefaultFormat creatformat = format();
            if (i18n != null) {
                creatformat.setFileDecompressionTechnique(append(creatformat.getFileDecompressionTechnique(), i18n));
            }
            if (i != null) {
                creatformat.setFormatSpecificationCitation(new DefaultCitation(i));
            }
        }
    }

//    public static void main(String[] args) {
//        MetadataBuilder a = new MetadataBuilder();
//        a.addIdentifier("sadafsf");
////        a.addAbstract("dfsfdsf");
//        a.addMaximumSampleValue(122342343);
////        a.addTitle("ffs");
////        a.parseLegalNotice("dfdsf");
//        a.addCompression("dfdf", "sfsfs");
//        System.out.println(a.result());
//    }
}
