

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.Random;

import javax.swing.table.DefaultTableModel;

public class Meta_MinMIMO_CRMSA_kSPs {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private ArrayList<Path> lightPaths;
	long t1, t2, solution_time;
	public Meta_MinMIMO_CRMSA_kSPs() {
	    // create a copy of the array so that we can operate on this array
		lightPaths = new ArrayList<Path> ();
	}
	public Meta_MinMIMO_CRMSA_kSPs(Network graph) {
		    // create a copy of the array so that we can operate on this array
			totalnumFS=0;
			maxIndex=0;
			no_Served = 0;
	}
	
	
	public void execute (Network net, ArrayList<Demand> demands, DefaultTableModel formatsMF, DefaultTableModel formatsMCF, int S, double GB){
		t1 = System.currentTimeMillis();

		ArrayList<Path> spPaths = new ArrayList<Path> (); //Array of Shortest-Path corresponding one to each demand
		ArrayList<Path> mimoLPs = new ArrayList<Path>();
		//String st = null;
		int numFS=0;
		for (Demand d:demands){
			DijkstraAlgorithm dijkstra = new DijkstraAlgorithm (net);
			dijkstra.execute(d.GetSrcNode());
			Path path = dijkstra.getPath(d.GetSrcNode(), d.GetDstNode());
			//Compute number of frequency slots needed by the SPD
			//double bitRate = d.GetBitRate();
			//int col = formats.findColumn(Double.toString(bitRate));
			for (int row=1;row<formatsMF.getRowCount();row++){
				if (path.getLength()>Double.parseDouble((String)formatsMF.getValueAt(row-1, 1))&&path.getLength()<=Double.parseDouble((String)formatsMF.getValueAt(row, 1))){
					//st = (String)formats.getValueAt(row, col);
					int SE = Integer.parseInt((String)formatsMF.getValueAt(row, 0));
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
				//int idx = formatsMF.findColumn(Double.toString(demandscopy.get(j).GetBitRate()));
				//ArrayList<Path> admissibleListPathf = net.GetPaths(demandscopy.get(j).GetSrcNode().GetId(), demandscopy.get(j).GetDstNode().GetId(), idx);
				ArrayList<Path> admissibleListPathf = computePaths(true, demandscopy.get(j), formatsMF, S, GB); //true: MF
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
		//printListOfLPaths(lightPaths);
		/////MetaHeuristica /////
		
		minLPswithMIMO (formatsMF, formatsMCF, S, GB, lightPaths);
		int iterations = 10, a=1; //number of explorations for diversification.  
		int size_group = 4;
		int referenceMaxIndex = 133;
		int delta = 0;
		int nshuffle = 3; //in each region n_suffle permutations are executed to find out the local optimal solution. 
		int numberOfMiMoLPs;
	
		ArrayList<Path> bestSubGroup = new ArrayList<Path>(), aux = new ArrayList<Path>();
		while (a <= iterations){
			//count LPs with MIMO
			for (Path l:lightPaths){
				if (l.getMimoStatus()==1){
					mimoLPs.add(l);
				}
			}
			//end 
			numberOfMiMoLPs = mimoLPs.size();
			ArrayList<Path> mimoLPs_subgroup = extractGroup(mimoLPs, size_group); //original
			//Save the original resources assigned to extracted demands
			ArrayList<Path> mimoLPs_subgroup_original = (ArrayList<Path>)mimoLPs_subgroup.clone(); //copy of the original lps  
			ArrayList<ArrayList<Integer>> auxFSs = new ArrayList<ArrayList<Integer>> () ;	
			ArrayList<Path> new_lps;
			int z=0;
			ArrayList<Integer> ids = new ArrayList<Integer>();
			for (Path p:mimoLPs_subgroup_original){
				System.out.println("Demand id: "+p.getDemandId());
				ids = new ArrayList<Integer>();
				for (FrequencySlot fs:p.getFreqSlots()){
					ids.add(fs.getId());
					System.out.println("Fs_id_original: "+fs.getId());
				}
				auxFSs.add(z,ids);
				z++;				
			}
			//end
			mimoLPs.clear();
			//Local Search ()
			int j=0;
			releaseResources(mimoLPs_subgroup, net);
			while (j<nshuffle){
				Collections.shuffle(mimoLPs_subgroup);
				new_lps = new ArrayList<Path> ();
				boolean allocation = false;
				for (Path l:mimoLPs_subgroup){
					Demand d=null;
					for (Demand demand: demands){
						if (demand.GetId()==l.getDemandId()){
							d=demand;
							break;
						}
					}
					ArrayList<Path> admissibleListPathf = computePaths(false, d, formatsMCF, S, GB); 
					int k=0; //k-ShortestPath
					for (Path p:admissibleListPathf){
						if (k==3) break;
						accomodateFS(p, p, d, 320);
						if (maxIndex > (referenceMaxIndex+delta)){
							aux.add(p);
							releaseResources(aux, net);
							aux.clear();
							allocation = false;
						}
						else {
							//selectedLP = p;
							allocation = true;
							lightPaths.add(p);
							new_lps.add(p);
							break;
						}
						k++;
					}
					
					if (!allocation){
						System.out.println("with MIMO");
						ArrayList<Path> admissibleListPathfMiMo = computePaths(true, d, formatsMF, S, GB); 
						int k_=0; //k-ShortestPath
						for (Path p:admissibleListPathfMiMo){
							if (k_==3) break;
							accomodateFS(p, p, d, 320);
							if (maxIndex > (referenceMaxIndex+delta)){
								aux.add(p);
								releaseResources(aux, net);
								aux.clear();
								allocation = false;
							}
							else {
								//selectedLP = p;
								allocation = true;
								lightPaths.add(p);
								new_lps.add(p);
								break;
							}
							k_++;
						}
						
						//computeNumFS(net, formatsMF, true, d, l, S, GB);
						//accomodateFS(l,l,d,320);
						//OJO: verificar igual q no sobrepase el numFSreferencia
						if (!allocation){
							System.out.println("MaxIndex: "+maxIndex+", Sub-group no considered for the final solution");
							break;
						}
					}
				}
				//count LPs with MIMO
				for (Path l:lightPaths){
					if (l.getMimoStatus()==1){
						mimoLPs.add(l);
					}
				}
				System.out.println("size_mimoLPS_after: "+mimoLPs.size());
				//end
				if (mimoLPs.size()<numberOfMiMoLPs && allocation){ //maxIndex <= (referenceMaxIndex+delta)){
					System.out.println("Entra: "+j);
					numberOfMiMoLPs = mimoLPs.size();
					bestSubGroup = (ArrayList<Path>)mimoLPs_subgroup.clone(); //Local optimal solution 
				}
				mimoLPs.clear();
				releaseResources (new_lps,net);
				j++;
			}
			//end
			//Implement the bestSubGroup --> Local optimal solution
			//releaseResources(mimoLPs_subgroup, net); //OJO: AQUI SI ES NECESARIO
			if (bestSubGroup.size()==0) rollback(net, mimoLPs_subgroup_original, auxFSs);//go back to the original solution 
			boolean allocation = false;
			for (Path l:bestSubGroup){
				System.out.println("ENTRA--------------------------");
				Demand d=null;
				for (Demand demand: demands){
					if (demand.GetId()==l.getDemandId()){
						d=demand;
						break;
					}
				}
				ArrayList<Path> admissibleListPathf = computePaths(false, d, formatsMCF, S, GB); 
				int k=0; //k-ShortestPath
				for (Path p:admissibleListPathf){
					if (k==3) break;
					accomodateFS(p, p, d, 320);
					if (maxIndex > (referenceMaxIndex+delta)){
						aux.add(p);
						releaseResources(aux, net);
						aux.clear();
						allocation = false;
					}
					else {
						//selectedLP = p;
						allocation = true;
						lightPaths.add(p);
						break;
					}
					k++;
				}
				
				if (!allocation){
					System.out.println("with MIMO");
					ArrayList<Path> admissibleListPathfMiMo = computePaths(true, d, formatsMF, S, GB); 
					int k_=0; //k-ShortestPath
					for (Path p:admissibleListPathfMiMo){
						if (k_==3) break;
						accomodateFS(p, p, d, 320);
						if (maxIndex > (referenceMaxIndex+delta)){
							aux.add(p);
							releaseResources(aux, net);
							aux.clear();
							allocation = false;
						}
						else {
							//selectedLP = p;
							allocation = true;
							lightPaths.add(p);
							break;
						}
						k_++;
					}
				}
			}
			bestSubGroup.clear();
			//end
			a++;
		}
		//count LPs with MIMO
		for (Path l:lightPaths){
			if (l.getMimoStatus()==1){
				mimoLPs.add(l);
			}
		}
		//end
		System.out.println("size lps: "+lightPaths.size());
		System.out.println("Max index of Freq. Slot used: "+maxIndex);
		System.out.println("No. LPs with MIMO: "+mimoLPs.size());
		System.out.printf("Percentage of lps with MIMO: %.5f ",(double)mimoLPs.size()/(double)lightPaths.size());
		//printListOfLPaths(lightPaths);
		t2 = System.currentTimeMillis();
		solution_time = t2-t1;
		System.out.println("Found feasible solution in "+(solution_time)/1000.0+" sec.");
	}
	public ArrayList<Path> computePaths (boolean  fiberType, Demand d, DefaultTableModel formats, int S, double GB){
		Node srcnode, dstnode;
		int mimoStatus;
		ArrayList <Path> pathListf, pathListr, admissiblePathListf; 
		srcnode = d.GetSrcNode();
		dstnode = d.GetDstNode();
		//Avoid repeat the algorithm for same spectrum path request
		if (fiberType) mimoStatus = 1; //1: MF
		else mimoStatus = 0;//0: MCF
		AllDistinctPaths r= new AllDistinctPaths();
		//compute spectrum path request in both forward and reverse direction
		pathListf = r.ComputeAllDistinctPaths(srcnode, dstnode);
		pathListr = r.ComputeAllDistinctPaths(dstnode,srcnode);
		Collections.sort(pathListf); // Ordenar por Length de menor a mayor y luego insertar ordenados en Table de Network
		Collections.sort(pathListr); // Ordenar por Length de menor a mayor y luego insertar ordenados en Table de Network
		int row;
		admissiblePathListf = new ArrayList<Path>();
		//Forward Path
		for (Path p:pathListf){
			int numFS=0; //initValue
			for (row=1;row<formats.getRowCount();row++){
				if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					//String st = (String)formats.getValueAt(row, col);
					//numFS = Integer.parseInt(st);
					int SE = Integer.parseInt((String)formats.getValueAt(row, 0));
					if (SE>0)numFS = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
					else numFS = 0; //outside max reach.  
					break;
				}
			}
			//If distance outside from maxTR, then select the modulation format with least spectral efficiency (last row). Assuming simple regeneration each maxTR???
			if (numFS==0) break;
			p.setnumFS(numFS);
			p.setbitRate(d.GetBitRate());
			p.setMiMoStatus(mimoStatus);
			admissiblePathListf.add(p);
		}
		return admissiblePathListf;
	}
	public ArrayList<Path> extractGroup (ArrayList<Path> MiMoLPs, int size){
		ArrayList<Path> mimoLPsSubGroup = new ArrayList<Path>();
		Random randompos = new Random();
		int pos;
		for (int i=0;i<size;i++){
			do{
				pos = randompos.nextInt(MiMoLPs.size());
			} while(mimoLPsSubGroup.contains(MiMoLPs.get(pos))); 
			mimoLPsSubGroup.add(MiMoLPs.get(pos));
		}
		System.out.println("subgroup_mimo_size: "+mimoLPsSubGroup.size());
		return mimoLPsSubGroup;
	}
	public void releaseResources (ArrayList<Path> lps, Network net){
		for (Path l:lps){
			for (Link e:l.GetPath()){
				for (FrequencySlot fs:l.getFreqSlots()){
					e.getFSs().get(fs.getId()).setOccupation(true); 
				}
			}
			l.getFreqSlots().clear();
			//Remove LPs
			int n = lightPaths.size();
			for (int j=0;j<n;j++){
				if (lightPaths.get(j).getDemandId()==l.getDemandId()){
					lightPaths.remove(lightPaths.get(j));
					j--;
					n--;
				}
			}
			//end
		}
		//update MaxIndex
		int maxIndexAux=0;
		for (int i=0;i<320;i++){
			for (Link e:net.getLinks()){
				if (!e.getFSs().get(i).getState()){
					maxIndexAux = i;
					break;
				}
			}
		}
		//end
		maxIndex = maxIndexAux; 
	}
	public Path allocate (Network net, DefaultTableModel formats, Demand d, boolean fiberType, int S, double GB){
		Path selectedLP= null;
		ArrayList<Path> admissibleListPathf = computePaths(fiberType, d, formats, S, GB); 
		int k=0; //k-ShortestPath
		for (Path p:admissibleListPathf){
			if (k==3) break;
			boolean allocation = accomodateFS(p, p, d, 320);
			if (allocation){
				selectedLP = p;
				lightPaths.add(p);
				break;
			}
			k++;
		}
		return selectedLP;
	}
	public void computeNumFS (Network net, DefaultTableModel formats, boolean fiberType, Demand d, Path path, int S, double GB){
		int numFS;
		for (int row=1;row<formats.getRowCount();row++){
			if (path.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&path.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
				int SE = Integer.parseInt((String)formats.getValueAt(row, 0));
				if (SE>0) numFS = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
				else numFS = 0;
				//path.setnumFS(Integer.parseInt(st));
				path.setnumFS(numFS);
				//new
				if (fiberType) path.setMiMoStatus(1);
				else path.setMiMoStatus(0);
				//end
				break;
			}
		}
	}
	public void rollback (Network net, ArrayList<Path> original_lps, ArrayList<ArrayList<Integer>> FSs){
		int x, z;
		ArrayList<Integer> fs_aux = new ArrayList<Integer>();
		z=0;
		for (Path lp:original_lps){
			System.out.println("rollback executed for demand id: "+lp.getDemandId());	
			x=0;
			for (Link e_aux:lp.GetPath()){
				for (Link e:net.getLinks()){
					if (e.GetSrcNode().GetId()==e_aux.GetSrcNode().GetId()&&e.GetDstNode().GetId()==e_aux.GetDstNode().GetId()){
						for (Integer id:FSs.get(z)){
							e.getFSs().get(id).setOccupation(false); 
						}
						if (x==0){
							fs_aux = (ArrayList<Integer>)FSs.get(z).clone();
							System.out.println("Entra");
						}
						x++;
						break;
					}
				}
			}
			/*
			for (Path l:lightPaths){
				if (l.getDemandId()==lp.getDemandId()){
					l.getFreqSlots().clear();
					for (Integer id:fs_aux){
						//l.GetPath().get(0).getFSs().get(id);
						l.setFrequencySlot(l.GetPath().get(0).getFSs().get(id)); //PENDIENTE
						System.out.println("fs_id: "+id);
					}
					//fs_aux.clear();
					break;
				}
			}
			*/
			for (Integer id:fs_aux){
				lp.getFreqSlots().clear();
				lp.setFrequencySlot(lp.GetPath().get(0).getFSs().get(id)); //PENDIENTE
				System.out.println("fs_id: "+id);
			}
			System.out.println("MiMo Status: "+lp.getMimoStatus());
			lightPaths.add(lp);
			z++;
		}
		//update MaxIndex
		int maxIndexAux=0;
		for (int i=0;i<320;i++){
			for (Link e:net.getLinks()){
				if (!e.getFSs().get(i).getState()){
					maxIndexAux = i;
					break;
				}
			}
		}
		//end
		maxIndex = maxIndexAux; 
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
					pathf.setDemandId(d.GetId());
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
			System.out.print("\nDemand "+path.getDemandId()+"-->LightPath "+(i+1)+"("+src+"->"+dst+"): {");
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
	public static void minLPswithMIMO (DefaultTableModel formatsMF, DefaultTableModel formatsMCF, int S, double GB, ArrayList<Path> lightPaths){
		//Create a new table of modulation formats with column names for MF
		int lp_noMIMO=0;
		int row;
		for (Path l:lightPaths){
			int numFS_MF=0; //initValue
			for (row=1;row<formatsMF.getRowCount();row++){
				if (l.getLength()>Double.parseDouble((String)formatsMF.getValueAt(row-1, 1))&&l.getLength()<=Double.parseDouble((String)formatsMF.getValueAt(row, 1))){
					//String st = (String)formats.getValueAt(row, col);
					//numFS = Integer.parseInt(st);
					int SE = Integer.parseInt((String)formatsMF.getValueAt(row, 0));
					if (SE>0)numFS_MF = (int)Math.ceil((l.getBitRate()/(S*SE)+GB)/12.5);
					else numFS_MF = 0; //outside max reach.  
					break;
				}
			}
			int numFS_MCF=0; //initValue
			for (row=1;row<formatsMCF.getRowCount();row++){
				if (l.getLength()>Double.parseDouble((String)formatsMCF.getValueAt(row-1, 1))&&l.getLength()<=Double.parseDouble((String)formatsMCF.getValueAt(row, 1))){
					//String st = (String)formats.getValueAt(row, col);
					//numFS = Integer.parseInt(st);
					int SE = Integer.parseInt((String)formatsMCF.getValueAt(row, 0));
					if (SE>0)numFS_MCF = (int)Math.ceil((l.getBitRate()/(S*SE)+GB)/12.5);
					else numFS_MCF = 0; //outside max reach.  
					break;
				}
			}
			if (numFS_MF==numFS_MCF){
				l.setMiMoStatus(0);//delete MIMO to lp l
				lp_noMIMO++;
			}
		}
		System.out.printf("\nPercentage of lps with MIMO: %.5f ",(double)(lightPaths.size()-lp_noMIMO)/(double)lightPaths.size());
		
	}
}
