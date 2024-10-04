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
package org.apache.sis.gui.coverage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.util.Classes;

/**
 * A view to the internal tile matrices defined in a Resource.
 *
 * @todo change the text area to a split pane with a tree view on the left and a description pane on the right
 * @todo if the resource is writable, add tiling modification controls
 * @author Johann Sorel (Geomatys)
 */
public class TileMatrixSetPane extends BorderPane{

    private final TextArea area = new TextArea();

    public TileMatrixSetPane() {
        area.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        area.setEditable(false);
        setCenter(area);
    }

    public void setContent(Resource resource) {
        final StringBuilder sb = new StringBuilder();
        if (resource instanceof TiledResource) {
            final TiledResource tr = (TiledResource) resource;
            try {
                for (TileMatrixSet tms : tr.getTileMatrixSets()) {
                    sb.append(toString(tms));
                }
            } catch (DataStoreException ex){
                sb.append(ex.getMessage());
            }
        } else {
            sb.append("Resource has no tile matrices.");
        }

        area.setText(sb.toString());
    }

    /**
     * Pretty print output of given pyramid.
     * @param pyramid not null
     */
    public static String toString(TileMatrixSet pyramid) {
        final List<String> elements = new ArrayList<>();
        elements.add("id : " + pyramid.getIdentifier());
        elements.add("crs : " + IdentifiedObjects.getIdentifierOrName(pyramid.getCoordinateReferenceSystem()));
        elements.add(toStringTree("matrices", pyramid.getTileMatrices().values().stream().map(TileMatrixSetPane::toString).toList()));
        return toStringTree(Classes.getShortClassName(pyramid), elements);
    }

    /**
     * Pretty print outut of given mosaic.
     * @param matrix not null
     */
    public static String toString(TileMatrix matrix) {
        final StringBuilder sb = new StringBuilder(Classes.getShortClassName(matrix));
        sb.append("   id = ").append(matrix.getIdentifier());
        sb.append("   resolution = ").append(Arrays.toString(matrix.getTilingScheme().getResolution(true)));
        sb.append("   gridSize = ").append(matrix.getTilingScheme().getExtent());
        sb.append("   bbox = ").append(new GeneralEnvelope(matrix.getTilingScheme().getEnvelope()).toString());
        return sb.toString();
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
}
