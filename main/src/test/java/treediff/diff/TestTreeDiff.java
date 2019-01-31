package treediff.diff;

import java.util.ArrayList;
import java.util.Arrays;

import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import treediff.util.*;
import treediff.diff.*;

public class TestTreeDiff {
	Tree tree_one, tree_two, tree_three, tree_four;
	
	@Before
	public void setUp() {
		TreeNode a = new TreeNode("A");
		TreeNode b = new TreeNode("B");
		a.add_child(b);
		TreeNode d = new TreeNode("D");
		b.add_child(d);
		tree_one = new Tree(a);
		tree_one.build_caches();

		a = new TreeNode("A");
		b = new TreeNode("B");
		TreeNode c = new TreeNode("C");
		d = new TreeNode("D");
		a.add_child(b);
		a.add_child(c);
		c.add_child(d);
		tree_two = new Tree(a);
		tree_two.build_caches();

		a = new TreeNode("A");
		b = new TreeNode("B");
		c = new TreeNode("C");
		d = new TreeNode("D");
		TreeNode e = new TreeNode("E");
		a.add_child(b);
		a.add_child(c);
		c.add_child(d);
		d.add_child(e);
		tree_three = new Tree(a);
		tree_three.build_caches();
		
		a = new TreeNode("A");
		b = new TreeNode("B");
		c = new TreeNode("CC");
		d = new TreeNode("D");
		e = new TreeNode("E");
		a.add_child(b);
		a.add_child(c);
		c.add_child(d);
		d.add_child(e);
		tree_four = new Tree(a);
		tree_four.build_caches();
	}

	@Test
	public void testDistance() {
		assertEquals(2,(int)TreeDiff.computeDiff(tree_one, tree_two).getValue0());
		assertEquals(3,(int)TreeDiff.computeDiff(tree_one, tree_three).getValue0());
		assertEquals(1,(int)TreeDiff.computeDiff(tree_two, tree_three).getValue0());
		assertEquals(1,(int)TreeDiff.computeDiff(tree_three, tree_four).getValue0());
		assertEquals(0,(int)TreeDiff.computeDiff(tree_two, tree_two).getValue0());
	}
	
	@Test
	public void testMapping() {
		ArrayList<Pair<Object, Object>> mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_one, tree_two).getValue1();
		String expectedMapping = "[[1, 1], [2, 3], [3, 4], [alpha, 2]]";
		assertEquals(expectedMapping, mapping.toString());
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_one, tree_three).getValue1();
		expectedMapping = "[[1, 1], [2, 3], [3, 4], [alpha, 2], [alpha, 5]]";
		assertEquals(expectedMapping, mapping.toString());
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_two, tree_three).getValue1();
		expectedMapping = "[[1, 1], [2, 2], [3, 3], [4, 4], [alpha, 5]]";
		assertEquals(expectedMapping, mapping.toString());
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_three, tree_four).getValue1();
		expectedMapping = "[[1, 1], [2, 2], [3, 3], [4, 4], [5, 5]]";
		assertEquals(expectedMapping, mapping.toString());
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_two, tree_two).getValue1();
		expectedMapping = "[[1, 1], [2, 2], [3, 3], [4, 4]]";
		assertEquals(expectedMapping, mapping.toString());	
	}

	@Test
	public void testProduceHumanFriendlyMapping() {
		ArrayList<Pair<Object, Object>> mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_one, tree_two).getValue1();
		ArrayList<String> description = TreeDiff.produceHumanFriendlyMapping(mapping, tree_one, tree_two);
		ArrayList<String> expected = new ArrayList<>(Arrays.asList("No change for A (@1 and @1)",
		        "Change from B (@2) to C (@3)", "No change for D (@3 and @4)", "Insert B (@2)"));
		assertEquals(expected, description);
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_one, tree_three).getValue1();
		description = TreeDiff.produceHumanFriendlyMapping(mapping, tree_one, tree_three);
		expected = new ArrayList<>(Arrays.asList("No change for A (@1 and @1)",
		        "Change from B (@2) to C (@3)", "No change for D (@3 and @4)", "Insert B (@2)', 'Insert E (@5)"));
		assertEquals(expected, description);
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_two, tree_three).getValue1();
		description = TreeDiff.produceHumanFriendlyMapping(mapping, tree_two, tree_three);
		expected = new ArrayList<>(Arrays.asList("No change for A (@1 and @1)", "No change for B (@2 and @2)",
		        "No change for C (@3 and @3)", "No change for D (@4 and @4)", "Insert E (@5)"));
		assertEquals(expected, description);
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_three, tree_four).getValue1();
		description = TreeDiff.produceHumanFriendlyMapping(mapping, tree_three, tree_four);
		expected = new ArrayList<>(Arrays.asList("No change for A (@1 and @1)", "No change for B (@2 and @2)",
		        "Change from C (@3) to CC (@3)", "No change for D (@4 and @4)","No change for E (@5 and @5)"));
		assertEquals(expected, description);
		
		mapping = (ArrayList<Pair<Object, Object>>)TreeDiff.computeDiff(tree_two, tree_two).getValue1();
		description = TreeDiff.produceHumanFriendlyMapping(mapping, tree_two, tree_two);
		expected = new ArrayList<>(Arrays.asList("No change for A (@1 and @1)", "No change for B (@2 and @2)",
		        "No change for C (@3 and @3)", "No change for D (@4 and @4)"));
		assertEquals(expected, description);
		
		
	}
	
}
