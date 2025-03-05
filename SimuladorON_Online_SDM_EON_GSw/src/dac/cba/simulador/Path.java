package dac.cba.simulador;

import java.util.ArrayList;

public class Path implements Comparable <Path> {
	//Lighpath: set of nodes, links and frequency slots. Can be used to multiple demands in case of group sharing
	private ArrayList<Integer> id = new ArrayList<Integer>(); //For Dynamic FlexGrid purposes, these id are all demands served with the same light-path, and then after holding-time we can release the cores&spectrum of demand id 
	private Node src, dst;
	private ArrayList<Link> route = new ArrayList<Link>();
	private int hops;
	private Double length;
	private int numFS; //for FlexGrid, numFS required
	private int codModFormat; //This is the spectral efficiency. Pending update name
	private ArrayList<FrequencySlot> freqSlots = new ArrayList<FrequencySlot>(); // FlexGrid:  established Spectrum Path (i) uses these Frequency Slots. Only it gives the indexes of the properly FS.  
	private ArrayList<Channel> channels = new ArrayList<Channel>(); // Implements concept of channel (group of frequency slices)
	private ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>(route.size());// For SDM, array of cores used in LightPath. Only it gives the indexes of the properly cores.
	private int numCores; //For SDM, total number of the cores occupied in the group G=|C| --> Joint Switching  // Revisar tipo Double or Int
	private double bw_mod; //Lightpath:  bw occupied by the optical modulator. Equivalent to the baud rate (Electrical signal)
	private int n_OC; //number of OCs per spatial channel 

	public Path() {
		// TODO Auto-generated constructor stub
	}
	public Path(Node src, Node dst, ArrayList<Link> LinkList, int h) {
		// TODO Auto-generated constructor stub
		this.src=src;
		this.dst=dst;
		this.route = LinkList;
		this.hops = h;
		Integer hops = new Integer (h); 
		this.length= hops.doubleValue(); //Initialized length value to number of hops
	}
	public Path(Node src, Node dst, ArrayList<Link> LinkList, double length) {
		// TODO Auto-generated constructor stub
		this.src=src;
		this.dst=dst;
		this.route = LinkList;
		this.length= length;
	}
	public Path(Node src, Node dst, ArrayList<Link> LinkList, int h, double length) {
		// TODO Auto-generated constructor stub
		this.src=src;
		this.dst=dst;
		this.route = LinkList;
		this.hops = h;
		this.length= length;
	}
	
	public ArrayList<Integer> getArrayId(){
		return id;
	}
	public ArrayList<Link> GetPath (){
		return route;
	}
	public Node getSrcNode (){
		return src;
	}
	public Node getDstNode (){
		return dst;
	}
	public int getHops (){
		return hops;
	}
	public double getLength (){
		return length;
	}
	public int getnumFS (){
		return numFS;
	}
	public int getcodModFormat (){
		return codModFormat;
	}
	public ArrayList<FrequencySlot> getFreqSlots (){
		return freqSlots;
	}
	public ArrayList<Channel> getChannels(){
		return channels;
	}
	public ArrayList<ArrayList<Core>> getCores(){
		return cores;
	}
	public int getNumberOfSetOfChannels(){
		return channels.size();
	}
	public int getNumberOfCores(){
		return numCores;
	}
	public double getBwMod(){
		return bw_mod;
	}
	public int getOCs(){
		return n_OC;
	}
	public void setId (int id){
		this.id.add(id);
	}
	public void setLength (double l){
		length = l;
	}
	public void setnumFS (int nFS){
		numFS = nFS;
	}
	public void setcodModFormat (int code){
		codModFormat = code;
	}
	public void setFrequencySlot (FrequencySlot fs){
		freqSlots.add(fs);
	}
	public void setCores (ArrayList<Core> core){
		cores.add(core);
	}
	public void setChannels (Channel channel){
		channels.add(channel);
	}
	public void setnumCores (int number){
		this.numCores = number;
	}
	public void setBwMod (double bwmod){
		this.bw_mod = bwmod;
	}
	public void setOCs (int n_OC){
		this.n_OC = n_OC;
	}
	public void addCores (int numCores){
		this.numCores+=numCores;
	}
	@Override
	
	public int compareTo(Path path){
		if (this.length > path.length){ //> ascending
			return 1;
		}else if (this.length < path.length){
			return -1;
		}
		else{
			return 0;
		}
	}
	
	/*
	public int compareTo(Path path){
		if (this.numFS < path.numFS){
			return 1;
		}else if (this.numFS > path.numFS){
			return -1;
		}
		else{
			return 0;
		}
	}
	*/
}
