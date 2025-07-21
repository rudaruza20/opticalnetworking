package dac.cba.simulador;

public class Load implements Comparable <Load> {
	private int id;
	private double numFS;
	private double gap;
	public Load(int id, double load, double gap) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.numFS = load;
		this.gap = gap;
	}
	public int getId(){
		return this.id;
	}
	public double getnumFS(){
		return this.numFS;
	}
	public double getGap (){
		return this.gap;
	}
	public int compareTo(Load load){
		if (this.numFS > load.numFS){
			return 1;
		}else if (this.numFS < load.numFS){
			return -1;
		}
		else{
			return 0;
		}
	}
}