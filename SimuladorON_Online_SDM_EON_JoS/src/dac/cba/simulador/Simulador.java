package dac.cba.simulador;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

	interface Comparables {
	    boolean lessThan(Comparables y);
	}

	abstract class  AbstractEvent implements Comparables {
	    abstract void execute(AbstractSimulator simulator);
	}

	abstract class OrderedSet {
	    abstract void insert(Comparables x);
	    abstract Comparables  removeFirst();
	    abstract int size();
	    abstract Comparables remove(Comparables sx);
	}
	    
	class AbstractSimulator {
	    OrderedSet events;
	    void insert(AbstractEvent e) {
	        events.insert(e);
	    }
	    AbstractEvent cancel(AbstractEvent e)  {
	        throw new java.lang.RuntimeException("Method not implemented");
	    }
	}
	abstract class Event extends AbstractEvent {
	    double time;
	    public boolean lessThan(Comparables y) {
	        Event e = (Event) y;  // Will throw an exception if y is not an Event
	        return this.time < e.time;
	    }
	}

	class Simulator extends AbstractSimulator {
	    double time;
	    double now() {
	        return time;
	    }
	    void doAllEvents() {
	        Event e;
	        while ( (e= (Event) events.removeFirst()) != null) {
	            time = e.time;
	            //System.out.print("At time "+time);
	            e.execute(this);
	        }
	    }
	}

	class ListQueue extends OrderedSet {
		java.util.Vector elements = new java.util.Vector();
		void insert(Comparables x) {
			int i = 0;
			while (i < elements.size() && ((Comparables) elements.elementAt(i)).lessThan(x)) {
					i++;
			}
			elements.insertElementAt(x,i);
		}
		
		Comparables removeFirst() {
			if (elements.size() ==0) return null;
			Comparables x = (Comparables) elements.firstElement();
			elements.removeElementAt(0);
			return x;
		}
		
		Comparables remove(Comparables x) {
			for (int i = 0; i < elements.size(); i++) {
				if (elements.elementAt(i).equals(x)) {
					Object y = elements.elementAt(i);
					elements.removeElementAt(i);
					return (Comparables) y;
				}
			}
			return null;
		}
		public int size() {
			return elements.size();
		}
	}	

	class RandomTime {
		static double exponential(double mean) {
	        return - mean * Math.log(Math.random());
	    }
	    static boolean bernoulli(double p) {
	    	return Math.random() < p;
	    }
	    /* .. and other distributions */
	}

	
	
	/**
	* Generate a stream of demands for X time units.
	*/
	class Generator extends Event {
		int simulationTime;
	    Network net;
	    DefaultTableModel formats;
	    int capacity; //spectrum capacity by core
	    double load;
	    ArrayList<Double> intervals;
	    ArrayList<Integer> d_r; //demand requirements
	    double totalbr;
	    double totalbw;
	    boolean MF; //type of fiber MF=1 MCF=0
	    String alpha; //weight for number of FS, (1-alpha) weight for number of Sb-Ch
	    double guard_band;
	    String strategy;
	    double bbp;
	    double max_baud_rate;
	    String ROADMType;
	    String t_baudrate; //22072022
	    File file;
	    FileWriter writer;
	    HFF_SpatialStrategy_SpaSCh_JoS hffspatial;
	    /**
	    * Create a new Demand and
	    * schedule the creation of the next demand
	    */
	    Generator (Network net, DefaultTableModel formats, int F, int elapsed, double load, DefaultTableModel trafficProfile, boolean MF, String alpha, double GB, String strategy, int Smax, int G, double max_baud_rate, String ROADMType, String groomingStrategy, String t_baudrate){
	    	this.simulationTime = elapsed; 
	    	this.net = net;
	    	this.formats = formats;
	    	this.capacity= F;
	    	this.load = load;
	    	this.totalbr = 0;
	    	this.totalbw = 0;
	    	this.MF = MF;
	    	this.alpha= alpha;
	    	this.guard_band=GB;
	    	this.strategy=strategy;
	    	this.bbp=0;
	    	this.max_baud_rate = max_baud_rate;
	    	this.ROADMType = ROADMType;
	    	this.t_baudrate = t_baudrate;
	    	this.file = new File ("throughput"+Smax+".txt");
	    	ArrayList<Double> probabilities = new ArrayList<Double> (trafficProfile.getRowCount());
	    	intervals = new ArrayList<Double> (trafficProfile.getRowCount());
	    	d_r = new ArrayList<Integer> (trafficProfile.getRowCount());
	    	for (int j=0;j<trafficProfile.getColumnCount();j++){
	    		for (int i=0;i<trafficProfile.getRowCount();i++){
	    			if (j==0) d_r.add(Integer.parseInt((String)trafficProfile.getValueAt(i,j)));
	    			else{
	    				double prob;
	    				String s = (String)trafficProfile.getValueAt(i, j);
	    				if (s.contains("/")){
	    					Scanner sl = new Scanner(s);
	    					sl.useDelimiter("\\s*/\\s*");
	    					ArrayList<Integer> values = new ArrayList<Integer> (2);
	    					while (sl.hasNext()){
	    						values.add(Integer.parseInt(sl.next()));
	    					}
	    					prob = (double)values.get(0)/(double)values.get(1);
	    				}
	    				else prob = Double.parseDouble(s);
	    				probabilities.add(prob);
	    			}
	    		}
	    	}
	    	
	    	double sum=0;
	    	for (Double prob:probabilities){
	    		intervals.add(sum+prob);
	    		sum+=prob;
	    	}
	    	
	    	/*
	    	try {
				this.file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	try {
				 this.writer = new FileWriter(this.file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
	    	
	    	hffspatial = new HFF_SpatialStrategy_SpaSCh_JoS (this.guard_band, this.strategy, Smax, max_baud_rate, groomingStrategy, t_baudrate);
	    	
	    	
	    }
	    void execute(AbstractSimulator simulator) {
	    	
	    	Demand demand = new Demand(net, intervals, d_r, max_baud_rate, t_baudrate);
	    	//Demand demand = new Demand(2,3,6,net,max_baud_rate);
	    	//this.totalbr+=demand.getBitRate(); //demand bit-rate may change during allocation in the case of fixed baudrate. So, we can move line 203 and 204 after allocation execution (line 219 and 220)
	    	//this.totalbw+=demand.getBw();
	        
	        int bNoServed = hffspatial.getNumberOfNoServedDemands();
	        //long t1 = System.currentTimeMillis(); //To measure the time of heuristic execution
	        hffspatial.execute(this.net, demand, this.formats, this.capacity, this.MF);
	        //long t2 = System.currentTimeMillis();
	        //long time = t2-t1;
	        //System.out.println(time/1000.0);
	        
	        int aNoServed = hffspatial.getNumberOfNoServedDemands();
	        double blockedbr = hffspatial.getBlockedBitRate();
	        double servedbr = hffspatial.getServedBitRate();//new 18022025
	        double blockedbr_tr = hffspatial.getTRBlockedBitRate();
	        double blockedbr_spe = hffspatial.getSpeBlockedBitRate();
	        double blockedbw = hffspatial.getBlockedBw();
	        this.totalbr+=servedbr;
	    	//this.totalbw+=demand.getBw(); ?? check when it measures be necessary
	        //System.out.println("served bit rate: "+servedbr+" Gbps");
			//Schedule release event if demand was served
	        //if (bNoServed == aNoServed){
	        	for (Demand d:hffspatial.getListOfDemands()){
	        		net.instantaneusCarriedBitRate+=2*d.getBitRate();    
	        		net.cummulativeCarriedBitRate+=net.instantaneusCarriedBitRate;
	        		net.measuredTimes++;;
	        		ReleaseConn release = new ReleaseConn(this.net, this.load, this.simulationTime, this.writer);
	        		release.insert(simulator, d, this.load);
	        		//System.out.println("Demand id scheduled to be released: "+d.getId());
	        	}
	        //}
	        //else {
	        	//System.out.printf("Blocking Factor: %.5f \n",((double)aNoServed/((double)demand.getId()+1))); 
	        	//System.out.printf("Bandwidth-Blocking Factor: %.5f \n",((double)blockedbr/(double)this.totalbr));
	        //}
	        //Schedule new arrival demand event
	        time += RandomTime.exponential(1.0); // Load = holding/inter-arrival= 20/1 (20:1)
	        if (time < this.simulationTime){ /*System.out.println("Next demand is scheduled at "+time+" [time]");*/simulator.insert(this); }
	        else{
	        	// Print-out key-value pairs (number_of_act._trxs, as they are sampled in the CDF only after each connection event - not after release -)
	        	//Network-wide
	        	System.out.println("CDF_TRXs");
	        	Set keys = net.getMapOfActiveNumberOfTRX().keySet();
	        	for (Iterator i = keys.iterator(); i.hasNext();) {
	        	     Integer key = (Integer) i.next();
	        	     Integer value = (Integer) net.getMapOfActiveNumberOfTRX().get(key);
	        	     System.out.println(key + "\t" + value);
	        	}
	        	// Print-out key-value pairs (number_of_act. bvtxps) Network-wide
	        	System.out.println("CDF_BVTXP");
	        	Set keys2 = net.getMapOfActiveNumberOfBVTXP().keySet();
	        	for (Iterator i = keys2.iterator(); i.hasNext();) {
	        	     Integer key2 = (Integer) i.next();
	        	     Integer value2 = (Integer) net.getMapOfActiveNumberOfBVTXP().get(key2);
	        	     System.out.println(key2 + "\t" + value2);
	        	}
	        	// Print-out key-value pairs (number_of_act. bvtxps) Node-wide
	        	System.out.println("CDF_BVTXP");
	        	for (Node node:net.getNodes()){
	        		System.out.println("Node: "+node.GetId());
	        		Set keys3 = node.getMapOfActiveNumberOfBVT().keySet();
	        		for (Iterator i = keys3.iterator(); i.hasNext();) {
	        			Integer key3 = (Integer) i.next();
	        			Integer value3 = (Integer) node.getMapOfActiveNumberOfBVT().get(key3);
	        			System.out.println(key3 + "\t" + value3);
	        		}
	        	}
	        	//new-02072022
	        	System.out.println("Number of Reused Lightpaths: "+hffspatial.getCounterOfReusedLightpaths());
	        	//
	        	//fin
	        	//Print-out first results.- output format:
	        	//overall BP \t overall Avg. cores/fibers under use per SCh \t Avg. n_OCs per Sb-Ch \t Avg. baud-rate per Sb-Ch per Spa-SCh \t Avg. number of FS per connection \t lreuseFactor \tAvg. Number of connections that employ 64-QAM \t Avg. Number of connections that employ 16-QAM \t Avg. Number of connections that employ 16-QAM \t Avg. Number of connections that employ QPSK \t Avg. Number of connections that employ BPSK 
	        	
	        	System.out.printf("%.5f \t",((double)aNoServed/((double)demand.getId()+1)));
	        	//System.out.printf("Global Blocking-Bandwidth-Probability(BBP): %.5f \n",((double)blockedbr/((double)this.totalbr)));
	        	//System.out.printf("Total_TR Blocking-Bandwidth-Probability(BBP): %.5f \n",((double)blockedbr_tr/((double)this.totalbr)));//OJO pending add Core (LC)
	        	//System.out.printf("Total_SPE Blocking-Bandwidth-Probability(BBP): %.5f \n",((double)blockedbr_spe/((double)this.totalbr)));// OJO pending add Core (LC)
	        	//System.out.println("Total Generated BitRate: "+totalbr+" Gbps");
	        	System.out.printf("%.2f\t",(double)hffspatial.getNumberOfCoresUsed()/(double)(demand.getId()+1-aNoServed)); //Only for Spa-SCh // AVG=sum(CoresUnderUse)/Demands_Served
	        	
	        	//last version for both JoS and InS
	        	System.out.printf("%.5f\t",hffspatial.getNumberOfOC()/(demand.getId()+1-aNoServed)); //Avg. OC per spatial channel (or Spe-SCh BVTXP)
	        	System.out.printf("%.2f\t",hffspatial.getTotalBaudrate()/(demand.getId()+1-aNoServed)); //Avg. baud-rate per spatial channel (or Spe-SCh BVTXP)
	        	System.out.printf("%.2f\t",(double)hffspatial.getCounterOfNumberOfFSs()/(double)(demand.getId()+1-aNoServed)); //Avg. number of FS per Connection
	        	//System.out.printf("%.2f\t",(double)hffspatial.getCounterOfReusedLightpaths()/(double)(demand.getId()+1-aNoServed)); //Ligthpath Reuse Factor.- Only for JoS with Spa grooming
	        	System.out.printf("%.2f\t",(hffspatial.getNumberOfTXP64QAM()/(demand.getId()+1-aNoServed))*100); //Avg. % of BV-TXP@64QAM
	        	System.out.printf("%.2f\t",(hffspatial.getNumberOfTXP16QAM()/(demand.getId()+1-aNoServed))*100); //Avg. % of BV-TXP@16QAM
	        	System.out.printf("%.2f\t",(hffspatial.getNumberOfTXPQPSK()/(demand.getId()+1-aNoServed))*100); //Avg. % of BV-TXP@QPSK
	        	System.out.printf("%.2f\n",(hffspatial.getNumberOfTXPBPSK()/(demand.getId()+1-aNoServed))*100); //Avg. % of BV-TXP@BPSK	
	        	//fin
	        	
	        	this.bbp=(double)blockedbr/((double)this.totalbr);
	        	//11022025: line 296 and 297 in case of expressing the bit-rate in terms of n_OCs for fixed baudrate
	        	//if (t_baudrate=="fixed") this.bbp=(double)blockedbw/((double)this.totalbw);
	        	//else this.bbp=(double)blockedbr/((double)this.totalbr);
	        	demand.setUniqueId(0); //Reset demandId to 0 for consecutive simulations (different loads)
	        	// view CDF of baud-rate, n_s, n_OC and alpha
	        	for (Integer i:hffspatial.getCDF_nc())
	        		System.out.printf("\t"+i);
	        	System.out.println();
	        	
	        	for (Integer i:hffspatial.getCDF_baud_rate())
	        		System.out.printf("\t"+i);
	        	System.out.println();
	        	
	        	for (Integer i:hffspatial.getCDF_nOC())
	        		System.out.printf("\t"+i);
	        	System.out.println();
	        	
	        	for (Integer i:hffspatial.getCDF_nTRx())
	        		System.out.printf("\t"+i);
	        	System.out.println();
	        	
	        	//For InS and CCC-ROADM
	        	/*
	        	for (Integer i:hffspatial.getCDF_alpha())
	        		System.out.printf("\t"+i);
	        	System.out.println();
	        	*/
	        	
	        	// end
	        	System.out.println("Number of Reused Lightpaths: "+hffspatial.getCounterOfReusedLightpaths());
	        }
	    }
	}

	
	class ReleaseConn extends Event {
		private Demand connBeingReleased;
		Network net;
		private double load;
		private double simulationTime;
		private FileWriter writer;
		private int x;
		ReleaseConn (Network network, double load, double simulationTime, FileWriter writer){
			this.net=network;
			this.load= load;
			this.simulationTime = simulationTime;
			this.writer = writer;
			this.x = 0; //in order to only considerate one event the release of the forward and the reverse path
		}
		void execute(AbstractSimulator simulator) {
			int i=0, r=0; //i and r: Avoid printout twice due to reverse path
			//System.out.print("connection established for demand "+connBeingReleased.getId()+" ("+connBeingReleased.getSrcNode().GetId()+"->"+connBeingReleased.getDstNode().GetId()+") was released with following Set-Of-fs(id)= {");
			int n = net.getLightPaths().size();
	    	//for (Path path:net.getLightPaths()){
			for (int z=0;z<n;z++){
				if (this.x ==2) break;
				Path path = net.getLightPaths().get(z);
				if (path.getArrayId().contains(connBeingReleased.getId())){
					this.x++;
					//Release transmission devices after each release event.
					//for number of trxs (network-wide): n_c per each direction x n_OC per core/fibre
						net.setNumberOfActiveTransceivers(net.getNumberOfActiveTransceivers()-connBeingReleased.getnumCores()*connBeingReleased.getOCs());
					//for number of bv-txp (network-wide): number of bv-txp (Spe or Spa) is equal to the number of n_c per SCh or n_OC per core, respectively. 
						if (connBeingReleased.getnumCores()>connBeingReleased.getOCs()) net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()-connBeingReleased.getOCs());
						else net.setNumberOfActiveBVTXP(net.getNumberOfActiveBVTXP()-connBeingReleased.getnumCores());
					//end
					//release BVT per node: Only SRC because individual lightpaths (fw and rw) are liberated once per time according to the demand id 
						net.getNodes().get(path.getSrcNode().GetId()).setIncrementNumberOfActiveBVT(-1);
					//end
					//release TRXs per node:  Only SRC because individual lightpath (fw and rw) are liberated once per time according to the demand id
					//In order to compute the peak number of TRxs per node	
						net.getNodes().get(path.getSrcNode().GetId()).setIncrementNumberOfActiveTrx(-1*path.getNumberOfCores()*path.getOCs());
					//end	
					//System.out.println("Demand id "+connBeingReleased.getId()+" is being released");
					path.addCores(-1*connBeingReleased.getnumCores());
					int index = path.getArrayId().indexOf(connBeingReleased.getId());
					path.getArrayId().remove(index);
					if (this.x==1){
						net.instantaneusCarriedBitRate-=2*connBeingReleased.getBitRate();
						/*
						try {
							this.writer.write(((Simulator)simulator).now()+"\t"+net.instantaneusCarriedBitRate+"\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						*/
						//if (((Simulator)simulator).now()>this.load*2 && ((Simulator)simulator).now()<this.simulationTime){ //it suppresses both transients at the beginning and at the ending of the simulation
						net.cummulativeCarriedBitRate+=net.instantaneusCarriedBitRate;
						net.measuredTimes++;
						//}
					}
					//Release FSs and LightPaths - 22072022 For fixed baudrate --> pending to change by using number of OCs (numbers of subchanels * spatial channels). Number of subchannels = ceil (number of required OCs / number of spatial channels)
					if (path.getNumberOfCores()==0){
					//System.out.print("number of occupied cores for lightpath being released: "+path.getNumberOfCores()+" ");
					int k=0; //Index of link e to obtain coreList per link
					for (Link link:path.GetPath()){
						int j=0; //printout fs id with "," or not
						for (Core core:path.getCores().get(k)){
							for (FrequencySlot fs:path.getFreqSlots()){
								// IMPORTANTE: Take the correct core in the forward and reverse path to liberate the properly FS. 
								//System.out.println("FS state: "+link.getCores().get(core.getId()).getFSs().get(fs.getId()).getState());
								link.getCores().get(core.getId()).getFSs().get(fs.getId()).setOccupation(true); // release fs(i) in each link (core) in both forward and reverse path (free= true)
								//System.out.println("FS state: "+link.getCores().get(core.getId()).getFSs().get(fs.getId()).getState());
								
								if (i==0)
								{	
									/*
									if (j<(path.getFreqSlots().size()-1))
										System.out.print(fs.getId()+",");
									else System.out.println(fs.getId()+"}");
									*/
								}
								j++;
							}
							i++;
						}
						k++;
					}
					net.getLightPaths().remove(z);
					//System.out.println("Lightpath "+z+" for demand "+connBeingReleased.getId()+" was released");
					n--;
					z--;
					}
					//Printout List of ALL cores per each link used by LightPath i, which it is being released.
					/*
					if (r==0){
						System.out.print(" in Core(id)-set= {");
						int t = 0;
						for (ArrayList<Core> core:path.getCores()){
							System.out.print(core.get(0).getId());
							for (int u=1;u<core.size();u++){
								System.out.print("-"+core.get(u).getId());
							}
							if (t<(path.getCores().size()-1))
								System.out.print(",");
							t++;
						}
						System.out.print("}");
					}
					r++;
					*/
				}
			}
	    	//System.out.println();
			if(simulator.events.size()==0){
				boolean test = false;
	        	for (Link e:net.getLinks()){
	        		for (Core c:e.getCores()){
	        			if (c.getNumberOfFsInUse()!=0){
	        				//System.out.println("Link: "+e.GetSrcNode().GetId()+"-->"+e.GetDstNode().GetId()+" Core: "+c.getId()+" "+c.getNumberOfFsInUse()+" slots ocupados");
	        				test=true;
	        				//break;
	        			}
	        		}
	        		//if (test) break;
	        	}
	        	System.out.println("Net with load: "+test);
			}
		}
		void insert(AbstractSimulator simulator, Demand demand, double load) {
	        this.connBeingReleased = demand;
	        double releaseTime = RandomTime.exponential(load);
	        time = ((Simulator)simulator).now() + releaseTime;
	        //System.out.println("Release event for demand "+connBeingReleased.getId()+" is scheduled at "+time+" [time]");
	        simulator.insert(this);
	    }
	}
	
	// Fin




public class Simulador extends Simulator {

	public Simulador() {
		// TODO Auto-generated constructor stub
	}
	public static DefaultTableModel ReadFile (String Path, boolean b){
		//boolean b -->"1" Square matrix, "0" Not square Matrix
		File f = new File(Path);
		Integer row=0, column=0; /*Matrix Dimension row*column */
		Integer i=0,j=0;
		DefaultTableModel table = new DefaultTableModel();
		try {
			Scanner s = new Scanner(f);
			while (s.hasNextLine()) {
				String linea = s.nextLine();
				row++;
			}
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (b){
			column = row;
			table = new DefaultTableModel(row, column);
		}
		else {
			try{
			Scanner s = new Scanner(f);
			String linea = s.nextLine();
			Scanner sl = new Scanner(linea);
			sl.useDelimiter("\\s*,\\s*");
			while (sl.hasNext()){
				sl.next();
				column++;
			}
			table = new DefaultTableModel(row, column);
			sl.close();
			s.close();
			}catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		try{
			table = new DefaultTableModel(row, column);
			Scanner s= new Scanner(f);
			while (i<row || s.hasNextLine()){
				String linea = s.nextLine();
				Scanner sl = new Scanner(linea);
				sl.useDelimiter("\\s*,\\s*");
				while (j<column && sl.hasNext()){
					table.setValueAt(sl.next(), i, j);
					j++;
				}
				i++;
				j=0;
				sl.close();
			}
			s.close();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return table;
	}
	public static Network BuildNode(int d){
		Integer id;
		Network net = new Network();
		for (id=0;id<d;id++){
			ArrayList<Link> outl = new ArrayList<Link>();
			ArrayList<Link> inl = new ArrayList<Link>();
			Node node=new Node(id,outl,inl);
			net.AddNode(node);
			System.out.println("Created NodeId: "+node.GetId());
		}
		return net;
	}
	public static void BuildLink(DefaultTableModel adjacencies, Network net, int nCores){
		int i,j;
		Node srcnode;
		Node dstnode;
		for (i=0;i<adjacencies.getColumnCount();i++)
		{
			srcnode= net.GetNode(i);
			for (j=0;j<adjacencies.getColumnCount();j++)
			{
				long weigth = Long.parseLong((String)adjacencies.getValueAt(i,j)); //weight= value(i,j)<>0 (distance)
				if (weigth!=0){
					dstnode= net.GetNode(j);
					Link link= new Link(srcnode, dstnode, weigth, nCores);
					srcnode.SetOutlink(link);
					dstnode.SetInlink(link);
					net.AddLink(link);
					System.out.println("Creado Link node "+srcnode.GetId()+" --> "+dstnode.GetId());
				}
			}	
		}
	}
	
	
	public static void PrintListOfNodes (Network net){
	
		int size = net.GetNumberOfNodes();
		for (int i=0;i<size;i++){
			Node node = net.GetNode(i);
			int id = node.GetId();
			System.out.println("NodeId "+id+" :");
			System.out.println("Number de Outlinks:"+node.GetSizeOutLinks());
			ArrayList<Link> outlinks = node.GetOutLinks();
			for (Link outlink : outlinks){
				Node dst = outlink.GetDstNode();
				System.out.println("\t SrcNode: "+id+" ----> DstNode: "+dst.GetId());
			}
			System.out.println("Number de Inlinks:"+node.GetSizeInLinks());
			ArrayList<Link> inlinks = node.GetInLinks();
			for (Link inlink:inlinks){
				Node src = inlink.GetSrcNode();
				System.out.println("\t SrcNode: "+src.GetId()+" ----> DstNode: "+id);
			}
		}
	}
	
	public static void PrintListOfLinks (Network net){
		int size = net.GetNumberOfLinks();
		for (int i=0;i<size;i++){
			Link link = net.GetLink(i);
			System.out.println("Link "+i+":");
			Node src = link.GetSrcNode();
			Node dst = link.GetDstNode();
			long w = link.GetWeight(); 
			System.out.println(src.GetId()+"-->" +dst.GetId()+", Weigth: "+w+", with "+link.getCores().size()+" core Fibers");
			
			//Print List of Active Frequency Slots per Core
			for (Core c:link.getCores()){
				if (c.getNumberOfFsInUse()!=0){ 
					System.out.print("Active fs(id) in Core "+c.getId()+"= {");
					//Collections.sort(link.getFSs());
					for (FrequencySlot freqS:c.getFSs()){
						if (!freqS.getState())
							System.out.print(","+freqS.getId());
					}
					System.out.println("}");
				}
			}
		}
	}
	
	public static DefaultTableModel formatTable (Network net, DefaultTableModel table){
		DefaultTableModel formats;
		//Create a new table with column names
		Vector<Vector> vData= table.getDataVector(); //fixed 12022025
		Vector<String> columnNames = new Vector<String>(); 
		for (int j=0;j<table.getColumnCount();j++){
			//Obtain the column names with value of BitRate cast in Double format
			String br = (String)table.getValueAt(0,j);
			Double brate = Double.parseDouble(br);
			columnNames.add(Double.toString(brate));
		}
		formats = new DefaultTableModel (columnNames,1);//number of rows equal to 1
		return formats;
		
	}
	public static void PrintListOfPaths (ArrayList<Path> paths){
		int i=0;
		if (paths==null){
			System.out.println("\nERROR: The path between src and dst node should be computed first or not exist");
			return;
		}
		if (paths.size()==0){	//OJO for search of LightPaths from i to j node
			System.out.println("\nERROR: The (L)Path between src and dst node should be computed first or not exist");
			return;
		}
		
		for (Path path:paths){
			//System.out.print("\n"+path.getId()+": Path "+(i+1)+"("+path.getSrcNode().GetId()+"->"+path.getDstNode().GetId()+"): {");
			i++;
			System.out.print(path.getSrcNode().GetId());
			for (Link link:path.GetPath()){
				System.out.print(","+link.GetDstNode().GetId());
			}
			System.out.print("} --> Length: "+path.getLength()+" [u], numFS: "+path.getnumFS());
			/*
			//Printout Candidate Channels C(d)
			System.out.println(","+"("+path.getNumberOfSetOfChannels()+") Candidate Channels C(d):");
			System.out.print("\t{");
			for (Channel ch:path.getChannels()){
				System.out.print("{"+ch.getstartFS()+".."+ch.getendFS()+"},");
			}
			System.out.print("}");
			//if (i==4) break;
			*/
		}
	}
	public static void FindOutPaths (Network net, DefaultTableModel formats){
		String option;
		int idsrc, iddst,col;
		double bitRate;
		
		do{
			System.out.println("\nDo you want to find out all distinct paths from one origin to one destination (Y/N): ");
			Scanner op = new Scanner (System.in);
			option = op.nextLine();
			if ("Y".equals(option)){
				System.out.println("Enter src node id: ");
				Scanner src = new Scanner (System.in);
				idsrc = src.nextInt();
				System.out.println("Enter dst node id: ");
				Scanner dst = new Scanner (System.in);
				iddst = dst.nextInt();
				System.out.println("Enter bitRate: ");
				Scanner br = new Scanner (System.in);
				bitRate = br.nextDouble();
				col = formats.findColumn(Double.toString(bitRate));
				ArrayList<Path> paths = net.GetPaths(idsrc, iddst, col-2);
				if (paths!=null)
					System.out.println("\n\nAll Distict Paths from node "+idsrc+" to node "+iddst+" ("+paths.size()+"):");
				PrintListOfPaths(paths);
				
			}else break;
		}while ("Y".equals(option));
		return;
	}
	
	public static void PrintListOfTransceiversPerNode (Network net){
		int size = net.GetNumberOfNodes(); 
		int sumpeak=0;
		for (int i=0;i<size;i++){
			Node node = net.GetNode(i);
			int id = node.GetId();
			//System.out.println("NodeId "+id+" :");
			sumpeak+=node.getPeakNumberOfActiveTrx();
			//System.out.println("NumberOfActiveTrx: "+node.getNumberOfActiveTrx());
			//System.out.println("PeakNumberOfActiveTrx: "+node.getPeakNumberOfActiveTrx());
		}
		//System.out.println("sumOfPeaks: "+sumpeak);
		System.out.printf("AvgPeakNumberOfActiveTrxPerNode: %.3f",(double)sumpeak/(double)net.getNodes().size());
	}
	
	public static void resetCountersForStadistics (Network net){
		//reset the counter of active trx and remove all key-value pairs before new iteration (if apply)
		//Network-wide
    	net.getMapOfActiveNumberOfTRX().clear(); //For CDF of TRxs
    	net.setNumberOfActiveTransceivers(0); //For CDF of TRxs
    	net.getMapOfActiveNumberOfBVTXP().clear(); //For CDF of BVTs
    	net.setNumberOfActiveBVTXP(0); //For CDF of BVTs
    	net.cummulativeCarriedBitRate=0;
    	net.instantaneusCarriedBitRate=0;
    	net.measuredTimes=0;
    	//Node-wide
    	for (Node node:net.getNodes()){
    		node.getMapOfActiveNumberOfBVT().clear(); //For CDF of BVTs
    		node.setNumberOfActiveBVT(0); //For CDF of BVTs
    		node.setPeakNumberOfActiveTrx(0);// For average peak number of active TRxs
    		node.setNumberOfActiveTrx(0); // For average peak number of active TRxs
    	}
	}
		public static void main(String[] args) {
			
			new Simulador().start(args);
		}
		void start(String[] args) {
			DefaultTableModel adjacencies=null, optical_reach=null, trafficProfile=null;
			int nSC=0, F=0,simulationTime = 0, Smax=0, G; //F:  spectrum capacity by each Fiber Core. nSC --> spatial channel count
			boolean MF;
			double load=0.0,load_init, load_end, GB, max_baud_rate=0; //load.- Carga de la Red en Erlangs
			double target_BBP=0.01, attained_BBP;
			String strategy, alpha, ROADMType, groomingStrategy, t_baudrate;
	        events = new ListQueue();
	        
	        if (args.length>0){
	        	/*
	        	adjacencies = ReadFile(args[0],true);
	        	optical_reach = ReadFile(args[1],false);
	        	nSC = Integer.parseInt(args[2]);
	        	MF =  args[3].equals("1");
	        	F = Integer.parseInt(args[4]);
	        	simulationTime = Integer.parseInt(args[5]);
	        	load_init = Double.parseDouble(args[6]);
	        	load_end = Double.parseDouble(args[7]);
	        	alpha = args[8];
	        	trafficProfile = ReadFile(args[9],false);
	        	GB = Double.parseDouble(args[10]);
	        	strategy = args[11];
	        	Smax = Integer.parseInt(args[12]);
	        	G = Integer.parseInt(args[13]);
	        	max_baud_rate = Double.parseDouble(args[14]); 
	        	ROADMType = args[15];
	        	groomingStrategy = args[16];
	        	t_baudrate = args[17];
	        	System.out.println("adja-"+"opt_reach-"+nSC+"-"+MF+"-"+F+"-"+simulationTime+"-"+load_init+"-"+load_end+"-"+alpha+"-"+"profile-"+GB+"-"+strategy+"-"+Smax+"-"+G+"-"+max_baud_rate+"-"+ROADMType+"-"+groomingStrategy+"-"+t_baudrate);
	        	*/
	        	nSC = Integer.parseInt(args[0]);
	        	simulationTime = Integer.parseInt(args[1]);
	        	load_init = Double.parseDouble(args[2]);
	        	load_end = Double.parseDouble(args[3]);
	        	trafficProfile = ReadFile(args[4],false);
	        	Smax = Integer.parseInt(args[5]);
	        	MF =  args[6].equals("1"); //aux. boolean variable to define baudrate type
	        	if (MF) t_baudrate = "flexible"; else t_baudrate = "fixed";
	        	
	        	adjacencies= ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/AdjacencyMatrix16n48eEON.txt",true); //15n46eNSF -6nNew_bigdistances
	        	optical_reach = ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/Sources/ModFormats/TR_SSMF_64GBd.txt",false);
	        	F = 320;//6 20 40
	        	//MF = false; // depurar
	            alpha = "1.0"; //options:  value|incremental|dynamic // depurar
	        	//trafficProfile = ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/Sources/tpfullspecswbitrate3.txt",false); //tpfullspecswbitrate
	        	GB = 10;
	        	strategy = "PCA"; //options: FSA|PSA. Only for JoS // 
	        	G = 1; // group size for FJoS - new -
	        	max_baud_rate = 64; //in GBd
	        	ROADMType = "CCC"; //options: FNB|CCC|JoS. Useful for InS w/ or w/o LC // dep
	        	//t_baudrate = "flexible"; //options for type of baudrate: flexible|fixed
	        	groomingStrategy = "dynamic";
	        }
	        else {
	        	/* Read adjacencies matrix and Build topology */
	        	adjacencies= ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/AdjacencyMatrix16n48eEON.txt",true); //15n46eNSF -6nNew_bigdistances
	        	optical_reach = ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/Sources/ModFormats/TR_SSMF_64GBd.txt",false);
	        	nSC = 2; //4 7
	        	F = 320;//6 20 40
	        	MF = false; // depurar
	        	simulationTime = 10000;//20 100 10000
	        	load_init = 6.0;
	        	load_end = 6.0; //50.0 50.0 100.0
	            alpha = "1.0"; //options:  value|incremental|dynamic // depurar
	        	trafficProfile = ReadFile("C:/Users/Ruben/OneDrive/Respaldos Laptop MSI 28092024/CommLetters/Sources/tpfullspecswbitrate4.txt",false); //tpfullspecswbitrate
	        	GB = 10;
	        	strategy = "PCA"; //options: FSA|PSA. Only for JoS //
	        	Smax = 2; //max number of spatial channels to compute the Spa-SCh - new - 
	        	G = 1; // group size for FJoS - new -
	        	max_baud_rate = 64; //in GBd
	        	ROADMType = "CCC"; //options: FNB|CCC|JoS. Useful for InS w/ or w/o LC // depurar
	        	groomingStrategy = "dynamic"; //options: non|predefined|dynamic. Only for JoS
	        	t_baudrate = "flexible"; //options for type of baudrate: flexible|fixed
	        }	        	
			int dim= adjacencies.getColumnCount();
			Network net = BuildNode (dim);
			int P = nSC/G;// equivalent number of parallel paths for SDM - new -
			// System.out.println("number of P: "+P);
			BuildLink(adjacencies, net, P); //new
			//PrintListOfNodes(net);
			//PrintListOfLinks(net);
			//net.CreatePathsTable(); //no se usa, pues se calcula por cada demanda allpaths for forward and reverse direction
	        //iterate until BBP is close to target BBP
			int i=15; //iterations <= 15
	        do {
	        	load = (load_init+load_end)/2;
	        	/* Create the generator & release objects */
	        	Generator generator = new Generator(net, optical_reach, F, simulationTime,load,trafficProfile,MF,alpha,GB,strategy,Smax,G,max_baud_rate,ROADMType,groomingStrategy,t_baudrate);
	        
	        	/* Start the generator by creating one demand immediately */
	        	generator.time = 0.0;
	        	insert(generator);

	        	doAllEvents();
	        	//FindOutLpaths(net, formats);
	        	//PrintListOfPaths(net.getLightPaths());
	        	//PrintListOfLinks(net); //only to probe that at finish of simulation, ALL FS(i) of each core is free. 
	        	PrintListOfTransceiversPerNode(net);
	        	attained_BBP=generator.bbp;
	        	if ((generator.bbp-(target_BBP+0.001))>0)
	        		load_end=load;
	        	else load_init=load;
	        	System.out.printf("\n%.5f \t",load);
	        	System.out.printf("%.5f \t",attained_BBP);
	        	System.out.printf("%.2f\t", (double)net.cummulativeCarriedBitRate/(double)net.measuredTimes);
	        	System.out.printf("%.2f\n", (double)net.instantaneusCarriedBitRate);
	        	i++;
	        	System.gc();
	        	//reset the counters for stadistics (e.g. BVTs and TRxs clearing all key-value pairs before executing new iteration (if apply)
	        	resetCountersForStadistics(net);
	        }while ((attained_BBP< (target_BBP-0.001) || attained_BBP>(target_BBP+0.001)) && i<=15);
	    }
	}	