package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.util.collection.Cache;

import static org.apache.sis.util.ArgumentChecks.ensureNonEmpty;

/**
 * Not THREAD-SAFE !
 * Search for geometric information in specialized SQL for Simple feature tables (refer to
 * <a href="https://www.opengeospatial.org/standards/sfs">OGC 06-104r4 (Simple feature access - Part 2: SQL option)</a>).
 *
 * @implNote <a href="https://www.jooq.org/doc/3.12/manual/sql-execution/fetching/pojos/#N5EFC1">I miss JOOQ...</a>
 */
class GeometryIdentification implements SQLCloseable {

    final PreparedStatement identifySchemaQuery;
    /**
     * A statement serving two purposes:
     * <ol>
     *     <li>Searching all available geometric columns of a specified table</li>
     *     <li>Fetching geometric information for a specific column</li>
     * </ol>
     *
     * @implNote The statement definition is able to serve both purposes by changing geometry column filter to no-op.
     */
    final PreparedStatement columnQuery;
    final CRSIdentification crsIdent;

    /**
     * Describes if geometry column registry include a column for geometry types, according that one can apparently omit
     * it (see Simple_feature_access_-_Part_2_SQL_option_v1.2.1, section 6.2: Architecture - SQL implementation using
     * Geometry Types).
     */
    final boolean typeIncluded;

    public GeometryIdentification(Connection c, Cache<Integer, CoordinateReferenceSystem> sessionCache) throws SQLException {
        this(c, "geometry_columns", "f_geometry_column", "geometry_type", sessionCache);
    }

    public GeometryIdentification(Connection c, String identificationTable, String geometryColumnName, String typeColumnName, Cache<Integer, CoordinateReferenceSystem> sessionCache) throws SQLException {
        typeIncluded = typeColumnName != null && !(typeColumnName=typeColumnName.trim()).isEmpty();
        identifySchemaQuery = c.prepareStatement("SELECT DISTINCT(f_table_schema) FROM "+identificationTable+" WHERE f_table_name = ?");
        columnQuery = c.prepareStatement(
                "SELECT "+geometryColumnName+", coord_dimension, srid" + (typeIncluded ? ", "+typeColumnName : "") + ' ' +
                "FROM "+identificationTable+" " +
                "WHERE f_table_schema LIKE ? " +
                "AND f_table_name = ? " +
                "AND "+geometryColumnName+" LIKE ?"
        );
        crsIdent = new CRSIdentification(c, sessionCache);
    }

    Set<GeometryColumn> fetchGeometricColumns(String schema, final String table) throws SQLException, ParseException {
        ensureNonEmpty("Table name", table);
        if (schema == null || (schema = schema.trim()).isEmpty()) {
            // To avoid ambiguity, we have to restrict search to a single schema
            identifySchemaQuery.setString(1, table);
            try (ResultSet result = identifySchemaQuery.executeQuery()) {
                if (!result.next()) return Collections.EMPTY_SET;
                schema = result.getString(1);
                if (result.next()) throw new IllegalArgumentException("Multiple tables match given name. Please specify schema to remove all ambiguities");
            }
        }

        columnQuery.setString(1, schema);
        columnQuery.setString(2, table);
        columnQuery.setString(3, "%");
        try (ResultSet result = columnQuery.executeQuery()) {
            final HashSet<GeometryColumn> cols = new HashSet<>();
            while (result.next()) {
                cols.add(create(result));
            }
            return cols;
        } finally {
            columnQuery.clearParameters();
        }
    }

    Optional<GeometryColumn> fetch(SQLColumn target) throws SQLException, ParseException {
        if (target == null || target.origin == null) return Optional.empty();

        String schema = target.origin.schema;
        if (schema == null || (schema = schema.trim()).isEmpty()) schema = "%";
        columnQuery.setString(1, schema);
        columnQuery.setString(2, target.origin.table);
        columnQuery.setString(3, target.naming.getColumnName());

        try (ResultSet result = columnQuery.executeQuery()) {
            if (result.next()) return Optional.of(create(result));
        } finally {
            columnQuery.clearParameters();
        }
        return Optional.empty();
    }

    private GeometryColumn create(final ResultSet cursor) throws SQLException, ParseException {
        final String name = cursor.getString(1);
        final int dimension = cursor.getInt(2);
        final int pgSrid = cursor.getInt(3);
        final String type = typeIncluded ? cursor.getString(4) : null;
        // Note: we make a query per entry, which could impact performance. However, 99% of defined tables
        // will have only one geometry column. Moreover, even with more than one, with prepared statement, the
        // performance impact should stay low.
        final CoordinateReferenceSystem crs = crsIdent.fetchCrs(pgSrid);
        return new GeometryColumn(name, dimension, pgSrid, type, crs);
    }

    @Override
    public void close() throws SQLException {
        try (
                SQLCloseable c1 = columnQuery::close;
                SQLCloseable c2 = identifySchemaQuery::close;
                SQLCloseable c3 = crsIdent
         ) {}
    }

    static final class GeometryColumn {
        final String name;
        final int dimension;
        final int pgSrid;
        final String type;

        final CoordinateReferenceSystem crs;

        private GeometryColumn(String name, int dimension, int srid, final String type, CoordinateReferenceSystem crs) {
            this.name = name;
            this.dimension = dimension;
            this.pgSrid = srid;
            this.crs = crs;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GeometryColumn that = (GeometryColumn) o;
            return dimension == that.dimension &&
                    pgSrid == that.pgSrid &&
                    name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
