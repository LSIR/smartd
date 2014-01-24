package gsn.utils.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class TestGraph {
	
	@Test
	public void testRemoveCycles() throws NodeNotExistsExeption{
		Graph<String> graph = new Graph<String>();
		graph.addNode("n1");
		graph.addNode("n2");
		graph.addNode("n3");
		graph.addNode("n4");
		graph.addNode("n5");
		graph.addNode("n6");
		graph.addEdge("n2", "n3");
		graph.addEdge("n3", "n4");
		graph.addEdge("n1", "n5");
		graph.addEdge("n2", "n6");
		assertFalse(graph.hasCycle());
		graph.addEdge("n4", "n2");
		printGraph(graph, "testRemoveCycles1");
		assertTrue(graph.hasCycle());
		graph.removeNode("n4");
		assertNull(graph.findNode("n4"));
		assertNull(graph.findNode("n3"));
		assertNull(graph.findNode("n2"));
		printGraph(graph, "testRemoveCycles2");
		graph.addEdge("n6", "n5");
		graph.addEdge("n6", "n6");
		printGraph(graph, "testRemoveCycles3");
		assertTrue(graph.hasCycle());
		graph.removeNode("n6");
		assertNull(graph.findNode("n6"));
		assertNotNull(graph.findNode("n5"));
		printGraph(graph, "testRemoveCycles4");
	}
	
	@Test
	public void testRemoveNode() throws NodeNotExistsExeption{
		Graph<String> graph = new Graph<String>();
		graph.addNode("n1");
		graph.addNode("n2");
		graph.addNode("n3");
		graph.addNode("n4");
		graph.addNode("n5");
		graph.addNode("n6");
		graph.addEdge("n2", "n3");
		graph.addEdge("n3", "n4");
		graph.addEdge("n1", "n5");
		graph.addEdge("n2", "n6");
		printGraph(graph, "testRemoveNode1");
		graph.removeNode("n4");
		assertNull(graph.findNode("n2"));
		assertNotNull(graph.findNode("n6"));
		printGraph(graph, "testRemoveNode2");
	}
	
	@Test
	public void testFindRootNode() throws NodeNotExistsExeption{
		Graph<String> graph = new Graph<String>();
		graph.addNode("n1");
		graph.addNode("n2");
		graph.addNode("n3");
		graph.addNode("n4");
		graph.addNode("n5");
		graph.addNode("n6");
		graph.addEdge("n2", "n3");
		graph.addEdge("n3", "n4");
		graph.addEdge("n1", "n5");
		graph.addEdge("n2", "n6");
		assertEquals(graph.findRootNode(graph.findNode("n5")), graph.findNode("n1"));
		assertEquals(graph.findRootNode(graph.findNode("n1")), graph.findNode("n1"));
		assertNotSame(graph.findRootNode(graph.findNode("n1")), graph.findNode("n5"));
		assertEquals(graph.findRootNode(graph.findNode("n4")), graph.findNode("n2"));
	}

	private void printGraph(Graph graph, String message) {
		System.out.println("===================" + message + "==================");
		System.out.println(graph);
	}
}
