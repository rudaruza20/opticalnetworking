/**
 * 
 */
package dac.cba.simulador;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;


/**
 * @author rudaruza
 *
 */
public class Network {

	private ArrayList<Node> nodes;
	private ArrayList<Link> links;
	private DefaultTableModel paths;
	private ArrayList<Channel> channels =  new ArrayList<Channel>(); //To obtain Array of complete set of Channels for all demands  
	
	public Network (){
		//
		this.nodes = new ArrayList<Node>();
		this.links = new ArrayList<Link>();
	}
	
	public Network(Node node, Link link) {
		// TODO Auto-generated constructor stub
		this.nodes.add(node);
		this.links.add(link);
	}
	
	public Network(Node node) {
		// TODO Auto-generated constructor stub
		this.nodes.add(node);	
	}
	
	public Network(Link link) {
		// TODO Auto-generated constructor stub
		this.links.add(link);
	}
	
	public void AddNode (Node node){
		nodes.add(node);	
	}
	
	public void AddLink (Link link){
		links.add(link);
	}
	
	public void CreatePathsTable (){
		paths = new DefaultTableModel(this.GetNumberOfNodes(),this.GetNumberOfNodes());
	}
	
	public void AddListOfPaths (int idsrc, int iddst, ArrayList<Path> PathList, int index, int capacity){
		ArrayList<ArrayList<Path>> pathListsf;
		//ArrayList<ArrayList<Path>> pathListsr;
		if (paths.getValueAt(idsrc, iddst)!=null){
			pathListsf = (ArrayList<ArrayList<Path>>)paths.getValueAt(idsrc, iddst);
			//pathListsr= (ArrayList<ArrayList<Path>>)paths.getValueAt(idsrc, iddst);
		}
		else{
			pathListsf = new ArrayList<ArrayList<Path>>(capacity);
			//pathListsr = new ArrayList<ArrayList<Path>>(capacity);
			for (int i=0;i<capacity;i++){
				pathListsf.add(null);
				//pathListsr.add(null);
			}
				
		}
		if (pathListsf.get(index)==null){
			pathListsf.set(index,PathList);
			//pathListsr.set(index,PathList);
			paths.setValueAt(pathListsf, idsrc, iddst);
			//paths.setValueAt(pathListsr, iddst, idsrc);// same distinct paths in forward and reverse direction - Bidirectional links -
		}
	}
	
	public Node GetNode (int index){
		Node node = nodes.get(index);
		return node;
	}
	public Link GetLink (int index){
		Link link = links.get(index);
		return link;	
	}
	public int GetNumberOfNodes (){
		return nodes.size();
	}
	
	public int GetNumberOfLinks (){
		return links.size();
	}
	public int getIndexOfLink(Link link){
		return links.indexOf(link);
	}
	public ArrayList<Node> getNodes (){
		return nodes;
	}
	public ArrayList<Link> getLinks (){
		return links;
	}
	public ArrayList<Path> GetPaths(int idsrc, int iddst, int index){
		ArrayList<ArrayList<Path>> pathLists;
		if (paths.getValueAt(idsrc,iddst)!=null){
			pathLists = (ArrayList<ArrayList<Path>>)paths.getValueAt(idsrc,iddst);
			return pathLists.get(index);
		}else{
			pathLists = new ArrayList<ArrayList<Path>>();
			return null;
		}
	}
	public ArrayList<Channel> getChannels(){
		return channels;
	}
	//For implement concept of channels 
	public int getNumberOfChannels(){
		return channels.size();
	}
	
	public Link searchLink (Node src, Node dst){
		Link link=null;
		for (Link l:this.links){
			if (l.GetSrcNode().equals(src)&&l.GetDstNode().equals(dst)){
				link=l;
			}
		}
		return link;
	}
	public void setChannels (Channel channel){
		channels.add(channel);
	}
	public void joinChannels (){
		
		int n=channels.size();
		for (int i=0; i<n;i++){
			for (int j=i+1; j<n;j++){
				if ((channels.get(i).getstartFS()==channels.get(j).getstartFS())&&(channels.get(i).getendFS()==channels.get(j).getendFS())){
					channels.remove(j);
					j--;
					n = channels.size();
				}
			}
		}
	}
}
