package dac.cba.simulador;

import java.util.*;

public class Path implements Comparable <Path> {
	//Lighpath: set of nodes, links and frequency slots. Can be used to multiple demands in case of group sharing
	private ArrayList<Integer> id = new ArrayList<Integer>(); //These ids are all demands served with the same light-path, and then after holding-time we can release the cores&spectrum of demand id 
	private Node src, dst;
	private ArrayList<Link> route = new ArrayList<Link>();
	private int hops;
	private Double length;
	private int numFS; //for FlexGrid, numFS required
	private int SE; //(Ideal) Spectral Efficiency
	private ArrayList<FrequencySlot> freqSlots = new ArrayList<FrequencySlot>(); //established Spectrum Path (i) uses these Frequency Slots. Only it gives the indexes of the properly FS.
	private HashMap fsMap = new HashMap(); //(K, V) HashMap (demand id, freqSlots) // 15062022
	private ArrayList<Channel> channels = new ArrayList<Channel>(); // Implements concept of channel (group of frequency slices)
	private ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>(route.size());// For SDM, array of cores used in LightPath. Only it gives the indexes of the properly cores.
	private int numCores; //For SDM, total number of the cores occupied in the group G=|C| --> Joint Switching  // Revisar tipo Double or Int
	private double bw_mod; //Lightpath:  bw occupied by the optical modulator. Equivalent to the baud rate (Electrical signal)
	private int n_OC; //number of OCs per Spe-SCh in the case of fixed baudrate

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
	public int getSpectralEfficiency (){
		return SE;
	}
	public ArrayList<FrequencySlot> getFreqSlots (){
		return freqSlots;
	}
	//new 09062022
	public ArrayList<FrequencySlot> getFreqSlotsByDemand (Demand d){
		//System.out.println("FS array size: "+((ArrayList<FrequencySlot>)fsMap.get(d.getId())).size());
		return (ArrayList<FrequencySlot>)(fsMap.get(d.getId()));  //Check
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
	//new 08022025. Return list of nodes of Path i
	public ArrayList<Integer> getNodes(){
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(this.GetPath().get(0).GetSrcNode().GetId());
		for (Link l:this.GetPath()) {
			list.add(l.GetDstNode().GetId());
		}
		//list.contains(1);
		return list;
	}
	//end
	public void setId (int id){
		this.id.add(id);
	}
	public void setLength (double l){
		this.length = l;
	}
	public void setnumFS (int nFS){
		this.numFS = nFS;
	}
	public void setSpectralEfficiency (int SE){
		this.SE = SE;
	}
	public void setFrequencySlot (FrequencySlot fs){
		this.freqSlots.add(fs);
	}
	//new 15062022: Map demand and occupied frequency slots in each lightpath for realising purposes 
	public void addFrequencySlot (Demand d, ArrayList<FrequencySlot> slots){
		this.fsMap.put(d.getId(), slots);
	}
	public void setCores (ArrayList<Core> core){
		this.cores.add(core);
	}
	public void setChannels (Channel channel){
		this.channels.add(channel);
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
	//new 09062022
	public void removeId (int index, int id){
		// remove demand id and (K, V) from HashMap (demand id, freqSlots)
		this.id.remove(index);
		this.fsMap.remove(id);
		//System.out.println("Capacity: "+this.fsMap.size());
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
