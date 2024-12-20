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


/**
 * Java NIO wrappers for Amazon Simple Storage Service (S3).
 * The wrapped framework is AWS SDK version 2.
 *
 * <h2><abbr>URL</abbr> syntax</h2>
 * The S3 storage mechanism is similar to a {@code java.util.Map}:
 * a set of key-value pairs where the keys have no particular meaning for S3.
 * However, Apache <abbr>SIS</abbr> interprets the key names in a way similar to file paths.
 * In this module, <dfn>files</dfn> can be understood as S3 <em>keys</em>
 * with a special meaning given by Apache <abbr>SIS</abbr> to the {@code '/'} character.
 * The file system view in this package accepts <abbr>URI</abbr>s of the following forms:
 *
 * <ul>
 *   <li>{@code S3://bucket/file}</li>
 *   <li>{@code S3://accessKey@bucket/file} (password not allowed in <abbr>URI</abbr>)</li>
 * </ul>
 *
 * The separator character for the file components is {@code '/'} on all platforms.
 * The password and the region can be specified at
 * {@linkplain java.nio.file.FileSystems#newFileSystem file system initialization time}.
 * The endpoint (e.g. {@code "s3.eu-central-1.amazonaws.com"}) shall <em>not</em> be specified in the <abbr>URI</abbr>.
 * In particular, the region ({@code "eu-central-1"} in the above example) can depend on the server location
 * instead of the data to access, and can be a global configuration for the server.
 *
 * <p>After a {@link java.nio.file.Path} instance has been created,
 * the following syntax can be used on {@code Path} methods expecting a {@code String} argument.
 * The {@code file} can be a string with {@code '/'} separators.</p>
 *
 * <ul>
 *   <li>{@code "S3://bucket/file"}</li>
 *   <li>{@code "/bucket/file"} (absolute path)</li>
 *   <li>{@code "file"} (relative path)</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * All classes provided by this package are safe of usage in multi-threading environment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/index.html">AWS SDK for Java</a>
 *
 * @since 1.2
 */
package org.apache.sis.cloud.aws.s3;
