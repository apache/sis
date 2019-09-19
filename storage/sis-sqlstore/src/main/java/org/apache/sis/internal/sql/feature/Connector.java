package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.opengis.feature.Feature;

import org.apache.sis.storage.DataStoreException;

public interface Connector {
    Stream<Feature> connect(Connection connection) throws SQLException, DataStoreException;

    String estimateStatement(final boolean count);
}
