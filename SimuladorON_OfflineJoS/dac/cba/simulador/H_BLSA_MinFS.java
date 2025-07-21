
/*
 * BLSA (Balanced Load Spectrum Allocation) Algorithm implementation suggested in A Study of the Routing and Spectrum Allocation in
Spectrum-sliced Elastic Optical Path Networks, Yang Wang, et al.
 */


package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;


import javax.swing.table.DefaultTableModel;

public class H_BLSA_MinFS {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	public H_BLSA_MinFS() {
	    // create a copy of the array so that we can operate on this array
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
	}
	public void execute (Network net, ArrayList<Demand> demands, DefaultTableModel formats, int F){
		ArrayList<Path> spPaths = new ArrayList<Path> (); //Array of Shortest-Path corresponding one to each demand
		ArrayList<Path> lightPaths = new ArrayList<Path>();
		String st = null;
		for (Demand d:demands){
			DijkstraAlgorithm dijkstra = new DijkstraAlgorithm (net);
			dijkstra.execute(d.GetSrcNode());
			Path path = dijkstra.getPath(d.GetSrcNode(), d.GetDstNode());
			double bitRate = d.GetBitRate();
			int col = formats.findColumn(Double.toString(bitRate));
			for (int row=1;row<formats.getRowCount();row++){
				if (path.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&path.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					st = (String)formats.getValueAt(row, col);
					path.setnumFS(Integer.parseInt(st));
				}
			}
			//Label each demand with the required numFS for the shortest-path
			d.SetWeigth(Double.parseDouble(st));
			spPaths.add(path);
		}
		//sort Path by numFS.- ver como implementar dos comparable methods para un mismo objeto/clase
		//Collections.sort(spPaths);
		int i=0;
		for (Path p:spPaths){
			System.out.println("Spectrum Path Request "+i+": from "+p.getSrcNode().GetId()+" to "+p.getDstNode().GetId()+" with "+p.getLength()+"Km and "+p.getnumFS()+ " required Slots using SPD");
			i++;
		}
		//Sort the demands in descending order by numFS
		Collections.sort(demands);
		System.out.println("\nSort demands in descending order by numFS required executed according to the SPD:");
		for (Demand d:demands){
			System.out.println("\tDemand "+d.GetId()+": from "+d.GetSrcNode().GetId()+" to "+d.GetDstNode().GetId()+" with "+d.GetWeigth()+" required Slots");
		}
		for (Demand d:demands){
			int idx = formats.findColumn(Double.toString(d.GetBitRate()));
			ArrayList<Path> allshortestPathf =  net.GetPaths(d.GetSrcNode().GetId(), d.GetDstNode().GetId(), idx-2);
			//k=3, 3 primeros(mejores) caminos
			ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPathf.clone();
			
			
			int size= allshortestPathf.size();
			if (size>3){
				for (int index=3;index<size;index++)
					kshortestPath.remove(3);
			}
			
			ArrayList<Load> loadpd = new ArrayList<Load>(kshortestPath.size());
			int pos=0;
			for (Path p:kshortestPath){
				//System.out.print("Path id "+pos+" {"+p.getSrcNode().GetId());
				int maxActiveSlots = 0; //number of active slots in link e with most load in path d
				for (Link e:p.GetPath()){
					if (e.getNumberOfFsInUse()>maxActiveSlots){
						maxActiveSlots = e.getNumberOfFsInUse();  
					}
				}
				loadpd.add(new Load(pos, maxActiveSlots+p.getnumFS(), 0)); //total freq. slots =  required + busy (freq. slots in link e)
				pos++;
			}
			Collections.sort(loadpd); //ascending order
			int h = loadpd.get(0).getId(); //Path with min Load.  h = index[MIN (LD(pd))] h = [1-1,k=3-1]
			double w = loadpd.get(0).getnumFS();
			
			//if the Load is the same, then select path with index h, which has less hops 
			int hops = kshortestPath.get(h).getHops();
			for (int j=0;j<kshortestPath.size();j++){
				if (loadpd.get(j).getnumFS()==w){
					if (kshortestPath.get(loadpd.get(j).getId()).getHops()<hops){
						hops = kshortestPath.get(loadpd.get(j).getId()).getHops();
						h = loadpd.get(j).getId();
					}
				}
			}
			//end 
			System.out.println("Selected Path id: "+h);
			Path pathf = kshortestPath.get(h);
			//Path pathr = net.GetPaths(d.GetDstNode().GetId(), d.GetSrcNode().GetId(), idx-2).get(h); //No always pueden haber paths con el mismo length pero distanta ruta
			
			//Find-out reverse path
			ArrayList<Path> allshortestPathr =  net.GetPaths(d.GetDstNode().GetId(),d.GetSrcNode().GetId(), idx-2);
			Path pathr = null;
			for (Path path:allshortestPathr){
				if (path.GetPath().size()==pathf.GetPath().size()){
					int n=pathf.GetPath().size();
					boolean b = true;
					for (int j=0;j<n;j++){
						if (pathf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
							b = b && true;
						}
						else {
							b = b && false;
							break;
						}
					}
					if (b){
						pathr = path;
						break;
					}
				}
			}
			accomodateFS(pathf,pathr,d, F);
			lightPaths.add(pathf);
		}
		System.out.println("\nTotal number of Freq. Slots required according to path selection: "+totalnumFS);
		System.out.println("Max index of Freq. Slot used: "+maxIndex);
		System.out.printf("Approximated Reuse Factor: %.2f",((double)totalnumFS/(double)(maxIndex+1))*100);
		System.out.println("%");
		System.out.println("Number of No served Demands: "+no_Served);
		System.out.printf("Blocking Factor: %.2f", (double)no_Served/(double)demands.size());
	}
	public void accomodateFS(Path pathf,Path pathr, Demand d, int F){
		// Channels not available in the spectrum of the path links, consequently demand (i) is not served.
		//one demand may require minimum 2 FS, 100Gbps with distance <= 375Km; therefore last index of FS, for 12.5GHz channel width, is F (spectrum capacity)-2
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			boolean eval= true;
			for (Link l:pathf.GetPath())
				eval = eval && l.getFSs().get(s).getState();	
			if (eval){ //continuous constraint
				//If same slots are free in links (continuous constraint), then evaluate if contiguous slots are free (contiguous constraint)
				boolean eval2 = true;
				//Evaluate if consecutive required slots fit in the available spectrum
				for (Link e:pathf.GetPath()){
					for (int q=0;q<numFS;q++){
						eval2 = eval2 && e.getFSs().get(s+q).getState();
					}
				}
				
				if (eval2){ // Contiguous constraint 
					allocate = true; //demand has been served
					totalnumFS+=numFS;//Suma de los FS requeridos para las demandas atendidas
					int n=0;
					System.out.println("Demand "+d.GetId()+" served: ");
					int src = pathf.GetPath().get(0).GetSrcNode().GetId();
					int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
					System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.GetBitRate()+"Gbps)"+"["+pathf.getLength()+"Km]: {");
					System.out.print(src);
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					System.out.print("}, with following ("+numFS+") Frequency Slots: {");
					for (int i=0;i<pathf.GetPath().size();i++){
						for (int q=0;q<numFS;q++){
							//Full-duplex connections
							pathf.GetPath().get(i).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links of the shortest path
							//System.out.println(pathr.GetPath().get(i).getFSs().get(s+q).getState());
							pathr.GetPath().get(i).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links of the shortest path
							//System.out.println(pathr.GetPath().get(i).getFSs().get(s+q).getState());
							if (n==0){
								pathf.setFrequencySlot(pathf.GetPath().get(i).getFSs().get(s+q));
								pathr.setFrequencySlot(pathr.GetPath().get(i).getFSs().get(s+q));
								if (q<(numFS-1))
									System.out.print((s+q)+",");
								else System.out.print((s+q));
								if ((s+q)>maxIndex)
									maxIndex = (s+q);
							}	
						}
						n++;
					}
					System.out.println("}");
					break;
				}	
			}
		}
		if (!allocate)
			System.out.println("Demand "+d.GetId()+" has not been served due to limited frequency slots"+". Total number of no served Demands: "+(++no_Served));
		/*
		//Print out the freq. slot state of each link of each spectrum path 
		int n=0;
		System.out.println("Links of the shortest Path corresponding to demand "+d.GetId()+"("+d.GetSrcNode().GetId()+"->"+d.GetDstNode().GetId()+":");
		for (Link l:pathf.GetPath()){
			System.out.println("Link "+n+":");
			for (int r=0;r<320;r++){
				System.out.println("\t Frequency Slot"+r+" -> "+l.getFSs().get(r).getState());
			}
			n++;
		}
		*/
	}
	
	public static void printListOfLPaths (ArrayList<Path> paths){
		int i=0;
		for (Path path:paths){
			int src = paths.get(i).GetPath().get(0).GetSrcNode().GetId();
			int dst = paths.get(i).GetPath().get(paths.get(i).GetPath().size()-1).GetDstNode().GetId();
			System.out.print("\nSpectrum Path "+(i+1)+"("+src+"->"+dst+"): {");
			i++;
			System.out.print(src);
			for (Link link:path.GetPath()){
				System.out.print(","+link.GetDstNode().GetId());
			}
			System.out.print("}, with "+path.getFreqSlots().size()+" Frequency Slots: {");
			for (FrequencySlot fs:path.getFreqSlots()){
				if (!fs.equals(path.getFreqSlots().get(path.getFreqSlots().size()-1)))
					System.out.print(fs.getId()+",");
				else System.out.print(fs.getId());
			}
			System.out.println("}");
		}
	}
}