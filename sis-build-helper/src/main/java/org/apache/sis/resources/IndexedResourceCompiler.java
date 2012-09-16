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
package org.apache.sis.resources;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import java.util.regex.Pattern;


/**
 * Reads a given list of {@code .properties} files and copies their content to {@code .utf} files
 * using UTF-8 encoding. It also checks for key validity and checks values for {@link MessageFormat}
 * compatibility. Finally, it writes the key values in the Java source files.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.2)
 * @version 0.3
 */
public class IndexedResourceCompiler implements FilenameFilter, Comparator<Object> {
    /**
     * Extension for java source files.
     */
    static final String JAVA_EXT = ".java";

    /**
     * Extension for properties source files.
     */
    static final String PROPERTIES_EXT = ".properties";

    /**
     * Extension for resource target files.
     */
    private static final String RESOURCES_EXT = ".utf";

    /**
     * Prefix for argument count in resource key names. For example, a resource
     * expecting one argument may have a key name like "{@code HelloWorld_1}".
     */
    private static final String ARGUMENT_COUNT_PREFIX = "_";

    /**
     * The maximal length of comment lines.
     */
    private static final int COMMENT_LENGTH = 92;

    /**
     * The name of the inner class which will contains key values.
     */
    private static final String KEYS_INNER_CLASS = "Keys";

    /**
     * Encoding of Java source file (<strong>not</strong> property files).
     */
    private static final String JAVA_ENCODING = "UTF-8";

    /**
     * The Java modifiers applies on the key constants to be generated.
     */
    private static final String KEY_MODIFIERS = "public static final int ";

    /**
     * The source directory of the java and properties files.
     */
    private final File sourceDirectory;

    /**
     * The target directory where to write the UTF files.
     */
    private final File buildDirectory;

    /**
     * The resource bundle base class being processed.
     * Example: {@code org/apache/sis/resources/Vocabulary.java}.
     */
    private File bundleClass;

    /**
     * Integer IDs allocated to resource keys. This map will be shared for all languages
     * of a given resource bundle.
     */
    private final Map<Integer,String> allocatedIDs = new HashMap<Integer,String>();

    /**
     * Resource keys and their localized values. This map will be cleared for each language
     * in a resource bundle.
     */
    private final Map<Object,Object> resources = new HashMap<Object,Object>();

    /**
     * The resources bundle base classes.
     */
    private final File[] resourcesToProcess;

    /**
     * Number of errors found.
     */
    private int errors;

    /**
     * Constructs a new {@code IndexedResourceCompiler}.
     *
     * @param sourceDirectory The source directory.
     * @param buildDirectory  The target directory where to write UTF files.
     * @param resourcesToProcess The resource bundle base classes
     *        (e.g. {@code org/apache/sis/resources/Vocabulary.java}).
     */
    public IndexedResourceCompiler(final File sourceDirectory, final File buildDirectory,
            final File[] resourcesToProcess)
    {
        this.sourceDirectory    = sourceDirectory;
        this.buildDirectory     = buildDirectory;
        this.resourcesToProcess = resourcesToProcess;
    }

    /**
     * Runs the resource compiler.
     *
     * @throws ResourceCompilerException If an error occurred.
     * @return The number of errors found.
     */
    public int run() throws ResourceCompilerException {
        if (!sourceDirectory.isDirectory()) {
            throw new ResourceCompilerException(sourceDirectory + " not found or is not a directory.");
        }
        for (int i=0; i<resourcesToProcess.length; i++) {
            bundleClass = resourcesToProcess[i];
            allocatedIDs.clear();
            resources.clear();
            try {
                loadKeyValues();
                scanForResources();
            } catch (IOException exception) {
                throw new ResourceCompilerException(exception);
            }
        }
        return errors;
    }

    /**
     * Loads the existing key values from the source file. This is used in order to avoid
     * the need to recompile the whole application when new entries are added.
     *
     * @throws IOException If an error occurred while reading the source file.
     */
    private void loadKeyValues() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(bundleClass), JAVA_ENCODING));
        String line;
        while ((line = in.readLine()) != null) {
            if ((line = line.trim()).startsWith(KEY_MODIFIERS)) {
                final int s = line.indexOf('=', KEY_MODIFIERS.length());
                if (s >= 0) {
                    final int c = line.indexOf(';', s);
                    if (c >= 0) {
                        final String key = line.substring(KEY_MODIFIERS.length(), s).trim();
                        final Integer ID = Integer.valueOf(line.substring(s+1, c).trim());
                        final String old = allocatedIDs.put(ID, key);
                        if (old != null) {
                            warning("Key " + ID + " is used by " + old + " and " + key);
                            errors++;
                        }
                    }
                }
            }
        }
        in.close();
    }

    /**
     * Returns the path of the given file relative to the given directory.
     */
    private static String relative(final File directory, final File file) throws IOException {
        String path = file.getPath();
        final String expected = directory.getPath();
        if (!path.startsWith(expected)) {
            throw new IOException(path + " is not relative to " + expected);
        }
        path = path.substring(expected.length());
        if (!path.startsWith(File.separator)) {
            path = path.substring(File.separator.length());
        }
        return path;
    }

    /**
     * Scans the package of {@link #bundleClass} for its {@code .properties} files.
     * The following methods are invoked by this method:
     *
     * <ul>
     *   <li>{@link #loadProperties}</li>
     *   <li>{@link #writeUTF}</li>
     *   <li>{@link #writeJavaSource}</li>
     * </ul>
     *
     * @throws IOException if an input/output operation failed.
     */
    private void scanForResources() throws IOException {
        String classname = bundleClass.getName();
        classname = classname.substring(0, classname.lastIndexOf('.'));
        final File srcDir = bundleClass.getParentFile();
        final File utfDir = new File(buildDirectory, relative(sourceDirectory, srcDir));
        if (!srcDir.isDirectory()) {
            throw new FileNotFoundException("\"" + srcDir + "\" is not a directory.");
        }
        if (utfDir.exists() && !utfDir.isDirectory()) {
            throw new FileNotFoundException("\"" + utfDir + "\" is not a directory.");
        }
        final File[] content = srcDir.listFiles(this);
        File defaultLanguage = null;
        for (int i=0; i<content.length; i++) {
            final File file = content[i];
            final String filename = file.getName();
            if (filename.startsWith(classname)) {
                loadProperties(file);
                final String noExt = filename.substring(0, filename.length() - PROPERTIES_EXT.length());
                final File utfFile = new File(utfDir, noExt + RESOURCES_EXT);
                writeUTF(utfFile);
                if (noExt.equals(classname)) {
                    defaultLanguage = file;
                }
            }
        }
        if (defaultLanguage != null) {
            resources.clear();
            resources.putAll(loadRawProperties(defaultLanguage));
        }
        writeJavaSource();
    }

    /**
     * Returns {@code true} if the given file is a property file.
     *
     * @param directory The directory (ignored).
     * @param name The file name.
     * @return {@code true} if the given file is a property file.
     */
    @Override
    public final boolean accept(final File directory, final String name) {
        return name.endsWith(PROPERTIES_EXT);
    }

    /**
     * Loads the specified property file. No processing are performed on them.
     *
     * @param  file The property file to load.
     * @return The properties.
     * @throws IOException if the file can not be read.
     */
    private static Properties loadRawProperties(final File file) throws IOException {
        final InputStream input = new FileInputStream(file);
        final Properties properties = new Properties();
        properties.load(input);
        input.close();
        return properties;
    }

    /**
     * Loads all properties from a {@code .properties} file. Resource keys are checked for naming
     * conventions (i.e. resources expecting some arguments must have a key name ending with
     * {@code "_$n"} where {@code "n"} is the number of arguments). This method transforms resource
     * values into legal {@link MessageFormat} patterns when necessary.
     * <p>
     * The following methods must be invoked before this one:
     *
     * <ul>
     *   <li>{@link #initialize}</li>
     *   <li>{@link #setResourceBundle}</li>
     * </ul>
     *
     * @param  file The properties file to read.
     * @throws IOException if an input/output operation failed.
     */
    private void loadProperties(final File file) throws IOException {
        resources.clear();
        final Properties properties = loadRawProperties(file);
        for (final Map.Entry<Object,Object> entry : properties.entrySet()) {
            final String key   = (String) entry.getKey();
            final String value = (String) entry.getValue();
            /*
             * Checks key and value validity.
             */
            if (key.trim().length() == 0) {
                warning(file, key, "Empty key.", null);
                continue;
            }
            if (value.trim().length() == 0) {
                warning(file, key, "Empty value.", null);
                continue;
            }
            /*
             * Checks if the resource value is a legal MessageFormat pattern.
             */
            final MessageFormat message;
            try {
                message = new MessageFormat(toMessageFormatString(value));
            } catch (IllegalArgumentException exception) {
                warning(file, key, "Bad resource value", exception);
                continue;
            }
            /*
             * Checks if the expected arguments count (according to naming conventions)
             * matches the arguments count found in the MessageFormat pattern.
             */
            final int argumentCount;
            final int index = key.lastIndexOf(ARGUMENT_COUNT_PREFIX);
            if (index < 0) {
                argumentCount = 0;
                resources.put(key, value); // Text will not be formatted using MessageFormat.
            } else try {
                String suffix = key.substring(index + ARGUMENT_COUNT_PREFIX.length());
                argumentCount = Integer.parseInt(suffix);
                resources.put(key, message.toPattern());
            } catch (NumberFormatException exception) {
                warning(file, key, "Bad number in resource key", exception);
                continue;
            }
            final int expected = message.getFormatsByArgumentIndex().length;
            if (argumentCount != expected) {
                final String suffix = ARGUMENT_COUNT_PREFIX + expected;
                warning(file, key, "Key name should ends with \"" + suffix + "\".", null);
                continue;
            }
        }
        /*
         * Allocates an ID for each new key.
         */
        final String[] keys = resources.keySet().toArray(new String[resources.size()]);
        Arrays.sort(keys, this);
        int freeID = 0;
        for (int i=0; i<keys.length; i++) {
            final String key = keys[i];
            if (!allocatedIDs.containsValue(key)) {
                Integer ID;
                do {
                    ID = freeID++;
                } while (allocatedIDs.containsKey(ID));
                allocatedIDs.put(ID, key);
            }
        }
    }

    /**
     * Changes a "normal" text string into a pattern compatible with {@link MessageFormat}.
     * The main operation consists of changing ' for '', except for '{' and '}' strings.
     */
    private static String toMessageFormatString(final String text) {
        int level =  0;
        int last  = -1;
        final StringBuilder buffer = new StringBuilder(text);
search: for (int i=0; i<buffer.length(); i++) { // Length of 'buffer' will vary.
            switch (buffer.charAt(i)) {
                /*
                 * Left and right braces take us up or down a level.  Quotes will only be doubled
                 * if we are at level 0.  If the brace is between quotes it will not be taken into
                 * account as it will have been skipped over during the previous pass through the
                 * loop.
                 */
                case '{' : level++; last=i; break;
                case '}' : level--; last=i; break;
                case '\'': {
                    /*
                     * If a brace ('{' or '}') is found between quotes, the entire block is
                     * ignored and we continue with the character following the closing quote.
                     */
                    if (i+2 < buffer.length()  &&  buffer.charAt(i+2) == '\'') {
                        switch (buffer.charAt(i+1)) {
                            case '{': i += 2; continue search;
                            case '}': i += 2; continue search;
                        }
                    }
                    if (level <= 0) {
                        /*
                         * If we weren't between braces, we must double the quotes.
                         */
                        buffer.insert(i++, '\'');
                        continue search;
                    }
                    /*
                     * If we find ourselves between braces, we don't normally need to double
                     * our quotes.  However, the format {0,choice,...} is an exception.
                     */
                    if (last >= 0  &&  buffer.charAt(last) == '{') {
                        int scan = last;
                        do if (scan >= i) continue search;
                        while (Character.isDigit(buffer.charAt(++scan)));
                        final String choice = ",choice,";
                        final int end = scan + choice.length();
                        if (end < buffer.length() && buffer.substring(scan, end).equalsIgnoreCase(choice)) {
                            buffer.insert(i++, '\'');
                            continue search;
                        }
                    }
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Writes UTF file. The following methods must be invoked before this one:
     *
     * <ul>
     *   <li>{@link #initialize}</li>
     *   <li>{@link #setResourceBundle}</li>
     *   <li>{@link #loadProperties}</li>
     * </ul>
     *
     * @param  file The destination file.
     * @throws IOException if an input/output operation failed.
     */
    private void writeUTF(final File file) throws IOException {
        final File directory = file.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Can't create the " + directory + " directory.");
        }
        final int count = allocatedIDs.isEmpty() ? 0 : Collections.max(allocatedIDs.keySet()) + 1;
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeInt(count);
        for (int i=0; i<count; i++) {
            final String value = (String) resources.get(allocatedIDs.get(i));
            out.writeUTF((value != null) ? value : "");
        }
        out.close();
    }

    /**
     * Creates a source file for resource keys.
     * The following methods must be invoked before this one:
     *
     * <ul>
     *   <li>{@link #initialize}</li>
     *   <li>{@link #setResourceBundle}</li>
     *   <li>{@link #loadProperties}</li>
     * </ul>
     *
     * @throws IOException if an input/output operation failed.
     */
    private void writeJavaSource() throws IOException {
        /*
         * Opens the source file for reading. We will copy a subset of its content in a buffer.
         */
        final File file = bundleClass;
        if (!file.getParentFile().isDirectory()) {
            throw new FileNotFoundException("Parent directory not found for " + file);
        }
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), JAVA_ENCODING));
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuilder buffer = new StringBuilder(4096);
        /*
         * Copies everything up to (including) the declaration of the Keys inner class.
         * The declaration must follow Sun's convention on brace location (i.e. must be
         * on the same line than the class declaration).
         */
        final Pattern classKeys = Pattern.compile("[\\s\\w]*class\\s+" + KEYS_INNER_CLASS + "\\s*\\{");
        String line;
        do {
            line = in.readLine();
            if (line == null) {
                in.close();
                throw new EOFException(file.toString());
            }
            buffer.append(line).append(lineSeparator);
        } while (!classKeys.matcher(line).matches());
        /*
         * Writes the constructor, then write keys values.
         * We stopped reading the file for now (will continue later).
         */
        final String margin = "        "; // 8 spaces
        buffer.append(margin).append("private ").append(KEYS_INNER_CLASS).append("() {").append(lineSeparator)
              .append(margin).append('}').append(lineSeparator);
        final Map.Entry<?,?>[] entries = allocatedIDs.entrySet().toArray(new Map.Entry<?,?>[allocatedIDs.size()]);
        Arrays.sort(entries, this);
        for (int i=0; i<entries.length; i++) {
            buffer.append(lineSeparator);
            final String key = (String) entries[i].getValue();
            final String ID  = entries[i].getKey().toString();
            String message = (String) resources.get(key);
            if (message != null) {
                message = message.replace('\t', ' ');
                buffer.append(margin).append("/**").append(lineSeparator);
                while (((message=message.trim()).length()) != 0) {
                    buffer.append(margin).append(" * ");
                    int stop = message.indexOf('\n');
                    if (stop < 0) {
                        stop = message.length();
                    }
                    if (stop > COMMENT_LENGTH) {
                        stop = COMMENT_LENGTH;
                        while (stop>20 && !Character.isWhitespace(message.charAt(stop))) {
                            stop--;
                        }
                    }
                    buffer.append(message.substring(0, stop).trim()).append(lineSeparator);
                    message = message.substring(stop);
                }
                buffer.append(margin).append(" */").append(lineSeparator);
            }
            buffer.append(margin).append(KEY_MODIFIERS).append(key).append(" = ")
                    .append(ID).append(';').append(lineSeparator);
        }
        /*
         * Continue reading the input file, skipping the old key values.
         * Once we have reached the closing bracket, copies all remaining lines.
         */
        int brackets = 1;
        do {
            line = in.readLine();
            if (line == null) {
                in.close();
                throw new EOFException(file.toString());
            }
            for (int i=0; i<line.length(); i++) {
                switch (line.charAt(i)) {
                    case '{': brackets++; break;
                    case '}': brackets--; break;
                }
            }
        } while (brackets != 0);
        buffer.append(line).append(lineSeparator);
        while ((line = in.readLine()) != null) {
            buffer.append(line).append(lineSeparator);
        }
        in.close();
        /*
         * Now writes the results to disk, overwriting the original file.
         */
        final Writer out = new OutputStreamWriter(new FileOutputStream(file), JAVA_ENCODING);
        out.write(buffer.toString());
        out.close();
    }

    /**
     * Compares two resource keys. Object {@code o1} and {@code o2} are usually {@link String}
     * objects representing resource keys (for example, "{@code MISMATCHED_DIMENSION}"), but
     * may also be {@link java.util.Map.Entry}.
     *
     * @param  o1 The resource key to compare.
     * @param  o2 The second resource key to compare.
     * @return -1, 0 or +1 based on the alphabetic order of resource keys.
     */
    @Override
    public final int compare(Object o1, Object o2) {
        if (o1 instanceof Map.Entry<?,?>) o1 = ((Map.Entry<?,?>) o1).getValue();
        if (o2 instanceof Map.Entry<?,?>) o2 = ((Map.Entry<?,?>) o2).getValue();
        final String key1 = (String) o1;
        final String key2 = (String) o2;
        return key1.compareTo(key2);
    }

    /**
     * Logs the given message at the {@code INFO} level.
     * The default implementation just sent it to the standard output stream.
     *
     * @param message The message to log.
     */
    protected void info(final String message) {
        System.out.println(message);
    }

    /**
     * Logs the given message at the {@code WARNING} level.
     * The default implementation just sent it to the standard output stream.
     *
     * @param message The message to log.
     */
    protected void warning(final String message) {
        System.out.println(message);
    }

    /**
     * Logs the given message at the {@code WARNING} level.
     *
     * @param file      File that produced the error, or {@code null} if none.
     * @param key       Resource key that produced the error, or {@code null} if none.
     * @param message   The message string.
     * @param exception An optional exception that is the cause of this warning.
     */
    private void warning(final File file,      final String key,
                         final String message, final Exception exception)
    {
        final StringBuilder buffer = new StringBuilder("ERROR ");
        if (file != null) {
            String filename = file.getPath();
            if (filename.endsWith(PROPERTIES_EXT)) {
                filename = filename.substring(0, filename.length() - PROPERTIES_EXT.length());
            }
            buffer.append('(').append(filename).append(')');
        }
        buffer.append(": ");
        if (key != null) {
            buffer.append('"').append(key).append('"');
        }
        warning(buffer.toString());
        buffer.setLength(0);
        buffer.append(message);
        if (exception != null) {
            buffer.append(": ").append(exception.getLocalizedMessage());
        }
        warning(buffer.toString());
        errors++;
    }
}
