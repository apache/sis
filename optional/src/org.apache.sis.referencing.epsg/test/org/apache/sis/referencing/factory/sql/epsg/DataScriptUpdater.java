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
package org.apache.sis.referencing.factory.sql.epsg;

import java.nio.file.Path;
import java.sql.Connection;

// Test dependencies
import org.apache.sis.metadata.sql.TestDatabase;


/**
 * A command-line tool for updating the <abbr>EPSG</abbr> {@code Data.sql} file distributed by Apache <abbr>SIS</abbr>.
 * The steps to follow are documented in the {@code README.md} file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DataScriptUpdater {
    /**
     * Do not allow instantiation of this class.
     */
    private DataScriptUpdater() {
    }

    /**
     * Compacts the {@code *Data_Script.sql} file provided by <abbr>EPSG</abbr>.
     * This method expects two arguments:
     *
     * <ol>
     *   <li>The file of the <abbr>SQL</abbr> script to convert, which must exist.</li>
     *   <li>The file where to write the compacted script, which will be overwritten without warning if it exists.</li>
     * </ol>
     *
     * The values of those arguments are typically as below, where the {@code EPSG_SCRIPTS} and {@code NON_FREE_DIR}
     * environment variables are defined in the {@code README.md} file in the same directory as the source code of
     * this class:
     *
     * <ol>
     *   <li>{@code $EPSG_SCRIPTS/PostgreSQL_Data_Script.sql}</li>
     *   <li>{@code $NON_FREE_DIR/epsg/Data.sql}</li>
     * </ol>
     *
     * @param  arguments  the source file and the destination file.
     * @throws Exception if an error occurred while reading of writing the file.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            System.err.println("Expected two arguments: source SQL file and target SQL file.");
            return;
        }
        try (TestDatabase db = TestDatabase.create("dummy");
             Connection c = db.source.getConnection())
        {
            final var formatter = new DataScriptFormatter(c);
            /*
             * The version number noted in the history table is a copy-and-paste error.
             */
            formatter.addSpellingChange("Version History", "'8.9'",
                    "Version 8.8 full release of Dataset.",
                    "Version 8.9 full release of Dataset.");
            /*
             * Add missing accents on some letters of texts in non-English languages.
             */
            formatter.addAccentedCharacters("Ancienne Triangulation Française");
            formatter.addAccentedCharacters("Nouvelle Triangulation Française");
            formatter.addAccentedCharacters("Nivellement Général de la Corse");
            formatter.addAccentedCharacters("Nivellement Général de la France");
            formatter.addAccentedCharacters("Nivellement Général de Nouvelle Calédonie");
            formatter.addAccentedCharacters("Nivellement Général de Polynésie Française");
            formatter.addAccentedCharacters("Nivellement Général Guyanais");
            formatter.addAccentedCharacters("Réseau de Référence des Antilles Françaises");
            formatter.addAccentedCharacters("Réseau Géodesique Français");
            formatter.addAccentedCharacters("Réseau Géodésique de la Polynésie Française");
            formatter.addAccentedCharacters("Réseau Géodésique de la RDC");
            formatter.addAccentedCharacters("Réseau Géodésique de la Réunion");
            formatter.addAccentedCharacters("Réseau Géodésique de Mayotte");
            formatter.addAccentedCharacters("Réseau Géodésique de Nouvelle Calédonie");
            formatter.addAccentedCharacters("Réseau Géodésique de Saint Pierre et Miquelon");
            formatter.addAccentedCharacters("Réseau Géodésique de Wallis et Futuna");
            formatter.addAccentedCharacters("Réseau Géodésique des Antilles Françaises");
            formatter.addAccentedCharacters("Réseau Géodésique des Terres Australes et Antarctiques Françaises");
            formatter.addAccentedCharacters("Réseau National Belge");
            formatter.addAccentedCharacters("Posiciones Geodésicas Argentinas");
            formatter.run(Path.of(arguments[0]), Path.of(arguments[1]));
            formatter.printSpellingChangeCount(System.out);
        }
    }
}
