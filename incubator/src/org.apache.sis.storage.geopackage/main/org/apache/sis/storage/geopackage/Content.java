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
package org.apache.sis.storage.geopackage;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.Locale;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.sql.DataAccess;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.base.MetadataFetcher;
import org.apache.sis.metadata.sql.internal.shared.SQLUtilities;


/**
 * Information about a resource declared in a Geopackage database.
 * Each {@code Content} instance is a row in the {@value #TABLE_NAME} table.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class Content extends ResourceDefinition {
    /**
     * Name of the content table in a Geopackage database.
     */
    public static final String TABLE_NAME = "gpkg_contents";

    /**
     * Name of the column used as the primary key in the content table.
     * This is the column that contains the name of the table containing the features.
     * Values in this column are mandatory (not null).
     *
     * @see #tableName
     */
    static final String PRIMARY_KEY = "table_name";

    /**
     * Name of the column specifying the type of data.
     * Values in this column are mandatory (not null).
     *
     * @see #dataType
     */
    static final String DATA_TYPE = "data_type";

    /**
     * The table or view name for the actual content (tiles, features, or attributes).
     * This is not a pattern: wildcard characters, if present, should be interpreted verbatim.
     * This is the primary key of the {@value #TABLE_NAME} table.
     */
    private final String tableName;

    /**
     * Type of data stored in the table or view.
     */
    private final String dataType;

    /**
     * A human-readable identifier (short name) for the content, or {@code null} if none.
     * Each non-null identifier is unique in a Geopackage file.
     */
    private final String identifier;

    /**
     * A human-readable description for the content, or {@code null} if none.
     */
    private final String description;

    /**
     * Timestamp of last change to content in UTC timezone, or {@code null} if unparseable.
     */
    private final Instant lastChange;

    /**
     * The minimum and maximum coordinate values, or {@code null} if unspecified.
     * If tiles, this is informational and the tile matrix set should be used for calculating tile coordinates.
     */
    private final Envelope bounds;

    /**
     * The <abbr>CRS</abbr> constructed from the {@link #srsId}, or {@code null} if none or unparseable.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * The spatial Reference System identifier.
     */
    private final int srsId;

    /**
     * Whether the SRID is present.
     */
    private final boolean hasSRID;

    /**
     * The handler for creating a resource from this content, or {@code null} for the default.
     * The default is implemented by {@link org.apache.sis.storage.sql.SQLStore}, which handles
     * the content as a {@link org.apache.sis.storage.FeatureSet}.
     *
     * @see #findHandler(ServiceLoader)
     */
    private ContentHandler handler;

    /**
     * The resource created by the content handler, or {@code null} if none.
     *
     * @see #resource(GpkgStore)
     */
    private Resource resource;

    /**
     * Creates a new content from the given resource.
     * The property values are inferred from the metadata as below:
     *
     * <ul>
     *   <li>The {@linkplain DataSet#getIdentifier() resource identifier} will be the table name.</li>
     *   <li>A {@linkplain Citation#getIdentifiers() citation identifier} will be the content identifier
     *       if different than the table name.</li>
     *   <li>A {@linkplain Citation#getDates() citation date} will be the date of last update.
     *       This constructor looks for {@link DateType#LAST_UPDATE} first, and fallbacks on
     *       {@link DateType#LAST_REVISION} and some other types if no last update is found.</li>
     *   <li>The {@linkplain Citation#getTitle() citation title} will be the description.</li>
     *   <li>The {@linkplain DataSet#getEnvelope() resource envelope} will be the bounds.</li>
     * </ul>
     *
     * This constructor is used for writing a new resource in a Geopackage.
     *
     * @param  dao       the data access object providing helper methods for fetching the <abbr>SRID</abbr>.
     * @param  data      the resource to add.
     * @param  dataType  the value to write in the {@code "data_type"} column of the {@value #TABLE_NAME} table.
     * @throws DataStoreException if an error occurred while fetching information from the given resource.
     */
    @SuppressWarnings("this-escape")    // Call to `super.getName()` is safe.
    public Content(final DataAccess dao, final DataSet data, final String dataType) throws DataStoreException {
        super(data.getIdentifier().orElseThrow(() -> new DataStoreException("Resource has no identifier")), null);
        ArgumentChecks.ensureNonEmpty("dataType", dataType);
        final Locale locale = dao.getLocale(Locale.Category.FORMAT);
        final Metadata metadata = data.getMetadata();
        this.tableName   = super.getName().tip().toString();
        this.identifier  = ContentWriter.firstDistinctString(metadata, tableName, null, locale);
        this.dataType    = dataType;
        this.description = ContentWriter.firstDistinctString(metadata, tableName, identifier, locale);
        this.lastChange  = MetadataFetcher.lastUpdate(metadata, null, GpkgStore.listeners(dao.getDataStore()));
        this.bounds      = data.getEnvelope().orElse(null);
        this.crs         = (bounds != null) ? bounds.getCoordinateReferenceSystem() : null;
        this.hasSRID     = (crs != null);
        this.srsId       = hasSRID ? dao.findSRID(crs) : 0;
        this.resource    = data;
    }

    /**
     * Creates a new content from a row of the {@value #TABLE_NAME} table.
     * This constructor is used for reading a resource from a Geopackage.
     *
     * @param  dao        the data access object providing helper methods for fetching the CRS.
     * @param  name       the table name including catalog and schema (if any) and with wildcards escaped.
     * @param  tableName  the table name verbatim (no escaping of wildcard characters).
     * @param  result     the result with the cursor on the row to parse.
     * @param  listeners  where to log non-fatal warnings.
     * @throws SQLException if an error occurred while fetching a column value.
     * @throws DataStoreException if an error occurred while getting a <abbr>CRS</abbr>.
     */
    private Content(final DataAccess dao, final GenericName name, final String tableName, final ResultSet rs,
                    final StoreListeners listeners) throws SQLException, DataStoreException
    {
        super(name, null);
        this.tableName   = tableName;                      // "table_name"  column.
        this.dataType    = getString (rs, 2);              // "data_type"   column.
        this.identifier  = getString (rs, 3);              // "identifier"  column.
        this.description = getString (rs, 4);              // "description" column.
        this.lastChange  = getInstant(rs, 5, listeners);   // "last_change" column.
        this.srsId       = rs.getInt(6);                   // "srs_id"      column.
        this.hasSRID     = !rs.wasNull();

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        CoordinateReferenceSystem crs = null;
        if (hasSRID) try {
            crs = dao.findCRS(srsId);
        } catch (DataStoreReferencingException e) {
            listeners.warning(e);
        }
        this.crs = crs;
        int column = 6;
        final int dim = 2;
        final var min = new double[dim]; Arrays.fill(min, Double.NEGATIVE_INFINITY);
        final var max = new double[dim]; Arrays.fill(max, Double.POSITIVE_INFINITY);
        boolean defined = false;
        for (int i=0; i<2*dim; i++) {
            double v = rs.getDouble(++column);
            if (!rs.wasNull() && Double.isFinite(v)) {
                ((i & 1) == 0 ? min : max)[i >>> 1] = v;
                defined = true;
            }
        }
        bounds = defined ? new ImmutableEnvelope(min, max, crs) : null;
    }

    /**
     * Reads fully the {@value #TABLE_NAME} table.
     *
     * @param  dao        the data access object providing helper methods for fetching the CRS.
     * @param  listeners  where to log non-fatal warnings.
     * @return the full table content. The returned list is modifiable.
     * @throws DataStoreException if an error occurred while reading the content table.
     */
    static List<Content> readFromTable(final DataAccess dao, final StoreListeners listeners) throws DataStoreException {
        final var contents = new ArrayList<Content>();
        final NameFactory factory = DefaultNameFactory.provider();
        final NameSpace namespace = dao.getDataStore().getIdentifier()
                .map((id) -> factory.createNameSpace(id, null)).orElse(null);
        try (Statement stmt = dao.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + PRIMARY_KEY + ", " + DATA_TYPE + ", identifier, "
                    + "description, last_change, srs_id, min_x, max_x, min_y, max_y FROM " + TABLE_NAME))
        {
            final String escape = stmt.getConnection().getMetaData().getSearchStringEscape();
            while (rs.next()) {
                String tableName = getString(rs, 1);
                GenericName name = factory.createLocalName(namespace, SQLUtilities.escapeWildcards(tableName, escape));
                contents.add(new Content(dao, name, tableName, rs, listeners));
            }
        } catch (SQLException e) {
            if ("HV00R".equalsIgnoreCase(e.getSQLState())) {    // Table not found.
                listeners.warning("Given database do not have a " + TABLE_NAME + " table, it is not a geopackage.", e);
            } else {
                throw GpkgStore.cannotExecute("Cannot read the content table.", e);
            }
        }
        return contents;
    }

    /**
     * Returns the value of the given result set with white space trimmed, or {@code null} if empty.
     *
     * @param  rs      the result set from which to get a string.
     * @param  column  the column to fetch.
     * @return value in the given column, or {@code null} if blank or empty.
     * @throws SQLException if an error occurred while fetching the column value.
     */
    private static String getString(final ResultSet rs, final int column) throws SQLException {
        String value = rs.getString(column);
        if (rs.wasNull() || (value = value.trim()).isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * Finds a handler for this content.
     *
     * @param  contentHandlers  the handlers to test.
     * @return whether a handler has been found.
     */
    final boolean findHandler(final ServiceLoader<ContentHandler> contentHandlers) {
        for (ContentHandler c : contentHandlers) {
            if (c.canOpen(this)) {
                handler = c;
                return true;
            }
        }
        return false;
    }

    /**
     * If this content is for the given resource, returns the handler.
     * Otherwise returns {@code null}.
     */
    final ContentHandler handler(final Resource target) {
        return resource == target ? handler : null;
    }

    /**
     * Gets the cached resource, or reads the resource if this is the first invocation of this method.
     * This method shall be invoked only on instances for which {@link #findHandler(ServiceLoader)}
     * returned {@code true}.
     */
    final Resource resource(final DataAccess dao) throws DataStoreException {
        assert Thread.holdsLock(dao.getDataStore());
        if (resource == null) {
            resource = handler.open(dao, this);
        }
        return resource;
    }

    /**
     * Gets the cached resource, or reads the resource if this is the first invocation of this method.
     * This method shall be invoked only on instances for which {@link #findHandler(ServiceLoader)}
     * returned {@code true}.
     */
    final Resource resource(final GpkgStore store) throws DataStoreException {
        assert Thread.holdsLock(store);
        if (resource == null) {
            try (DataAccess dao = store.newDataAccess(false)) {
                resource = handler.open(dao, this);
            } catch (Exception e) {
                throw GpkgStore.cannotExecute(null, e);
            }
        }
        return resource;
    }

    /**
     * Clears the resource and the handler for letting the garbage collector do its work.
     * This is done in case the user has kept a reference to this {@code Content} instance.
     */
    final void clear() {
        resource = null;
        handler  = null;
    }

    /**
     * Returns the instant of the given result set with white space trimmed, or {@code null} if empty.
     *
     * @param  rs         the result set from which to get an instant.
     * @param  column     the column to fetch.
     * @param  listeners  where to log a warning if the date cannot be parsed.
     * @return value in the given column, or {@code null} if blank or empty.
     * @throws SQLException if an error occurred while fetching the column value.
     */
    private static Instant getInstant(final ResultSet rs, final int column, final StoreListeners listeners)
            throws SQLException
    {
        final String str = getString(rs, column);
        if (str != null) try {
            return Instant.parse(str);
        } catch (DateTimeParseException e) {
            listeners.warning(e);
        }
        return null;
    }

    /**
     * Returns the table or view name for the actual content (tiles, features, or attributes).
     * This is not a pattern: wildcard characters, if present, should be interpreted verbatim.
     * This is the primary key of the {@value #TABLE_NAME} table.
     *
     * @return the verbatim table or view name.
     *
     * @see #getName()
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the type of data stored in the table or view.
     * Some values are:
     *
     * <ul>
     *   <li>{@code "features"}:   table of features having exactly one geometry column.</li>
     *   <li>{@code "attributes"}: table of features having no geometry.</li>
     *   <li>{@code "tiles"}:      table of tile matrix sets.</li>
     *   <li>{@code "general"}:    other (e.g. metadata).</li>
     * </ul>
     *
     * @return type of data stored in the table or view.
     */
    public String dataType() {
        return dataType;
    }

    /**
     * Returns a human-readable identifier (short name) for the content.
     * Each identifier is unique in a Geopackage file.
     *
     * @return a human-readable identifier for the content.
     */
    public Optional<String> identifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Returns a human-readable description for the content.
     *
     * @return a human-readable description for the content.
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the timestamp of last change to content, in UTC timezone.
     * This value is only informative, there is no guarantee that every
     * database changes will update this value.
     *
     * @return timestamp of last change to content, in UTC timezone.
     *         Should not be absent, unless the timestamp is unparseable.
     */
    public Optional<Instant> lastChange() {
        return Optional.ofNullable(lastChange);
    }

    /**
     * Returns the minimum and maximum coordinate values.
     * The envelope can be used as the extents of a default view, but there are no guarantees
     * that this envelope is exact or represent the minimum bounding box of the content.
     *
     * <p>If this content is a set of tiles, then the bounds are informational
     * and the tile matrix set should be used for calculating tile coordinates.</p>
     *
     * @return the minimum and maximum coordinate values. May contain infinite values.
     */
    public Optional<Envelope> bounds() {
        return Optional.ofNullable(bounds);
    }

    /**
     * Returns the coordinate reference system constructed from the <abbr>SRID</abbr>.
     * The value may be absent if the <abbr>WKT</abbr> associated to the <abbr>SRID</abbr>
     * cannot be parsed.
     *
     * @return the <abbr>CRS</abbr> constructed from the <abbr>SRID</abbr>.
     */
    public Optional<CoordinateReferenceSystem> crs() {
        return Optional.ofNullable(crs);
    }

    /**
     * Returns the Spatial Reference System identifier. If the {@linkplain #dataType() data type} is
     * {@code "features"}, then this is the <abbr>SRID</abbr> of all geometries. If the data type is
     * {@code "tiles"}, then this is the <abbr>SRID</abbr> of all tiles.
     *
     * @return the spatial Reference System identifier.
     */
    public OptionalInt srsId() {
        return hasSRID ? OptionalInt.of(srsId) : OptionalInt.empty();
    }
}
