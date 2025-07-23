package dac.cba.simulador;

public class Demand implements Comparable <Demand>{

	private int identifier;
	private Node source, destination;
	private double BitRate;
	private double weight;
	public Demand() {
		// TODO Auto-generated constructor stub
	}
	public Demand(int id, Node src, Node dst, Double bitrate) {
		// TODO Auto-generated constructor stub
		this.identifier=id;
		this.source=src;
		this.destination=dst;
		this.BitRate = bitrate;
		//this.weight = bitrate; /*Initialized weight value to BitRate value*/
	}
	public Demand(int id, Node src, Node dst, Double bitrate, double w) {
		// TODO Auto-generated constructor stub
		this.identifier=id;
		this.source=src;
		this.destination=dst;
		this.BitRate = bitrate;
		this.weight = w;
	}
	public int GetId (){
		return identifier;
	}
	public Node GetSrcNode (){
		return source;
	}
	public Node GetDstNode (){
		return destination;
	}
	public double GetBitRate (){
		return BitRate;
	}
	public double GetWeigth (){
		return weight;
	}
	public void SetWeigth (double w){
		this.weight = w;
	}
	// Overriding the compareTo method
	
	public int compareTo(Demand demand){
		if (this.weight < demand.weight){
			return 1;
		}else if (this.weight > demand.weight){
			return -1;
		}
		else{
			return 0;
		}
	}
	/*
	public int compareTo(Demand demand){
		if (this.source.GetId() > demand.source.GetId()){
			return 1;
		}else if (this.source.GetId() < demand.source.GetId()){
			return -1;
		}
		else{
			return 0;
		}
	}
	*/
}
