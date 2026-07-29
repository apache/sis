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
package org.apache.sis.storage.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.StringBuilders;


/**
 *
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public final class StringUtilities {

    public static final String TREE_BLANK = "\u00A0\u00A0\u00A0\u00A0";
    public static final String TREE_LINE  = "\u00A0\u00A0\u2502\u00A0";
    public static final String TREE_CROSS = "\u00A0\u00A0\u251C\u2500";
    public static final String TREE_END   = "\u00A0\u00A0\u2514\u2500";
    private static final String START = "\u001B[";
    private static final char END = 'm';

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private StringUtilities() {}

    /**
     * Encode the specified string with MD5 algorithm.
     *
     * @param key   the string to encode.
     * @return the value (string) hexadecimal on 32 bits
     */
    public static String MD5encode(final String key) {

        final byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            // we get an object allowing to crypt the string
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);

        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        final StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            final String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    /**
     * Clean a string from its leading and trailing whitespaces, and the tabulation or end of line
     * characters.
     */
    public static String clean(String s) {
        s = s.trim();
        s = s.replace("\t", "");
        s = s.replace("\n", "");
        s = s.replace("\r", "");
        return s;
    }

    /**
     * Clean a list of String by removing all the white space, tabulation and carriage in all the strings.
     */
    public static List<String> cleanCharSequences(final List<String> list) {
        final List<String> result = new ArrayList<String>();
        for (String s : list) {
            //we remove the bad character before the real value
           s = s.replace(" ", "");
           s = s.replace("\t", "");
           s = s.replace("\n", "");
           result.add(s);
        }
        return result;
    }

    /**
     * Returns true if the list contains a string in one of the list elements.
     * This test is not case sensitive.
     *
     * @see org.apache.sis.util.ArraysExt#containsIgnoreCase(String[], String)
     *
     * @param list A list of String.
     * @param str The value searched.
     */
    public static boolean containsIgnoreCase(final List<String> list, final String str) {
        boolean strAvailable = false;
        if (list != null) {
            for (String s : list) {
                if (s.equalsIgnoreCase(str)) {
                    strAvailable = true;
                    break;
                }
            }
        }
        return strAvailable;
    }

    /**
     * Generate a {@code String} whose first character will be in upper case.
     * <p>
     * For example: {@code firstToUpper("hello")} would return {@code "Hello"},
     * while {@code firstToUpper("Hi!") would return {@code "Hi!"} unchanged.
     *
     * @param s The {@code String} to evaluate, not {@code null}.
     *
     * @return A {@code String} with the first character in upper case.
     */
    public static String firstToUpper(final String s) {
        if (s != null && !s.isEmpty()) {
            final String first = s.substring(0, 1);
            String result      = s.substring(1);
            result             = first.toUpperCase() + result;
            return result;
        }
        return s;
    }

    /**
     * Search a string for all occurrence of the char.
     *
     * @param s  String to search in
     * @param occ  Occurrence to search
     * @return array of all occurrence indexes
     */
    public static int[] getIndexes(final String s, final char occ) {
        int pos = s.indexOf(occ);
        if (pos <0) {
            return EMPTY_INT_ARRAY;
        } else {
            int[] indexes = new int[]{pos};
            pos = s.indexOf(occ, pos+1);
            for(; pos >= 0; pos = s.indexOf(occ, pos+1)){
                int end = indexes.length;
                indexes = ArraysExt.resize(indexes, end+1);
                indexes[end] = pos;
            }
            return indexes;
        }

    }

    /**
     * Convert the given string into a string recognize by HTML.
     *
     * @param text The string to convert.
     * @return The string wit no special chars, which are not recognized in HTML.
     */
    public static String htmlEncodeSpecialChars(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i<text.length(); i++) {
            final char c = text.charAt(i);
            if (c > 127) { // special chars
                sb.append("&#").append((int)c).append(";");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if one of the {@code String} elements in a {@code List}
     * matches the given {@code String}, insensitive to case.
     *
     * @param list A {@code List<String>} with elements to be tested.
     * @param str  The {@code String} to evaluate.
     *
     * @return {@code true}, if at least one element of the list matches the
     *           parameter, {@code false} otherwise.
     */
    public static boolean matchesStringfromList(final List<String> list,final String str) {
        boolean strAvailable = false;
        for (String s : list) {
            final Pattern pattern = Pattern.compile(str,Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ);
            final Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                strAvailable = true;
            }
        }
        return strAvailable;
    }

    /**
     * Replace all the <ns**:localPart and </ns**:localPart by <prefix:localPart and </prefix:localPart>
     */
    public static String replacePrefix(final String s, final String localPart, final String prefix) {
        return s.replaceAll("[a-zA-Z0-9]*:" + localPart, prefix + ":" + localPart);
    }

    /**
     * Remove all the XML namespace declaration.
     *
     * @deprecated Used only in test classes. Tests should use XML comparator instead.
     */
    @Deprecated
    public static String removeXmlns(final String xml) {
        String s = xml;
        s = s.replaceAll("xmlns=\"[^\"]*\" ", "");
        s = s.replaceAll("xmlns=\"[^\"]*\"", "");
        s = s.replaceAll("xmlns:[^=]*=\"[^\"]*\" ", "");
        s = s.replaceAll("xmlns:[^=]*=\"[^\"]*\"", "");
        return s;
    }

    /**
     * Remove the prefix on propertyName.
     * example : removePrefix(csw:GetRecords) return "GetRecords".
     */
    public static String removePrefix(String s) {
        final int i = s.indexOf(':');
        if ( i != -1) {
            s = s.substring(i + 1, s.length());
        }
        return s;
    }

    /**
     * Returns the values of the list separated by commas.
     *
     * @param values The collection to extract values.
     * @return A string which contains values concatened with comma(s), or an empty
     *         string if the list is empty or {@code null}.
     */
    public static String toCommaSeparatedValues(final Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(Object obj : values){
            if(first){
                first = false;
            }else{
                builder.append(',');
            }
            builder.append(obj);
        }
        return builder.toString();
    }

    /**
     * Returns the values of the array separated by commas.
     *
     * @param values The array to extract values.
     * @return A string which contains values concatened with comma(s), or an empty
     *         string if the array is empty or {@code null}.
     */
    public static String toCommaSeparatedValues(final Object ... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        final int size = values.length;
        for (int i=0; i<size; i++) {
            if (i>0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    /**
     * Split a string on commas, and return it as a list.
     *
     * @param commaSeparatedString A string which contains comma(s).
     * @return A list of elements that were contained in the string separated by comma(s).
     */
    public static List<String> toStringList(final String commaSeparatedString) {
        return toStringList(commaSeparatedString, ',');
    }

    /**
     * Split a string on a special character, and return it as a list.
     *
     * @param toSplit   A string to split on a specific character.
     * @param separator The special character on which the given string will be splitted.
     * @return A list of elements that were contained in the string separated by the special
     *         character.
     *
     * @deprecated Replaced by {@link org.apache.sis.util.CharSequences#split(CharSequence, char)}.
     */
    @Deprecated
    public static List<String> toStringList(String toSplit, final char separator) {
        if (toSplit == null) {
            return Collections.emptyList();
        }
        final List<String> strings = new ArrayList<>();
        int last = 0;
        toSplit = toSplit.trim();
        for (int i=toSplit.indexOf(separator); i>=0; i=toSplit.indexOf(separator, i)) {
            strings.add(toSplit.substring(last, i).trim());
            last = ++i;
        }
        strings.add(toSplit.substring(last).trim());
        return strings;
    }

    /**
     * Formats the given elements as a (typically) comma-separated list. This method is similar to
     * {@link java.util.AbstractCollection#toString()} or {@link java.util.Arrays#toString(Object[])}
     * except for the following:
     *
     * <ul>
     *   <li>There is no leading {@code '['} or trailing {@code ']'} characters.</li>
     *   <li>Null elements are ignored instead than formatted as {@code "null"}.</li>
     *   <li>If the {@code collection} argument is null or contains only null elements,
     *       then this method returns {@code null}.</li>
     *   <li>In the common case where the collection contains a single {@link String} element,
     *       that string is returned directly (no object duplication).</li>
     * </ul>
     *
     * @param  collection The elements to format in a (typically) comma-separated list, or {@code null}.
     * @param  separator  The element separator, which is usually {@code ", "}.
     * @return The (typically) comma-separated list, or {@code null} if the given {@code collection}
     *         was null or contains only null elements.
     *
     * @see java.util.StringJoiner
     * @see java.util.Arrays#toString(Object[])
     */
    public static String toString(final Iterable<?> collection, final String separator) {
        ArgumentChecks.ensureNonNull("separator", separator);
        String list = null;
        if (collection != null) {
            StringBuilder buffer = null;
            for (final Object element : collection) {
                if (element != null) {
                    if (list == null) {
                        list = element.toString();
                    } else {
                        if (buffer == null) {
                            buffer = new StringBuilder(list);
                        }
                        buffer.append(separator);
                        if (element instanceof CharSequence) {
                            // StringBuilder has numerous optimizations for this case.
                            buffer.append((CharSequence) element);
                        } else {
                            buffer.append(element);
                        }
                    }
                }
            }
            if (buffer != null) {
                list = buffer.toString();
            }
        }
        return list;
    }

    /**
     * Transform an exception code into the OWS specification.
     * Example : MISSING_PARAMETER_VALUE become MissingParameterValue.
     */
    public static String transformCodeName(String code) {
        final StringBuilder result = new StringBuilder();
        while (code.indexOf('_') != -1) {
            final String tmp = code.substring(0, code.indexOf('_')).toLowerCase();
            result.append(firstToUpper(tmp));
            code = code.substring(code.indexOf('_') + 1, code.length());
        }
        code = code.toLowerCase();
        result.append(firstToUpper(code));
        return result.toString();
    }

    public static String toStringTree(final Object ... objects){
        return toStringTree("", Arrays.asList(objects));
    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    public static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "\u2514\u2500 " : "\u251C\u2500 ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '\u2502');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }


    /**
     * Replaces escape codes in the given string by HTML {@code <font>} instructions.
     * If no HTML instruction is associated to the given escape code, then the escape
     * sequence is removed.
     *
     * @param  text The text with X3.64 sequences.
     * @return The text with HTML {@code <font>} instructions.
     */
    public static String X364toHTML(final String text) {

        final StringBuilder buffer = new StringBuilder(text);
        StringBuilders.replace(buffer, "&", "&amp;");
        StringBuilders.replace(buffer, "<", "&lt;");
        StringBuilders.replace(buffer, ">", "&gt;");
        boolean fontApplied = false;
        StringBuilder tmp = null;
        for (int i=buffer.indexOf(START); i>=0; i=buffer.indexOf(START, i)) {
            int lower  = i + START.length();
            int upper  = lower;
            int length = buffer.length();
            while (upper < length) {
                if (buffer.charAt(upper++) == END) {
                    break;
                }
            }
            final int code;
            try {
                code = Integer.parseInt(buffer.substring(lower, upper-1));
            } catch (NumberFormatException e) {
                buffer.delete(i, upper);
                continue;
            }
            final String color;
            switch (code) {
                case 31: color="red";     break;
                case 32: color="green";   break;
                case 33: color="olive";   break; // "yellow" is too bright.
                case 34: color="blue";    break;
                case 35: color="magenta"; break;
                case 36: color="teal";    break; // "cyan" is not in HTML 4, while "teal" is.
                case 37: color="gray";    break;
                case 39: // Fall through
                case 0:  color=null; break;
                default: {
                    buffer.delete(i, upper);
                    continue;
                }
            }
            if (tmp == null) {
                tmp = new StringBuilder(24);
            }
            if (fontApplied) {
                tmp.append("</font>");
                fontApplied = false;
            }
            if (color != null) {
                tmp.append("<font color=\"").append(color).append("\">");
                fontApplied = true;
            }
            buffer.replace(i, upper, tmp.toString());
            tmp.setLength(0);
        }
        final String result = buffer.toString();
        return result.equals(text) ? text : result;
    }
}
