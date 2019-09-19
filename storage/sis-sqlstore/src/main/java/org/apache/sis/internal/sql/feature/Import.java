package org.apache.sis.internal.sql.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Import {

    final String propertyName;

    final List<String> fkColumns;

    final TableReference target;

    public Import(String propertyName, Collection<String> fkColumns, TableReference target) {
        this.propertyName = propertyName;
        this.fkColumns = Collections.unmodifiableList(new ArrayList<>(fkColumns));
        this.target = target;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public List<String> getFkColumns() {
        return fkColumns;
    }

    public TableReference getTarget() {
        return target;
    }
}
