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
package org.apache.sis.process;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.AbstractInternationalString;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.Types;


/**
 * Common categories for organizing processes in a tree, such as a toolbox.
 * A category is a {@link GenericName} where each {@linkplain GenericName#getParsedNames() component}
 * is a folder of the tree, from the root ({@linkplain GenericName#head() head}) to the folder containing
 * the process ({@linkplain GenericName#tip() tip}). For each component:
 *
 * <ul>
 *   <li>{@code toString()} returns a locale-independent code identifying the folder.
 *       Folders declared by different processes are merged when their codes are equal.</li>
 *   <li>{@code toInternationalString()} returns the label to show to users.</li>
 * </ul>
 *
 * The {@code toString()} value of a whole category is the path of codes separated by {@code '/'},
 * for example {@code "raster/terrain"} for {@link #RASTER_TERRAIN}.
 * Processes which do not fit in the categories defined here can use their own categories,
 * created by {@link #create(GenericName, String, CharSequence)} either as roots or as
 * sub-categories of the constants defined in this class.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ProcessDescriptor#getCategory()
 */
public final class ProcessCategories {
    /**
     * The separator between the components of a category path.
     */
    private static final String SEPARATOR = "/";

    /**
     * Processes working on vector data, for example features.
     */
    public static final GenericName VECTOR = root("vector", "Vector", "Vecteur");

    /**
     * Processes creating or modifying geometries one by one,
     * for example buffer, centroid, simplification or convex hull.
     */
    public static final GenericName VECTOR_GEOMETRY = child(VECTOR, "geometry", "Geometry", "Géométrie");

    /**
     * Processes combining the geometries of two or more layers,
     * for example clip, intersection, union or difference.
     */
    public static final GenericName VECTOR_OVERLAY = child(VECTOR, "overlay", "Overlay", "Superposition");

    /**
     * Processes selecting or extracting a subset of features,
     * for example by attribute values or by spatial relationship.
     */
    public static final GenericName VECTOR_SELECTION = child(VECTOR, "selection", "Selection", "Sélection");

    /**
     * Processes working on feature attributes,
     * for example computing, renaming or joining attributes.
     */
    public static final GenericName VECTOR_TABLE = child(VECTOR, "table", "Attribute table", "Table attributaire");

    /**
     * Processes computing statistics or relationships on features,
     * for example counting points in polygons or finding nearest neighbors.
     */
    public static final GenericName VECTOR_ANALYSIS = child(VECTOR, "analysis", "Analysis", "Analyse");

    /**
     * Processes creating new vector data,
     * for example regular grids or random points.
     */
    public static final GenericName VECTOR_CREATION = child(VECTOR, "creation", "Creation", "Création");

    /**
     * Processes working on raster data, for example grid coverages.
     */
    public static final GenericName RASTER = root("raster", "Raster", "Raster");

    /**
     * Processes computing cell values from expressions on one or more rasters,
     * for example band arithmetic or conditional expressions.
     */
    public static final GenericName RASTER_ALGEBRA = child(RASTER, "algebra", "Map algebra", "Algèbre de cartes");

    /**
     * Processes computing statistics or classifications on rasters,
     * for example zonal statistics or reclassification.
     */
    public static final GenericName RASTER_ANALYSIS = child(RASTER, "analysis", "Analysis", "Analyse");

    /**
     * Processes working on elevation models,
     * for example slope, aspect, hillshade or viewshed.
     */
    public static final GenericName RASTER_TERRAIN = child(RASTER, "terrain", "Terrain analysis", "Analyse de terrain");

    /**
     * Processes extracting a part of a raster,
     * for example clip by extent, by mask or band selection.
     */
    public static final GenericName RASTER_EXTRACTION = child(RASTER, "extraction", "Extraction", "Extraction");

    /**
     * Processes creating a raster from sparse values,
     * for example inverse distance weighting or kriging.
     */
    public static final GenericName RASTER_INTERPOLATION = child(RASTER, "interpolation", "Interpolation", "Interpolation");

    /**
     * Processes working on remote sensing images,
     * for example radiometric correction, pansharpening or image classification.
     */
    public static final GenericName IMAGERY = root("imagery", "Imagery", "Imagerie");

    /**
     * Processes working on point clouds,
     * for example LiDAR filtering, classification or thinning.
     */
    public static final GenericName POINT_CLOUD = root("pointCloud", "Point cloud", "Nuage de points");

    /**
     * Processes working on meshes and other three-dimensional geometries,
     * for example triangulation or mesh simplification.
     */
    public static final GenericName MESH = root("mesh", "Mesh", "Maillage");

    /**
     * Processes working on time series,
     * for example temporal aggregation or gap filling.
     */
    public static final GenericName TEMPORAL = root("temporal", "Temporal", "Temporel");

    /**
     * Processes working on coordinate reference systems,
     * for example reprojection or datum shift.
     */
    public static final GenericName REFERENCING = root("referencing",
            "Projections and transformations", "Projections et transformations");

    /**
     * Processes converting data from one format or model to another,
     * for example raster to vector or file format conversion.
     */
    public static final GenericName CONVERSION = root("conversion", "Conversion", "Conversion");

    /**
     * Processes managing datasets and their metadata,
     * for example copying, merging, indexing or editing metadata.
     */
    public static final GenericName DATA_MANAGEMENT = root("dataManagement", "Data management", "Gestion des données");

    /**
     * Processes preparing data for map production,
     * for example generalization, labelling or symbology.
     */
    public static final GenericName CARTOGRAPHY = root("cartography", "Cartography", "Cartographie");

    /**
     * Do not allow instantiation of this class.
     */
    private ProcessCategories() {
    }

    /**
     * Creates a root category with English and French labels.
     *
     * @param  code     locale-independent identifier of the category.
     * @param  english  label in English, also used as the fallback for languages other than French.
     * @param  french   label in French.
     * @return the root category.
     */
    private static GenericName root(final String code, final String english, final String french) {
        return create(null, code, i18n(english, french));
    }

    /**
     * Creates a sub-category with English and French labels.
     *
     * @param  parent   the parent category.
     * @param  code     locale-independent identifier of the category.
     * @param  english  label in English, also used as the fallback for languages other than French.
     * @param  french   label in French.
     * @return the sub-category.
     */
    private static GenericName child(final GenericName parent, final String code, final String english, final String french) {
        return create(parent, code, i18n(english, french));
    }

    /**
     * Returns a label with an English text used as the fallback for all languages except French.
     *
     * @param  english  label in English.
     * @param  french   label in French.
     * @return the localized label.
     */
    private static InternationalString i18n(final String english, final String french) {
        return new DefaultInternationalString(Map.of(Locale.ROOT, english, Locale.FRENCH, french));
    }

    /**
     * Creates a category with the given code and label.
     * The {@code code} is a locale-independent identifier used for merging folders declared by different processes.
     * It is the value returned by {@code toString()} on the {@linkplain GenericName#tip() tip} of the returned name.
     * The {@code label} is the text to show to users, and can be an {@link InternationalString} for localization.
     *
     * @param  parent  the parent category, or {@code null} for creating a root category.
     * @param  code    locale-independent identifier of the category. Shall not contain the {@code '/'} character.
     * @param  label   text to show to users, or {@code null} for using the code as the label.
     * @return the category.
     * @throws IllegalArgumentException if the code is empty or contains the {@code '/'} character.
     */
    public static GenericName create(final GenericName parent, final String code, final CharSequence label) {
        Objects.requireNonNull(code, "code");
        if (code.isBlank() || code.contains(SEPARATOR)) {
            throw new IllegalArgumentException("Illegal category code: \"" + code + "\".");
        }
        final var name = new Label(code, (label != null) ? Types.toInternationalString(label) : null);
        return (parent != null) ? Names.createScopedName(parent, SEPARATOR, name)
                                : Names.createLocalName(null, SEPARATOR, name);
    }

    /**
     * A category name component which is a code in the unlocalized (root) locale,
     * and a label in all other locales.
     */
    private static final class Label extends AbstractInternationalString implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6153028412436591471L;

        /**
         * Locale-independent identifier of the category.
         */
        private final String code;

        /**
         * Text to show to users, or {@code null} for using the code.
         */
        @SuppressWarnings("serial")         // Most SIS implementations are serializable.
        private final InternationalString label;

        /**
         * Creates a new component.
         *
         * @param  code   locale-independent identifier of the category.
         * @param  label  text to show to users, or {@code null} for using the code.
         */
        Label(final String code, final InternationalString label) {
            this.code  = code;
            this.label = label;
        }

        /**
         * Returns the code if the given locale is {@code null} or {@link Locale#ROOT},
         * or the label in the given locale otherwise.
         */
        @Override
        public String toString(final Locale locale) {
            if (label == null || locale == null || locale.equals(Locale.ROOT)) {
                return code;
            }
            return label.toString(locale);
        }

        /**
         * Compares this component with the given object for equality.
         */
        @Override
        public boolean equals(final Object other) {
            if (other instanceof Label) {
                final var that = (Label) other;
                return code.equals(that.code) && Objects.equals(label, that.label);
            }
            return false;
        }

        /**
         * Returns a hash code value for this component.
         */
        @Override
        public int hashCode() {
            return code.hashCode() + 31 * Objects.hashCode(label);
        }
    }
}
