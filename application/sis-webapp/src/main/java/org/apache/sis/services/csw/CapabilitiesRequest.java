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
package org.apache.sis.services.csw;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author haonguyen
 */
public class CapabilitiesRequest {

    public CapabilitiesRequest() {
    }

    public List<GetCapabilitie> GetCapabilitiesRequest() {
        List<GetCapabilitie> m1 = new ArrayList<>();
        String[] version = {"2.0.2", "2.0.0", "1.0.7"};
        String[] ouputformat = {"application/xml"};
        Capacibilities a1 = new Capacibilities();
        a1.setVersion(version);
        Capacibilities a2 = new Capacibilities();
        a2.setOutputFormat(ouputformat);
        GetCapabilitie m2 = new GetCapabilitie(a1, a2);
        m1.add(m2);//cai nay da thay set may cai east + bound dau?
        return m1;
    }
     
}
