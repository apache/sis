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
package org.apache.sis.internal.style;

import java.awt.Color;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.util.InternationalString;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractStyleTests extends TestCase {

    protected static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();
    protected static final Expression<Feature,Double> EXP_DOUBLE = FF.literal(5.1);
    protected static final Expression<Feature,Double> EXP_DOUBLE_2 = FF.literal(10.5);
    protected static final Expression<Feature,String> EXP_STRING = FF.literal("some text");
    protected static final Expression<Feature,Color> EXP_COLOR = FF.literal(Color.YELLOW);
    protected static final InternationalString SAMPLE_ISTRING = new SimpleInternationalString("some international text");
    protected static final String SAMPLE_STRING = "some text";
}
