/*
 * Dijkstra algorithm implementation for single-source shortest path with negative weights. (Long weight)
 */

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.DefaultTableModel;


public class DijkstraAlgorithm {

  private final List<Node> nodes;
  private final List<Link> edges;
  private Set<Node> settledNodes;
  private Set<Node> unSettledNodes;
  private Map<Node, Node> predecessors;
  private Map<Node, Long> distance;

  public DijkstraAlgorithm(Network graph) {
	    // create a copy of the array so that we can operate on this array
	    this.nodes = new ArrayList<Node>(graph.getNodes());
	    this.edges = new ArrayList<Link>(graph.getLinks());
  }
  public DijkstraAlgorithm(LinkProtection graph) {
	    // create a copy of the array so that we can operate on this array
	    this.nodes = new ArrayList<Node>(graph.getNodes());
	    this.edges = new ArrayList<Link>(graph.getLinks());
  }
  public DijkstraAlgorithm(KLinkDisjointPaths graph) {
	    // create a copy of the array so that we can operate on this array
	    this.nodes = new ArrayList<Node>(graph.getNodes());
	    this.edges = new ArrayList<Link>(graph.getLinks());
  }
  public DijkstraAlgorithm(H_SPSR_MinFS graph) {
	    // create a copy of the array so that we can operate on this array
	    this.nodes = new ArrayList<Node>(graph.getNodes());
	    this.edges = new ArrayList<Link>(graph.getLinks());
}
  /*
   * 
   */

  public void execute(Node source) {
    settledNodes = new HashSet<Node>();
    unSettledNodes = new HashSet<Node>();
    distance = new HashMap<Node, Long>();
    predecessors = new HashMap<Node, Node>();
    distance.put(source, 0L);
    unSettledNodes.add(source);
    while (unSettledNodes.size() > 0) {
      Node node = getMinimum(unSettledNodes);
      settledNodes.add(node);
      unSettledNodes.remove(node);
      findMinimalDistances(node);
    }
  }

  private void findMinimalDistances(Node node) {
    List<Node> adjacentNodes = getNeighbors(node);
    for (Node target : adjacentNodes) {
      if (getShortestDistance(target) > getShortestDistance(node)
          + getDistance(node, target)) {
        distance.put(target, getShortestDistance(node)
            + getDistance(node, target));
        predecessors.put(target, node);
        unSettledNodes.add(target);
      }
    }

  }

  private long getDistance(Node node, Node target) {
    for (Link edge : edges) {
      if (edge.GetSrcNode().equals(node)
          && edge.GetDstNode().equals(target)) {
        return edge.GetWeight();
      }
    }
    throw new RuntimeException("Should not happen");
  }

  private List<Node> getNeighbors(Node node) {
    List<Node> neighbors = new ArrayList<Node>();
    for (Link edge : edges) {
      if (edge.GetSrcNode().equals(node)
          && !isSettled(edge.GetDstNode())) {
        neighbors.add(edge.GetDstNode());
      }
    }
    return neighbors;
  }

  private Node getMinimum(Set<Node> vertexes) {
    Node minimum = null;
    for (Node vertex : vertexes) {
      if (minimum == null) {
        minimum = vertex;
      } else {
        if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
          minimum = vertex;
        }
      }
    }
    return minimum;
  }

  private boolean isSettled(Node vertex) {
    return settledNodes.contains(vertex);
  }

  private long getShortestDistance(Node destination) {
    Long d = distance.get(destination);
    if (d == null) {
      return Integer.MAX_VALUE;
    } else {
      return d;
    }
  }

  /*
   * This method returns the path from the source to the selected target and
   * NULL if no path exists
   */
  public Path getPath(Node src, Node target) {
    LinkedList<Node> path = new LinkedList<Node>();
    ArrayList<Link> lpath = new ArrayList<Link>();
    Path route;
    Node step = target;
    //DefaultTableModel table = new DefaultTableModel(1,2);
    double w=0;
    // check if a path exists
    if (predecessors.get(step) == null) {
    	//table.setValueAt(null, 0, 0);
        //table.setValueAt(null, 0, 1);
    	route = null;
      return route;
    }
    path.add(step);
    while (predecessors.get(step) != null) {
      step = predecessors.get(step);
      path.add(step);
    }
    // Put it into the correct order
    Collections.reverse(path);
    for (int i=0;i<path.size()-1;i++){
    	for (Link link:edges){
    		if (link.GetSrcNode().equals(path.get(i))&&(link.GetDstNode().equals(path.get(i+1)))){
    			lpath.add(link);
    			w+=link.GetWeight();
		    }
    	}	
	}
    route = new Path (src, target, lpath, w);
    //table.setValueAt(lpath, 0, 0);
    //table.setValueAt(route, 0, 1);
    return route;
  }

} 

