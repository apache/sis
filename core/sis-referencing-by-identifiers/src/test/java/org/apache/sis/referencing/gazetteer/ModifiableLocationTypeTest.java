package org.apache.sis.referencing.gazetteer;

import org.apache.sis.util.iso.DefaultInternationalString;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link ModifiableLocationType}.
 *
 * @see ModifiableLocationType
 */
public class ModifiableLocationTypeTest {

    @Test
    public void testRemoveParentThrowsIllegalArgumentException() {
        ModifiableLocationType modifiableLocationType = new ModifiableLocationType("java.awt.Dimension[width=7,height=-1183]");

        try {
            modifiableLocationType.removeParent(modifiableLocationType);
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(ModifiableLocationType.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testAddParentThrowsIllegalStateException() {
        ModifiableLocationType modifiableLocationType = new ModifiableLocationType("deprecated");
        ModifiableLocationType modifiableLocationTypeTwo = new ModifiableLocationType("overallOwner");
        modifiableLocationType.addParent(modifiableLocationTypeTwo);

        try {
            modifiableLocationType.addParent(modifiableLocationTypeTwo);
            fail("Expecting exception: IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(ModifiableLocationType.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testRemoveIdentificationThrowsIllegalArgumentException() {
        ModifiableLocationType modifiableLocationType = new ModifiableLocationType("locale");

        try {
            modifiableLocationType.removeIdentification("theme");
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(ModifiableLocationType.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testAddIdentificationThrowsIllegalArgumentException() {
        DefaultInternationalString defaultInternationalString = new DefaultInternationalString();
        ModifiableLocationType modifiableLocationType = new ModifiableLocationType(defaultInternationalString);
        modifiableLocationType.addIdentification("org.apache.sis.internal.referencing.j2d.AffineMatrix");

        try {
            modifiableLocationType.addIdentification("org.apache.sis.internal.referencing.j2d.AffineMatrix");
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(ModifiableLocationType.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }
}