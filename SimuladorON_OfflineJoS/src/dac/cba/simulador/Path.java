package dac.cba.simulador;

import java.util.ArrayList;

public class Path implements Comparable <Path> {
	private Node src, dst;
	private ArrayList<Link> route = new ArrayList<Link>();
	private int hops;
	private Double length;
	private int numFS; //for FlexGrid, numFS required
	private ArrayList<FrequencySlot> freqSlots = new ArrayList<FrequencySlot>(); // FlexGrid:  established Spectrum Path (i) uses these Frequency Slots.
	private ArrayList<Channel> channels = new ArrayList<Channel>(); // Implements concept of channel (group of frequency slices)
	private int d_id; //In the case of lightpath.- This id identify the demand id associated to. 
	private double bitRate; //In the case of lightpath.- This identifies the capacity of the lp.
	private int mimo; //for the case of MCF: "1" lightpath with MIMO "0" lightpath without MIMO

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
	public ArrayList<FrequencySlot> getFreqSlots (){
		return freqSlots;
	}
	public ArrayList<Channel> getChannels(){
		return channels;
	}
	public int getNumberOfSetOfChannels(){
		return channels.size();
	}
	public int getDemandId(){
		return d_id;
	}
	public double getBitRate(){
		return bitRate;
	}
	public int getMimoStatus (){
		return mimo;
	} 
	public void setLength (double l){
		length = l;
	}
	public void setnumFS (int nFS){
		numFS = nFS;
	}
	public void setFrequencySlot (FrequencySlot fs){
		freqSlots.add(fs);
	}
	public void setArrayFrequencySlots (ArrayList<FrequencySlot> FSs){
		freqSlots.clear();
		freqSlots = FSs;
	}
	public void setChannels (Channel channel){
		channels.add(channel);
	}
	public void setDemandId(int id){
		d_id = id;
	}
	public void setbitRate(double capacity){
		bitRate = capacity;
	}
	public void setMiMoStatus (int state){
		mimo = state;
	}
	
	@Override
	
	public int compareTo(Path path){
		if (this.length > path.length){ //> increasing
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
