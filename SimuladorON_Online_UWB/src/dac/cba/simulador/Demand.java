package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Demand {
	private static AtomicInteger uniqueId=new AtomicInteger(0);
	private int id;
	private Node srcnode, dstnode;
	private int nc; //Number of Cores used to be serve demand d ---useful for connection release 
	private int n_OC; //Number of Optical Carriers per spatial channel employed to be serve demand d ---useful for connection release
	private double bitRate;
	private double bw; // analogue BW in GHz (OJO not used) 25062022
	private double weight;
	
	public Demand (Network net, ArrayList<Double> intervals, ArrayList<Integer> d_r, double baudrate, String t_baudrate){
		//d_r: demand requirement. It can be bit-rate or number of sub-channels 
		this.id=uniqueId.getAndIncrement();
		double probability;
		int src, dst;
		Random randomsrc = new Random();
		Random randomdst = new Random();
		Random randomprb = new Random();
		probability = randomprb.nextDouble();
		src = randomsrc.nextInt(net.GetNumberOfNodes());
		do{
			dst=randomdst.nextInt(net.GetNumberOfNodes());			
		}while (src == dst);
		this.srcnode = net.GetNode(src);
		this.dstnode = net.GetNode(dst);
		if (t_baudrate=="fixed"){
			for (int i=0;i<d_r.size();i++){
				if (probability <= intervals.get(i)){
					this.bitRate = d_r.get(i); //11022025. Bit-rate in terms of bps
					//this.n_OC = d_r.get(i); 09062022. Bit-rate in terms of number of OCs (other traffic profile)
					//this.bw = d_r.get(i)*baudrate; // 09062022. Bit-rate in terms of number of OCs (other traffic profile)
					break;
				}
			}
		}
		else{
			for (int i=0;i<d_r.size();i++){
				if (probability <= intervals.get(i)){
					this.bitRate =d_r.get(i);
					break;
				}
			}
		}
	}
	public Demand (int id, int srcid, int dstid, double bitrate, Network net){
		this.id=id;
		this.srcnode = net.GetNode(srcid);
		this.dstnode = net.GetNode(dstid);
		this.bitRate = bitrate;
	}
	public Demand (int srcid, int dstid, int n_OC, Network net, double baudrate){
		this.id=uniqueId.getAndIncrement();
		this.srcnode = net.GetNode(srcid);
		this.dstnode = net.GetNode(dstid);
		this.n_OC = n_OC;
		this.bw = n_OC*baudrate;

	}
	public int getId (){
		return id;
	}
	
	public Node getSrcNode (){
		return srcnode;
	}
	public Node getDstNode (){
		return dstnode;
	}
	public double getBitRate (){
		return bitRate;
	}
	public double getWeigth (){
		return weight;
	}
	public int getnumCores(){
		return nc;
	}
	public int getOCs(){
		return n_OC;
	}
	public double getBw(){
		return bw;
	}
	public int getNextId(){
		return uniqueId.getAndIncrement();
	}
	public int getCurrentId(){
		return uniqueId.get();
	}
	public void setBitRate (double bitRate){
		this.bitRate = bitRate;
	}
	public void setBw (double bw){
		this.bw = bw;
	}
	public void setWeigth (double w){
		this.weight = w;
	}
	public void setNc (int numCores){
		this.nc=numCores;
	}
	public void setOCs (int n_OC){
		this.n_OC=n_OC;
	}
	public void setUniqueId (int number){
		uniqueId = new AtomicInteger(number);
	}
}
