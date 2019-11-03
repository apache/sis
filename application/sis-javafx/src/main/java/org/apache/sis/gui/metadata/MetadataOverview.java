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
package org.apache.sis.gui.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.StringJoiner;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.opengis.metadata.Metadata;
import org.opengis.util.ControlledVocabulary;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;



/**
 * A panel showing a summary of metadata.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MetadataOverview {
    /**
     * Titles panes for different metadata sections (identification info, spatial information, <i>etc</i>).
     * This is similar to {@link javafx.scene.control.Accordion} except that we allow an arbitrary amount
     * of titled panes to be opened in same time.
     */
    private final ScrollPane content;

    /**
     * The resources for localized strings. Stored because needed often.
     */
    final Resources localized;

    /**
     * The locale to use for date/number formatters.
     */
    private final Locale formatLocale;

    /**
     * The format to use for writing numbers, created when first needed.
     *
     * @see #getNumberFormat()
     */
    private NumberFormat numberFormat;

    /**
     * The format to use for writing dates, created when first needed.
     *
     * @see #getDateFormat()
     */
    private DateFormat dateFormat;

    /**
     * An image of size 360×180 pixels showing a map of the world.
     * This is loaded when first needed.
     *
     * @see #getWorldMap()
     */
    private Image worldMap;

    /**
     * Whether we already tried to load {@link #worldMap}.
     */
    private boolean worldMapLoaded;

    /**
     * If this {@link MetadataOverview} is loading metadata, the worker doing this task.
     * Otherwise {@code null}. This is used for cancelling the currently running loading
     * process if {@link #setMetadata(Resource)} is invoked again before completion.
     */
    private Worker<Metadata> loader;

    /**
     * If the metadata or the grid geometry can not be obtained, the reason.
     *
     * @todo show in this control.
     */
    private Throwable error;

    /**
     * The pane where to show information about resource identification, spatial representation, etc.
     * Those panes will be added in the {@link #content} when we determined that they are not empty.
     * The content of those panes is updated by {@link #setMetadata(Metadata)}.
     */
    private final TitledPane[] information;

    /**
     * Creates an initially empty metadata overview.
     *
     * @param  textLocale  the locale for the text.
     * @param  dataLocale  the locale for formatting numbers and dates.
     *                     This is often the same than {@code textLocale}.
     */
    public MetadataOverview(final Locale textLocale, final Locale dataLocale) {
        localized    = Resources.forLocale(textLocale);
        formatLocale = dataLocale;
        information  = new TitledPane[] {
            new TitledPane(localized.getString(Resources.Keys.ResourceIdentification), new IdentificationInfo(this)),
            new TitledPane(localized.getString(Resources.Keys.SpatialRepresentation),  new RepresentationInfo(this))
        };
        content = new ScrollPane(new VBox());
        content.setFitToWidth(true);
    }

    /**
     * Returns the region containing the visual components managed by this {@code MetadataOverview}.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return content;
    }

    /**
     * Returns the format to use for writing numbers.
     */
    final NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance(formatLocale);
        }
        return numberFormat;
    }

    /**
     * Returns the format to use for writing dates.
     */
    final DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, formatLocale);
        }
        return dateFormat;
    }

    /**
     * Fetches the metadata in a background thread and delegates to
     * {@link #setMetadata(Metadata)} when ready.
     *
     * @param  resource  the resource for which to show metadata, or {@code null}.
     */
    public void setMetadata(final Resource resource) {
        assert Platform.isFxApplicationThread();
        if (loader != null) {
            loader.cancel();
            loader = null;
        }
        if (resource == null) {
            setMetadata((Metadata) null);
        } else {
            final class Getter extends Task<Metadata> {
                /**
                 * Invoked in a background thread for fetching metadata,
                 * eventually with other information like grid geometry.
                 */
                @Override protected Metadata call() throws DataStoreException {
                    return resource.getMetadata();
                }

                /**
                 * Shows the result, unless another {@link #setMetadata(Resource)} has been invoked.
                 */
                @Override protected void succeeded() {
                    if (!isCancelled()) {
                        setMetadata(getValue());
                    }
                }

                /**
                 * Invoked when an error occurred while fetching metadata.
                 */
                @Override protected void failed() {
                    if (!isCancelled()) {
                        setMetadata((Metadata) null);
                        error = getException();
                    }
                }
            }
            BackgroundThreads.execute(new Getter());
        }
    }

    /**
     * Sets the content of this pane to the given metadata.
     *
     * @param  metadata  the metadata to show, or {@code null}.
     */
    public void setMetadata(final Metadata metadata) {
        assert Platform.isFxApplicationThread();
        error = null;
        final ObservableList<Node> children = ((VBox) content.getContent()).getChildren();
        /*
         * We want to include only the non-empty panes in the children list. But instead of
         * removing everything and adding back non-empty panes, we check case-by-case if a
         * child should be added or removed. It will often result in no modification at all.
         */
        int i = 0;
        for (TitledPane pane : information) {
            final Section<?> info = (Section<?>) pane.getContent();
            info.setInformation(metadata);
            final boolean isEmpty   = info.isEmpty();
            final boolean isPresent = (i < children.size()) && children.get(i) == pane;
            if (isEmpty == isPresent) {     // Should not be present if empty, or should be present if non-empty.
                if (isEmpty) {
                    children.remove(i);
                } else {
                    children.add(i, pane);
                }
            }
            if (!isEmpty) i++;
        }
    }

    /**
     * Returns an image of size 360×180 pixels showing a map of the world,
     * or {@code null} if we failed to load the image.
     */
    final Image getWorldMap() {
        if (!worldMapLoaded) {
            worldMapLoaded = true;                  // Set now for avoiding retries in case of failure.
            Exception error;
            try (InputStream in = MetadataOverview.class.getResourceAsStream("WorldMap360x180.png")) {
                worldMap = new Image(in);
                error = worldMap.getException();
            } catch (IOException e) {
                error = e;
            }
            if (error != null) {
                Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), MetadataOverview.class, "getWorldMap", error);
            }
        }
        return worldMap;
    }

    /**
     * Returns all code lists in a comma-separated list.
     */
    final String string(final Collection<? extends ControlledVocabulary> codes) {
        final StringJoiner buffer = new StringJoiner(", ");
        for (final ControlledVocabulary c : codes) {
            final String text = string(Types.getCodeTitle(c));
            if (text != null) buffer.add(text);
        }
        return buffer.length() != 0 ? buffer.toString() : null;
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    final String string(final InternationalString i18n) {
        if (i18n != null) {
            String t = i18n.toString(localized.getLocale());
            if (t != null && !(t = t.trim()).isEmpty()) {
                return t;
            }
        }
        return null;
    }
}
