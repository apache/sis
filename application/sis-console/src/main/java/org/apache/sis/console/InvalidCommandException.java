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
 * Thrown when a unknown command has been given by the user on the command-line.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
public class InvalidCommandException extends Exception {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4751525514150949949L;

    /**
     * The name of the invalid command.
     */
    private final String command;

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message  the detail message.
     * @param command  the name of the invalid command.
     */
    public InvalidCommandException(final String message, final String command) {
        super(message);
        this.command = command;
    }

    /**
     * Returns the name of the invalid command.
     *
     * @return the name of the invalid command.
     */
    public String getCommand() {
        return command;
    }
}
