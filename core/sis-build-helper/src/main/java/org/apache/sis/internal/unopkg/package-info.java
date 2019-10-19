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
 * Creates a {@code .oxt} file for <a href="http://www.openoffice.org">OpenOffice.org</a> add-in.
 * Those files are basically ZIP files with {@code META-INF/manifest.xml} and {@code .rdb} entries.
 * The principle is similar to Java JAR files.
 *
 * <p>This Maven plugin has be written for building the Apache SIS add-in for OpenOffice.org,
 * but can also be used for other projects. See the Apache SIS add-in module for an example
 * about how a module can be configured in order to use this Maven plugin.</p>
 *
 * <h2>Usage</h2>
 * Below is an example of {@code pom.xml} build configuration.
 * The following strings need to be replaced:
 *
 * <ul>
 *   <li>{@code com.myproject.Registration} —
 *       The fully qualified class name of your class which contain the {@code __getServiceFactory(…)}
 *       and {@code __writeRegistryServiceInfo(…)} public static methods.</li>
 *   <li>{@code myAddinFilename} —
 *       The final filename, without the {@code .oxt} extension.</li>
 * </ul>
 *
 * <h2>Maven project file</h2>
 * {@preformat xml
 *   <dependencies>
 *     <!-- Put all your project dependencies here, including transitive dependencies. -->
 *   </dependencies>
 *
 *   <build>
 *     <plugins>
 *       <!-- Add a manifest entry for add-ins registration in OpenOffice -->
 *       <plugin>
 *         <groupId>org.apache.maven.plugins</groupId>
 *         <artifactId>maven-jar-plugin</artifactId>
 *         <configuration>
 *           <archive>
 *             <manifestEntries>
 *               <RegistrationClassName>
 *                 com.myproject.Registration
 *               </RegistrationClassName>
 *             </manifestEntries>
 *           </archive>
 *         </configuration>
 *       </plugin>
 *
 *       <!-- Create the oxt file. -->
 *       <plugin>
 *         <groupId>org.apache.sis.core</groupId>
 *         <artifactId>sis-build-helper</artifactId>
 *         <configuration>
 *           <oxtName>myAddinFilename</oxtName>
 *         </configuration>
 *         <executions>
 *           <execution>
 *             <goals>
 *               <goal>javamaker</goal>
 *               <goal>unopkg</goal>
 *             </goals>
 *           </execution>
 *         </executions>
 *       </plugin>
 *     </plugins>
 *   </build>
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
package org.apache.sis.internal.unopkg;
