/**
 * 
 */
package dac.cba.simulador;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.table.DefaultTableModel;


/**
 * @author rudaruza
 *
 */
public class Network {

	private ArrayList<Node> nodes;
	private ArrayList<Link> links;
	private DefaultTableModel paths;
	//private DefaultTableModel lightPaths; /*for FlexGrid*/
	private ArrayList<Path> lightPaths = new ArrayList<Path>(); 
	private ArrayList<Channel> channels =  new ArrayList<Channel>(); // for implement concept of channels - pending of erasing - 
	private Integer active_trx_counter;
	private Integer active_bvtxp_counter;
	private TreeMap mapa_active_trx; //To obtain CDF of the active transceivers in each connection event. Transceivers: tx/rx and it has one or more modulators, e.g. one per polarization
	private TreeMap mapa_active_bvtxp; //To obtain CDF of the active bvtxp in each connection event. bvtxp: Spe or Spa, a set of trxs and laser(s)
	long instantaneusCarriedBitRate, cummulativeCarriedBitRate;
	int measuredTimes;
	int maxUniqueDemandId;
	
	public Network (){
		//
		this.nodes = new ArrayList<Node>();
		this.links = new ArrayList<Link>();
		this.active_trx_counter = 0;
		this.active_bvtxp_counter = 0;
		this.mapa_active_trx = new TreeMap();
		this.mapa_active_bvtxp = new TreeMap();
		this.instantaneusCarriedBitRate=0;
		this.cummulativeCarriedBitRate=0;
		this.measuredTimes=0;
		this.maxUniqueDemandId=0;
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
	
	/*
	public void CreateLpathsTable (){
		lightPaths = new DefaultTableModel(this.GetNumberOfNodes(),this.GetNumberOfNodes());
		for (int i=0; i<lightPaths.getRowCount();i++){
			for (int j=0; j<lightPaths.getColumnCount();j++){
				lightPaths.setValueAt(new ArrayList<Path>(), i, j);
			}
		}
	}
	*/
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
	/*
	public void addLightPath (int idsrc, int iddst, Path lightPath){
		ArrayList<Path> flightpaths = (ArrayList<Path>)lightPaths.getValueAt(idsrc, iddst);
		flightpaths.add(lightPath);
		lightPaths.setValueAt(flightpaths, idsrc, iddst);
	}
	*/
	public void addLightPath (Path lightPath){
		lightPaths.add(lightPath);
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
	public ArrayList<Path> getLightPaths(){
		return lightPaths;
	}
	public ArrayList<ArrayList<Path>> getLightPathsWithResources(Demand d){
		//For JoS with spatial grooming
		ArrayList<Path> pf = new ArrayList<Path> (); //forward lightpaths for demand d with available spatial resources
		ArrayList<Path> pr = new ArrayList<Path> (); //reverse lightpaths for demand d with available spatial resources
		ArrayList<ArrayList<Path>> list = new ArrayList<ArrayList<Path>> ();
		int idsrc, iddst;
		for (Path p:lightPaths){
			idsrc=d.getSrcNode().GetId();
			iddst=d.getDstNode().GetId();
			if (p.getSrcNode().GetId()==idsrc && p.getDstNode().GetId()==iddst && p.getNumberOfCores()<links.get(0).getNumberOfCores())
				pf.add(p);
			if (p.getSrcNode().GetId()==iddst && p.getDstNode().GetId()==idsrc && p.getNumberOfCores()<links.get(0).getNumberOfCores())
				pr.add(p);
		}
		
		list.add(pf); 
		list.add(pr);
		
		return list;
	}
	public Link searchLink (Node src, Node dst){
		Link link=null;
		int idsrc = src.GetId();
		int iddst = dst.GetId();
		for (Link l:this.links){
			if (l.GetSrcNode().GetId()==idsrc&&l.GetDstNode().GetId()==iddst){
				link=l;
				break;
			}
		}
		return link;
	}
	//For implement concept of channels 
	public ArrayList<Channel> getChannels(){
		return channels;
	}
	public int getNumberOfChannels(){
		return channels.size();
	}
	public Integer getNumberOfActiveBVTXP (){
		return active_bvtxp_counter;
	}
	public TreeMap getMapOfActiveNumberOfBVTXP (){
		return mapa_active_bvtxp;
	}
	public Integer getNumberOfActiveTransceivers (){
		return active_trx_counter;
	}
	public TreeMap getMapOfActiveNumberOfTRX (){
		return mapa_active_trx;
	}
	public void setNumberOfActiveTransceivers (Integer num){
		 this.active_trx_counter = num;;
	}
	public void setNumberOfActiveBVTXP (Integer num){
		 this.active_bvtxp_counter = num;;
	}
	public void setChannels (Channel channel){
		channels.add(channel);
	}
	public void joinChannels (){
		//Erasing duplicate elements
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
