package dac.cba.simulador;

public class Channel {
	private int numFS;
	private int startFS;
	private int endFS;
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

}
