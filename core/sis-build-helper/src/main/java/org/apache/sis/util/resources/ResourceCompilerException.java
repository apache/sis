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
package org.apache.sis.util.resources;

import org.apache.maven.plugin.MojoExecutionException;


/**
 * Thrown when the {@link ResourceCompilerMojo} exit abnormally.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
@SuppressWarnings("serial")
final class ResourceCompilerException extends MojoExecutionException {
    /**
     * Creates an exception with the given detail message.
     *
     * @param message  the detail message.
     */
    ResourceCompilerException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with the given cause.
     *
     * @param cause  the cause of this exception.
     */
    ResourceCompilerException(final Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }
}
