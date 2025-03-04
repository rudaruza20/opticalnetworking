package dac.cba.simulador;
import java.util.ArrayList;

public class Core implements Comparable <Core>{
	private int id;
	private ArrayList<FrequencySlot> fSlots = new ArrayList<FrequencySlot>(320) ; //For RSA Purposes. 320 for FS width =  12.5Ghz
	private int load; //For sorting purposes in order to find out least loaded cores. Also, -numOfOccupiedFSs- to identify the number of FSs allocated in a link l
	public Core(int id, int F) {
		// TODO Auto-generated constructor stub
		this.id =  id;
		for (int i=0;i<F; i++)
			fSlots.add(new FrequencySlot(i));
	}
	public ArrayList<FrequencySlot> getFSs (){
		return fSlots ;
	}
	public int getId(){
		return id; 
	}
	
	public int getNumberOfFsInUse (){
		int k=0;
		for (FrequencySlot s:this.fSlots)
			if (!s.getState()){
				k++;
			}
		
		this.load=k;
		return k;
	}
	public void setOccupationOfFs (int index, boolean b){
		fSlots.get(index).setOccupation(b);
	}
	
	public int compareTo(Core core){
		if (this.load > core.load){ //> increasing
			return 1;
		}else if (this.load < core.load){
			return -1;
		}
		else{
			return 0;
		}
	}

}
