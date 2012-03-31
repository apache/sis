package org.apache.sis.storage;

import junit.framework.TestCase;

public class TestQuadTreeNode extends TestCase{

	/**
	 * @since SIS-39 
	 */
	public void testCapacityGreaterThanZero(){
		QuadTreeNode node = new QuadTreeNode(-1, -5);
		assertNotNull(node);
		assertTrue(node.getCapacity() > 0);
	}
}
