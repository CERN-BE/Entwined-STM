/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.stm.Node;

/**
 * Unit tests for {@link Node} class.
 * 
 * @author Ivan Koblik
 */
public class NodeTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private <T> Node<T> createNode(T value) {
        return new Node<T>(value);
    }

    @Test
    public void testNodeT() {
        createNode(10);
    }

    @Test
    public void testNodeT_nullValue() {
        createNode(null);
    }

    @Test
    public void testGetChildren_emptyList() {
        Node<Integer> root = createNode(20);
        assertTrue("No children", root.getChildren().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetChildren_cannotBeModified() {
        Node<Integer> root = createNode(20);
        root.getChildren().add(root);
    }

    @Test
    public void testGetChildren_ListsElementsInRightOrder() {
        Node<Integer> root = createNode(20);
        List<Node<Integer>> children = new ArrayList<Node<Integer>>();
        children.add(createNode(20));
        children.add(createNode(30));
        children.add(createNode(40));
        for (Node<Integer> child : children) {
            root.addChild(child);
        }
        assertEquals(children, root.getChildren());
    }

    @Test
    public void testGetNumberOfChildren_noChildren() {
        Node<Integer> root = createNode(20);
        assertEquals("Empty node", 0, root.getNumberOfChildren());
    }

    @Test
    public void testGetNumberOfChildren_withChildren() {
        Node<Integer> root = createNode(20);
        root.addChild(createNode(30));
        root.addChild(createNode(40));
        root.addChild(createNode(50));
        assertEquals("Empty node", 3, root.getNumberOfChildren());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddChild_fail_sameChild() {
        Node<Integer> root = createNode(20);
        Node<Integer> child = createNode(10);
        root.addChild(child);
        root.addChild(child);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddChild_fail_twoParents() {
        Node<Integer> root = createNode(20);
        Node<Integer> root2 = createNode(20);
        Node<Integer> child = createNode(10);
        root.addChild(child);
        root2.addChild(child);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddChild_failNull() {
        Node<Integer> root = createNode(20);
        root.addChild(null);

    }

    @Test
    public void testRemoveChild_fromEmptyNode() {
        assertNull("Removed child", createNode(10).removeChild());
    }

    @Test
    public void testRemoveChild_fromFilledNode() {
        Node<Integer> root = createNode(20);
        Node<Integer> child1 = createNode(30);
        Node<Integer> child2 = createNode(40);
        root.addChild(child1);
        root.addChild(child2);

        assertSame("Removed child", child2, root.removeChild());
        assertEquals("One less", 1, root.getNumberOfChildren());
        assertSame("Removed child", child1, root.removeChild());
        assertEquals("No children", 0, root.getNumberOfChildren());
    }

    @Test
    public void testGetValue() {
        assertEquals("Assigned value", (Integer) 10, createNode(10).getValue());
    }

    @Test
    public void testGetValue_nullValue() {
        assertEquals("Assigned value", null, createNode(null).getValue());
    }

    @Test
    public void testGetParent_noParent() {
        assertNull(createNode(100).getParent());
    }

    @Test
    public void testGetParent_withParent() {
        Node<Integer> root = createNode(10);
        Node<Integer> child = createNode(100);
        root.addChild(child);
        assertSame("Root node", root, child.getParent());
    }

    @Test
    public void testGetParent_removedFromParent() {
        Node<Integer> root = createNode(10);
        Node<Integer> child = createNode(100);
        root.addChild(child);
        assertNull("Root node", root.removeChild().getParent());
    }

    @Test
    public void testToString_containsValue() {
        assertThat(createNode(10).toString(), Matchers.containsString("10"));
    }

    @Test
    public void testToString_nullValue() {
        assertThat(createNode(null).toString(), Matchers.containsString(String.valueOf((Object) null)));
    }

}
