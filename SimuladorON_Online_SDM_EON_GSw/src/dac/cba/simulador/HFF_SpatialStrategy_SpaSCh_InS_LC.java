/*
 * Space Assignment Strategy: Least-Congested (LC) 
 */

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import javax.swing.table.DefaultTableModel;

public class HFF_SpatialStrategy_SpaSCh_InS_LC {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private double blockedbr;
	private double blockedbr_tr;
	private double blockedbr_spe;
	private double avgCoreUsed;
	
	
	public HFF_SpatialStrategy_SpaSCh_InS_LC() {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		blockedbr_spe = 0;
		avgCoreUsed = 0;
		
	}
	public int getNumberOfNoServedDemands(){
		return this.no_Served;
	}
	public double getBlockedBitRate(){
		return this.blockedbr;
	}
	public double getTRBlockedBitRate(){
		return this.blockedbr_tr;
	}
	public double getSpeBlockedBitRate(){
		return this.blockedbr_spe;
	}
	public double getNumberOfCoresUsed(){
		return this.avgCoreUsed;
	}
	
	public void execute (Network net, Demand d, DefaultTableModel formats, int F, boolean MF){
		ArrayList<Path> kshortestPath;
		ArrayList<ArrayList<Path>> allshortestPath =  computeCandidatePaths(d.getSrcNode(), d.getDstNode(), formats, d, F, net, MF);
		
		if (!allshortestPath.get(0).isEmpty()){
			//1: Select Candidate Paths, k=3 --> first three best paths
			kshortestPath = (ArrayList<Path>)allshortestPath.get(0).clone(); //0: Forward Paths, 1: Reverse Paths
			int size= allshortestPath.get(0).size();
			if (size>3){
				for (int index=3;index<size;index++)
					kshortestPath.remove(3);
			}
			System.out.println("Number of feasible candidate paths: "+kshortestPath.size());
		}
		else{ 
			System.out.println("Demand "+d.getId()+"("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to the fact it exceeds the maximum transmission reach"+". Total number of no served Demands: "+(++this.no_Served));
			this.blockedbr+=d.getBitRate();
			this.blockedbr_tr += d.getBitRate();
			return;
		}
		
		boolean b = false;
		for (Path pathf:kshortestPath){
			ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>();
			//2: core Assignment: Some Cores in all links, it depends of nc --> demand bit-rate and mod. format
			
			for (Link e:pathf.GetPath()){
				//Obtengo la carga actual en cada core del link e
				for (Core c:e.getCores()){
					c.getNumberOfFsInUse();
				}
				//Ordeno de menor a mayor carga los cores en el link e
				ArrayList<Core> c = new ArrayList<Core>();
				c = (ArrayList<Core>)e.getCores().clone();
				Collections.sort(c);
				//Escogo los nc primeros cores de cada link e
				int size = c.size();
				for (int i=(int)pathf.getNumberOfCores();i<size;i++){
					c.remove((int)pathf.getNumberOfCores());
				}
				//añado al array general los cores de cada link e
				cores.add(c);
			}
			
			//Find-out reverse path
			Path pathr = null;
			for (Path path:allshortestPath.get(1)){
				if (path.GetPath().size()==pathf.GetPath().size()){
					int n=pathf.GetPath().size();
					boolean f = true;
					for (int j=0;j<n;j++){
						if (pathf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
							f = f && true;
						}
						else {
							f = f && false;
							break;
						}
					}
					if (f){
						pathr = path;
						break;
					}
				}
			}
			//end
			pathf.setId(d.getId());
			pathr.setId(d.getId());
			//3: Spectrum assignment
			b = accomodateFS(pathf,pathr,d,F,cores,net); 
			if (b){
				net.addLightPath(pathf);
				net.addLightPath(pathr);
				break;
			}
		}
		if (!b){
			System.out.println("Demand "+d.getId()+ "("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to limited frequency slots"+". Total number of no served Demands: "+(++this.no_Served));
			this.blockedbr+=d.getBitRate();
			this.blockedbr_spe += d.getBitRate();
		}
	}
	public ArrayList<ArrayList<Path>> computeCandidatePaths(Node srcnode, Node dstnode, DefaultTableModel formats, Demand d, int F, Network net, boolean MF){
		ArrayList <Path> pathListf, pathListr, pf= new ArrayList<Path>(), pr= new ArrayList<Path>();
		ArrayList<ArrayList<Path>> paths = new ArrayList<ArrayList<Path>> ();
		
		//Assume 10G/20G/25G/30G electrical sub-carriers
		int col;
		double bitRate = d.getBitRate();
		col = formats.findColumn(Double.toString(bitRate));
		//
	   
		AllDistinctPaths r= new AllDistinctPaths();
		//compute spectrum path in both forward and reverse direction
		pathListf = r.ComputeAllDistinctPaths(srcnode, dstnode);
		pathListr = r.ComputeAllDistinctPaths(dstnode,srcnode);
		Collections.sort(pathListf); // Ordenar por Length de menor a mayor 
		Collections.sort(pathListr); // Ordenar por Length de menor a mayor 
		
		//Max k=3 for forward and reverse Path
		int size= pathListf.size();
		if (size>3){
			for (int index=3;index<size;index++)
				pathListf.remove(3);
		}	
		int size_= pathListr.size();
		if (size_>3){
			for (int index=3;index<size_;index++)
				pathListr.remove(3);
		}
		//
		
		int row;
		boolean f=false; //To know if consecutive paths are no candidates paths due to tx reach
		//Forward Path
		for (Path p:pathListf){
			int numFS=100; //initValue
			for (row=1;row<formats.getRowCount();row++){
				if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					String st = (String)formats.getValueAt(row, col);
					numFS = Integer.parseInt(st);
					if (numFS>0)
						pf.add(p);
					else f=true;
					break;
				}
				/*
				if (numFS==0){
					//System.out.println("Length of evaluated path: "+p.getLength());
					f=true;
					break;
				}
				*/
			}
			
			//If distance is larger than that with least efficient modulation format, then block demand by tx reach
			//Only if f=0, then set to path the nd
			if (!f){
				p.setnumFS(numFS);
				char[] code = formats.getValueAt(row, 0).toString().toCharArray();
				p.setcodModFormat(Integer.parseInt(""+code[col-3]));
				//Find the number of cores needed to transmit data signal (Spa-SCh) 
				int numCores = getNumberOfCores(formats,row,col-3);
				p.setnumCores(numCores);
				//Create and assign channel objects to each path
				for (int i=0;i<=F-numFS;i++){
					Channel ch =  new Channel(numFS,i,i+numFS-1);
					p.setChannels(ch);
					//net.setChannels(ch);
				}
			}
			else break;
		}
		//Reverse Path
		f = false;
		for (Path p:pathListr){
			int numFS = 100;//init value
			for (row=1;row<formats.getRowCount();row++){
				if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					String st = (String)formats.getValueAt(row, col);
					numFS = Integer.parseInt(st);
					if (numFS>0)
						pr.add(p);
					else f=true;
					break;
				}
				/*
				if (numFS==0){
					f=true;
					break;
				}
				*/
			}
			
			//If distance is larger than that with least efficient modulation format, then block demand by tx reach
			//Only if f=0, then set to path the nd
			if (!f){
				p.setnumFS(numFS);
				char[] code = formats.getValueAt(row, 0).toString().toCharArray();
				p.setcodModFormat(Integer.parseInt(""+code[col-3]));
				//Find the number of cores needed to transmit data signal (Spa-SCh) 
				int numCores = getNumberOfCores(formats,row,col-3);
				p.setnumCores(numCores);
				//Create and assign channel objects to each path
				for (int i=0;i<=F-numFS;i++){
					Channel ch =  new Channel(numFS,i,i+numFS-1);
					p.setChannels(ch);
					//net.setChannels(ch);
				}
			}
			else break;
		}
		
		paths.add(pf);
		paths.add(pr);
		
		return paths;
	}
	public int getNumberOfCores (DefaultTableModel formats, int row, int col){
		String texto = formats.getValueAt(row, 2).toString();
		Scanner sl = new Scanner(texto);
		sl.useDelimiter("\\s*/\\s*");
		ArrayList<String> cores = new ArrayList<String>();
		while (sl.hasNext()){
			cores.add(sl.next());
		}
		sl.close();
		return Integer.parseInt(cores.get(col)); //NumberOfCores
	}
	
	
	public boolean accomodateFS(Path pathf,Path pathr, Demand d, int F, ArrayList<ArrayList<Core>> cores,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			//Core and Link spectrum continuity 
			boolean eval= true;
			int r = 0;
			for (Link l:pathf.GetPath()){
				for (Core core:cores.get(r)){
					eval = eval && l.getCores().get(core.getId()).getFSs().get(s).getState();  //core.getFSs().get(s).getState();
				}
				r++;
			}
			if (eval){ //continuous constraint
				//If same slots are free in links (continuous constraint), then evaluate if contiguous slots are free (contiguous constraint)
				boolean eval2 = true;
				//Evaluate if consecutive required slots fit in the available spectrum
				int z = 0;
				for (Link e:pathf.GetPath()){
					for (Core core:cores.get(z)){
						for (int q=0;q<numFS;q++){
							eval2 = eval2 && e.getCores().get(core.getId()).getFSs().get(s+q).getState();//core.getFSs().get(s+q).getState();	
						}
					}
					z++;
				}
				
				if (eval2){ // Contiguous constraint 
					allocate = true; //demand has been served
					totalnumFS+=numFS;//Suma de los FS requeridos para las demandas atendidas
					System.out.println("Demand "+d.getId()+" served: ");
					int src = pathf.GetPath().get(0).GetSrcNode().GetId();
					int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
					System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+"Gbps): {");
					System.out.print(src);
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					System.out.print("} ["+pathf.getLength()+"Km], Modulation level-code:"+ pathf.getcodModFormat()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
					this.avgCoreUsed+=pathf.getNumberOfCores();
					d.setNc((int)pathf.getNumberOfCores());
					
					for (int i=0;i<pathf.GetPath().size();i++){
						for (int j=0;j<cores.get(i).size();j++){
							for (int q=0;q<numFS;q++){
								//Bidirectional and Full-duplex connections
								//pathf.GetPath().get(i).getCores().get(cores.get(i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links of the shortest path
								pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q).setOccupation(false);
								//pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links of the shortest path
								pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q).setOccupation(false); 
								//aggregate to ligthPath the frequency slots used
								if (i==0){
									if (j==0){
										pathf.setFrequencySlot(pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q));
										pathr.setFrequencySlot(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q));
										if (q<(numFS-1))
											System.out.print((s+q)+",");
										else System.out.print((s+q));
										if ((s+q)>maxIndex)
											maxIndex = (s+q);
									}	
								}
							}
						}
						/*In forward path put cores in normally ordered list*/
						//pathf.setCores(pathf.GetPath().get(i).getCores().get(cores.get(i).getId()));				
						pathf.setCores(cores.get(i));//OK
						/*In reverse path put cores in inversely ordered list*/
						//pathr.setCores(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).getId()));
						pathr.setCores(cores.get(pathr.getHops()-1-i));//OK
						//n++;
					}
					System.out.print("}, Set-of-Core(id) allocated = {");
					int x=0;
					for (ArrayList<Core> core:cores){
						int t = 0;
						for (Core c:core){
							if (t<(core.size()-1))
								System.out.print(c.getId()+"-");
							else System.out.print(c.getId());
							t++;
						}
						if (x<(cores.size()-1)){
							System.out.print(",");
						}
						x++;
					}
					System.out.println("}");
					break;
				}	
			}
		}
						
		if (!allocate)
			System.out.println("Demand "+d.getId()+" has not been served in this candidate path, plength: "+pathf.getLength()+"Km");
		
		return allocate;
	}
}

