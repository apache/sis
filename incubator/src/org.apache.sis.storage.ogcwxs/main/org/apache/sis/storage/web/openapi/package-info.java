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
 * Bindings for OGC-API services for java 11 HttpClient.
 *
 * <p>
 * Based bindings have been generated with https://github.com/OpenAPITools/openapi-generator
 * then cleaned and simplified.
 * <p>
 * Exemple :
 * <pre>
 * OpenApiConfiguration conf = new OpenApiConfiguration();
 * conf.setBasePath("https://server.com/service-openapi/");
 *
 * ConformanceApi api = new ConformanceApi(conf);
 * try {
 *     ConfClasses result = api.getConformance("json");
 *     System.out.println(result);
 * } catch (ApiException e) {
 *     System.err.println("Exception when calling ConformanceApi#getConformance");
 *     System.err.println("Status code: " + e.getCode());
 *     System.err.println("Reason: " + e.getResponseBody());
 *     System.err.println("Response headers: " + e.getResponseHeaders());
 *     e.printStackTrace();
 * }
 * </pre>
 *
 *
 */
package org.apache.sis.storage.web.openapi;
