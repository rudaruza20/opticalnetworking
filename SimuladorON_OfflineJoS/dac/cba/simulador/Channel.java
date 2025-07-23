package dac.cba.simulador;

public class Channel {
	private int numFS;
	private int startFS;
	private int endFS;
	private int mimo; //for the case of MCF: "1" lightpath with MIMO "0" lightpath without MIMO
	public Channel(int numFS, int startFS, int endFS) {
		// TODO Auto-generated constructor stub
		this.numFS= numFS;
		this.startFS= startFS;
		this.endFS = endFS;
	}
	public int getstartFS (){
		return startFS;
	}
	public int getendFS (){
		return endFS;
	}
	public int getMimoStatus (){
		return mimo;
	}
	public void setMiMoStatus (int state){
		mimo = state;
	}
	public boolean containsSlot (int s){
		if (s>=this.getstartFS() && s<=this.getendFS()){
			return true;
		}
		else {
			return false;
		}
	}
}
