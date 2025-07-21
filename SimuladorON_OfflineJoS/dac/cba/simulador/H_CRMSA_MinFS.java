
/*
 * SPSR (Shortest Path with Maximum Spectrum Reuse) Algorithm implementation suggested in A Study of the Routing and Spectrum Allocation in
Spectrum-sliced Elastic Optical Path Networks, Yang Wang, et al.
 */


package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class H_CRMSA_MinFS {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	
	public H_CRMSA_MinFS() {
	    // create a copy of the array so that we can operate on this array
		
	}
	public H_CRMSA_MinFS(Network graph) {
		    // create a copy of the array so that we can operate on this array
			totalnumFS=0;
			maxIndex=0;
			no_Served = 0;
	}
	
	
	public ArrayList<Path> execute (Network net, ArrayList<Demand> demands, DefaultTableModel formats, int F, int S, double GB){
		ArrayList<Path> spPaths = new ArrayList<Path> (); //Array of Shortest-Path corresponding one to each demand
		ArrayList<Path> lightPaths = new ArrayList<Path>();
		//String st = null;
		int numFS=0;
		for (Demand d:demands){
			DijkstraAlgorithm dijkstra = new DijkstraAlgorithm (net);
			dijkstra.execute(d.GetSrcNode());
			Path path = dijkstra.getPath(d.GetSrcNode(), d.GetDstNode());
			//Compute number of frequency slots needed by the SPD
			//double bitRate = d.GetBitRate();
			//int col = formats.findColumn(Double.toString(bitRate));
			for (int row=1;row<formats.getRowCount();row++){
				if (path.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&path.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					//st = (String)formats.getValueAt(row, col);
					int SE = Integer.parseInt((String)formats.getValueAt(row, 0));
					if (SE>0) numFS = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
					else numFS = 0;
					//path.setnumFS(Integer.parseInt(st));
					path.setnumFS(numFS);
					break;
				}
			}
			//end
			//Label each demand with the required numFS for the shortest-path
			d.SetWeigth(numFS);
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
		System.out.println("Sorting the demands in descending order by numFS required executed according to the SPD:");
		//int totalnumFS=0;
		for (Demand d:demands){
			System.out.println("\tDemand "+d.GetId()+": from "+d.GetSrcNode().GetId()+" to "+d.GetDstNode().GetId()+" with "+d.GetWeigth()+" required Slots");
			//totalnumFS+=(int)d.GetWeigth();
		}
		
		/*
		 * Example
		 */
		ArrayList<Demand> demandscopy = (ArrayList<Demand>)demands.clone();
		
		int maxFS=0;
		while (demandscopy.size()!=0){
			if (demandscopy.get(0).GetWeigth()==0){
				System.out.println("distance of this candidate path for this demand (id)"+demandscopy.get(0).GetId()+ "is outside max reach");
				demandscopy.remove(0);
				continue;
			}
			maxFS +=demandscopy.get(0).GetWeigth();
			int n= demandscopy.size();
			for (int j=0;j<n;j++){
				int idx = formats.findColumn(Double.toString(demandscopy.get(j).GetBitRate()));
				ArrayList<Path> admissibleListPathf = net.GetPaths(demandscopy.get(j).GetSrcNode().GetId(), demandscopy.get(j).GetDstNode().GetId(), idx-2);
				int k=0; //k-ShortestPath
				for (Path p:admissibleListPathf){
					if (k==3) break;
					boolean allocation = accomodateFS(p, p, demandscopy.get(j), maxFS);
					//System.out.println("1.- src: "+p.getSrcNode().GetId()+" dst: "+p.getDstNode().GetId());
					if (allocation){
						lightPaths.add(p);
						demandscopy.remove(j);
						n--;
						j--;
						break;
					}
					k++;
				}
 			}
		}
			

		System.out.println("\nTotal number of Freq. Slots required according to path selection: "+(totalnumFS*22));//x22: number of cores
		System.out.println("Max index of Freq. Slot used: "+maxIndex);
		System.out.printf("Approximated Reuse Factor: %.2f",((double)totalnumFS/(double)(maxIndex+1))*100);
		System.out.println("%");
		System.out.println("Number of No served Demands: "+no_Served);
		System.out.printf("Blocking Factor: %.2f", (double)no_Served/(double)demands.size());
		return lightPaths;
	}
	public boolean accomodateFS(Path pathf,Path pathr, Demand d, int F){
		
		//System.out.println("src: "+d.GetSrcNode().GetId()+" dst: "+d.GetDstNode().GetId());
		//System.out.println("src: "+pathf.getSrcNode().GetId()+" dst: "+pathf.getDstNode().GetId());
		boolean allocate=false;
		int numFS =  pathf.getnumFS(); //numFS of the selected Path
		for (int s=0;s<=(F-numFS);s++){  // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			boolean eval= true;
			for (Link l:pathf.GetPath()){
				if (!l.getFSs().get(s).getState()){
					eval = false;
					break;
				}
			}
			if (eval){ //continuous constraint
				//If same slots are free in links (continuous constraint) then evaluate if contiguous slots are free (contiguous constraint)
				boolean eval2 = true;
				//Evaluate if consecutive required slots fit in the available spectrum
				for (Link e:pathf.GetPath()){
					for (int q=1;q<numFS;q++){
						if (!e.getFSs().get(s+q).getState()){
							eval2 = false;
							break;
						} 
					}
					if (!eval2) break;
				}
				if (eval2){ // Contiguous constraint 
					allocate = true; //demand has been served
					totalnumFS+=pathf.getnumFS()*pathf.GetPath().size();//Suma de los FS requeridos (network-wide) para las demandas atendidas
					int n=0;
					System.out.println("Demand "+d.GetId()+" served:");
					int src = pathf.GetPath().get(0).GetSrcNode().GetId();
					int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
					System.out.print("\t Spectrum Path ("+src+"->"+dst+"): {");
					System.out.print(src);
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					System.out.print("}, with following ("+numFS+") Frequency Slots: {");
					for (int i=0;i<pathf.GetPath().size();i++){
						for (int q=0;q<numFS;q++){
							//Full-duplex or uni-directional connections
							pathf.GetPath().get(i).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links of the shortest path
							//pathr.GetPath().get(i).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links of the shortest path
							
							if (n==0){
								pathf.setFrequencySlot(pathf.GetPath().get(i).getFSs().get(s+q));
								//pathr.setFrequencySlot(pathr.GetPath().get(i).getFSs().get(s+q));
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
		/*
		if (!allocate)
			System.out.println("Demand "+d.GetId()+" has not been served due to limited frequency slots"+". Total number of no served Demands: "+(++no_Served));
		*/	
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
		return allocate;
	}
	
	public void removeLinks_bi (ArrayList<Link> lpath, H_SPSR_MinFS graph){
		for (Link edge:lpath){
			for (Link flink:graph.getLinks()){
				if (edge.equals(flink)){
					Node r_src=edge.GetDstNode();
					Node r_dst=edge.GetSrcNode();
					Link rlink=graph.searchLink(r_src,r_dst);
					graph.getLinks().remove(flink);
					graph.getLinks().remove(rlink);
					break;
				}
			}
		}
	}
	public void removeLinks_uni (ArrayList<Link> lpath, H_SPSR_MinFS graph){
		for (Link edge:lpath){
			for (Link flink:graph.getLinks()){
				if (edge.equals(flink)){
					graph.getLinks().remove(flink);
					break;
				}
			}
		}
	}
	public static void printListOfLPaths (ArrayList<Path> paths){
		int i=0;
		for (Path path:paths){
			int src = paths.get(i).GetPath().get(0).GetSrcNode().GetId();
			int dst = paths.get(i).GetPath().get(paths.get(i).GetPath().size()-1).GetDstNode().GetId();
			System.out.print("\nLightPath "+(i+1)+"("+src+"->"+dst+"): {");
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
