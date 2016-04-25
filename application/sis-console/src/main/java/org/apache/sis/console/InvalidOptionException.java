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
package org.apache.sis.console;


/**
 * Thrown when an illegal option has been given by the user on the command-line.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class InvalidOptionException extends Exception {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2947996310387227986L;

    /**
     * The name of the invalid option.
     */
    private final String option;

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message  the detail message.
     * @param option   the name of the invalid option.
     */
    public InvalidOptionException(final String message, final String option) {
        super(message);
        this.option = option;
    }

    /**
     * Constructs an exception with the specified detail message and cause.
     *
     * @param message  the detail message.
     * @param cause    the cause of this exception.
     * @param option   the name of the invalid option.
     */
    public InvalidOptionException(final String message, final Throwable cause, final String option) {
        super(message, cause);
        this.option = option;
    }

    /**
     * Returns the name of the invalid option.
     *
     * @return the name of the invalid option.
     */
    public String getOption() {
        return option;
    }
}
