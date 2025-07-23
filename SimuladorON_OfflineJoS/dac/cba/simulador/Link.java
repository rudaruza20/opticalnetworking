package dac.cba.simulador;

import java.util.ArrayList;

public class Link {
	private Node source;
	private Node destination;
	private ArrayList<FrequencySlot> fSlots = new ArrayList<FrequencySlot>(320) ; //For RSA Purposes. 320 for FS width =  12.5Ghz
	private long weight; //  In case of negative weights
	public Link(Node srcnode, Node dstnode, long w) {
		// TODO Auto-generated constructor stub
		this.source= srcnode;
		this.destination= dstnode;
		this.weight = w;
		for (int i=0;i<320;i++)
			fSlots.add(new FrequencySlot(i));
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
	
	public ArrayList<FrequencySlot> getFSs (){
		return fSlots ;
	}
	
	public int getNumberOfFsInUse (){
		int k=0;
		for (FrequencySlot s:this.fSlots){
			if (!s.getState()){
				k++;
			}
		}
			
		return k;
	}
	/*
	public void addWavelength (Wavelength w){
		this.wavelengths.add(w);
	}
	*/
	public void setOccupationOfFs (int index, boolean b){
		fSlots.get(index).setOccupation(b);
	}
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
