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
package org.apache.sis.gui.referencing;

import java.util.function.Predicate;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableView;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Filters the list of CRS codes based on keywords.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CodeFilter implements Predicate<Code> {
    /**
     * The list of codes to filter.
     */
    private final AuthorityCodes allCodes;

    /**
     * Keywords that must be present in filtered CRS.
     */
    private final String[] tokens;

    /**
     * Creates a new filter.
     */
    private CodeFilter(final AuthorityCodes allCodes, final String[] tokens) {
        this.allCodes = allCodes;
        this.tokens   = tokens;
    }

    /**
     * Displays only the CRS whose names contains the specified keywords. The {@code keywords}
     * argument is a space-separated list provided by the user after he pressed "Enter" key in
     * {@link CRSChooser#searchField}.
     *
     * @param  table     the table on which to apply filtering.
     * @param  keywords  space-separated list of keywords to look for.
     */
    static void apply(final TableView<Code> table, String keywords) {
        final ObservableList<Code> items = table.getItems();
        final AuthorityCodes allCodes;
        FilteredList<Code> filtered;
        if (items instanceof AuthorityCodes) {
            allCodes = (AuthorityCodes) items;
            filtered = null;
        } else {
            filtered = (FilteredList<Code>) items;
            allCodes = (AuthorityCodes) filtered.getSource();
        }
        keywords = Strings.trimOrNull(keywords);
        if (keywords != null) {
            keywords = keywords.toLowerCase(allCodes.locale);
            final String[] tokens = (String[]) CharSequences.split(keywords, ' ');
            if (tokens.length != 0) {
                final Predicate<Code> p = new CodeFilter(allCodes, tokens);
                if (filtered == null) {
                    filtered = new FilteredList<>(allCodes, p);
                    table.setItems(filtered);
                } else {
                    filtered.setPredicate(p);
                }
                return;
            }
        }
        table.setItems(allCodes);
    }

    /**
     * Returns {@code true} if the given code should be included in the filtered list.
     * This method is invoked by {@link FilteredList}.
     */
    @Override
    public boolean test(final Code code) {
        final String id = code.code.toLowerCase(allCodes.locale);
        String name = allCodes.getName(code).getValue();
        name = (name != null) ? name.toLowerCase(allCodes.locale) : "";
        for (final String token : tokens) {
            if (!name.contains(token) && !id.equals(token)) {
                return false;
            }
        }
        return true;
    }
}
