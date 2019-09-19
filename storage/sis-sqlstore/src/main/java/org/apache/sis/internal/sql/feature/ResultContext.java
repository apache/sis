package org.apache.sis.internal.sql.feature;

import java.sql.ResultSet;

class ResultContext {
    final ResultSet source;

    ResultContext(ResultSet source) {
        this.source = source;
    }

    Cell cell(int columnIndex, String propertyName) {
        return new Cell(columnIndex, propertyName);
    }

    class Cell {
        final int colIdx;
        final String propertyName;

        private Cell(int colIdx, String propertyName) {
            this.colIdx = colIdx;
            this.propertyName = propertyName;
        }
    }
}
