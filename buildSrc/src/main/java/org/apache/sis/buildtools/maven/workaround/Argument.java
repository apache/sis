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
package org.apache.sis.buildtools.maven.workaround;

import java.util.Arrays;
import java.util.EnumMap;


/**
 * Command-line argument for {@link ProxyGenerator#main(String[])}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum Argument {
    /**
     * Directory where to write the generated files.
     */
    OUT("Directory where to write the generated files."),

    /**
     * Java package where to put generated Java code (if needed).
     */
    PACKAGE("Java package where to put generated Java code (if needed).");

    /**
     * Explanation shown in help screen for this option.
     */
    private final String description;

    /**
     * Creates a new option.
     *
     * @param  description  explanation shown in help screen for this option.
     */
    private Argument(final String description) {
        this.description = description;
    }

    /**
     * Parses the options. For each parsed option, the corresponding element in the given array
     * is set to {@code null}. Non-null elements that are left after this method completion are
     * JAR files to process.
     *
     * @param  args  command line options.
     * @return parsed options, or an empty map in case of parsing error.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static EnumMap<Argument,String> parse(final String[] args) {
        String message = "Illegal option: %s%n";
        final EnumMap<Argument,String> map = new EnumMap<>(Argument.class);
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                args[i] = null;
                final int s = arg.indexOf('=');
                if (s >= 0) try {
                    final var option = Argument.valueOf(arg.substring(2,s).trim().toUpperCase());
                    if (map.put(option, arg.substring(s+1).trim()) == null) {
                        continue;
                    }
                    message = "--%s cannot be repeated.%n";
                    arg = option.name().toLowerCase();
                } catch (IllegalArgumentException e) {
                    arg = arg.substring(0, s).trim();
                }
                System.err.printf(message, arg);
                Arrays.fill(args, null);
                map.clear();
                break;
            }
        }
        return map;
    }

    /**
     * Prints all available options.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static void help() {
        System.out.println("Syntax: [option=value] --out=<dir> jar...");
        System.out.println("Options:");
        for (final Argument option : values()) {
            System.out.printf("  --%-8s %s%n", option.name().toLowerCase(), option.description);
        }
    }
}
