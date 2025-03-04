/*
 * Est� pensado para any MF/MCF
 */

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;


import javax.swing.table.DefaultTableModel;

public class HFF_SpatialStrategy_SpaSCh_JoS {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private double blockedbr;
	private double blockedbr_tr;
	private double blockedbr_spe;
	private double blockedbw;
	private double acumBitRate; //total bit-rate served by this execution as part of total required demand bit-rate. It is possible to attend a partial demand in the case of demand splitting (flexible baud-rate). It accounts for demand bit-rate fulfillment 
	private double sumCoresUsed; //counter of number of cores per Spa-SCh (sum of n_c of all served demands)
	private double GB; //Guard-Band width
	private String strategy; //Only for JoS {PSA|FSA}
	private int Smax; //Max. Number of cores/fibers/transceivers allowed to use per Spa-SCh
	private double max_baud_rate; //Max. operational baud-rate of the optical modulators 
	private Integer active_trx_counter;
	private int sum_nOC; // counter of number of OCs per spatial channel (sum of n_OC per spatial channel of all served demands)
	private double sum_baud_rate; //counter of baud-rate values (sum of baud-rates of all served demands) 
	private ArrayList<Demand> listDemands; //List of demands served by this execution (useful to know served demands in case of demand splitting)
	private ArrayList<Integer> cdf_s;
	private ArrayList<Integer> cdf_baud_rate;
	private ArrayList<Integer> cdf_nOC; //CDF for n_OC per core (spatial channel)
	private ArrayList<Integer> cdf_nTrx; //CDF for n_TRx per SCh
	private int lreuseFactor; //counter for lightpath reuse
	private int c_nfs; // counter for the number of FS
	private String groomingStrategy; //Form of doing grooming. Options:  non|predefined|dynamic
	private String t_baudrate;// 14072022: Type of Baudrate {flexible | fixed}
	//counter of connections that employ a one specific modulation format.
	private int n_64q;   
	private int n_16q;
	private int n_q;
	private int n_b;
	public HFF_SpatialStrategy_SpaSCh_JoS () {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		blockedbr_spe = 0;
		blockedbw = 0;
		sumCoresUsed = 0;
		active_trx_counter = 0;
	}
	public HFF_SpatialStrategy_SpaSCh_JoS (double GB, String strategy, int Smax, double max_baud_rate, String groomingStrategy,String t_baudrate) {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		this.acumBitRate=0;//new 18022025
		blockedbr_spe = 0;
		blockedbw = 0;
		sumCoresUsed = 0;
		this.GB=GB;
		this.strategy=strategy;
		this.listDemands = new ArrayList<Demand> ();
		this.cdf_s =  new ArrayList<Integer> (Smax);
		this.cdf_baud_rate =  new ArrayList<Integer> ((int)max_baud_rate);
		this.cdf_nOC =  new ArrayList<Integer> (10);
		this.cdf_nTrx =  new ArrayList<Integer> (50);
		for (int i=0;i<Smax;i++)
			cdf_s.add(0);
		for (int i=0;i<max_baud_rate;i++)
			cdf_baud_rate.add(0);
		for (int i=0;i<10;i++)
			cdf_nOC.add(0);
		for (int i=0;i<50;i++)
			cdf_nTrx.add(0);
		this.Smax=Smax;
		this.max_baud_rate=max_baud_rate;
		this.sum_nOC=0;
		this.sum_baud_rate=0;
		this.groomingStrategy = groomingStrategy;
		this.t_baudrate = t_baudrate;
		n_64q=0;
		n_16q=0;
		n_q=0;
		n_b=0;
		lreuseFactor=0;
		c_nfs = 0;
		
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
	//new 18022025
	public double getServedBitRate(){
		return this.acumBitRate;
	}
	//end
	public double getBlockedBw(){
		return this.blockedbw;
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
	public ArrayList<Demand> getListOfDemands() {
		return this.listDemands;
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
	public ArrayList<Integer> getCDF_nTRx(){
		return this.cdf_nTrx;
	}
	public Integer getNumberOfActiveTransceivers (){
		return active_trx_counter;
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
	public int getCounterOfReusedLightpaths() {
		return this.lreuseFactor;
	}
	public int getCounterOfNumberOfFSs() {
		return this.c_nfs;
	}
	public void execute (Network net, Demand d, DefaultTableModel formats, int F, boolean MF){
		this.acumBitRate=0; //new 18022025. Re-init the counter for each demand allocation
		ArrayList<Path> kshortestPath;
		boolean e2e_grooming; 
		DefaultTableModel parameters = new DefaultTableModel ();
		listDemands.clear(); // 22072022 re-initiate the list of Demands served by this execution
		if (t_baudrate=="flexible"){
			switch (groomingStrategy){
				case "non":
					e2e_grooming = false; 
					break;
				case "predefined":
					e2e_grooming = spatialPartialMatchGroomingWithPreDefinedSChConf (d,formats,net);
					break;
				case "dynamic":
					e2e_grooming = spatialPartialMatchGroomingWithDynamicSChConf (d,formats,net);
					break;
				default: e2e_grooming = false; 
			}
		}	
		else {
			parameters = spatialGroomingFixed(d, formats,net);
			e2e_grooming = (boolean)parameters.getValueAt(0, 0);
		}
		if (e2e_grooming) { this.acumBitRate+=d.getBitRate(); return;}//grooming successful
		//Try to accommodate new lightpath
		if (t_baudrate=="fixed") d = (Demand)parameters.getValueAt(0, 1);
		ArrayList<ArrayList<Path>> allshortestPath = computeCandidatePaths(d.getSrcNode(), d.getDstNode(), formats, d, F, net, MF);
		if (!allshortestPath.get(0).isEmpty()){
			//1: Select Candidate Paths, k=3 --> first three best paths
			kshortestPath = (ArrayList<Path>)allshortestPath.get(0).clone(); //0: Forward Paths, 1: Reverse Paths
			int size= allshortestPath.get(0).size();
			if (size>3){
				for (int index=3;index<size;index++)
					kshortestPath.remove(3);
			}
			//System.out.println("Number of feasible candidate paths: "+kshortestPath.size());
		}
		else{ 
			//System.out.println("Demand "+d.getId()+"("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to the fact it exceeds the maximum transmission reach"+". Total number of no served Demands: "+(++this.no_Served));
			this.no_Served++;
			this.blockedbr+=d.getBitRate();
			this.blockedbr_tr += d.getBitRate();
			this.blockedbw += d.getBw();
			return;
		}
		
		boolean b = false;
		int requiredSC =0;
		double requiredBitRate, deficitBitRate; //new 15022025
		for (Path pathf:kshortestPath){
			ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>();
			//2: core Assignment: Some Cores in all links but the reservation is in ALL cores
			for (Link e:pathf.GetPath()){
				ArrayList<Core> c = new ArrayList<Core>();
				c.add(e.getCores().get(0));  //init core id = 0;
				cores.add(c); // ArrayList Cores with one core per link 
			}
			
			//Find-out reverse path: Asegurar que el reverse path es sim�trico al de forward
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
			if (t_baudrate=="flexible"){
				//pathf.setId(d.getId()); //OJO
				//pathr.setId(d.getId());
				//3: Spectrum assignment
				b = accomodateFS(pathf,pathr,d,F,cores,net); 
				if (b){
					pathf.setId(d.getId()); //OJO
					pathr.setId(d.getId());
					net.addLightPath(pathf);
					net.addLightPath(pathr);
					listDemands.add(d);
					this.acumBitRate+=d.getBitRate();
					break;
				}
			}
			else {	
			//updated 15022025
			requiredBitRate = d.getBitRate();
			//getResourcesForFixedBaudRate(pathf, formats, d); //or it can be computed # OCs by this function (d.getOCs directly) and don't use pathf.getOCs
			//3: Spectrum assignment
			do {
				//Try to attend the whole demand d in the same path as long as it has available spectra resources (no necessarily over contiguous FSs). Otherwise, try with other k-shortest paths
				//To this end, it is necessary work a repeated copies of physical path to form new light-paths.
				Path auxpathf = new Path(pathf.getSrcNode(), pathf.getDstNode(), pathf.GetPath(), pathf.getHops(), pathf.getLength()); //it is needed new auxiliary path for each split demand (from original) 
				Path auxpathr = new Path(pathr.getSrcNode(), pathr.getDstNode(), pathr.GetPath(), pathr.getHops(), pathr.getLength()); //it is needed new auxiliary path for each split demand (from original)
				auxpathf.setnumFS(pathf.getnumFS());
				auxpathr.setnumFS(pathf.getnumFS());
				auxpathf.setSpectralEfficiency(pathf.getSpectralEfficiency());
				auxpathr.setSpectralEfficiency(pathr.getSpectralEfficiency());
				auxpathf.setOCs(pathf.getOCs());
				auxpathr.setOCs(pathr.getOCs());
				b = accomodateFS(auxpathf,auxpathr,d,F,cores,net);
				if (b){
					//System.out.println("\nDemand id "+d.getId()+" has been allocated with new light-path");
					//1st time all Smax spatial channels are available for allocation
					int deficitSC = auxpathf.getOCs()-net.getLinks().get(0).getNumberOfCores();	
					//System.out.println("Needed "+auxpathf.getOCs()+" OCs. Deficit: "+deficitSC);
					auxpathf.setId(d.getId());
					auxpathr.setId(d.getId());
					net.addLightPath(auxpathf);
					net.addLightPath(auxpathr);
					if (deficitSC<=0){
						// demand d has been completely served 
						d.setOCs(auxpathf.getOCs());
						d.setNc(d.getOCs()); // 1 OC / Sp-Ch
						auxpathf.setnumCores(auxpathf.getNumberOfCores()+d.getOCs()); // 1 OC / Sp-Ch
						d.setBitRate(d.getOCs()*max_baud_rate*auxpathf.getSpectralEfficiency());
						auxpathr.setnumCores(auxpathr.getNumberOfCores()+d.getOCs()); // 1 OC / Sp-Ch
						//auxpathf.setnumCores(d.getOCs());
						//auxpathr.setnumCores(d.getOCs());
						listDemands.add(d);
						this.acumBitRate += d.getOCs()*max_baud_rate*auxpathf.getSpectralEfficiency();
						//System.out.println("\nDemand id has been completely served: "+d.getId());
						break;
					}
					else{ //this demand d requires more OCs than the spatial channel count one link has (1 OC / Sp-Ch)
						//Try to attend the whole demand d in the same path as long as it has available spectra resources. Otherwise, try with other k-shortest paths			
						//1st time all Smax spatial channels are available for allocation
						d.setOCs(net.getLinks().get(0).getNumberOfCores()); //re-difine the number of OCs that this demand d carries
						auxpathf.setnumCores(auxpathf.getNumberOfCores()+d.getOCs()); // 1 OC / SC
						//auxpathf.setnumCores(d.getOCs()); // 1 OC / SC
						d.setBitRate(d.getOCs()*max_baud_rate*auxpathf.getSpectralEfficiency()); //re-difine the bit-rate that this demand d carries
						d.setBw(d.getOCs()*max_baud_rate);
						d.setNc(net.getLinks().get(0).getNumberOfCores()); // 1 OC / Sp-Ch
						listDemands.add(d);
						auxpathr.setnumCores(auxpathr.getNumberOfCores()+d.getOCs()); // 1 OC / SC
						//auxpathr.setnumCores(d.getOCs()); // 1 OC / SC
						this.acumBitRate += d.getOCs()*max_baud_rate*auxpathf.getSpectralEfficiency();
						deficitBitRate = requiredBitRate-this.acumBitRate;
						//System.out.println(d.getBitRate()+" Gbps of demand id: "+d.getId()+" has been served");
						d = new Demand(d.getSrcNode().GetId(),d.getDstNode().GetId(),deficitBitRate,net);
						computeFSs_fixed(d, pathf); //update n_oc for demand d, and numFS needed for both pathf and pathr
						computeFSs_fixed(d, pathr); //update n_oc for demand d, and numFS needed for both pathf and pathr
						//System.out.println("new demand id (splitting): "+d.getId()+" with "+d.getBitRate()+" Gbps");
					}
				}
				else break; //Try to attend the whole demand d in the same path as long as it has available spectra resources. Otherwise, try with other k-shortest paths
			} while (this.acumBitRate < requiredBitRate);
			if (this.acumBitRate >= requiredSC) break;
			}
		}	
		//if (counter < requiredSC){ //or (!b)
		if (!b){
			this.no_Served++;
			this.blockedbr+=d.getBitRate(); //it only accounts for deficit bit-rate that hasn't been served
			this.blockedbr_spe += d.getBitRate(); //wrong - candidate to remove / depurate
			this.blockedbw += d.getBw();
			//System.out.println("Demand id "+d.getId()+" has been blocked due to lack of resources. "+this.blockedbr+" - "+d.getBitRate());
		}
		return;
	}
	public boolean spatialGroomingWithPreDefinedSChConf(Demand d, DefaultTableModel formats, Network net){
		//The SCh configuration is equivalent to alpha=1 (ComNet), always minimize n_fs.
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		ArrayList <Integer> resources =  new ArrayList<Integer> (3); // resources required: n_fs (numFSs required) and n_c (numCores required)
		boolean groom=false;
		if (net.getLightPaths().isEmpty()) return groom;
		for (Path path:net.getLightPaths()){
			int idsrc=d.getSrcNode().GetId();
			int iddst=d.getDstNode().GetId();
			if (path.getSrcNode().GetId()==idsrc && path.getDstNode().GetId()==iddst && path.getNumberOfCores()<path.GetPath().get(0).getNumberOfCores())
				lightpathsf.add(path);
			if (path.getSrcNode().GetId()==iddst && path.getDstNode().GetId()==idsrc && path.getNumberOfCores()<path.GetPath().get(0).getNumberOfCores())
				lightpathsr.add(path);
		}
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int id;
		for (Path pf:lightpathsf){
			resources = getResourcesForFlexibleBaudRate (pf, formats, d);
			int n_fs = resources.get(0);
			int n_c = resources.get(1);
			int n_OC = resources.get(2);
			//if (n_c== 17) continue;
			if (n_fs <= pf.getnumFS()&&(pf.getNumberOfCores()+n_c)<=net.getLinks().get(0).getNumberOfCores()){
				groom=true;
				id = pf.getArrayId().get(0);
				pf.addCores(n_c);
				d.setNc(n_c);
				d.setOCs(n_OC);
				pf.setId(d.getId());
				listDemands.add(d);
				lreuseFactor++;
				
				/*operational baud-rate re-adjusted, since op_baud-rate_*n*n_c = d_br*/
				double operational_baud_rate = d.getBitRate()/(pf.getSpectralEfficiency()*n_c*n_OC);
				//for statistics
				sum_baud_rate+=operational_baud_rate;
				sum_nOC+=n_OC;
				sumCoresUsed+=n_c;
				c_nfs +=pf.getnumFS();
				//active TRxs network-wide
				net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+n_c*2*n_OC); //x2 (bidirectional connections)
				if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
					net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
				else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
				//active BVT node-wide
				//increment number of active BVT: in src and dst nodes as the connections are bidirectional
				int src = d.getSrcNode().GetId();
				int dst = d.getDstNode().GetId();
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
				for (int i=(int)Math.ceil(operational_baud_rate)-1;i<max_baud_rate;i++)
					cdf_baud_rate.set(i, cdf_baud_rate.get(i)+1);
				
				for (int i=n_c-1;i<Smax;i++)
					cdf_s.set(i, cdf_s.get(i)+1);
				
				for (int i=n_OC-1;i<10;i++)
					cdf_nOC.set(i, cdf_nOC.get(i)+1);
				
				for (int i=n_OC*n_c-1;i<50;i++)
					cdf_nTrx.set(i, cdf_nTrx.get(i)+1);
				//fin
				
				//counter connections that employ specific mod. format
				switch (pf.getSpectralEfficiency()){
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
				
				//reverse path		
				Path pr = null;
				for (Path path:lightpathsr){
					if (path.getArrayId().contains(id)&&(path.GetPath().size()==pf.GetPath().size())){
						int n=pf.GetPath().size();
						boolean f = true;
						for (int j=0;j<n;j++){
							if (pf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
								f = f && true;
							}
							else {
								f = f && false;
								break;
							}
						}
						if (f){
							pr = path;
							break;
						}
					}
				}
				//end
				pr.setId(d.getId());
				pr.addCores(n_c);
				break;
			}
			
		}
		return groom;
	}
	
	public boolean spatialGroomingWithDynamicSChConf(Demand d, DefaultTableModel formats, Network net){
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		boolean groom=false;
		if (net.getLightPaths().isEmpty()) return groom;
		for (Path path:net.getLightPaths()){
			int idsrc=d.getSrcNode().GetId();
			int iddst=d.getDstNode().GetId();
			if (path.getSrcNode().GetId()==idsrc && path.getDstNode().GetId()==iddst)
				lightpathsf.add(path);
			if (path.getSrcNode().GetId()==iddst && path.getDstNode().GetId()==idsrc)
				lightpathsr.add(path);
		}
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int id;
		for (Path pf:lightpathsf){
			DefaultTableModel results = computeSChConfiguration (pf, net, d);
			//resources = getResources (pf, formats, d);
			//int n_fs = resources.get(0);
			//int n_c = resources.get(1);
			//int n_OC = resources.get(2);
			ArrayList<Integer> S = (ArrayList<Integer>)results.getValueAt(0,0);
			ArrayList<Integer> Nfs = (ArrayList<Integer>)results.getValueAt(0,1);
			//for (int i=S.size()-1;i>=0;i--){ // descending
			for (int i=0;i<S.size();i++){ // ascending
				if (Nfs.get(i) <= pf.getnumFS()&&(pf.getNumberOfCores()+S.get(i))<=net.getLinks().get(0).getNumberOfCores()){
					groom=true;
					id = pf.getArrayId().get(0);
					pf.addCores(S.get(i));
					d.setNc(S.get(i));
					double t_baud_rate_pca = Nfs.get(i)*12.5-GB; // total operational baud-rate
					int n_OC=0; // number of OC per Sb-Ch (0.- init value)
					n_OC = (int)Math.ceil((double)t_baud_rate_pca/(double)max_baud_rate);
					d.setOCs(n_OC);
					pf.setId(d.getId());
					listDemands.add(d);
					lreuseFactor++;
				
					/*operational baud-rate re-adjusted, since op_baud-rate_*n*n_c = d_br*/
					double operational_baud_rate = d.getBitRate()/(pf.getSpectralEfficiency()*S.get(i)*n_OC);
					//for statistics
					sum_baud_rate+=operational_baud_rate;
					sum_nOC+=n_OC;
					sumCoresUsed+=S.get(i);
					c_nfs +=pf.getnumFS();
					//active TRx network-wide
					net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+S.get(i)*2*n_OC); //x2 (bidirectional connections)
					if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
						net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
					else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
					//active BVT node-wide
					//increment number of active BVT: in src and dst nodes as the connections are bidirectional
					int src = d.getSrcNode().GetId();
					int dst = d.getDstNode().GetId();
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
					for (int j=(int)Math.ceil(operational_baud_rate)-1;j<max_baud_rate;j++)
						cdf_baud_rate.set(j, cdf_baud_rate.get(j)+1);
				
					for (int k=S.get(i)-1;k<Smax;k++)
						cdf_s.set(k, cdf_s.get(k)+1);
					
					for (int l=n_OC-1;l<10;l++)
						cdf_nOC.set(l, cdf_nOC.get(l)+1);
					
					for (int m=n_OC*S.get(i)-1;m<50;m++)
						cdf_nTrx.set(m, cdf_nTrx.get(m)+1);
					//fin
				
					//counter connections that employ specific mod. format
					switch (pf.getSpectralEfficiency()){
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
				
					//reverse path		
					Path pr = null;
					for (Path path:lightpathsr){
						if (path.getArrayId().contains(id)&&(path.GetPath().size()==pf.GetPath().size())){
							int n=pf.GetPath().size();
							boolean f = true;
							for (int j=0;j<n;j++){
								if (pf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
									f = f && true;
								}
								else {
									f = f && false;
									break;
								}
							}
							if (f){
								pr = path;
								break;
							}
						}
					}
					//end
					pr.setId(d.getId());
					pr.addCores(S.get(i));
					break;
				}
			}
			if (groom) break;
		}
		return groom;
	}
	public DefaultTableModel spatialGroomingFixed(Demand d, DefaultTableModel formats, Network net){
		//The SCh configuration is equivalent to alpha=1 (ComNet), always minimize n_fs.
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		DefaultTableModel parameters = new DefaultTableModel(1,2); 
		boolean groom=false;
		parameters.setValueAt(groom, 0, 0);
		parameters.setValueAt(d, 0, 1);

		double requiredBitRate = d.getBitRate();
		double deficitBitRate;
		if (net.getLightPathsWithResources(d).get(0).isEmpty()) return parameters;
		lightpathsf = net.getLightPathsWithResources(d).get(0);
		lightpathsr	= net.getLightPathsWithResources(d).get(1);
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int availableSC, deficitSC, id;
		for (Path pf:lightpathsf){
			getResourcesForFixedBaudRate(pf, formats, d);
			availableSC = net.getLinks().get(0).getNumberOfCores()-pf.getNumberOfCores();
			deficitSC = d.getOCs()-availableSC;	
			if (deficitSC<=0){
				//groom OK
				groom = true;
				pf.setnumCores(pf.getNumberOfCores()+d.getOCs()); // 1 OC / Sp-Ch
				d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				d.setNc(d.getOCs()); // 1 OC / Sp-Ch
				pf.setId(d.getId());
				id = pf.getArrayId().get(0);
				Path pr = getReversePath(pf, lightpathsr, id);
				pr.setId(d.getId());
				pr.setnumCores(pr.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				listDemands.add(d);
				lreuseFactor++;
				this.acumBitRate += d.getOCs()*max_baud_rate*pf.getSpectralEfficiency();
				//System.out.println("Demand id: "+d.getId()+" has been served by grooming");
				break;
			}
			else{
				d.setOCs(availableSC);
				pf.setnumCores(pf.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				d.setBw(d.getOCs()*max_baud_rate);
				d.setNc(d.getOCs()); // 1 OC / SC
				listDemands.add(d);
				lreuseFactor++;
				pf.setId(d.getId());
				id = pf.getArrayId().get(0);
				Path pr = getReversePath(pf, lightpathsr, id);
				pr.setId(d.getId());
				pr.setnumCores(pr.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				this.acumBitRate += availableSC*max_baud_rate*pf.getSpectralEfficiency();
				deficitBitRate = requiredBitRate-this.acumBitRate; 
				//System.out.println(d.getBitRate()+" Gbps of demand id: "+d.getId()+" has been served by grooming");
				d = new Demand(d.getSrcNode().GetId(),d.getDstNode().GetId(),deficitBitRate,net);
				//System.out.println("new demand id (splitting): "+d.getId());
			}
		}
		if (this.acumBitRate >= requiredBitRate){
			groom = true;
			parameters.setValueAt(groom, 0, 0);
		}
		parameters.setValueAt(d, 0, 1);
		return parameters;
	}
	
	//Begin Partial-match grooming
	public boolean spatialPartialMatchGroomingWithPreDefinedSChConf(Demand d, DefaultTableModel formats, Network net){
		//The SCh configuration is equivalent to alpha=1 (ComNet), always minimize n_fs.
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		ArrayList <Integer> resources =  new ArrayList<Integer> (3); // resources required: n_fs (numFSs required) and n_c (numCores required)
		boolean groom=false;
		if (net.getLightPaths().isEmpty()) return groom;
		for (Path path:net.getLightPaths()){
			int idsrc=d.getSrcNode().GetId();
			int iddst=d.getDstNode().GetId();
			ArrayList <Integer> list = path.getNodes();
			if (list.contains(idsrc) && list.contains(iddst) && (list.indexOf(idsrc)-list.indexOf(iddst))<0)
				lightpathsf.add(path);
			if (list.contains(idsrc) && list.contains(iddst) && (list.indexOf(idsrc)-list.indexOf(iddst))>0)
				lightpathsr.add(path);
		}
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int id;
		for (Path pf:lightpathsf){
			resources = getResourcesForFlexibleBaudRate (pf, formats, d);
			int n_fs = resources.get(0);
			int n_c = resources.get(1);
			int n_OC = resources.get(2);
			//if (n_c== 17) continue;
			if (n_fs <= pf.getnumFS()&&(pf.getNumberOfCores()+n_c)<=net.getLinks().get(0).getNumberOfCores()){
				groom=true;
				id = pf.getArrayId().get(0);
				pf.addCores(n_c);
				d.setNc(n_c);
				d.setOCs(n_OC);
				pf.setId(d.getId());
				listDemands.add(d);
				lreuseFactor++;
				
				/*operational baud-rate re-adjusted, since op_baud-rate_*n*n_c = d_br*/
				double operational_baud_rate = d.getBitRate()/(pf.getSpectralEfficiency()*n_c*n_OC);
				//for statistics
				sum_baud_rate+=operational_baud_rate;
				sum_nOC+=n_OC;
				sumCoresUsed+=n_c;
				c_nfs +=pf.getnumFS();
				//active TRxs network-wide
				net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+n_c*2*n_OC); //x2 (bidirectional connections)
				if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
					net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
				else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
				//active BVT node-wide
				//increment number of active BVT: in src and dst nodes as the connections are bidirectional
				int src = d.getSrcNode().GetId();
				int dst = d.getDstNode().GetId();
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
				for (int i=(int)Math.ceil(operational_baud_rate)-1;i<max_baud_rate;i++)
					cdf_baud_rate.set(i, cdf_baud_rate.get(i)+1);
				
				for (int i=n_c-1;i<Smax;i++)
					cdf_s.set(i, cdf_s.get(i)+1);
				
				for (int i=n_OC-1;i<10;i++)
					cdf_nOC.set(i, cdf_nOC.get(i)+1);
				
				for (int i=n_OC*n_c-1;i<50;i++)
					cdf_nTrx.set(i, cdf_nTrx.get(i)+1);
				//fin
				
				//counter connections that employ specific mod. format
				switch (pf.getSpectralEfficiency()){
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
				
				//reverse path		
				Path pr = null;
				for (Path path:lightpathsr){
					if (path.getArrayId().contains(id)&&(path.GetPath().size()==pf.GetPath().size())){
						int n=pf.GetPath().size();
						boolean f = true;
						for (int j=0;j<n;j++){
							if (pf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
								f = f && true;
							}
							else {
								f = f && false;
								break;
							}
						}
						if (f){
							pr = path;
							break;
						}
					}
				}
				//end
				pr.setId(d.getId());
				pr.addCores(n_c);
				break;
			}
			
		}
		return groom;
	}
	
	public boolean spatialPartialMatchGroomingWithDynamicSChConf(Demand d, DefaultTableModel formats, Network net){
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		boolean groom=false;
		if (net.getLightPaths().isEmpty()) return groom;
		for (Path path:net.getLightPaths()){
			int idsrc=d.getSrcNode().GetId();
			int iddst=d.getDstNode().GetId();
			ArrayList <Integer> list = path.getNodes();
			if (list.contains(idsrc) && list.contains(iddst) && (list.indexOf(idsrc)-list.indexOf(iddst))<0)
				lightpathsf.add(path);
			if (list.contains(idsrc) && list.contains(iddst) && (list.indexOf(idsrc)-list.indexOf(iddst))>0)
				lightpathsr.add(path);
		}
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int id;
		for (Path pf:lightpathsf){
			DefaultTableModel results = computeSChConfiguration (pf, net, d);
			//resources = getResources (pf, formats, d);
			//int n_fs = resources.get(0);
			//int n_c = resources.get(1);
			//int n_OC = resources.get(2);
			ArrayList<Integer> S = (ArrayList<Integer>)results.getValueAt(0,0);
			ArrayList<Integer> Nfs = (ArrayList<Integer>)results.getValueAt(0,1);
			//for (int i=S.size()-1;i>=0;i--){ // descending
			for (int i=0;i<S.size();i++){ // ascending
				if (Nfs.get(i) <= pf.getnumFS()&&(pf.getNumberOfCores()+S.get(i))<=net.getLinks().get(0).getNumberOfCores()){
					groom=true;
					id = pf.getArrayId().get(0);
					pf.addCores(S.get(i));
					d.setNc(S.get(i));
					double t_baud_rate_pca = Nfs.get(i)*12.5-GB; // total operational baud-rate
					int n_OC=0; // number of OC per Sb-Ch (0.- init value)
					n_OC = (int)Math.ceil((double)t_baud_rate_pca/(double)max_baud_rate);
					d.setOCs(n_OC);
					pf.setId(d.getId());
					listDemands.add(d);
					lreuseFactor++;
				
					/*operational baud-rate re-adjusted, since op_baud-rate_*n*n_c = d_br*/
					double operational_baud_rate = d.getBitRate()/(pf.getSpectralEfficiency()*S.get(i)*n_OC);
					//for statistics
					sum_baud_rate+=operational_baud_rate;
					sum_nOC+=n_OC;
					sumCoresUsed+=S.get(i);
					c_nfs +=pf.getnumFS();
					//active TRx network-wide
					net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+S.get(i)*2*n_OC); //x2 (bidirectional connections)
					if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
						net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
					else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
					//active BVT node-wide
					//increment number of active BVT: in src and dst nodes as the connections are bidirectional
					int src = d.getSrcNode().GetId();
					int dst = d.getDstNode().GetId();
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
					for (int j=(int)Math.ceil(operational_baud_rate)-1;j<max_baud_rate;j++)
						cdf_baud_rate.set(j, cdf_baud_rate.get(j)+1);
				
					for (int k=S.get(i)-1;k<Smax;k++)
						cdf_s.set(k, cdf_s.get(k)+1);
					
					for (int l=n_OC-1;l<10;l++)
						cdf_nOC.set(l, cdf_nOC.get(l)+1);
					
					for (int m=n_OC*S.get(i)-1;m<50;m++)
						cdf_nTrx.set(m, cdf_nTrx.get(m)+1);
					//fin
				
					//counter connections that employ specific mod. format
					switch (pf.getSpectralEfficiency()){
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
				
					//reverse path		
					Path pr = null;
					for (Path path:lightpathsr){
						if (path.getArrayId().contains(id)&&(path.GetPath().size()==pf.GetPath().size())){
							int n=pf.GetPath().size();
							boolean f = true;
							for (int j=0;j<n;j++){
								if (pf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
									f = f && true;
								}
								else {
									f = f && false;
									break;
								}
							}
							if (f){
								pr = path;
								break;
							}
						}
					}
					//end
					pr.setId(d.getId());
					pr.addCores(S.get(i));
					break;
				}
			}
			if (groom) break;
		}
		return groom;
	}
	
	public DefaultTableModel spatialPartialMatchGroomingFixed(Demand d, DefaultTableModel formats, Network net){
		//The SCh configuration is equivalent to alpha=1 (ComNet), always minimize n_fs.
		ArrayList <Path> lightpathsf = new ArrayList<Path>(); //candidates
		ArrayList <Path> lightpathsr = new ArrayList<Path>(); //candidates
		DefaultTableModel parameters = new DefaultTableModel(1,2); 
		boolean groom=false;
		parameters.setValueAt(groom, 0, 0);
		parameters.setValueAt(d, 0, 1);

		double requiredBitRate = d.getBitRate();
		double deficitBitRate;
		if (net.getPartialMatchLightPathsWithResources(d).get(0).isEmpty()) return parameters;
		lightpathsf = net.getPartialMatchLightPathsWithResources(d).get(0);
		lightpathsr	= net.getPartialMatchLightPathsWithResources(d).get(1);
		Collections.sort(lightpathsf); // sort candidate light-paths by length
		Collections.sort(lightpathsr);
		int availableSC, deficitSC, id;
		for (Path pf:lightpathsf){
			getResourcesForFixedBaudRate(pf, formats, d);
			availableSC = net.getLinks().get(0).getNumberOfCores()-pf.getNumberOfCores();
			deficitSC = d.getOCs()-availableSC;	
			if (deficitSC<=0){
				//groom OK
				groom = true;
				pf.setnumCores(pf.getNumberOfCores()+d.getOCs()); // 1 OC / Sp-Ch
				d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				d.setNc(d.getOCs()); // 1 OC / Sp-Ch
				pf.setId(d.getId());
				id = pf.getArrayId().get(0);
				Path pr = getReversePath(pf, lightpathsr, id);
				pr.setId(d.getId());
				pr.setnumCores(pr.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				listDemands.add(d);
				lreuseFactor++;
				this.acumBitRate += d.getOCs()*max_baud_rate*pf.getSpectralEfficiency();
				//System.out.println("Demand id: "+d.getId()+" has been served by grooming");
				break;
			}
			else{
				d.setOCs(availableSC);
				pf.setnumCores(pf.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				d.setBw(d.getOCs()*max_baud_rate);
				d.setNc(d.getOCs()); // 1 OC / SC
				listDemands.add(d);
				lreuseFactor++;
				pf.setId(d.getId());
				id = pf.getArrayId().get(0);
				Path pr = getReversePath(pf, lightpathsr, id);
				pr.setId(d.getId());
				pr.setnumCores(pr.getNumberOfCores()+d.getOCs()); // 1 OC / SC
				this.acumBitRate += availableSC*max_baud_rate*pf.getSpectralEfficiency();
				deficitBitRate = requiredBitRate-this.acumBitRate; 
				//System.out.println(d.getBitRate()+" Gbps of demand id: "+d.getId()+" has been served by grooming");
				d = new Demand(d.getSrcNode().GetId(),d.getDstNode().GetId(),deficitBitRate,net);
				//System.out.println("new demand id (splitting): "+d.getId());
			}
		}
		if (this.acumBitRate >= requiredBitRate){
			groom = true;
			parameters.setValueAt(groom, 0, 0);	
		}
		parameters.setValueAt(d, 0, 1);
		return parameters;
	}
	//end Partial-match grooming
	
	public Path getReversePath (Path pf, ArrayList<Path> lightpathsr, int id){
		Path pr = null;
		for (Path path:lightpathsr){
			if (path.getArrayId().contains(id)&&(path.GetPath().size()==pf.GetPath().size())){
				int n=pf.GetPath().size();
				boolean f = true;
				for (int j=0;j<n;j++){
					if (pf.GetPath().get(j).GetDstNode().GetId()==path.GetPath().get(n-1-j).GetSrcNode().GetId()){
						f = f && true;
					}
					else {
						f = f && false;
						break;
					}
				}
				if (f){
					pr = path;
					break;
				}
			}
		}
		return pr;
	}
	public DefaultTableModel computeSChConfiguration (Path pf, Network net, Demand d){
		DefaultTableModel results = new DefaultTableModel(1,2);
		//Compute nc and nfs
		int s1 = net.getLinks().get(0).getNumberOfCores(); // All fibers/cores
		int nfs = 0, nfs_aux=10000000; // init values
		ArrayList<Integer> S = new ArrayList<Integer>(); // Solution space for S
		ArrayList<Integer> Nfs = new ArrayList<Integer>(); // Solution space for nfs
		for (int i=1;i<=s1;i++){
			nfs = (int)Math.ceil(((((double)d.getBitRate()/i)/pf.getSpectralEfficiency())+GB)/(double)12.5);
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
	public ArrayList<Integer> fca (Demand d, Path p, int n, int S, double max_baud_rate){
		double t_baud_rate, operational_baud_rate; //total required baud-rate 
		int flag=0, nOC=0; //check if the resulted baud-rate is lower than max_baud_rate, "0" by default
		ArrayList<Integer> values = new ArrayList<Integer> (2);
		t_baud_rate =  (d.getBitRate()/S)/n;
		if (t_baud_rate>max_baud_rate) flag=1; //if baud-rate is greater than max_baud_rate using all allowable cores/fibres then it is necessary 2 or more OC per Sp-Ch (JoS for S2-SCh)
		int nfs =  (int)Math.ceil((t_baud_rate+GB)/12.5);
		p.setnumFS(nfs);
		p.setnumCores(S);
		nOC = (int)Math.ceil((double)t_baud_rate/(double)max_baud_rate);
		operational_baud_rate=t_baud_rate/nOC;
		p.setBwMod(operational_baud_rate);
		p.setOCs(nOC);
		values.add(nfs);
		values.add(flag);
		return values;
	}
	public void pca (Demand d,Path p,int n,int nfs, double max_baud_rate){
		double t_baud_rate = nfs*12.5-GB; // total operational baud-rate
		double operational_baud_rate=0; //init value
		int nOC=0; // number of OC per Sb-Ch
		int nc = (int)Math.ceil(d.getBitRate()/(t_baud_rate*n));
		p.setnumFS(nfs);
		p.setnumCores(nc);
		nOC = (int)Math.ceil((double)t_baud_rate/(double)max_baud_rate);
		operational_baud_rate=t_baud_rate/nOC;
		
		//operational baud-rate re-adjusted, since op_baud-rate_*n*n_c*n_OC = d_br (S2-SCh)
		operational_baud_rate = d.getBitRate()/(n*nc*nOC);
		p.setBwMod(operational_baud_rate);
		p.setOCs(nOC);
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
			//Only if f=0, then set to path the nd
			if (f) break;
			p.setSpectralEfficiency(n);
			if (t_baudrate=="flexible"){
				if (strategy.equals("FCA")){
					fca (d,p,n,Smax, max_baud_rate);
					//if (values.get(1)==1) break; //if with this path_length (mod. format) the baud-rate should be higher than 32Gbaud to tx in Smax cores/fibers/modulators, then this path p is not a feasible candidate path
				}
				else{
					ArrayList<Integer> values = fca (d,p,n,Smax, max_baud_rate);
					//if (values.get(1)==1) break;
					pca(d,p,n,values.get(0), max_baud_rate);
				}	
			}
			else computeFSs_fixed(d,p);
			pf.add(p);// This path p is a feasible candidate path
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
			//Only if f=0, then set to path the nd
			if (f) break;
			p.setSpectralEfficiency(n);
			if (t_baudrate == "flexible"){
				if (strategy.equals("FCA")){
					fca (d,p,n,Smax,max_baud_rate);
					//if (values.get(1)==1) break; //if with this path_length (mod. format) the baud-rate should be higher than 32Gbaud to tx in Smax cores/fibers/modulators, then this path p is not a feasible candidate path
				}
				else{
					ArrayList<Integer> values = fca (d,p,n,Smax, max_baud_rate);
					//if (values.get(1)==1) break;
					pca(d,p,n,values.get(0), max_baud_rate);
				}
			}
			else computeFSs_fixed(d,p);
			pr.add(p); // This path p is a feasible candidate path
		}
		//
		paths.add(pf);
		paths.add(pr);
		
		return paths;
	}
	public void computeFSs_fixed (Demand d, Path p){
		//int n_par = (int)Math.ceil(Math.ceil(max_baud_rate/3.125)/2)*2;
		//int nfs =  (int)Math.ceil((n_par*3.125+GB)/12.5);
		int n_OC = (int)Math.ceil(d.getBitRate()/(max_baud_rate*p.getSpectralEfficiency()));
		int nfs =  (int)Math.ceil((max_baud_rate+GB)/12.5);
		p.setOCs(n_OC);
		p.setnumFS(nfs);
		//p.setnumCores(Smax); // It is computed and set later
	}
	
	public ArrayList<Integer> getResourcesForFlexibleBaudRate (Path p, DefaultTableModel formats, Demand d){
		ArrayList<Integer> resources = new ArrayList<Integer>(3);
		double t_baud_rate_fca =  (d.getBitRate()/Smax)/p.getSpectralEfficiency();
		int n_fs =  (int)Math.ceil((t_baud_rate_fca+GB)/12.5);
		//n_c computing
		double t_baud_rate_pca = n_fs*12.5-GB; // total operational baud-rate
		int nOC=0; // number of OC per Sb-Ch (0.- init value)
		int n_c = (int)Math.ceil(d.getBitRate()/(t_baud_rate_pca*p.getSpectralEfficiency()));
		nOC = (int)Math.ceil((double)t_baud_rate_pca/(double)max_baud_rate);
		//fin
		resources.add(n_fs);
		resources.add(n_c);
		resources.add(nOC);
		return resources;
	}
	
	public ArrayList<Integer> getResourcesForFixedBaudRate (Path p, DefaultTableModel formats, Demand d){
		ArrayList<Integer> resources = new ArrayList<Integer>(3);
		//int n_par = (int)Math.ceil(Math.ceil(max_baud_rate/3.125)/2)*2;
		int n_OC = (int)Math.ceil(d.getBitRate()/(max_baud_rate*p.getSpectralEfficiency()));
		int nfs =  (int)Math.ceil((max_baud_rate+GB)/12.5);
		d.setOCs(n_OC);
		//p.setnumFS(nfs);
		//p.setnumCores(Smax);
		//fin
		resources.add(nfs);
		//resources.add(n_c);
		resources.add(n_OC);
		return resources;
	}
	
	public boolean accomodateFS(Path pathf,Path pathr, Demand d, int F, ArrayList<ArrayList<Core>> cores,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			//Core and Link spectrum continuity 
			boolean eval= true;
			for (Link l:pathf.GetPath()){
				eval = eval && l.getCores().get(0).getFSs().get(s).getState(); //0: first core
			}
			if (eval){ //continuous constraint
				//If same slots are free in links (continuous constraint), then evaluate if contiguous slots are free (contiguous constraint)
				boolean eval2 = true;
				//Evaluate if consecutive required slots fit in the available spectrum
				for (Link e:pathf.GetPath()){
					for (int q=0;q<numFS;q++){
						eval2 = eval2 && e.getCores().get(0).getFSs().get(s+q).getState(); //0: one core and 1st. core						
					}
				}
				
				if (eval2){ // Contiguous constraint 
					allocate = true; //demand has been served
					totalnumFS+=numFS;//Suma de los FS requeridos para las demandas atendidas
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
					/*
					System.out.println("Demand "+d.getId()+" served: ");
					System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+" Gbps): {");
					System.out.print(src);
					
					for (Link link:pathf.GetPath()){
						System.out.print(","+link.GetDstNode().GetId());
					}
					System.out.print("} ["+pathf.getLength()+" km], Modulation level-code:"+ pathf.getSpectralEfficiency()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
					*/
					/*Network-wide*/
					//increment number of active trx: n_c per each direction, twice as the connections are bidirectional- and per number of OCs per spatial channel - 
					net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()+pathf.getNumberOfCores()*2*pathf.getOCs());
					//fill-up the map of active_trx per each connection event
					if (net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())!=null)
						net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), (Integer)net.getMapOfActiveNumberOfTRX().get(net.getNumberOfActiveTransceivers())+1);
					else net.getMapOfActiveNumberOfTRX().put(net.getNumberOfActiveTransceivers(), 1);
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
					if (t_baudrate == "flexible"){
						d.setNc((int)pathf.getNumberOfCores());
						d.setOCs(pathf.getOCs());
					}
					sum_nOC+=pathf.getOCs();
					sum_baud_rate+=pathf.getBwMod();
					c_nfs +=pathf.getnumFS();
					//counter connections that employ specific mod. format
					switch (pathf.getSpectralEfficiency()){
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
						for (int q=0;q<numFS;q++){
							//Bidirectional and Full-duplex connections. IMPORTANTE: Change the state of correct FS in proper links of both forward and reverse path
							pathf.GetPath().get(i).getCores().get(0).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links
							pathr.GetPath().get(i).getCores().get(0).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links
						
							if (i==0){
								pathf.setFrequencySlot(pathf.GetPath().get(i).getCores().get(0).getFSs().get(s+q));
								pathr.setFrequencySlot(pathr.GetPath().get(i).getCores().get(0).getFSs().get(s+q));
								/*
								if (q<(numFS-1))
									System.out.print((s+q)+",");
								else System.out.print((s+q));
								if ((s+q)>maxIndex)
									maxIndex = (s+q);
								*/
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
					int t = 0;
					for (ArrayList<Core> core:cores){
						if (t<(cores.size()-1))
							System.out.print(core.get(0).getId()+",");
						else System.out.print(core.get(0).getId());
						t++;
					}
					System.out.println("}");
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
