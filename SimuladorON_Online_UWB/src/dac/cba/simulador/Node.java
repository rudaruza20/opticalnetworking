package dac.cba.simulador;

import java.util.ArrayList;
import java.util.TreeMap;

public class Node {
	private int identifier;
	private ArrayList<Link> outLinks;
	private ArrayList<Link> inLinks;
	private int numberMod; //Single type of Modulator (Obselete?)
	private int peakNumberMod; //generic BV-TXP ?? (Obselete?)
	private int maxActiveTrx; // Max Number of Active TRX per node
	private int numberOfActiveTrx; // Actual Number of Active TRX per node 
	private int numberOfActiveBVT; // Actual Number of Active BVT per node
	private TreeMap mapa_active_bvt; //To obtain CDF of the active bvt per node. bvt: set of tx/rx. One Tx/Rx has one or more modulators/coherent detectors, e.g. one per polarization

	public Node(int id, ArrayList<Link> outlink, ArrayList<Link> inlink) {
		// TODO Auto-generated constructor stub
		this.identifier= id;
		this.outLinks=outlink;
		this.inLinks=inlink;
		this.numberMod=0;
		this.peakNumberMod=0; 
		this.maxActiveTrx=0;
		this.numberOfActiveTrx=0;
		this.numberOfActiveBVT=0;
		mapa_active_bvt = new TreeMap();
		
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
	public int getNumberMod (){
		return this.numberMod;
	}
	public int getPeakNumberMod (){
		return this.peakNumberMod;
	}
	public int getPeakNumberOfActiveTrx(){
		return this.maxActiveTrx;
	}
	public int getNumberOfActiveTrx(){
		return this.numberOfActiveTrx;
	}
	public int getNumberOfActiveBVT(){
		return this.numberOfActiveBVT;
	}
	public TreeMap getMapOfActiveNumberOfBVT (){
		return mapa_active_bvt;
	}
	public void SetOutlink (Link link){
		outLinks.add(link);
	}
	public void SetInlink (Link link){
		inLinks.add(link);
	}
	public void setNumberMod (int num){
		this.numberMod = num;
	}
	public void setIncrementNumberOfActiveTrx(int num){
		this.numberOfActiveTrx+=num;
	}
	public void setIncrementNumberOfActiveBVT(int num){
		this.numberOfActiveBVT+=num;
	}
	public void setNumberOfActiveTrx(int num){
		this.numberOfActiveTrx=num;
	}
	public void setNumberOfActiveBVT(int num){
		this.numberOfActiveBVT=num;
	}
	public void setPeakNumberOfActiveTrx(int num){
		this.maxActiveTrx=num;
	}
	public void setPeakNumberMod (int number){
		this.peakNumberMod=number;
	}
	
}
