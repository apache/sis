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
import java.sql.Types;


/**
 * Maps a few basic Java types to JDBC types.
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
        new TypeMapper(Boolean.class, Types.BOOLEAN,   "BOOLEAN"),
        new TypeMapper(Date   .class, Types.TIMESTAMP, "TIMESTAMP"),
        new TypeMapper(Double .class, Types.DOUBLE,    "DOUBLE PRECISION"),
        new TypeMapper(Float  .class, Types.REAL,      "REAL"),
        new TypeMapper(Long   .class, Types.BIGINT,    "BIGINT"),
        new TypeMapper(Integer.class, Types.INTEGER,   "INTEGER"),
        new TypeMapper(Short  .class, Types.SMALLINT,  "SMALLINT"),
        new TypeMapper(Byte   .class, Types.TINYINT,   "SMALLINT"),     // Derby does not support TINYINT.
        new TypeMapper(Number .class, Types.DECIMAL,   "DECIMAL")       // Implemented by BigDecimal.
    };

    /**
     * The Java class.
     */
    private final Class<?> classe;

    /**
     * A constant from the SQL {@link Types} enumeration.
     */
    private final int type;

    /**
     * The SQL keyword for that type.
     */
    private final String keyword;

    /**
     * For internal use only.
     */
    private TypeMapper(final Class<?> classe, final int type, final String keyword) {
        this.classe  = classe;
        this.type    = type;
        this.keyword = keyword;
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
                    return type.keyword;
                }
            }
        }
        return null;
    }

    /**
     * Return the Java class for the given SQL type, or {@code null} if none.
     *
     * @param  type  one of the {@link Types} constants.
     * @return the Java class, or {@code null} if none.
     */
    public static Class<?> toJavaType(final int type) {
        for (final TypeMapper t : TYPES) {
            if (t.type == type) {
                return t.classe;
            }
        }
        return null;
    }
}
