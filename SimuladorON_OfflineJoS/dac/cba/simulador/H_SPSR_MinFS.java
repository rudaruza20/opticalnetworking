
/*
 * SPSR (Shortest Path with Maximum Spectrum Reuse) Algorithm implementation suggested in A Study of the Routing and Spectrum Allocation in
Spectrum-sliced Elastic Optical Path Networks, Yang Wang, et al.
 */


package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class H_SPSR_MinFS {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private final List<Node> nodes;
	private final List<Link> links;
	
	public H_SPSR_MinFS() {
	    // create a copy of the array so that we can operate on this array
		nodes = null;
		links = null;
	}
	public H_SPSR_MinFS(Network graph) {
		    // create a copy of the array so that we can operate on this array
			totalnumFS=0;
			maxIndex=0;
			no_Served = 0;
		    this.nodes = new ArrayList<Node>(graph.getNodes());
		    this.links = new ArrayList<Link>(graph.getLinks());
	}
	
	public List<Node> getNodes (){
		return nodes;
	}
	public List<Link> getLinks (){
		return links;
	}
	public Link searchLink (Node src, Node dst){
		Link link=null;
		for (Link l:this.links){
			if (l.GetSrcNode().equals(src)&&l.GetDstNode().equals(dst)){
				link=l;
			}
		}
		return link;
	}
	public int getNumberOfNodes (){
		return nodes.size();
	}
	public int getNumberOfLinks (){
		return links.size();
	}
	public void execute (Network net, ArrayList<Demand> demands, DefaultTableModel formats, int F){
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
					numFS = (int)Math.ceil((d.GetBitRate()/(22*SE)+10)/12.5);
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
		
		Path pathr=null, pathf = null;
		while (demandscopy.size()!=0){
			H_SPSR_MinFS hspsr= new H_SPSR_MinFS(net);
			DijkstraAlgorithm dijf = new DijkstraAlgorithm (hspsr); //Create a new copy of original graph
			dijf.execute(demandscopy.get(0).GetSrcNode());//0.- first demand of the list
			pathf =  dijf.getPath(demandscopy.get(0).GetSrcNode(), demandscopy.get(0).GetDstNode()); // Forward Links of the shortest path
			//DijkstraAlgorithm dijr = new DijkstraAlgorithm (hspsr); //Create a new copy of original graph
			//dijr.execute(demandscopy.get(0).GetDstNode());
			//pathr =  dijr.getPath(demandscopy.get(0).GetDstNode(), demandscopy.get(0).GetSrcNode()); // Reverse Links of the shortest path
			//Modulation Level Assignment and computation of numFSs needed by the SPD
			//double bitRate = demandscopy.get(0).GetBitRate();
			//int col = formats.findColumn(Double.toString(bitRate));
			for (int row=1;row<formats.getRowCount();row++){
				if (pathf.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&pathf.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					//st = (String)formats.getValueAt(row, col);
					int SE = Integer.parseInt((String)formats.getValueAt(row, 0));
					numFS = (int)Math.ceil((demandscopy.get(0).GetBitRate()/(22*SE)+10)/12.5);
					//path.setnumFS(Integer.parseInt(st));
					pathf.setnumFS(numFS);
					break;
				}
			}
			//end
			accomodateFS(pathf,pathr,demandscopy.get(0), F);
			lightPaths.add(pathf);
			//removeLinks_bi(pathf.GetPath(),hspsr); //Remove forward and reverse links of the shortest-path (bi-directional)
			removeLinks_uni(pathf.GetPath(),hspsr); //Remove forward links of the shortest-path (uni-directional)
			demandscopy.remove(0);
			int n = demandscopy.size();
			//for next Demands find out disjoint paths in the current copy of the original graph
			Path pathF=null, pathR=null;
			for (int p=0;p<n;p++){
				DijkstraAlgorithm dijF = new DijkstraAlgorithm (hspsr); //In forward direction
				dijF.execute(demandscopy.get(p).GetSrcNode());
				pathF =  dijF.getPath(demandscopy.get(p).GetSrcNode(), demandscopy.get(p).GetDstNode()); // Forward Links of the shortest path
				//DijkstraAlgorithm dijR = new DijkstraAlgorithm (hspsr); //In reverse direction
				//dijR.execute(demandscopy.get(p).GetDstNode());
				//Path pathR =  dijR.getPath(demandscopy.get(p).GetDstNode(), demandscopy.get(p).GetSrcNode()); // Reverse Links of the shortest path
				if (pathF!=null){
					//Modulation Level Assignment and computation of numFSs needed by the SPD
					//double bitRate_c = demandscopy.get(p).GetBitRate();
					//int col_c = formats.findColumn(Double.toString(bitRate));
					for (int row=1;row<formats.getRowCount();row++){
						if (pathF.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&pathF.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
							//st = (String)formats.getValueAt(row, col);
							int SE = Integer.parseInt((String)formats.getValueAt(row, 0));
							numFS = (int)Math.ceil((demandscopy.get(p).GetBitRate()/(22*SE)+10)/12.5);
							//path.setnumFS(Integer.parseInt(st));
							pathF.setnumFS(numFS);
							break;
						}
					}
					//end
					accomodateFS(pathF, pathR,demandscopy.get(p),F);
					lightPaths.add(pathF);
					//removeLinks_bi(pathF.GetPath(),hspsr); //Remove forward and reverse links of the shortest-path (bi-directional)
					removeLinks_uni(pathF.GetPath(),hspsr); //Remove forward links of the shortest-path (uni-directional)
					demandscopy.remove(p);
					n--;
					p--;
				}
			}
		}
		System.out.println("\nTotal number of Freq. Slots required according to path selection: "+totalnumFS*22); //x22: number of cores
		System.out.println("Max index of Freq. Slot used: "+maxIndex);
		System.out.printf("Approximated Reuse Factor: %.2f",((double)totalnumFS/(double)(maxIndex+1))*100);
		System.out.println("%");
		System.out.println("Number of No served Demands: "+no_Served);
		System.out.printf("Blocking Factor: %.2f", (double)no_Served/(double)demands.size());
	}
	public void accomodateFS(Path pathf,Path pathr, Demand d, int F){
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
