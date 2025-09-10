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
package org.apache.sis.referencing.operation.provider;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import javax.measure.Unit;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Range;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.Deprecable;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.ProjectDirectories;


/**
 * Inserts comments with parameter names in the javadoc of parameters.
 * This class needs to be run explicitly; it is not part of JUnit tests.
 * After execution, files in the provider packages may be overwritten.
 * Developer should execute {@code "git diff"} and inspect the changes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ParameterNameTableGenerator extends SimpleFileVisitor<Path> {
    /**
     * Value as the kind of object expected in {@link DefaultParameterDescriptor}.
     */
    private static final Double ONE           =  1d,
                                POSITIVE_ZERO = +0d,
                                NEGATIVE_ZERO = -0d,
                                MIN_LONGITUDE = Longitude.MIN_VALUE,
                                MAX_LONGITUDE = Longitude.MAX_VALUE,
                                MIN_LATITUDE  =  Latitude.MIN_VALUE,
                                MAX_LATITUDE  =  Latitude.MAX_VALUE;

    /**
     * The directory of Java source code to scan.
     */
    private final Path directory;

    /**
     * Pattern of the lines to search.
     */
    private final Pattern toSearch;

    /**
     * A temporary buffer for creating lines of comment.
     */
    private final StringBuilder buffer;

    /**
     * All lines in the file being processed.
     */
    private List<String> lines;

    /**
     * For {@link #main(String[])} only.
     */
    private ParameterNameTableGenerator() {
        directory = new ProjectDirectories(getClass()).getSourcesPackageDirectory("core/sis-referencing");
        toSearch  = Pattern.compile(".*\\s+static\\s+.*ParameterDescriptor<\\w+>\\s*(\\w+)\\s*[=;].*");
        buffer    = new StringBuilder();
    }

    /**
     * Launches the insertion of comment lines.
     *
     * @param  args  ignored.
     * @throws IOException if an error occurred while reading or writing a file.
     */
    public static void main(final String[] args) throws IOException {
        final var cg = new ParameterNameTableGenerator();
        Files.walkFileTree(cg.directory, cg);
    }

    /**
     * Invoked before to enter in a sub-directory. This implementation skips all sub-directories.
     *
     * @param  dir    the directory in which to enter.
     * @param  attrs  ignored.
     * @return flag instructing whether to scan that directory or not.
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        return dir.equals(directory) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
    }

    /**
     * Invoked for each file in the scanned directory. If the file is a Java file,
     * searches for {@code ParameterDescriptor} declarations. Otherwise ignore.
     *
     * @param  file   the file.
     * @param  attrs  ignored.
     * @return {@link FileVisitResult#CONTINUE}.
     * @throws IOException if an error occurred while reading or writing the given file.
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final String name = file.getFileName().toString();
        if (name.endsWith(".java")) {
            addCommentsToJavaFile(file);
        }
        return super.visitFile(file, attrs);
    }

    /**
     * Returns the class for the given Java source file.
     */
    private Class<?> getClass(final Path file) throws ClassNotFoundException {
        String name = file.getFileName().toString();
        name = name.substring(0, name.lastIndexOf('.'));    // Remove the ".java" suffix.
        name = getClass().getPackageName() + '.' + name;
        return Class.forName(name);
    }

    /**
     * Finds the parameter descriptors in the given file, and add parameter names in comments.
     */
    private void addCommentsToJavaFile(final Path file) throws IOException {
        Class<?> classe = null;
        final Matcher matcher = toSearch.matcher("");
        lines = Files.readAllLines(file);
        for (int i=lines.size(); --i >= 0;) {
            final String line = lines.get(i);
            if (matcher.reset(line).matches()) {
                final String fieldName = matcher.group(1);
                final DefaultParameterDescriptor<?> descriptor;
                try {
                    if (classe == null) {
                        classe = getClass(file);
                    }
                    final Field field = classe.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    descriptor = (DefaultParameterDescriptor<?>) field.get(null);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                /*
                 * Find the line where to insert comments. We detect if the comment to insert already exists
                 * by looking for this class name (expected in a HTML comment) before the start of comments.
                 * If we find that line, then we delete everything between it and the end of comments before
                 * to regenerate them. Developer can execute "git diff" on the command line for checking if
                 * any lines changed as a result.
                 */
                int insertAt = i;
                String previous;
                do {
                    previous = lines.get(--insertAt).trim();
                    if (!previous.startsWith("*") && !previous.startsWith("@")) {
                        fail("Unexpected content in " + file.getFileName() + " at line " + insertAt);
                    }
                } while (!previous.equals("*/"));
                previous = lines.get(insertAt);
                buffer.setLength(0);
                buffer.append(previous, 0, previous.indexOf('*') + 1);
                for (int check = insertAt;;) {
                    previous = lines.get(--check).trim();
                    if (previous.equals("/**")) break;
                    if (previous.contains("ParameterNameTableGenerator")) {
                        lines.subList(check, insertAt).clear();
                        insertAt = check;
                        break;
                    }
                }
                /*
                 * Format a HTML table in the comment with the name and aliases of each parameter.
                 */
                write(insertAt++, "<!-- Generated by ParameterNameTableGenerator -->");
                write(insertAt++, "<table class=\"sis\">");
                write(insertAt++, "  <caption>Parameter names</caption>");
                write(insertAt++, descriptor.getName(), false);
                for (final GenericName alias : descriptor.getAlias()) {
                    write(insertAt++, (alias instanceof Identifier) ? (Identifier) alias : new NamedIdentifier(alias),
                                       alias instanceof Deprecable && ((Deprecable) alias).isDeprecated());
                }
                write(insertAt++, "</table>");
                /*
                 * Format other information: default value, value domain, whether the value is mandatory, etc.
                 * Default value of zero are omitted (i.e. unless otherwise specified, default values are zero
                 * in the tables that we format). Range of values of [-90 … 90]° for latitude or [-180 … 180]°
                 * for longitude are also omitted. In other words, we report only "unusual" things in the notes.
                 */
                Object  defaultValue = descriptor.getDefaultValue();
                Range<?> valueDomain = descriptor.getValueDomain();
                boolean  isOptional  = descriptor.getMinimumOccurs() == 0;
                boolean  noDefault   = (defaultValue == null);
                Object   minValue    = null;
                Object   maxValue    = null;
                if (valueDomain != null) {
                    minValue = valueDomain.getMinValue();
                    maxValue = valueDomain.getMaxValue();
                    final boolean inclusive = valueDomain.isMinIncluded() && valueDomain.isMaxIncluded();
                    if (fieldName.contains("LATITUDE") || fieldName.contains("PARALLEL")) {
                        if (inclusive && MIN_LATITUDE.equals(minValue) && MAX_LATITUDE.equals(maxValue)) {
                            valueDomain = null;
                        }
                    } else if (fieldName.contains("LONGITUDE") || fieldName.contains("MERIDIAN")) {
                        if (inclusive && MIN_LONGITUDE.equals(minValue) && MAX_LONGITUDE.equals(maxValue)) {
                            valueDomain = null;
                        }
                    } else if (fieldName.contains("SCALE")) {
                        if (!inclusive && POSITIVE_ZERO.equals(minValue) && maxValue == null) {
                            valueDomain = null;
                        }
                    } else if (minValue == null && maxValue == null) {
                        valueDomain = null;
                    }
                }
                if ((fieldName.contains("SCALE") ? ONE : POSITIVE_ZERO).equals(defaultValue)) {
                    defaultValue = null;
                }
                if (defaultValue != null || valueDomain != null || isOptional || noDefault) {
                    write(insertAt++, "<b>Notes:</b>");
                    write(insertAt++, "<ul>");
                    if (valueDomain != null) {
                        final int p = buffer.length();
                        buffer.append("   <li>");
                        if ((minValue != null && minValue.equals(maxValue)) ||
                            (NEGATIVE_ZERO.equals(minValue) && POSITIVE_ZERO.equals(maxValue)))
                        {
                            buffer.append("Value restricted to ").append(maxValue);
                            StringBuilders.trimFractionalPart(buffer);
                        } else {
                            buffer.append("Value domain: ").append(valueDomain);
                        }
                        lines.add(insertAt++, buffer.append("</li>").toString());
                        buffer.setLength(p);
                    }
                    if (defaultValue != null) {
                        final int p = buffer.length();
                        final boolean isText = !(defaultValue instanceof Number || defaultValue instanceof Angle);
                        buffer.append("   <li>").append("Default value: ");
                        if (isText) buffer.append("{@code ");
                        buffer.append(defaultValue);
                        if (isText) buffer.append('}');
                        StringBuilders.trimFractionalPart(buffer);
                        final Unit<?> unit = descriptor.getUnit();
                        if (unit != null) {
                            final String symbol = unit.getSymbol();
                            if (!symbol.isEmpty()) {
                                if (Character.isLetterOrDigit(symbol.charAt(0))) {
                                    buffer.append(' ');
                                }
                                buffer.append(symbol);
                            }
                        }
                        lines.add(insertAt++, buffer.append("</li>").toString());
                        buffer.setLength(p);
                    } else if (noDefault) {
                        write(insertAt++, "  <li>No default value</li>");
                    }
                    if (isOptional) {
                        write(insertAt++, "  <li>Optional</li>");
                    }
                    write(insertAt++, "</ul>");
                }
            }
        }
        /*
         * If at least one table has been formatted, rewrite the file.
         */
        if (classe != null) {
            Files.write(file, lines);
        }
        lines = null;
    }

    /**
     * Appends the given line at the given position. This method writes the margin
     * (typically spaces followed by {@code '*'} and a single space) before the line.
     */
    private void write(final int insertAt, final String line) {
        final int p = buffer.length();
        if (!line.isEmpty()) {
            buffer.append(' ').append(line);
        }
        lines.add(insertAt, buffer.toString());
        buffer.setLength(p);
    }

    /**
     * Appends the given authority and name at the given position. This method writes the margin
     * (typically spaces followed by {@code '*'} and a single space) before the line.
     */
    private void write(final int insertAt, final Identifier id, final boolean isDeprecated) {
        final int p = buffer.length();
        final String authority = id.getCodeSpace();
        buffer.append("   <tr><td> ").append(authority).append(':')
              .append(CharSequences.spaces(8 - authority.length()))
              .append("</td><td> ");
        if (isDeprecated) buffer.append("<del>");
        buffer.append(id.getCode());
        if (isDeprecated) buffer.append("</del>");
        buffer.append(" </td></tr>");
        lines.add(insertAt, buffer.toString());
        buffer.setLength(p);
    }
}
