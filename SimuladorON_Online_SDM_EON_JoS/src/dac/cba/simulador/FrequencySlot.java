package dac.cba.simulador;

public class FrequencySlot implements Comparable <FrequencySlot> {
		private int identifier;
		private boolean free;

		public FrequencySlot(int id) {
			// TODO Auto-generated constructor stub
			this.identifier= id;
			this.free=true;
		}
		public int getId (){
			return identifier;
		}
		public boolean getState(){
			return this.free;
		}
		public void setOccupation (boolean b){
			this.free = b;
		}

		@Override
		public int compareTo(FrequencySlot fs) {
			if (this.identifier > fs.identifier){
				return 1;
			}else if (this.identifier < fs.identifier){
				return -1;
			}
			else{
				return 0;
			}
		}

}
