package org.apache.sis.internal.sql.feature;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStoreContentException;

interface SQLTypeSpecification {
    /**
     *
     * @return Name for the feature type to build. Nullable.
     * @throws SQLException If an error occurs while retrieving information from database.
     */
    GenericName getName() throws SQLException;

    /**
     *
     * @return A succint description of the data source. Nullable.
     * @throws SQLException If an error occurs while retrieving information from database.
     */
    String getDefinition() throws SQLException;

    Optional<PrimaryKey> getPK() throws SQLException;

    List<SQLColumn> getColumns();

    List<Relation> getImports() throws SQLException;

    List<Relation> getExports() throws SQLException, DataStoreContentException;

    default Optional<String> getPrimaryGeometryColumn() {return Optional.empty();}
}
