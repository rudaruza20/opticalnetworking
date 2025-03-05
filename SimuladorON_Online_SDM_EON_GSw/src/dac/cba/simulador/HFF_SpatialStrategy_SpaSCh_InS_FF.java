/*
 * Space Assignment Strategy: First-Fit (FF) 
 */

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import javax.swing.table.DefaultTableModel;

public class HFF_SpatialStrategy_SpaSCh_InS_FF {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private double blockedbr;
	private double blockedbr_tr;
	private double blockedbr_spe;
	private double sumCoresUsed;
	private double GB; //Guard-Band width
	private int Smax; //Max. Number of spatial channels allowed to use per Spa-SCh
	private int G; //Group size for FJoS
	private String strategy;
	private double max_baud_rate; //Max. operational baud-rate of the optical modulators 
	private int sum_nOC; //counter of number of OCs per spatial channel (sum of n_OC per spatial channel of all served demands)
	private double sum_baud_rate; //counter of baud-rate values (sum of baud-rates of all served demands) 
	private ArrayList<Integer> cdf_s; //for CDF of number of fiber/cores per Spa-SCh 
	private ArrayList<Integer> cdf_baud_rate; //CDF for baud-rate per OC
	private ArrayList<Integer> cdf_nOC; //CDF for n_OC
	private ArrayList<Integer> cdf_alpha; //CDF for alpha value
	private ArrayList<Integer> cdf_nTrx; //CDF for n_TRx per core (spatial channel)
	private int n_bvtxp;
	private int c_nfs;
	//counter of connections that employ a one specific modulation format.
	private int n_64q;   
	private int n_16q;
	private int n_q;
	private int n_b;
	
	
	public HFF_SpatialStrategy_SpaSCh_InS_FF(double GB, String strategy, int Smax, int G, double max_baud_rate) {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		blockedbr_spe = 0;
		sumCoresUsed = 0;
		this.GB = GB;
		this.strategy=strategy;
		this.cdf_s =  new ArrayList<Integer> (Smax);
		this.cdf_baud_rate =  new ArrayList<Integer> ((int)max_baud_rate);
		this.cdf_nOC =  new ArrayList<Integer> (15);
		this.cdf_alpha =  new ArrayList<Integer> (11);
		this.cdf_nTrx =  new ArrayList<Integer> (50);
		for (int i=0;i<Smax;i++)
			cdf_s.add(0);
		for (int i=0;i<max_baud_rate;i++)
			cdf_baud_rate.add(0);
		for (int i=0;i<15;i++)
			cdf_nOC.add(0);
		for (int i=0;i<=10;i++)
			cdf_alpha.add(0);
		for (int i=0;i<50;i++)
			cdf_nTrx.add(0);
		this.Smax=Smax;
		this.G = G;
		this.max_baud_rate=max_baud_rate;
		sum_nOC=0;
		sum_baud_rate=0;
		n_bvtxp=0;
		c_nfs=0;
		n_64q=0;
		n_16q=0;
		n_q=0;
		n_b=0;
		
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
		return this.sumCoresUsed;
	}
	public double getNumberOfOC() {
		return this.sum_nOC;
	}
	public double getTotalBaudrate() {
		return this.sum_baud_rate;
	}
	public ArrayList<Integer> getCDF_nc(){
		return this.cdf_s;
	}
	public ArrayList<Integer> getCDF_baud_rate(){
		return this.cdf_baud_rate;
	}
	public ArrayList<Integer> getCDF_nOC(){
		return this.cdf_nOC;
	}
	public ArrayList<Integer> getCDF_alpha(){
		return this.cdf_alpha;
	}
	public ArrayList<Integer> getCDF_nTRx(){
		return this.cdf_nTrx;
	}
	public double getNumberOfBVTXP() {
		return this.n_bvtxp;
	}
	public double getNumberOfTXP64QAM() {
		return this.n_64q;
	}
	public double getNumberOfTXP16QAM() {
		return this.n_16q;
	}
	public double getNumberOfTXPQPSK() {
		return this.n_q;
	}
	public double getNumberOfTXPBPSK() {
		return this.n_b;
	}
	public double getCounterOfNumberOfFSs() {
		return this.c_nfs;
	}
	
	public void execute (Network net, Demand d, DefaultTableModel formats, int F, boolean MF, String alpha, String ROADMType){
		ArrayList<Path> kshortestPath;
		//1. FF Route&Modulation Assignment 
		ArrayList<ArrayList<Path>> kshortestPaths =  computeCandidatePaths(d.getSrcNode(), d.getDstNode(), formats, d, F, net, MF);
		
		if (kshortestPaths.get(0).isEmpty()){
			//System.out.println("Demand "+d.getId()+"("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to the fact it exceeds the maximum transmission reach"+". Total number of no served Demands: "+(++this.no_Served));
			this.no_Served++;
			this.blockedbr+=d.getBitRate();
			this.blockedbr_tr += d.getBitRate();
			return;
			
		}
		boolean b = false;
		kshortestPath = (ArrayList<Path>)kshortestPaths.get(0).clone(); //0: Forward Paths, 1: Reverse Paths
		for (Path pathf:kshortestPath){	
			//Find-out reverse path
			Path pathr = null;
			for (Path path:kshortestPaths.get(1)){
				if (path.GetPath().size()==pathf.GetPath().size()){
					int n=pathf.GetPath().size();
					boolean flag = true;
					for (int j=0;j<n;j++){
						if (pathf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
							flag = flag && true;
						}
						else {
							flag = flag && false;
							break;
						}
					}
					if (flag){
						pathr = path;
						break;
					}
				}
			}
			//end
			//Compute SCh configuration tuple (n_c, n_fs)
			if (alpha.equals("dynamic")){
				DefaultTableModel results = computeAllSChConfiguration (pathf, net, d);
				ArrayList<Integer> S = (ArrayList<Integer>)results.getValueAt(0,0);
				ArrayList<Integer> Nfs = (ArrayList<Integer>)results.getValueAt(0,1);
				pathf.setId(d.getId());
				pathr.setId(d.getId());
				//for (int i=S.size()-1;i>=0;i--){ //descending
				for (int i=0;i<S.size();i++){ // ascending
					//metrics for allocation and statistics
					//forward path
					pathf.setnumFS(Nfs.get(i));
					pathf.setnumCores(S.get(i)); 
					//reverse path
					pathr.setnumFS(Nfs.get(i));
					pathr.setnumCores(S.get(i)); 
					
					//Baud-rate and number of OCs per Sb-Ch computation
					double t_baud_rate = d.getBitRate()/(Smax*pathf.getcodModFormat()); // total operational baud-rate
					int nOC = (int)Math.ceil((double)t_baud_rate/(double)max_baud_rate);
					double operational_baud_rate = d.getBitRate()/(pathf.getcodModFormat()*Smax*nOC);
					//forward path
					pathf.setBwMod(operational_baud_rate);
					pathf.setOCs(nOC);
					//reverse path
					pathr.setBwMod(operational_baud_rate);
					pathr.setOCs(nOC);
					//end
					
					//
					//2: Spectrum&Core assignment
					if (ROADMType.equals("FNB"))b = accomodateFNB(pathf,pathr, d, F, net);
					else b = accomodateCCC(pathf,pathr, d, F, net);
					if (b){
						net.addLightPath(pathf);
						net.addLightPath(pathr);
						return;
					}
				}
				continue;
			}
			if (alpha.equals("incremental")){
				double a = 0.1;
				while (a<=1.0){
					computeSChConfiguration (pathf, pathr, a, net, d);
					pathf.setId(d.getId());
					pathr.setId(d.getId());
					//2: Spectrum&Core assignment
					if (ROADMType.equals("FNB"))b = accomodateFNB(pathf,pathr, d, F, net);
					else b = accomodateCCC(pathf,pathr, d, F, net);
					if (b){
						net.addLightPath(pathf);
						net.addLightPath(pathr);
						for (int i=(int)(a*10.0);i<11;i++)
							cdf_alpha.set(i, cdf_alpha.get(i)+1);
						return;
					}
					a +=0.1;
				}
			}
			else{
				double a = Double.parseDouble(alpha);
				computeSChConfiguration (pathf, pathr, a, net, d);
				pathf.setId(d.getId());
				pathr.setId(d.getId());
				//2: Spectrum&Core assignment
				if (ROADMType.equals("FNB"))b = accomodateFNB(pathf,pathr, d, F, net);
				else b = accomodateCCC(pathf,pathr, d, F, net);
				if (b){
					net.addLightPath(pathf);
					net.addLightPath(pathr);
					for (int i=(int)(a*10.0);i<11;i++)
						cdf_alpha.set(i, cdf_alpha.get(i)+1);
					return;
				}
			}
		}
		if (!b){
			//System.out.println("Demand "+d.getId()+ "("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to limited frequency slots"+". Total number of no served Demands: "+(++this.no_Served));
			this.no_Served++;
			this.blockedbr+=d.getBitRate();
			this.blockedbr_spe += d.getBitRate();
		}
	}
	public ArrayList<ArrayList<Path>> computeCandidatePaths(Node srcnode, Node dstnode, DefaultTableModel formats, Demand d, int F, Network net, boolean MF){
		
		ArrayList <Path> pathListf, pathListr, pf = new ArrayList<Path>(), pr=new ArrayList<Path>(); //pf and pr:  Array of feasible candidate paths
		ArrayList<ArrayList<Path>> paths = new ArrayList<ArrayList<Path>> ();
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
			int n=100; //initValue;  n-bits per symbol
			for (row=1;row<formats.getRowCount();row++){
				if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					String nbits = (String)formats.getValueAt(row, 0);
					n = Integer.parseInt(nbits);
					if (n==0)f=true;
					break;
				}
			}
			//If distance is larger than that with least efficient modulation format, then block demand by tx reach
			//Only if f=0, p is a feasible candidate path and set to path p the n (bits per symbol of the mod. format)
			if (f) break;
			pf.add(p);// This path p is a feasible candidate path
			p.setcodModFormat(n);
		}
			
		//Reverse Path
		f = false;
		for (Path p:pathListr){
			int n=100; //initValue;  n-bits per symbol
			for (row=1;row<formats.getRowCount();row++){
				if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
					String nbits = (String)formats.getValueAt(row, 0);
					n = Integer.parseInt(nbits);
					if (n==0)f=true;
					break;
				}
			}
			
			//If distance is larger than that with least efficient modulation format, then block demand by tx reach
			//Only if f=0, p is a feasible candidate path and set to path p the n (bits per symbol of the mod. format)
			if (f) break;
			pr.add(p);// This path p is a feasible candidate path
			p.setcodModFormat(n);
			
		}
		//
		paths.add(pf);
		paths.add(pr);
		
		return paths;
			
	}
	
	public void computeSChConfiguration (Path pf,Path pr, double alpha, Network net, Demand d){
		//Compute nc and nfs
		int s1 = Smax; // the maximum ns = Smax
		int nfs = 0, nfs_aux=10000000; // init values
		ArrayList<Double> Beta = new ArrayList<Double>(); // Set of cost values
		ArrayList<Integer> S = new ArrayList<Integer>(); // Solution space for S
		ArrayList<Integer> Nfs = new ArrayList<Integer>(); // Solution space for nfs
		for (int i=1;i<=s1;i++){
			nfs = (int)Math.ceil((((double)d.getBitRate()/i)/pf.getcodModFormat()+GB)/(double)12.5);
			if (nfs < nfs_aux){
				S.add(i);
				Nfs.add(nfs);
				double beta = (alpha)*nfs+(1-alpha)*i; //function cost
				Beta.add(beta);
			}
			nfs_aux = nfs;
		}
		//Determine the optimum (nc,nfs)
		for (int k=0;k<S.size()-1;k++){
			double cost_increment = (double)(Beta.get(k+1)-Beta.get(k))/(double)Beta.get(k);
			if (cost_increment>0.0){
				//as elements decrease until one value and after that start increasing (concave upward function), delete all elements from the one that is bigger than the previous element in the arraylist 
				int z = S.size();
				for (int l=k+1;l<z;l++){
					S.remove(k+1);
					Nfs.remove(k+1);
					Beta.remove(k+1);
				}
				break;
				//end
			}
		}
		// if two elements are equal, then select the one that uses lower nfs
		//forward path
		pf.setnumFS(Nfs.get(Nfs.size()-1));//last element is the most properly value 
		pf.setnumCores((int)Math.ceil(S.get(S.size()-1)/(double)G)); //last element is the most properly value
		//reverse path
		pr.setnumFS(Nfs.get(Nfs.size()-1));//last element is the most properly value 
		pr.setnumCores((int)Math.ceil(S.get(S.size()-1)/(double)G)); //last element is the most properly value
		
		//Baud-rate and number of OCs per Sb-Ch computation
		double t_baud_rate = d.getBitRate()/(Smax*pf.getcodModFormat()); // total operational baud-rate
		int nOC = (int)Math.ceil((double)t_baud_rate/(double)max_baud_rate);
		double operational_baud_rate = d.getBitRate()/(pf.getcodModFormat()*Smax*nOC);
		//forward path
		pf.setBwMod(operational_baud_rate);
		pf.setOCs(nOC);
		//reverse path
		pr.setBwMod(operational_baud_rate);
		pr.setOCs(nOC);
		//end
	}
	public DefaultTableModel computeAllSChConfiguration (Path pf, Network net, Demand d){
		DefaultTableModel results = new DefaultTableModel(1,2);
		//Compute nc and nfs
		int s1 = Smax; // max. ns = Smax
		int nfs = 0, nfs_aux=10000000; // init values
		ArrayList<Integer> S = new ArrayList<Integer>(); // Solution space for S
		ArrayList<Integer> Nfs = new ArrayList<Integer>(); // Solution space for nfs
		for (int i=1;i<=s1;i++){
			nfs = (int)Math.ceil(((((double)d.getBitRate()/i)/pf.getcodModFormat())+GB)/(double)12.5);
			if (nfs < nfs_aux){
				S.add(i);
				Nfs.add(nfs);
			}
			nfs_aux = nfs;
		}
		results.setValueAt(S, 0, 0);
		results.setValueAt(Nfs, 0, 1);
		
		return results;
		
	}
	
	
	//FNB:  Full Non-Blocking ROADM
	public boolean accomodateFNB(Path pathf,Path pathr, Demand d, int F,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		if (F<numFS) return allocate;
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>();
			//Core and Link spectrum continuity 
			boolean eval= true;
			for (Link l:pathf.GetPath()){
				ArrayList<Core> core = new ArrayList<Core>();
				for (Core c:l.getCores()){
					if (c.getFSs().get(s).getState()){ //(b). FF Spectrum Assignment Policy per core (per link)
						core.add(c);
					}
				}
				if (core.size()<pathf.getNumberOfCores()) {
					eval = false;
					break;
				}	
				else cores.add(core);
			}
			if (eval){ //Check if continuous constraint is fulfilled
				//If same slots are free in links (continuous constraint), then evaluate if consecutive slots are also free (contiguous constraint)
				boolean eval2 = true;
				//Evaluate if consecutive required slots fit in the available spectrum. Pending optimize heuristic if in one link e there are lower number of cores with s+q (free) than the needed, then break 
				int z = 0;
				for (Link e:pathf.GetPath()){
					int n = cores.get(z).size();
					for (int i=0;i<n;i++){
						for (int q=1;q<numFS;q++){
							if (!e.getCores().get(cores.get(z).get(i).getId()).getFSs().get(s+q).getState()){
								//cores.get(z).remove(cores.get(z).get(i));
								cores.get(z).remove(i);
								n--;
								i--;
								break;
							}
						}
					}
					if (cores.get(z).size()<pathf.getNumberOfCores()){
						eval2 = false;
						break;
					}
					z++;
				}
				//Select the nc first cores (FF policy) and remove the rest of the cores not needed per link e
				
				if (eval2){ //Check if Contiguous constraint is fulfilled 
					for (ArrayList<Core> core : cores){
						int size = core.size();
						for (int i=(int)pathf.getNumberOfCores();i<size;i++){
							core.remove((int)pathf.getNumberOfCores());
						}	
					}
					allocate = true; //demand has been served
					totalnumFS+=numFS;//Sum of number of FSs required by established/served demands
					//System.out.println("Demand "+d.getId()+" served: ");
					int src = pathf.GetPath().get(0).GetSrcNode().GetId();
					int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
					//Active TRX per node
					net.getNodes().get(src).setIncrementNumberOfActiveTrx(pathf.getNumberOfCores()*pathf.getOCs());
					net.getNodes().get(dst).setIncrementNumberOfActiveTrx(pathr.getNumberOfCores()*pathr.getOCs());
					//for src Node
					int peakNumberOfActiveTrx = net.getNodes().get(src).getPeakNumberOfActiveTrx();
					int numberOfActiveTrx = net.getNodes().get(src).getNumberOfActiveTrx();
					if (numberOfActiveTrx > peakNumberOfActiveTrx) net.getNodes().get(src).setPeakNumberOfActiveTrx(numberOfActiveTrx);
					//for dst Node
					peakNumberOfActiveTrx = net.getNodes().get(dst).getPeakNumberOfActiveTrx();
					numberOfActiveTrx = net.getNodes().get(dst).getNumberOfActiveTrx();
					if (numberOfActiveTrx > peakNumberOfActiveTrx) net.getNodes().get(dst).setPeakNumberOfActiveTrx(numberOfActiveTrx);
					//end
					//System.out.println("distance:\t"+pathf.getLength());
					//System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+"Gbps): {");
					//System.out.print(src);
					/*
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					*/
					//System.out.print("} ["+pathf.getLength()+"Km], Modulation level-code:"+ pathf.getcodModFormat()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
					//1.- increment number of active trxs: n_c x n_OC per each direction (twice as the connections are bidirectional)
					//2.- increment number of active bv-txp: n_OC per each direction (twice as the connections are bidirectional) 
					net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+pathf.getNumberOfCores()*pathf.getOCs()*2);
					if (pathf.getNumberOfCores()>pathf.getOCs()) net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()+pathf.getOCs()*2);
					else net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()+pathf.getNumberOfCores()*2);
					//fill-up the map of active_trx per each connection event
					if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
						net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
					else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
					//fin
					//fill-up the map of active_bvtxp per each connection event
					if (net.getMapOfActiveNumberOfBVTXP().get(net.getNumberOfActiveBVTXP())!=null)
						net.getMapOfActiveNumberOfBVTXP().put(net.getNumberOfActiveBVTXP(), (Integer)net.getMapOfActiveNumberOfBVTXP().get(net.getNumberOfActiveBVTXP())+1);
					else net.getMapOfActiveNumberOfBVTXP().put(net.getNumberOfActiveBVTXP(), 1);
					//fin
					/*Node-wide*/
					//increment number of active BVT: in src and dst nodes as the connections are bidirectional 
					net.getNodes().get(src).setIncrementNumberOfActiveBVT(1);
					net.getNodes().get(dst).setIncrementNumberOfActiveBVT(1);
					//fill-up the map of active_BVT in src and dst nodes per each connection event
					if (net.getNodes().get(src).getMapOfActiveNumberOfBVT().get(net.getNodes().get(src).getNumberOfActiveBVT())!=null)
						net.getNodes().get(src).getMapOfActiveNumberOfBVT().put(net.getNodes().get(src).getNumberOfActiveBVT(), (Integer)net.getNodes().get(src).getMapOfActiveNumberOfBVT().get(net.getNodes().get(src).getNumberOfActiveBVT())+1);
					else net.getNodes().get(src).getMapOfActiveNumberOfBVT().put(net.getNodes().get(src).getNumberOfActiveBVT(), 1);
					
					if (net.getNodes().get(dst).getMapOfActiveNumberOfBVT().get(net.getNodes().get(dst).getNumberOfActiveBVT())!=null)
						net.getNodes().get(dst).getMapOfActiveNumberOfBVT().put(net.getNodes().get(dst).getNumberOfActiveBVT(), (Integer)net.getNodes().get(dst).getMapOfActiveNumberOfBVT().get(net.getNodes().get(dst).getNumberOfActiveBVT())+1);
					else net.getNodes().get(dst).getMapOfActiveNumberOfBVT().put(net.getNodes().get(dst).getNumberOfActiveBVT(), 1);
					//fin
					this.sumCoresUsed+=pathf.getNumberOfCores();
					d.setNc((int)pathf.getNumberOfCores());
					d.setOCs(pathf.getOCs());
					//Compute average number of OC per BV-TXP, enough for one light-path direction. And compute average baud-rate
					sum_nOC+=pathf.getOCs();
					sum_baud_rate+=pathf.getBwMod();
					//fin
					this.n_bvtxp+=pathf.getNumberOfCores();
					this.c_nfs+=pathf.getnumFS();
					//Build CDF for baud-rate, number of assigned spatial channels n_s, and number of OCs per Sb-Ch  
					for (int i=pathf.getNumberOfCores()-1;i<Smax;i++)
						cdf_s.set(i, cdf_s.get(i)+1);
					
					for (int i=(int)Math.ceil(pathf.getBwMod())-1;i<max_baud_rate;i++)
						cdf_baud_rate.set(i, cdf_baud_rate.get(i)+1);
					//Configure max number of OCs per spatial channel 
					for (int i=pathf.getOCs()-1;i<15;i++)
						cdf_nOC.set(i, cdf_nOC.get(i)+1);
					//Build CDF for nTRx per SCh (BV-TXP)
					for (int i=pathf.getOCs()*pathf.getNumberOfCores()-1;i<50;i++)
						cdf_nTrx.set(i, cdf_nTrx.get(i)+1);
					
					//fin
					//counter connections that employ specific mod. format
					switch (pathf.getcodModFormat()){
					case 12:
						this.n_64q++;
						break;
					case 8:
						this.n_16q++;
						break;
					case 4:
						this.n_q++;
						break;
					case 2:
						this.n_b++;
						break;
					}	
					//fin
					for (int i=0;i<pathf.GetPath().size();i++){
						for (int j=0;j<cores.get(i).size();j++){
							for (int q=0;q<numFS;q++){
								//Bidirectional and Full-duplex connections
								//pathf.GetPath().get(i).getCores().get(cores.get(i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links
								//Link l = net.searchLink(pathf.GetPath().get(i).GetSrcNode(),pathf.GetPath().get(i).GetDstNode());
								//System.out.println("ANTES: "+l.getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q).getState());
								pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q).setOccupation(false);
								//System.out.println("DESPUES: "+l.getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q).getState());
								//pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links
								pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q).setOccupation(false); 
								//aggregate to ligthPath the frequency slots used
								if (i==0){
									if (j==0){
										pathf.setFrequencySlot(pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q));
										pathr.setFrequencySlot(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q));
										/*
										if (q<(numFS-1))
											System.out.print((s+q)+",");
										else System.out.print((s+q));
										if ((s+q)>maxIndex)
											maxIndex = (s+q);
										*/
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
					//System.out.print("}, Set-of-Core(id) allocated = {");
					/*
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
					*/
					/*
					//trace FSs used in net
					int k=0;
					for (Link e: net.getLinks()){
						for (Core c:e.getCores()){
							k+=c.getNumberOfFsInUse();
						}
					}
					System.out.println("FS used in the net"+k);
					//end
					*/
					break;
				}	
			}
		}
		/*			
		if (!allocate)
			System.out.println("Demand "+d.getId()+" has not been served in this candidate path, plength: "+pathf.getLength()+"Km");
		*/
		return allocate;
	}
	
	//CCC: Core Continuity Constraint
	public boolean accomodateCCC(Path pathf,Path pathr, Demand d, int F,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		if (F<numFS) return allocate;
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			ArrayList<Core> cores1 =  new ArrayList<Core>(net.getLinks().get(0).getCores().size());
			ArrayList<Core> cores2 =  new ArrayList<Core>(pathf.getNumberOfCores());
			//Core and Link spectrum continuity
			for (Core c:net.getLinks().get(0).getCores()){
				boolean eval= true;
				for (Link e:pathf.GetPath()){
					eval = eval && e.getCores().get(c.getId()).getFSs().get(s).getState(); //(b). FF Spectrum Assignment Policy per core (per link)
					if (!eval) break;
				}
				if (eval) cores1.add(c);
			}
			if (cores1.size()>=pathf.getNumberOfCores()){ //Check if there are enough cores to check the contiguous constraint
				//If same slots are free in links (continuous constraint), then evaluate if consecutive slots are also free (contiguous constraint)
				//Evaluate if consecutive required slots fit in the available spectrum
				boolean eval2 = true;
				int numCores = 0; // counter for cores that fulfill the continuous and contiguous constraint.  
				for (Core c: cores1){
					eval2 = true;
					for (Link e:pathf.GetPath()){
						for (int q=1;q<numFS;q++){
							eval2 = eval2 && e.getCores().get(c.getId()).getFSs().get(s+q).getState();
							//if (eval2) System.out.println("core id: "+c.getId());
							if (!eval2) break;
						}
						if (!eval2) break;
					}
					if (eval2){
						numCores++;
						cores2.add(c);
					}
					if (numCores==pathf.getNumberOfCores()) break;
				}
				if (numCores == pathf.getNumberOfCores()){ //Check if Contiguous constraint is fulfilled 
					allocate = true; //demand has been served
					totalnumFS+=numFS;//Sum of number of FSs required by served/established demands
					//System.out.println("Demand "+d.getId()+" served: ");
					int src = pathf.GetPath().get(0).GetSrcNode().GetId();
					int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
					//Active TRX per node
					net.getNodes().get(src).setIncrementNumberOfActiveTrx(pathf.getNumberOfCores()*pathf.getOCs());
					net.getNodes().get(dst).setIncrementNumberOfActiveTrx(pathr.getNumberOfCores()*pathr.getOCs());
					//for src Node
					int peakNumberOfActiveTrx = net.getNodes().get(src).getPeakNumberOfActiveTrx();
					int numberOfActiveTrx = net.getNodes().get(src).getNumberOfActiveTrx();
					if (numberOfActiveTrx > peakNumberOfActiveTrx) net.getNodes().get(src).setPeakNumberOfActiveTrx(numberOfActiveTrx);
					//for dst Node
					peakNumberOfActiveTrx = net.getNodes().get(dst).getPeakNumberOfActiveTrx();
					numberOfActiveTrx = net.getNodes().get(dst).getNumberOfActiveTrx();
					if (numberOfActiveTrx > peakNumberOfActiveTrx) net.getNodes().get(dst).setPeakNumberOfActiveTrx(numberOfActiveTrx);
					//end
					//System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+"Gbps): {");
					//System.out.print(src);
					/*
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					*/
					//System.out.print("} ["+pathf.getLength()+"Km], Modulation level-code:"+ pathf.getcodModFormat()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
					//1.- increment number of active trxs: n_c x n_OC per each direction (twice as the connections are bidirectional)
					//2.- increment number of active bv-txp: n_OC per each direction (twice as the connections are bidirectional) 
					net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+pathf.getNumberOfCores()*pathf.getOCs()*2);
					if (pathf.getNumberOfCores()>pathf.getOCs()) net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()+pathf.getOCs()*2); //n_oc Spa-SChs
					else net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()+pathf.getNumberOfCores()*2); // n_c Spe-SChs
					//fill-up the map of active_trx per each connection event (*Network-wide*)
					if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
						net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
					else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
					//fin
					//fill-up the map of active_bvtxp per each connection event (*Network-wide*)
					if (net.getMapOfActiveNumberOfBVTXP().get(net.getNumberOfActiveBVTXP())!=null)
						net.getMapOfActiveNumberOfBVTXP().put(net.getNumberOfActiveBVTXP(), (Integer)net.getMapOfActiveNumberOfBVTXP().get(net.getNumberOfActiveBVTXP())+1);
					else net.getMapOfActiveNumberOfBVTXP().put(net.getNumberOfActiveBVTXP(), 1);
					//fin
					/*Node-wide*/
					//increment number of active BVT: in src and dst nodes as the connections are bidirectional 
					net.getNodes().get(src).setIncrementNumberOfActiveBVT(1);
					net.getNodes().get(dst).setIncrementNumberOfActiveBVT(1);
					//fill-up the map of active_BVT in src and dst nodes per each connection event
					if (net.getNodes().get(src).getMapOfActiveNumberOfBVT().get(net.getNodes().get(src).getNumberOfActiveBVT())!=null)
						net.getNodes().get(src).getMapOfActiveNumberOfBVT().put(net.getNodes().get(src).getNumberOfActiveBVT(), (Integer)net.getNodes().get(src).getMapOfActiveNumberOfBVT().get(net.getNodes().get(src).getNumberOfActiveBVT())+1);
					else net.getNodes().get(src).getMapOfActiveNumberOfBVT().put(net.getNodes().get(src).getNumberOfActiveBVT(), 1);
					
					if (net.getNodes().get(dst).getMapOfActiveNumberOfBVT().get(net.getNodes().get(dst).getNumberOfActiveBVT())!=null)
						net.getNodes().get(dst).getMapOfActiveNumberOfBVT().put(net.getNodes().get(dst).getNumberOfActiveBVT(), (Integer)net.getNodes().get(dst).getMapOfActiveNumberOfBVT().get(net.getNodes().get(dst).getNumberOfActiveBVT())+1);
					else net.getNodes().get(dst).getMapOfActiveNumberOfBVT().put(net.getNodes().get(dst).getNumberOfActiveBVT(), 1);
					//fin
					this.sumCoresUsed+=pathf.getNumberOfCores();
					d.setNc((int)pathf.getNumberOfCores());
					d.setOCs(pathf.getOCs());
					//Compute average number of OC per BV-TXP, enough for one light-path direction. And compute average baud-rate
					sum_nOC+=pathf.getOCs();
					sum_baud_rate+=pathf.getBwMod();
					//fin
					this.n_bvtxp+=pathf.getNumberOfCores();
					this.c_nfs+=pathf.getnumFS();
					//Build CDF for baud-rate, number of assigned spatial channels n_s, and number of OCs per Sb-Ch  
					for (int i=pathf.getNumberOfCores()-1;i<Smax;i++)
						cdf_s.set(i, cdf_s.get(i)+1);
					
					for (int i=(int)Math.ceil(pathf.getBwMod())-1;i<max_baud_rate;i++)
						cdf_baud_rate.set(i, cdf_baud_rate.get(i)+1);
					
					for (int i=pathf.getOCs()-1;i<15;i++)
						cdf_nOC.set(i, cdf_nOC.get(i)+1);
					
					//Build CDF for nTRx per SCh (BV-TXP)
					for (int i=pathf.getOCs()*pathf.getNumberOfCores()-1;i<50;i++)
						cdf_nTrx.set(i, cdf_nTrx.get(i)+1);
					
					//fin
					//counter connections that employ specific mod. format
					switch (pathf.getcodModFormat()){
					case 12:
						this.n_64q++;
						break;
					case 8:
						this.n_16q++;
						break;
					case 4:
						this.n_q++;
						break;
					case 2:
						this.n_b++;
						break;
					}	
					//fin
					for (int i=0;i<pathf.GetPath().size();i++){
						for (int j=0;j<cores2.size();j++){
							for (int q=0;q<numFS;q++){
								//Bidirectional and Full-duplex connections
								//pathf.GetPath().get(i).getCores().get(cores.get(i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links of the shortest path
								pathf.GetPath().get(i).getCores().get(cores2.get(j).getId()).getFSs().get(s+q).setOccupation(false);
								//pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links of the shortest path
								pathr.GetPath().get(i).getCores().get(cores2.get(j).getId()).getFSs().get(s+q).setOccupation(false); 
								//aggregate to ligthPath the frequency slots used
								if (i==0){
									if (j==0){
										pathf.setFrequencySlot(pathf.GetPath().get(i).getCores().get(cores2.get(j).getId()).getFSs().get(s+q));
										pathr.setFrequencySlot(pathr.GetPath().get(i).getCores().get(cores2.get(j).getId()).getFSs().get(s+q));
										/*
										if (q<(numFS-1))
											System.out.print((s+q)+",");
										else System.out.print((s+q));
										if ((s+q)>maxIndex)
											maxIndex = (s+q);
										*/
									}	
								}
							}
						}
						/*In forward path put cores in normally ordered list*/
						//pathf.setCores(pathf.GetPath().get(i).getCores().get(cores.get(i).getId()));				
						pathf.setCores(cores2);//OK
						/*In reverse path put cores in inversely ordered list*/
						//pathr.setCores(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).getId()));
						pathr.setCores(cores2);//OK
						//n++;
					}
					//System.out.print("}, Set-of-Core(id) allocated = {");
					/*
					int x=0;
					for (Link e:pathf.GetPath()){
						int t = 0;
						for (Core c:cores2){
							if (t<(cores2.size()-1))
								System.out.print(c.getId()+"-");
							else System.out.print(c.getId());
							t++;
						}
						if (x<(pathf.GetPath().size()-1)){
							System.out.print(",");
						}
						x++;
					}
					System.out.println("}");
					*/
					/*
					//trace FSs used in net
					int k=0;
					for (Link e: net.getLinks()){
						for (Core c:e.getCores()){
							k+=c.getNumberOfFsInUse();
						}
					}
					System.out.println(k);
					//end
					*/
					break;
				}	
			}
		}
		/*				
		if (!allocate)
			System.out.println("Demand "+d.getId()+" has not been served in this candidate path, plength: "+pathf.getLength()+"Km");
		*/
		return allocate;
	}
}

