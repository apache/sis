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

import java.util.EnumSet;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTables;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;


/**
 * The "info" sub-command.
 * The content varies depending on the resource type.
 * For grid coverage, it contains the grid geometry.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class InfoCommand extends FormattedOutputCommand {
    /**
     * Returns valid options for the {@code "metadata"} command.
     */
    static EnumSet<Option> options() {
        return EnumSet.of(Option.LOCALE, Option.TIMEZONE, Option.COLORS, Option.VERBOSE, Option.HELP, Option.DEBUG);
    }

    /**
     * The bit mask of information to request from a grid geometry.
     */
    private int gridBitMask;

    /**
     * Creates the {@code "info"} sub-command.
     *
     * @param  commandIndex  index of the {@code arguments} element containing the {@code "info"} command name, or -1 if none.
     * @param  arguments     the command-line arguments provided by the user.
     * @throws InvalidOptionException if an illegal option has been provided, or the option has an illegal value.
     */
    InfoCommand(final int commandIndex, final String... arguments) throws InvalidOptionException {
        super(commandIndex, arguments, options(), OutputFormat.TEXT);
        gridBitMask = GridGeometry.EXTENT | GridGeometry.GEOGRAPHIC_EXTENT | GridGeometry.TEMPORAL_EXTENT
                    | GridGeometry.CRS | GridGeometry.RESOLUTION;
        if (options.containsKey(Option.VERBOSE)) {
            gridBitMask |= GridGeometry.ENVELOPE | GridGeometry.GRID_TO_CRS;
        }
    }

    /**
     * Prints resource information.
     *
     * @return 0 on success, or an exit code if the command failed for a reason other than an uncaught Java exception.
     */
    @Override
    public int run() throws Exception {
        final Object input;
        final String name;
        if (useStandardInput()) {
            input = System.in;
            name  = "stdin";
        } else {
            if (hasUnexpectedFileCount = hasUnexpectedFileCount(1, 1)) {
                return Command.INVALID_ARGUMENT_EXIT_CODE;
            }
            input = name = files.get(0);
        }
        final var tree = new DefaultTreeTable(TableColumn.VALUE_AS_TEXT);
        try (DataStore store = DataStores.open(input)) {
            define(tree.getRoot(), store);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
        final var tf = new TreeTableFormat(locale, timezone);
        tf.format(tree, out);
        return 0;
    }

    /**
     * Sets the values of the given node using information in the given resource.
     *
     * @param  target  the tree node to define.
     * @param  source  the resource from which to get the information.
     * @throws DataStoreException if an error occurred while reading the resource.
     */
    private void define(final TreeTable.Node target, final Resource source) throws DataStoreException {
        String name = source.getIdentifier().map((id) -> id.toInternationalString().toString(locale))
                .orElseGet(() -> Vocabulary.getResources(locale).getString(Vocabulary.Keys.Unnamed));
        target.setValue(TableColumn.VALUE_AS_TEXT, name);
        if (source instanceof GridCoverageResource) {
            final var grid = (GridCoverageResource) source;
            TreeTable.Node root = grid.getGridGeometry().toTree(locale, gridBitMask).getRoot();
            TreeTables.moveChildren(root, target);
            toTree(grid.getSampleDimensions(), target.newChild(), TableColumn.VALUE_AS_TEXT);
        } else if (source instanceof Aggregate) {
            for (Resource component : ((Aggregate) source).components()) {
                define(target.newChild(), component);
            }
        }
    }

    /**
     * Appends information about the sample dimensions in a tree.
     *
     * @param bands   the sample dimensions to format.
     * @param target  where to format the sample dimensions.
     * @param column  the column where to write the texts.
     */
    private void toTree(final Iterable<SampleDimension> bands, final TreeTable.Node target,
                        final TableColumn<? super String> column)
    {
        target.setValue(column, Vocabulary.getResources(locale).getString(Vocabulary.Keys.SampleDimensions));
        final var rf = new RangeFormat(locale, timezone);
        final var sb = new StringBuffer();
        for (SampleDimension band : bands) {
            band = band.forConvertedValues(true);
            final TreeTable.Node bn = target.newChild();
            bn.setValue(column, band.getName().toInternationalString().toString(locale));
            for (final Category category : band.getCategories()) {
                final TreeTable.Node cn = bn.newChild();
                sb.append(category.getName().toString(locale)).append(" (");
                rf.format(category.getSampleRange(), sb, null).append(')');
                cn.setValue(column, sb.toString());
                sb.setLength(0);
            }
        }
    }
}
