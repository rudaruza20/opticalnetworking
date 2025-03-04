package dac.cba.simulador;

import java.util.ArrayList;

public class Link {
	private Node source;
	private Node destination;
	//private ArrayList<FrequencySlot> fSlots = new ArrayList<FrequencySlot>(960) ; //For RSA Purposes. 320 for FS width =  12.5Ghz
	private long weight; //  In case of negative weights
	private int nCores; 
	private ArrayList<Core> cores = new ArrayList<Core>();
	private int Mod12Ghz,Mod25GHz, Mod37GHz, Mod50GHz; 
	public Link(Node srcnode, Node dstnode, long w, int nCores, int F) {
		// TODO Auto-generated constructor stub
		this.source= srcnode;
		this.destination= dstnode;
		this.weight = w;
		this.nCores= nCores;
		for (int i=0;i<nCores;i++)
			cores.add(new Core(i, F));
		this.Mod12Ghz=0;
		this.Mod25GHz=0;
		this.Mod37GHz=0;
	}
	public Node GetSrcNode (){	
		return source;
	}

	public Node GetDstNode (){
		return destination;
	}
	public long GetWeight (){
		return weight;
	}
	
	public ArrayList<Core> getCores (){
		return cores ;
	}
	public int getNumberOfCores(){
		return nCores;
	}
	/*
	public void addWavelength (Wavelength w){
		this.wavelengths.add(w);
	}
	*/
	
	/*
	@Override
	
	public int compareTo(Link link){
		if (this.wavelengths.size() > link.wavelengths.size()){
			return 1;
		}else if (this.wavelengths.size() < link.wavelengths.size()){
			return -1;
		}
		else{
			return 0;
		}
	}
	*/
	
}
