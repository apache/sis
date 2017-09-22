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
package org.apache.sis.geometry;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link ArrayEnvelope}.
 *
 * @date 2017-09-22
 * @see ArrayEnvelope
 *
 **/
public class ArrayEnvelopeTest{
  @Test
  public void testIsEmptyReturningTrue() {
      double[] doubleArray = new double[2];
      doubleArray[1] = (-356.683168);
      ArrayEnvelope arrayEnvelope = new ArrayEnvelope(doubleArray);

      assertTrue(arrayEnvelope.isEmpty());
  }

  @Test
  public void testGetMaximum() {
      double[] doubleArray = new double[4];
      doubleArray[2] = (-1728.18367);
      GeneralEnvelope generalEnvelope = new GeneralEnvelope(doubleArray);

      assertEquals(Double.POSITIVE_INFINITY, generalEnvelope.getMaximum(0), 0.01);
  }

  @Test
  public void testGetMinimum() {
      double[] doubleArray = new double[4];
      doubleArray[2] = (-1728.18367);
      GeneralEnvelope generalEnvelope = new GeneralEnvelope(doubleArray);
      ImmutableEnvelope immutableEnvelope = ImmutableEnvelope.castOrCopy(generalEnvelope);

      assertEquals(Double.NEGATIVE_INFINITY, immutableEnvelope.getMinimum(0), 0.01);
  }

  @Test
  public void testCreatesArrayEnvelopeTakingCharSequence() {
      ArrayEnvelope arrayEnvelope = new ArrayEnvelope("BOX6D(-5610.14928 -3642.514809668324 1957.44315184437 -170.01749999999993 -77.9698 -Infinity, -5610.14928 -3642.514809668324 1957.44315184437 -170.01749999999993 -77.9698 -Infinity)");

      assertEquals(6, arrayEnvelope.getDimension());
  }

  @Test
  public void testFailsToCreateArrayEnvelopeTakingCharSequenceThrowsNumberFormatException() {
      try {
        new ArrayEnvelope("IdentifierSpace[\"Xr]");
        fail("Expecting exception: NumberFormatException");
      
      } catch(NumberFormatException e) {
      }
  }

  @Test
  public void testCreatesArrayEnvelopeTakingInt() {
      ArrayEnvelope arrayEnvelope = new ArrayEnvelope(0);

      assertTrue(arrayEnvelope.isAllNaN());
  }

    @Test
    public void testEquals() {
        ArrayEnvelope arrayEnvelopeOne = new ArrayEnvelope("anchorPoint");
        ArrayEnvelope arrayEnvelopeTwo = new ArrayEnvelope("anchorPoint 1");

        assertFalse(arrayEnvelopeOne.equals(null));
        assertFalse(arrayEnvelopeOne.equals(arrayEnvelopeTwo));
        assertTrue(arrayEnvelopeOne.equals(arrayEnvelopeOne));
        assertTrue(arrayEnvelopeTwo.equals(arrayEnvelopeTwo));
    }

}