package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;

public class SQLQueryBuilder extends SQLBuilder {

    final DataSource source;

    public SQLQueryBuilder(DataSource source, final DatabaseMetaData metadata, final boolean quoteSchema) throws SQLException {
        super(metadata, quoteSchema);
        this.source = source;
    }

    public FeatureSet build(final Connection connection) throws SQLException, DataStoreException {
        final Analyzer analyzer = new Analyzer(source, connection.getMetaData(), null, null);
        // TODO: defensive copy of this builder.
        return new QueryFeatureSet(this, analyzer, source, connection);
    }


}
