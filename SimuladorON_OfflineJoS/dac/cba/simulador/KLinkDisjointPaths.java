package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class KLinkDisjointPaths {
	private final List<Node> nodes;
	private final List<Link> edges;
	
	public KLinkDisjointPaths(Network graph) {
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
	public Link searchLink (Node src, Node dst){
		Link link=null;
		for (Link l:this.edges){
			if (l.GetSrcNode().equals(src)&&l.GetDstNode().equals(dst)){
				link=l;
			}
		}
		return link;
	}
	public ArrayList<Path> execute (Node src, Node target, int k){
		ArrayList<Link> lpath;
		ArrayList<Path> paths = new ArrayList<Path>();
		
		for (int i=0;i<k;i++){
			DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(this);
			dijkstra.execute(src); 
			//DefaultTableModel table = dijkstra.getPath(src, target);
			Path path =  dijkstra.getPath(src, target);
			if (path!=null){
				//Path path=(Path)table.getValueAt(0, 1);
				paths.add(path);
				//lpath = (ArrayList<Link>)table.getValueAt(0, 0);
				lpath = path.GetPath();
				removeEdges(lpath, this);	
			} else break;
		}
		return paths;
		
	}
	public void removeEdges (ArrayList<Link> lpath, KLinkDisjointPaths graph){
		for (Link edge:lpath){
			Link flink=graph.searchLink(edge.GetSrcNode(), edge.GetDstNode());
			Link rlink = graph.searchLink(edge.GetDstNode(), edge.GetSrcNode());//Links are bidirectional
			edges.remove(flink);
			edges.remove(rlink);
		}
		
	}
}
		

	
	
