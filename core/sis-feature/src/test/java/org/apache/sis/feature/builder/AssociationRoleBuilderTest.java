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
package org.apache.sis.feature.builder;

import org.apache.sis.util.iso.SimpleInternationalString;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Locale;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.XLink;
import org.opengis.util.InternationalString;

/**
 * Unit tests for class {@link AssociationRoleBuilder}.
 *
 * @date 2017-09-22
 * @see AssociationRoleBuilder
 *
 **/
public class AssociationRoleBuilderTest{
  @Test
  public void testSetDescription() {
      DefaultNameFactory defaultNameFactory = new DefaultNameFactory();
      GeometryLibrary geometryLibrary = GeometryLibrary.JAVA2D;
      Locale locale = new Locale("`$v5w*nd");
      FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder(defaultNameFactory, geometryLibrary, locale);
      CharSequence[] charSequenceArray = new CharSequence[3];
      charSequenceArray[0] = (CharSequence) "`$v5w*nd 1";
      charSequenceArray[1] = (CharSequence) "`$v5w*nd 2";
      charSequenceArray[2] = (CharSequence) "`$v5w*nd 3";
      FeatureTypeBuilder featureTypeBuilderTwo = featureTypeBuilder.setName(charSequenceArray);
      CitationConstant.Authority<Object> citationConstant_Authority = new CitationConstant.Authority<Object>("Mojo `$v5w*nd");
      InternationalString internationalString = citationConstant_Authority.getTitle();
      DefaultFeatureType defaultFeatureType = featureTypeBuilderTwo.build();
      IdentifierSpace<XLink> identifierSpace = IdentifierSpace.XLINK;
      NamedIdentifier namedIdentifier = new NamedIdentifier(identifierSpace, "", charSequenceArray[1], "definition", internationalString);
      AssociationRoleBuilder associationRoleBuilder =
              new AssociationRoleBuilder(featureTypeBuilderTwo, defaultFeatureType, namedIdentifier).
                      setDescription(charSequenceArray[2]).
                      setDefinition("a").
                      setDesignation("b").
                      setMaximumOccurs(2).
                      setMinimumOccurs(1);

      DefaultAssociationRole defaultAssociationRole = associationRoleBuilder.build();

      assertEquals(1, defaultAssociationRole.getMinimumOccurs());
      assertEquals(2, defaultAssociationRole.getMaximumOccurs());
      assertEquals(new SimpleInternationalString("b"), defaultAssociationRole.getDesignation());
      assertEquals(new SimpleInternationalString("a"), defaultAssociationRole.getDefinition());
      assertEquals(new SimpleInternationalString("`$v5w*nd 3"), defaultAssociationRole.getDescription());
  }
}