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
package org.apache.sis.metadata.sql.internal.shared;

import java.util.Date;
import java.sql.JDBCType;


/**
 * Maps a few basic Java types to <abbr>JDBC</abbr> types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TypeMapper {
    /**
     * A list of Java classes to be mapped to SQL types. We do not want to map every SQL types,
     * but only the ones which are of interest for the Apache SIS metadata implementation.
     * The types will be tested in the order they are declared, so the last declarations are fallbacks.
     *
     * <p>The types declared here matches both the Derby and PostgreSQL mapping.</p>
     */
    private static final TypeMapper[] TYPES = {
        new TypeMapper(Boolean.class, JDBCType.BOOLEAN),
        new TypeMapper(Date   .class, JDBCType.TIMESTAMP),
        new TypeMapper(Double .class, JDBCType.DOUBLE),
        new TypeMapper(Float  .class, JDBCType.REAL),
        new TypeMapper(Long   .class, JDBCType.BIGINT),
        new TypeMapper(Integer.class, JDBCType.INTEGER),
        new TypeMapper(Short  .class, JDBCType.SMALLINT),
        new TypeMapper(Byte   .class, JDBCType.TINYINT),
        new TypeMapper(Number .class, JDBCType.DECIMAL)     // Implemented by BigDecimal.
    };

    /**
     * The Java class.
     */
    private final Class<?> classe;

    /**
     * The data type.
     */
    private final JDBCType type;

    /**
     * For internal use only.
     */
    private TypeMapper(final Class<?> classe, final JDBCType type) {
        this.classe = classe;
        this.type   = type;
    }

    /**
     * Returns the SQL keyword for storing an element of the given type, or {@code null} if unknown.
     * This method does not handle the text type, so {@link String} are treated as "unknown" as well.
     * We do that way because the caller will need to specify a value in {@code VARCHAR(n)} statement.
     *
     * @param  classe  the class for which to get the SQL keyword in a {@code CREATE TABLE} statement.
     * @return the SQL keyword, or {@code null} if unknown.
     */
    public static String keywordFor(final Class<?> classe) {
        if (classe != null) {
            for (final TypeMapper type : TYPES) {
                if (type.classe.isAssignableFrom(classe)) {
                    switch (type.type) {
                        case DOUBLE:  return "DOUBLE PRECISION";
                        case TINYINT: return "SMALLINT";   // Derby does not support TINYINT.
                        default: return type.type.name();
                    }
                }
            }
        }
        return null;
    }
}
