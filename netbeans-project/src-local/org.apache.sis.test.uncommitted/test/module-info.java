/**
 * A dummy module for performing tests in a NetBeans environment.
 * This module imports all Apache SIS modules for making easy to
 * test any feature.
 *
 * <p>This module is <em>not</em> versioned. It is ignored by Git,
 * except for this {@code module-info} and one {@code package-info}.
 * This is a space available for local experiments.</p>
 */
module org.apache.sis.test.uncommitted {
    requires org.apache.sis.util;
    requires org.apache.sis.metadata;
    requires org.apache.sis.referencing;
    requires org.apache.sis.referencing.gazetteer;
    requires org.apache.sis.feature;
    requires org.apache.sis.storage;
    requires org.apache.sis.storage.xml;
    requires org.apache.sis.storage.sql;
    requires org.apache.sis.storage.netcdf;
    requires org.apache.sis.storage.geotiff;
    requires org.apache.sis.storage.earthobservation;
    requires org.apache.sis.cloud.aws;
    requires org.apache.sis.console;
    requires org.apache.sis.openoffice;
    requires org.apache.sis.portrayal;
    requires org.apache.sis.profile.france;
    requires org.apache.sis.profile.japan;

    requires org.opengis.geoapi.conformance;
    requires org.apache.derby.tools;
    requires org.hsqldb;
}
