package org.apache.sis.internal.sql.feature;

/**
 * API to allow overrided SQL Stream to delegate a set of intermediate operations to native driver.
 */
interface QueryBuilder {

    QueryBuilder limit(long limit);

    QueryBuilder offset(long offset);

    default QueryBuilder distinct() { return distinct(true); }

    QueryBuilder distinct(boolean activate);

    Connector select(ColumnRef... columns);
}
