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
package org.apache.sis.services.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author haonguyen
 */

public class RunServer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(new String[] {           
            "org/apache/sis/services/catalog/restapp.xml"         
        });
        System.out.println(appContext.getBeanFactory());;
        GetCapabilitiesService categoryService = (GetCapabilitiesService) appContext.getBean("categoryService");      // Service instance      
        JAXRSServerFactoryBean restServer = new JAXRSServerFactoryBean();
        restServer.setServiceBean(categoryService);
        restServer.setAddress("http://localhost:9000/");
        restServer.create();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            br.readLine();
        } catch (IOException e) {
        }
        System.out.println("Server Stopped");
        System.exit(0);
    }
}
