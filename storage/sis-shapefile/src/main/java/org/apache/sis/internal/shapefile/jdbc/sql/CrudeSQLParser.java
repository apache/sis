package org.apache.sis.internal.shapefile.jdbc.sql;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

import org.apache.sis.internal.shapefile.jdbc.AbstractJDBC;
import org.apache.sis.internal.shapefile.jdbc.resultset.DBFRecordBasedResultSet;

/**
 * Simple and temporary SQL parser.
 * @author Marc LE BIHAN 
 */
public class CrudeSQLParser extends AbstractJDBC {
    /** ResultSet followed straight forward. */
    private DBFRecordBasedResultSet rs;
    
    /**
     * Construct a crude SQL parser.
     * @param resultset Target ResultSet.
     */
    public CrudeSQLParser(DBFRecordBasedResultSet resultset) {
        Objects.requireNonNull(resultset, "The ResultSet given to the SQL parser cannot be null.");
        rs = resultset;
    }
    
    /**
     * Get the unique conditional statement contained in an SQL statement.
     * @return Conditional clause or null if the statement wasn't accompanied by a where clause.
     * @throws SQLInvalidStatementException if the SQL statement is invalid. 
     */
    public ConditionalClauseResolver parse() throws SQLInvalidStatementException {
        logStep("parse");
        
        String sql = rs.getSQL().trim();
        
        if (sql.toLowerCase().startsWith("select * from ") == false) {
            String message = format(Level.WARNING, "excp.limited_feature_syntax", sql);
            throw new SQLInvalidStatementException(message, rs.getSQL(), rs.getFile());
        }
        
        final String whereWord = " where ";
        int whereIndex = sql.toLowerCase().indexOf(whereWord);

        // If the where clause has not been found, its not an error : there is no condition to set, that's all.
        if (whereIndex == -1)
            return null;

        // Get the conditions.
        int endOfwhereClause = whereIndex + whereWord.length();
        String whereCondition = sql.substring(endOfwhereClause).trim();
        
        // If the condition is empty, it's a syntax error because a WHERE clause went before.
        if (whereCondition.isEmpty()) {
            String message = format(Level.WARNING, "excp.where_without_conditions", sql);            
            throw new SQLInvalidStatementException(message, rs.getSQL(), rs.getFile());
        }
        
        // Currently, all the condition are made of three parts :
        // <Comparand 1> <operator> <Comparand 2>
        // i.e. : A < 5, CITY = 'Kratie', B >= 15.3
        // Spaces are currently expected between parts of the conditional expression.
        String[] parts = whereCondition.split(" ");
        
        if (parts.length != 3) {
            String message = format(Level.WARNING, "excp.limited_feature_conditional_parsing", whereCondition, sql);
            throw new SQLInvalidStatementException(message, rs.getSQL(), rs.getFile());
        }
        
        // Detect and promote litterals in parameters to their best types.
        Object comparand1 = convertToNearestParameterType(parts[0]);
        Object comparand2 = convertToNearestParameterType(parts[2]);
        String operand = parts[1];
        
        ConditionalClauseResolver resolver = new ConditionalClauseResolver(comparand1, comparand2, operand);
        return resolver;
    }

    /**
     * Promote a value to the best parameter available : Integer, then Double, then String.
     * TODO Convert to Date, and admit null values.
     * @param value Value.
     * @return Converted value or value kept as String if no convertion applies.
     */
    private Object convertToNearestParameterType(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e) {
            try {
                return Double.parseDouble(value);
            }
            catch(NumberFormatException ex) {
                return value;
            }
        }
    }
    
    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override public boolean isWrapperFor(Class<?> iface) {
        logStep("isWrapperFor", iface);
        return false;
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.AbstractJDBC#getFile()
     */
    @Override protected File getFile() {
        return rs.getFile();
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.AbstractJDBC#getInterface()
     */
    @Override protected Class<?> getInterface() {
        logStep("getInterface");
        return getClass();
    }
}
