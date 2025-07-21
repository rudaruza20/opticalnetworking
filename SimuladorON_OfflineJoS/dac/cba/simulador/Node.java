package dac.cba.simulador;

import java.util.ArrayList;

public class Node {
	private int identifier;
	private ArrayList<Link> outLinks;
	private ArrayList<Link> inLinks;

	public Node(int id, ArrayList<Link> outlink, ArrayList<Link> inlink) {
		// TODO Auto-generated constructor stub
		this.identifier= id;
		this.outLinks=outlink;
		this.inLinks=inlink;
	}
	
	public int GetId (){
		return identifier;
	}
	public int GetSizeOutLinks (){
		return outLinks.size();
	}
	
	public int GetSizeInLinks (){
		return inLinks.size();
	}
	public ArrayList<Link> GetOutLinks(){
		return outLinks;	
	}
	
	public ArrayList<Link> GetInLinks(){
		return inLinks;	
	}
	public Link GetLinkToNeighbor (Node endpoint){
		int i;
		int n = outLinks.size();
		for (i=0;i<n;i++){
			if (outLinks.get(i).GetDstNode().equals(endpoint)){
				break;
			}
		}
		return outLinks.get(i);
	}
	public void SetOutlink (Link link){
		outLinks.add(link);
	}
	
	public void SetInlink (Link link){
		inLinks.add(link);
	}
}
