package dac.cba.simulador;

public class Load implements Comparable <Load> {
	private int id;
	private double weigth;
	public Load(int id, double load) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.weigth = load;
	}
	public int getId(){
		return this.id;
	}
	public double getWeigth(){
		return this.weigth;
	}
	public int compareTo(Load load){
		if (this.weigth > load.weigth){
			return 1;
		}else if (this.weigth < load.weigth){
			return -1;
		}
		else{
			return 0;
		}
	}
}
