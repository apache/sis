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
 * <h2>URL syntax</h2>
 * <p>The file system in this package accepts URIs of the following forms:</p>
 *
 * <ul>
 *   <li>{@code S3://bucket/key}</li>
 *   <li>{@code S3://accessKey@bucket/key} (password not allowed)</li>
 * </ul>
 *
 * <p>Keys can be paths with components separated by the {@code '/'} separator.
 * The password and the region can be specified at
 * {@linkplain java.nio.file.FileSystems#newFileSystem file system initialization time}.
 * The endpoint (e.g. {@code "s3.eu-central-1.amazonaws.com"}) shall <em>not</em> be specified in the URI.
 * In particular the region ({@code "eu-central-1"} in above example) can depend on the server location
 * instead of the data to access, and can be a global configuration for the server.</p>
 *
 * <p>After a {@link java.nio.file.Path} instance has been created,
 * the following syntax can be used on {@code Path} methods expecting a {@code String} argument.
 * The {@code key} can be a path with {@code '/'} separators.</p>
 *
 * <ul>
 *   <li>{@code "S3://bucket/key"} (note that {@code "accessKey@bucket"} is not accepted)</li>
 *   <li>{@code "/bucket/key"} (absolute path)</li>
 *   <li>{@code "key"} (relative path)</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * All classes provided by this package are safe of usage in multi-threading environment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/index.html">AWS SDK for Java</a>
 *
 * @since 1.2
 * @module
 */
package org.apache.sis.cloud.aws.s3;
