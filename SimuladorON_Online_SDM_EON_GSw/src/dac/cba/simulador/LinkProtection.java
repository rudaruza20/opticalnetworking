
/*
 * Implementation of the algorithm purposed by [Routing, Flow, and Capacity design in Comm Networks and Comp. Networks] Michal Pióro et al.  
 */
package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class LinkProtection {
	private final List<Node> nodes;
	private final List<Link> edges;
	
	public LinkProtection(Network graph) {
		// create a copy of the array so that we can operate on this array
	    this.nodes = new ArrayList<Node>(graph.getNodes());
	    this.edges = new LinkedList<Link>(graph.getLinks());
	}
	public List<Node> getNodes (){
		return nodes;
	}
	public List<Link> getLinks (){
		return edges;
	}
	public ArrayList<Path> execute (Node src, Node dst){
		ArrayList<Link> joinEdgesSet;
		ArrayList<Path> paths;
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(this);
		dijkstra.execute(src); // 1. execute SPD in the original graph
		//DefaultTableModel table = dijkstra.getPath(src, dst);
		Path path1 =  dijkstra.getPath(src, dst); 
		//ArrayList<Link> lpath1 = (ArrayList<Link>)table.getValueAt(0, 0);
		ArrayList<Link> lpath1 = path1.GetPath();
		removeEdges(lpath1);// 2. Remove Links of the SPD previously obtained (only in one direction, the reverse path is mantainned according to Reference [Routing, Flow and Capacity Design in Computer Networks, Michal Pióro]  
		/*
		 * 3. Execute again SPD in the modified Graph
		 */
		DijkstraAlgorithm dijkstra2 = new DijkstraAlgorithm(this);
		dijkstra2.execute(src); 
		//DefaultTableModel table2 = dijkstra2.getPath(src,dst); 
		Path path2 =  dijkstra2.getPath(src,dst); 
		//ArrayList<Link> lpath2 = (ArrayList<Link>)table2.getValueAt(0, 0);
		ArrayList<Link> lpath2 = path2.GetPath();
		joinEdgesSet = joinUnrepeatedEdges(lpath1, lpath2); // 4. Join unrepeated Edges of the lpath1 and lpath2 
		
		System.out.print("\n\nSet of Unrepeated-Edges: {");
	    for (Link link : joinEdgesSet) {
		      System.out.print("{"+link.GetSrcNode().GetId()+","+link.GetDstNode().GetId()+"},");
		}
	    System.out.print("}");
	    
		paths = findDisjointPaths(joinEdgesSet, src, dst);
		return paths;
		
	}
	public void removeEdges (ArrayList<Link> lpath){
		for (Link edge:lpath){
			for (Link link:edges){
				if (edge.equals(link)){
					edges.remove(link);
					break;
				}
			}
		}
		
	}
	public ArrayList<Link> joinUnrepeatedEdges (ArrayList<Link> lpath1, ArrayList<Link> lpath2 ){
		ArrayList<Link> join= new ArrayList<Link>();
		Link linkAux;
		int i=0,k;
		for (Link edge:lpath1){
			for (Link link:lpath2){
				if ((edge.GetSrcNode().GetId()==link.GetSrcNode().GetId()&& edge.GetDstNode().GetId()==link.GetDstNode().GetId())||(edge.GetSrcNode().GetId()==link.GetDstNode().GetId()&& edge.GetDstNode().GetId()==link.GetSrcNode().GetId())){
					i++;
				}
			}
			if (i==0)
				join.add(edge);
			i=0;
		}
		int j=0;
		for (Link link:lpath2){
			for (Link edge:lpath1){
				if ((link.GetSrcNode().GetId()==edge.GetSrcNode().GetId()&& link.GetDstNode().GetId()==edge.GetDstNode().GetId())||(link.GetSrcNode().GetId()==edge.GetDstNode().GetId()&& link.GetDstNode().GetId()==edge.GetSrcNode().GetId())){
					j++;					
				}
			}
			if (j==0)
				join.add(link);
			j=0;
		}
		for (k=1;k<join.size();k++){
			if (join.get(k).GetSrcNode().equals(join.get(0).GetSrcNode())){
				linkAux = join.get(k);
				break;
			}
		}
		join.add(1,join.get(k));
		join.remove(k+1);
		
		return join;
	}
	public ArrayList<Path> findDisjointPaths(ArrayList<Link> links, Node src, Node target){
		long length;
		int i,n;
		Node dst;
		ArrayList<Path> disjointPaths = new ArrayList<Path>();
		ArrayList <Link> pathAux1 = new ArrayList<Link>();
		ArrayList <Link> pathAux2 = new ArrayList<Link>();
		do {
			pathAux1.add(links.get(0));
			dst = links.get(0).GetDstNode();
			length = links.get(0).GetWeight();
			n = links.size();
			for (i=0;i<n;i++){
				if (links.get(i).GetSrcNode().equals(dst)){
					pathAux1.add(links.get(i));
					dst = links.get(i).GetDstNode();
					length += links.get(i).GetWeight();
					links.remove(i);
					n-=1;
					i=0;
				}
			}
			links.remove(0);
			pathAux2 = (ArrayList<Link>)pathAux1.clone();
			Path path = new Path(src, target, pathAux2, length);
			disjointPaths.add(path);
			pathAux1.clear();
		}while ((n-1)!=0);
		//Collections.sort(disjointPaths);
		return disjointPaths;
	}
}
