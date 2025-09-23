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
package org.apache.sis.console;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.Console;
import java.io.PrintWriter;
import org.apache.sis.util.internal.shared.X364;
import org.apache.sis.system.Fallback;
import org.apache.sis.system.Environment;
import org.apache.sis.setup.OptionalInstallations;
import org.apache.sis.pending.jdk.JDK22;


/**
 * A provider for data licensed under different terms of use than the Apache license.
 * This class is in charge of downloading the data if necessary and asking user's agreement
 * before to install them. Authorities managed by the current implementation are:
 *
 * <ul>
 *   <li>{@code "EPSG"} for the EPSG geodetic dataset.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.7
 */
@Fallback
public class ResourcesDownloader extends OptionalInstallations {
    /**
     * The console to use for printing EPSG terms of use and asking for agreement, or {@code null} if none.
     */
    private final Console console;

    /**
     * Where to write. It should be {@code console.writer()},
     * but the latter seems partially broken when used from JShell.
     */
    private final PrintWriter out;

    /**
     * The locale to use for text display.
     */
    private final Locale locale;

    /**
     * {@code true} if colors can be applied for ANSI X3.64 compliant terminal.
     */
    private final boolean colors;

    /**
     * The localized answers expected from the users. Keys are words like "Yes" or "No"
     * and boolean values are the meaning of the keys.
     */
    private final Map<String,Boolean> answers = new HashMap<>();

    /**
     * Creates a new installation scripts provider.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public ResourcesDownloader() {
        super("text/plain");
        final CommandRunner command = CommandRunner.instance.get();
        if (command != null) {
            locale = command.locale;
            colors = command.colors;
        } else {
            locale = Locale.getDefault();
            colors = false;
        }
        console = System.console();
        out = Environment.writer(console, System.out);
    }

    /**
     * Returns the locale to use for messages shown to the user.
     *
     * @return the locale of messages shown to the user.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the name of the authority who provides data under non-Apache terms of use.
     * If this {@code ResourcesDownloader} cannot ask user's agreement because there is
     * no {@link Console} attached to the current Java virtual machine or the console is
     * not {@linkplain Console#isTerminal() a terminal}, then this method returns an empty set.
     *
     * @return {@code "EPSG"} or an empty set in this class cannot ask user's agreement.
     */
    @Override
    public Set<String> getAuthorities() {
        return (console != null && JDK22.isTerminal(console)) ? super.getAuthorities() : Set.of();
    }

    /**
     * Asks to the user if (s)he agree to download and install the resource for the given authority.
     * This method may be invoked twice for the same {@code authority} argument:
     * first with a null {@code license} argument for asking if the user agrees to download the data,
     * then with a non-null {@code license} argument for asking if the user agrees with the license terms.
     *
     * @param  authority  "EPSG".
     * @param  license    the license, or {@code null} for asking if the user wants to download the data.
     * @return whether user accepted.
     */
    @Override
    protected boolean askUserAgreement(final String authority, final String license) {
        if (console == null) {
            return false;
        }
        final ResourceBundle resources = ResourceBundle.getBundle("org.apache.sis.console.Messages", getLocale());
        if (answers.isEmpty()) {
            for (final String r : resources.getString("yes").split("\\|")) answers.put(r, Boolean.TRUE);
            for (final String r : resources.getString("no" ).split("\\|")) answers.put(r, Boolean.FALSE);
        }
        final String textColor, linkColor, linkOff, actionColor, resetColor;
        if (colors) {
            textColor   = X364.FOREGROUND_YELLOW .sequence();
            linkColor   = X364.UNDERLINE         .sequence();
            linkOff     = X364.NO_UNDERLINE      .sequence();
            actionColor = X364.FOREGROUND_GREEN  .sequence();
            resetColor  = X364.FOREGROUND_DEFAULT.sequence();
        } else {
            textColor = linkColor = linkOff = actionColor = resetColor = "";
        }
        /*
         * Show the question.
         */
        final String lineSeparator = System.lineSeparator();
        final String prompt, action;
        if (license == null) {
            prompt = "download";
            action = "downloading";
            format(resources.getString("install"), textColor, getSpaceRequirement(authority),
                           linkColor, destinationDirectory, linkOff, resetColor);
        } else {
            prompt = "accept";
            action = "installing";
            out.write(lineSeparator);
            out.write(license);
            out.write(lineSeparator);
        }
        out.flush();
        /*
         * Ask user agreement.
         */
        Boolean answer;
        do {
            answer = answers.get(console.readLine(resources.getString(prompt), textColor, resetColor).toLowerCase(getLocale()));
        } while (answer == null);
        if (answer) {
            format(resources.getString(action), actionColor, resetColor);
        } else {
            out.write(lineSeparator);
        }
        return answer;
    }

    /**
     * Writes a formatted output to the console.
     *
     * @param pattern  pattern to write.
     * @param args  arguments referenced by the pattern.
     */
    private void format(final String pattern, final Object... args) {
        if (console.writer() == out) {
            console.format(pattern, args);
        } else {
            out.format(pattern, args);
        }
    }
}
