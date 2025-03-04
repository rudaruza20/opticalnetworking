/*
 * Está pensado para any MF/MCF
 */

package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class FullSpectrumSw {
	private int totalnumFS;
	private int maxIndex;
	private int no_Served;
	private double blockedbr;
	private double blockedbr_tr;
	private double blockedbr_spe;
	private double blockedbw;
	private double sumCoresUsed; //counter of number of cores per Spa-SCh (sum of n_c of all served demands)
	private double GB; //Guard-Band width
	private int Smax; //Max. Number of cores/fibers/transceivers allowed to use per Spa-SCh
	private double max_baud_rate; //Max. operational baud-rate of the optical modulators 
	private Integer active_trx_counter;
	private int sum_nOC; // counter of number of OCs per spatial channel (sum of n_OC per spatial channel of all served demands)
	private double sum_baud_rate; //counter of baud-rate values (sum of baud-rates of all served demands) 
	private ArrayList<Integer> cdf_s;
	private ArrayList<Integer> cdf_baud_rate;
	private ArrayList<Integer> cdf_nOC; //CDF for n_OC per core (spatial channel)
	private ArrayList<Integer> cdf_nTrx; //CDF for n_TRx per SCh
	private int lreuseFactor; //counter for lightpath reuse
	private int c_nfs; // counter for the number of FS 
	//counter of connections that employ a one specific modulation format.
	private int n_64q;   
	private int n_16q;
	private int n_q;
	private int n_b;
	private String t_baudrate;// 14072022: Type of Baudrate {flexible | fixed}
	public FullSpectrumSw () {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		blockedbr_spe = 0;
		sumCoresUsed = 0;
		active_trx_counter = 0;
	}
	public FullSpectrumSw (double GB, int Smax, double max_baud_rate, String t_baudrate) {
		totalnumFS=0;
		maxIndex=0;
		no_Served=0;
		blockedbr=0;
		blockedbr_tr = 0;
		blockedbr_spe = 0;
		blockedbw = 0;
		sumCoresUsed = 0;
		this.GB=GB;
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
		n_64q=0;
		n_16q=0;
		n_q=0;
		n_b=0;
		lreuseFactor=0;
		c_nfs = 0;
		this.t_baudrate = t_baudrate;
		
	}
	public int getNumberOfNoServedDemands(){
		return this.no_Served;
	}
	public double getBlockedBitRate(){
		return this.blockedbr;
	}
	public double getBlockedBw(){
		return this.blockedbw;
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
	public void execute (Network net, Demand d, DefaultTableModel formats, int F){
		ArrayList<Path> kshortestPath;
		boolean e2e_grooming; 
		e2e_grooming = spectralGroomingWithPartialMatch (d,formats,net,F);
		//e2e_grooming = spectralGroomingWithPartialMatch (d,formats,net,F);
		if (e2e_grooming) return;
		ArrayList<ArrayList<Path>> allshortestPath =  computeCandidatePaths(d.getSrcNode(), d.getDstNode(), formats, d, F, net);
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
		for (Path pathf:kshortestPath){
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
			//3: Spectrum assignment
			//if (t_baudrate=="fixed") d.setBitRate(d.getOCs()*max_baud_rate*pathf.getSpectralEfficiency());
			b = accomodate_new(pathf,pathr,d,F,net); 
			if (b){
				if (t_baudrate=="fixed") d.setBitRate(pathf.getOCs()*max_baud_rate*pathf.getSpectralEfficiency());
				pathf.setId(d.getId());
				pathr.setId(d.getId());
				net.addLightPath(pathf);
				net.addLightPath(pathr);
				break;
			}
		}
		if (!b){
			//System.out.println("Demand "+d.getId()+ "("+d.getSrcNode().GetId()+"->"+d.getDstNode().GetId()+","+d.getBitRate()+"Gbps) has not been served due to limited frequency slots"+". Total number of no served Demands: "+(++this.no_Served));
			this.no_Served++;
			this.blockedbr+=d.getBitRate();
			this.blockedbr_spe += d.getBitRate();
			this.blockedbw += d.getBw();
		}
		return;
	}
	public boolean spectralGroomingWithFullMatch(Demand d, DefaultTableModel formats, Network net, int F){
		//e2e spectral traffic grooming 
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
			//find out the reverse path	
			id = pf.getArrayId().get(0); //one additional control (pr serves the same demand as pf)
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
			if (t_baudrate=="fixed"){
				computeFSs_fixed(d,pf);
				computeFSs_fixed(d,pr);
				//d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				
			}
			else{
				computeFSs_flexible(d,pf);
				computeFSs_flexible(d,pr);
			}
			groom = accomodateByReusing (pf, pr, d, F, net);
			if (groom){
				if (t_baudrate=="fixed") d.setBitRate(pf.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				pf.setId(d.getId());
				pr.setId(d.getId());
				lreuseFactor++;
				//System.out.println("Groom OK: "+pf.getArrayId().contains(d.getId())+"-->"+d.getId()+" - "+d.getSrcNode().GetId()+" - "+d.getDstNode().GetId());
				break;
			}
		}		
		return groom;
	}
	
	public boolean spectralGroomingWithPartialMatch(Demand d, DefaultTableModel formats, Network net, int F){
		//spectral traffic grooming where scr and dst are contained in the physical path of current light-path
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
			//find out the reverse path	
			id = pf.getArrayId().get(0); //one additional control (pr serves the same demand as pf)
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
			if (t_baudrate=="fixed"){
				computeFSs_fixed(d,pf);
				computeFSs_fixed(d,pr);
				//System.out.println("Number of OCs: "+d.getOCs());
				//d.setBitRate(d.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				
			}
			else{
				computeFSs_flexible(d,pf);
				computeFSs_flexible(d,pr);
			}
			groom = accomodateByReusing (pf, pr, d, F, net);
			if (groom){
				if (t_baudrate=="fixed") d.setBitRate(pf.getOCs()*max_baud_rate*pf.getSpectralEfficiency());
				pf.setId(d.getId());
				pr.setId(d.getId());
				lreuseFactor++;
				//System.out.println("Groom OK: "+pf.getArrayId().contains(d.getId())+"-->"+d.getId()+" - "+d.getSrcNode().GetId()+" - "+d.getDstNode().GetId());
				break;
			}
		}		
		return groom;
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
	
	public void computeFSs_fixed (Demand d, Path p){
		int n_par = (int)Math.ceil(Math.ceil(max_baud_rate/3.125)/2)*2;
		int n_OC = (int)Math.ceil(d.getBitRate()/(max_baud_rate*p.getSpectralEfficiency()));
		int nfs =  (int)Math.ceil((n_OC*n_par*3.125+GB)/12.5);
		p.setOCs(n_OC);
		p.setnumFS(nfs);
		p.setnumCores(Smax);
	}
	
	public void computeFSs_flexible (Demand d, Path p){
		int nfs = (int)Math.ceil((((double)d.getBitRate()/p.getSpectralEfficiency())+GB)/(double)12.5);
		p.setnumFS(nfs);
		p.setnumCores(Smax);
	}
	
	public ArrayList<ArrayList<Path>> computeCandidatePaths(Node srcnode, Node dstnode, DefaultTableModel formats, Demand d, int F, Network net){
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
			if (t_baudrate=="fixed") computeFSs_fixed(d,p);
			else computeFSs_flexible(d,p);
			pf.add(p); // This path p is a feasible candidate path
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
			if (t_baudrate=="fixed") computeFSs_fixed(d,p);
			else computeFSs_flexible(d,p);
			pr.add(p); // This path p is a feasible candidate path	
		}
		//
		paths.add(pf);
		paths.add(pr);
		
		return paths;
	}
	public ArrayList<Integer> getResources (Path p, DefaultTableModel formats, Demand d){
		//it is not being used - candidate to remove or it can be used to compute number OCs in fixed baudrate (d.setOCs(nOC))
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
	
	
	public boolean accomodate_new(Path pathf,Path pathr, Demand d, int F,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path
		if (F<numFS) return allocate;
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			ArrayList<ArrayList<Core>> cores =  new ArrayList<ArrayList<Core>>(); //candidate cores per link to serve demand
			//spectrum continuity 
			boolean eval= true;
			//Evaluate in all cores per link - output: candidate cores per link to serve demand. One core is candidate as long as it hasn't allocated any FSs yet (load == 0)
			for (Link l:pathf.GetPath()){
				ArrayList<Core> core = new ArrayList<Core>();
				for (Core c:l.getCores()){
					if (c.getFSs().get(s).getState() && c.getNumberOfFsInUse()==0){ //(b). FF Spectrum Assignment Policy per core (per link)
						core.add(c); //one core (sp-ch) is enough for full-spectrum switching if FF policy is considered (see line 423-426)
						break;
					}
				}
				if (core.size()<pathf.getNumberOfCores()) { // 1 core (sp-ch) per lightpath in the case of Full-spectrum switching
					eval = false;
					break;
				}	
				else cores.add(core);
			}
			if (eval){ //Check if continuous constraint is fulfilled
				//if wavelength continuity constraint is fulfilled, the wavelength contiguity constraint will be fulfilled as well because the core is free (load == 0)
				//Select the nc first cores (FF policy) and remove the rest of the cores not needed per link e 
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
				/*
				System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+" Gbps): {");
				System.out.print(src);
				for (Link link:pathf.GetPath()){
					System.out.print(","+link.GetDstNode().GetId());
				}
				System.out.print("} ["+pathf.getLength()+" km], Modulation level-code:"+ pathf.getSpectralEfficiency()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
				*/
				d.setNc((int)pathf.getNumberOfCores());
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
				ArrayList<FrequencySlot> feqSlotsfw = new ArrayList<FrequencySlot> (); //new 12062022
				ArrayList<FrequencySlot> feqSlotsrw = new ArrayList<FrequencySlot> (); //new 12062022
				for (int i=0;i<pathf.GetPath().size();i++){
					for (int j=0;j<cores.get(i).size();j++){
						for (int q=0;q<numFS;q++){
							pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q).setOccupation(false);
							pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q).setOccupation(false); 
							//aggregate to ligthPath the frequency slots used
							if (i==0){
								if (j==0){
									// pathf.setFrequencySlot(pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q));
									// pathr.setFrequencySlot(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q));
									feqSlotsfw.add(pathf.GetPath().get(i).getCores().get(cores.get(i).get(j).getId()).getFSs().get(s+q));
									feqSlotsrw.add(pathr.GetPath().get(i).getCores().get(cores.get(pathr.getHops()-1-i).get(j).getId()).getFSs().get(s+q));
									pathf.addFrequencySlot(d, feqSlotsfw);
									pathr.addFrequencySlot(d, feqSlotsrw);
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
					//System.out.println("Core id fw: "+cores.get(i).get(0).getId());
					//System.out.println("Core id rw: "+cores.get(pathr.getHops()-1-i).get(0).getId());
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
				*/
				//System.out.println("}");
				
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
		// if (!allocate) System.out.println("Demand "+d.getId()+" has not been served in this candidate path, plength: "+pathf.getLength()+"Km");
		return allocate;
	}
	
	public boolean accomodateByReusing (Path pathf,Path pathr,Demand d,int F,Network net){
		boolean allocate=false;
		int numFS =  (int)pathf.getnumFS(); // numFS of the selected path 
		for (int s=0;s<=(F-numFS);s++){   // fs from 0 to (F-nd) to avoid there is not lack of space in the spectrum. F.- Is the Spectrum Capacity and nd.- Is the number of slots required by demand(i)
			//Spectrum continuity 
			boolean eval= true;
			int z=0;
			for (Link l:pathf.GetPath())
				eval = eval && l.getCores().get(pathf.getCores().get(z++).get(0).getId()).getFSs().get(s).getState();  // 0: unique core of lightpath pathf - upgrade to G in case of uncoupled group of cores MCFs
			if (!eval) continue; // Continuity constraint is not fulfilled 
			//If same slots are free in links (continuous constraint), then evaluate if contiguous slots are free (contiguous constraint)
			boolean eval2 = true;
			//Contiguity constraint: evaluate if consecutive required slots fit in the available spectrum
			int y=0;
			for (Link e:pathf.GetPath()){
				for (int q=0;q<numFS;q++){
					eval2 = eval2 && e.getCores().get(pathf.getCores().get(y).get(0).getId()).getFSs().get(s+q).getState(); //0: one core and 1st. core
					if (!eval2) break;
				}
				if (!eval2) break;
				y++;
			}
			if (!eval2) continue; // Contiguity constraint is not fulfilled 
			allocate = true; //demand has been served
			totalnumFS+=numFS; //Suma de los FS requeridos para las demandas atendidas
			int src = pathf.GetPath().get(0).GetSrcNode().GetId();
			int dst = pathf.GetPath().get(pathf.GetPath().size()-1).GetDstNode().GetId();
			/*
			System.out.println("Demand "+d.getId()+" served: ");
			System.out.print("\t Spectrum Path ("+src+"->"+dst+","+d.getBitRate()+" Gbps): {");
			System.out.print(src);	
			for (Link link:pathf.GetPath())
				System.out.print(","+link.GetDstNode().GetId());	
			System.out.print("} ["+pathf.getLength()+" km], Modulation level-code:"+ pathf.getSpectralEfficiency()+", #Cores:"+pathf.getNumberOfCores()+", with following ("+numFS+") Frequency Slots: {");
			*/
			d.setNc((int)pathf.getNumberOfCores());
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
			//reserve spectrum portions in the forward and reverse paths
			ArrayList<FrequencySlot> feqSlotsfw = new ArrayList<FrequencySlot> (); //new 12062022
			ArrayList<FrequencySlot> feqSlotsrw = new ArrayList<FrequencySlot> (); //new 12062022
			for (int i=0;i<pathf.GetPath().size();i++){
				//System.out.println("Core id fw: "+pathf.getCores().get(i).get(0).getId());
				//System.out.println("Core id rw: "+pathr.getCores().get(i).get(0).getId());
				for (int q=0;q<numFS;q++){
					//Bidirectional and Full-duplex connections. IMPORTANTE: Change the state of correct FS in proper links of both forward and reverse path
					pathf.GetPath().get(i).getCores().get(pathf.getCores().get(i).get(0).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Forward Links
					pathr.GetPath().get(i).getCores().get(pathr.getCores().get(i).get(0).getId()).getFSs().get(s+q).setOccupation(false);// FS(s) is allocated in Reverse Links
					//System.out.println("Core-id to set-up: "+pathr.getCores().get(i).get(0).getId());
					if (i==0){
						feqSlotsfw.add(pathf.GetPath().get(i).getCores().get(pathf.getCores().get(i).get(0).getId()).getFSs().get(s+q));
						feqSlotsrw.add(pathr.GetPath().get(i).getCores().get(pathr.getCores().get(i).get(0).getId()).getFSs().get(s+q));
						
						/*
							if (q<(numFS-1))
								System.out.print((s+q)+",");
							else System.out.print((s+q));
							if ((s+q)>maxIndex)
								maxIndex = (s+q);
						*/
					}	
				}	
				if (i==0){
					pathf.addFrequencySlot(d, feqSlotsfw);
					pathr.addFrequencySlot(d, feqSlotsrw);
				}
			}	
			break;	
		}		
		//if (!allocate) System.out.println("Demand "+d.getId()+" has not been served in this candidate path, plength: "+pathf.getLength()+"Km");
		return allocate;
	}
}
