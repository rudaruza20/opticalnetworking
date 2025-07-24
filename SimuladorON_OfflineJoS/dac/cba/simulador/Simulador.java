package dac.cba.simulador;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

//PRUEBA DE VALIDACION REGLA BRANCH
//INTEGRACION DE DATOS 
// Prueba 24 de julio.
//Prueba sin lock branch
//Prueba con ruleset deshabilitado
//Prueba desde main
public class Simulador {
	private static ArrayList<Demand> demands = new ArrayList<Demand>();
	private static DefaultTableModel formatsMF; 
	private static DefaultTableModel formatsMCF;
	
	public Simulador() {
		// TODO Auto-generated constructor stub
	}
	public static void AddDemand (Demand demand){
		demands.add(demand);	
	}
	public static Demand GetDemand (int index){
		Demand demand = demands.get(index);
		return demand;	
	}
	public static ArrayList<Demand> GetDemands (){
		return demands;
	}
	public static int GetNumberOfDemands (){
		return demands.size();
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
	public static void BuildLink(DefaultTableModel adjacencies, Network net){
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
					Link link= new Link(srcnode, dstnode, weigth);
					srcnode.SetOutlink(link);
					dstnode.SetInlink(link);
					net.AddLink(link);
					System.out.println("Creado Link node "+srcnode.GetId()+" --> "+dstnode.GetId());
				}
			}	
		}
	}
	public static void BuildDemand(DefaultTableModel demands, Network net){
		int idsrc, iddst;
		Node srcnode;
		Node dstnode;
		
		for (int i=0; i<demands.getRowCount();i++){
			idsrc = Integer.parseInt((String)demands.getValueAt(i,1));
			iddst = Integer.parseInt((String)demands.getValueAt(i,2));
			double bitRate = Double.parseDouble((String)demands.getValueAt(i,3)); // Cast the Bit Rate Value in Double Format
			srcnode = net.GetNode(idsrc);
			dstnode = net.GetNode(iddst);
			Demand demand= new Demand(i,srcnode, dstnode,bitRate); 
			AddDemand(demand);
		}
		//Collections.sort(GetDemands()); /*ordenar Demandas por weigth de mayor a menor*/ 
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
			System.out.println(src.GetId()+"-->" +dst.GetId()+", Weigth: "+w+", "+link.getNumberOfFsInUse()+" FS in use");
			if (link.getNumberOfFsInUse()!=0){
				System.out.print("{");
				//Collections.sort(link.getFSs());
				for (FrequencySlot freqS:link.getFSs()){
					if (!freqS.getState())
						System.out.print(","+freqS.getId());
						System.out.print(","+freqS.getState());
				}
				System.out.println("}");
			}
		}
	}
	
	public static void PrintListOfDemands (Network net){
		int size = GetNumberOfDemands();
		for (int i=0;i<size;i++){
			Demand demand = GetDemand(i);
			System.out.println("Demand "+i+":");
			Node src = demand.GetSrcNode();
			Node dst = demand.GetDstNode();
			System.out.print(demand.GetId()+"\t"+src.GetId()+"\t"+dst.GetId()+"\t"+demand.GetBitRate()+"\t"+demand.GetWeigth()+"\n");
		}
	}
	/*
	public static void computeUserPaths (Network net){
		String option;
		int idsrc, iddst;
		Node srcnode, dstnode;
		ArrayList <Path> PathList;
		do{
			System.out.println("\n Do you want to compute all routes from one origin to one destination (Y/N): ");
			Scanner op = new Scanner (System.in);
			option = op.nextLine();
			if ("Y".equals(option)){
				System.out.println("Enter src node id: ");
				Scanner src = new Scanner (System.in);
				idsrc = src.nextInt();
				srcnode = net.GetNode(idsrc);
				System.out.println("Enter dst node id: ");
				Scanner dst = new Scanner (System.in);
				iddst = dst.nextInt();
				dstnode = net.GetNode(iddst);
				AllDistinctPaths r= new AllDistinctPaths();
				PathList = r.ComputeAllDistinctPaths(srcnode, dstnode);
				Collections.sort(PathList); // Ordenar por Weigth de mayor a menor y luego insertar ordenados en Table de Network
				net.AddListOfPaths(idsrc, iddst, PathList);
			}else break;
		}while ("Y".equals(option));
		return;
		
	}
	*/
	//for only one TR table, either MF or MCF
	public static void computePaths (Network net, DefaultTableModel modulationFormats, int F, int S, double GB, boolean fiberType){
		Node srcnode, dstnode;
		int col, mimoStatus;
		ArrayList <Path> pathListf, pathListr, admissiblePathListf, admissiblePathListr;  
		Channel channel;
	
		for (Demand d:demands)
		{
			srcnode = d.GetSrcNode();
			dstnode = d.GetDstNode();
			double bitRate = d.GetBitRate();
			col = modulationFormats.findColumn(Double.toString(bitRate));	
			//Avoid repeat the algorithm for same spectrum path request
			if ((net.GetPaths(srcnode.GetId(), dstnode.GetId(),col-2))==null){
				if (fiberType) mimoStatus = 1;
				else mimoStatus = 0;
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
					for (row=1;row<modulationFormats.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)modulationFormats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)modulationFormats.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)modulationFormats.getValueAt(row, 0));
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
					//Create and assign channel objects to each path
					for (int i=0;i<=F-numFS;i++){
						Channel ch =  new Channel(numFS,i,i+numFS-1);
						p.setChannels(ch);
						ch.setMiMoStatus(mimoStatus);
						//net.setChannels(ch); // Add to complete list of channels for all demands in Network 
					}
					admissiblePathListf.add(p);
				}
				admissiblePathListr = new ArrayList<Path>();
				//Reverse Path
				for (Path p:pathListr){
					int numFS = 0;
					for (row=1;row<modulationFormats.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)modulationFormats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)modulationFormats.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)modulationFormats.getValueAt(row, 0));
							if (SE>0)numFS = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
							else numFS = 0; //outside max reach.  
							break;
						}
					}
					if (numFS==0) break;
					p.setnumFS(numFS);
					p.setbitRate(d.GetBitRate());
					p.setMiMoStatus(mimoStatus);
					//Create and assign channel objects to each path
					for (int i=0;i<=F-numFS;i++){
						Channel ch =  new Channel(numFS,i,i+numFS-1);
						p.setChannels(ch);
						ch.setMiMoStatus(mimoStatus);
						//net.setChannels(ch);
					}
					admissiblePathListr.add(p);
				}
				net.AddListOfPaths(srcnode.GetId(), dstnode.GetId(), admissiblePathListf, col-2, modulationFormats.getColumnCount()-2); //Insert FW Path
				net.AddListOfPaths(dstnode.GetId(),srcnode.GetId(), admissiblePathListr, col-2, modulationFormats.getColumnCount()-2); //Insert Reverse Path
			}
			else{
				pathListf = net.GetPaths(srcnode.GetId(), dstnode.GetId(),col-2);
			}
			System.out.println("There are "+pathListf.size()+" candidate paths between node "+srcnode.GetId()+" and "+dstnode.GetId());	
		}
		//net.joinChannels(); // c Set: union of candidate channels
		return;
	}
	//for two TR tables,MF(with MIMO) and MCF (without MIMO)
	public static void computePathsMiMo (Network net, int F, int S, double GB){
		Node srcnode, dstnode;
		int col;
		ArrayList <Path> pathListf, pathListr, admissiblePathListf, admissiblePathListr;  
		Channel channel;
		//Build Candidate LightPaths with the corresponding numFS required.  
		for (Demand d:demands)
		{
			srcnode = d.GetSrcNode();
			dstnode = d.GetDstNode();
			double bitRate = d.GetBitRate();
			col = formatsMF.findColumn(Double.toString(bitRate));	
			//Avoid repeat the algorithm for same spectrum path request
			if ((net.GetPaths(srcnode.GetId(), dstnode.GetId(),col-2))==null){
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
					int numFS_MF=0; //initValue
					for (row=1;row<formatsMF.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)formatsMF.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formatsMF.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)formatsMF.getValueAt(row, 0));
							if (SE>0)numFS_MF = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
							else numFS_MF = 0; //outside max reach.  
							break;
						}
					}
					int numFS_MCF=0; //initValue
					for (row=1;row<formatsMCF.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)formatsMCF.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formatsMCF.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)formatsMCF.getValueAt(row, 0));
							if (SE>0)numFS_MCF = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
							else numFS_MCF = 0; //outside max reach.  
							break;
						}
					}
					if (numFS_MF==0) break;
					p.setnumFS(numFS_MF); //OJO:  providencial
					//Create and assign channel objects to each path
					if (numFS_MCF>0){
						for (int i=0;i<=F-numFS_MCF;i++){
							Channel ch =  new Channel(numFS_MCF,i,i+numFS_MCF-1);
							ch.setMiMoStatus(0);//no MIMO
							p.setChannels(ch);
							//net.setChannels(ch); // Add to complete list of channels for all demands in Network
						}
					}
					if (numFS_MF!=numFS_MCF){
						for (int i=0;i<=F-numFS_MF;i++){
							Channel ch =  new Channel(numFS_MF,i,i+numFS_MF-1);
							ch.setMiMoStatus(1);//with MIMO
							p.setChannels(ch);
							//net.setChannels(ch); // Add to complete list of channels for all demands in Network 
						}
					}
					admissiblePathListf.add(p);
				}
				admissiblePathListr = new ArrayList<Path>();
				//Reverse Path
				for (Path p:pathListr){
					int numFS_MF=0; //initValue
					for (row=1;row<formatsMF.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)formatsMF.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formatsMF.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)formatsMF.getValueAt(row, 0));
							if (SE>0)numFS_MF = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
							else numFS_MF = 0; //outside max reach.  
							break;
						}
					}
					int numFS_MCF=0; //initValue
					for (row=1;row<formatsMCF.getRowCount();row++){
						if (p.getLength()>Double.parseDouble((String)formatsMCF.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formatsMCF.getValueAt(row, 1))){
							//String st = (String)formats.getValueAt(row, col);
							//numFS = Integer.parseInt(st);
							int SE = Integer.parseInt((String)formatsMCF.getValueAt(row, 0));
							if (SE>0)numFS_MCF = (int)Math.ceil((d.GetBitRate()/(S*SE)+GB)/12.5);
							else numFS_MCF = 0; //outside max reach.  
							break;
						}
					}
					if (numFS_MF==0) break;
					p.setnumFS(numFS_MF); //OJO:  providencial
					//Create and assign channel objects to each path
					if (numFS_MCF>0){
						for (int i=0;i<=F-numFS_MCF;i++){
							Channel ch =  new Channel(numFS_MCF,i,i+numFS_MCF-1);
							ch.setMiMoStatus(0);//no MIMO
							p.setChannels(ch);
							//net.setChannels(ch); // Add to complete list of channels for all demands in Network 
						}
					}
					if (numFS_MF!=numFS_MCF){
						for (int i=0;i<=F-numFS_MF;i++){
							Channel ch =  new Channel(numFS_MF,i,i+numFS_MF-1);
							ch.setMiMoStatus(1);//MIMO
							p.setChannels(ch);
							//net.setChannels(ch); // Add to complete list of channels for all demands in Network 
						}
					}
					admissiblePathListr.add(p);
				}
				net.AddListOfPaths(srcnode.GetId(), dstnode.GetId(), admissiblePathListf, col-2, formatsMF.getColumnCount()-2); //Insert FW Path
				net.AddListOfPaths(dstnode.GetId(),srcnode.GetId(), admissiblePathListr, col-2, formatsMF.getColumnCount()-2); //Insert Reverse Path
			}
			else{
				pathListf = net.GetPaths(srcnode.GetId(), dstnode.GetId(),col-2);
			}
			System.out.println("There are "+pathListf.size()+" candidate paths between node "+srcnode.GetId()+" and "+dstnode.GetId());	
		}
		//net.joinChannels(); // c Set: union of candidate channels
		return;
	}
	public static void labelColumnNames(DefaultTableModel modulationFormatsMF, DefaultTableModel modulationFormatsMCF){
		Vector<Object> vData= modulationFormatsMF.getDataVector();
		Vector<String> columnNames = new Vector<String>(); 
		for (int j=0;j<modulationFormatsMF.getColumnCount();j++){
			//Obtain the column names with value of BitRate cast in Double format
			String br = (String)modulationFormatsMF.getValueAt(0,j);
			Double brate = Double.parseDouble(br);
			columnNames.add(Double.toString(brate));
			//columnNames.add((String)modulationFormats.getValueAt(0,j)); //funciona para valores de BitRate tipo Int
		}
		formatsMF = new DefaultTableModel (vData,columnNames);
		//Create a new table of modulation formats with column names for MCF
		Vector<Object> vDataMCF= modulationFormatsMCF.getDataVector();
		Vector<String> columnNamesMCF = new Vector<String>(); 
		for (int j=0;j<modulationFormatsMCF.getColumnCount();j++){
			//Obtain the column names with value of BitRate cast in Double format
			String br = (String)modulationFormatsMCF.getValueAt(0,j);
			Double brate = Double.parseDouble(br);
			columnNamesMCF.add(Double.toString(brate));
			//columnNames.add((String)modulationFormats.getValueAt(0,j)); //funciona para valores de BitRate tipo Int
		}
		formatsMCF = new DefaultTableModel (vDataMCF,columnNamesMCF);
		//Build Candidate Paths with the corresponding numFS required.  
	}
	public static void minLPswithMIMO (int S, double GB, ArrayList<Path> lightPaths){
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
		int src = paths.get(0).GetPath().get(0).GetSrcNode().GetId();
		int dst = paths.get(0).GetPath().get(paths.get(0).GetPath().size()-1).GetDstNode().GetId();
		//System.out.println("\n\n(L)Paths from node "+src+" to node "+dst+" ("+paths.size()+")");
		for (Path path:paths){
			System.out.print("\nPath "+(i+1)+"("+src+"->"+dst+"): {");
			i++;
			System.out.print(src);
			for (Link link:path.GetPath()){
				System.out.print(","+link.GetDstNode().GetId());
			}
			System.out.print("} --> Length: "+path.getLength()+" [u], numFS: "+path.getnumFS());
			//Printout Candidate Channels C(d)
			System.out.println(","+"("+path.getNumberOfSetOfChannels()+") Candidate Channels C(d):");
			System.out.print("\t{");
			for (Channel ch:path.getChannels()){
				System.out.print("{"+ch.getstartFS()+".."+ch.getendFS()+"},");
			}
			System.out.print("}");
			
			if (i==4) break;
		}
	}
	public static void FindOutPaths (Network net){
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
				col = formatsMF.findColumn(Double.toString(bitRate));
				ArrayList<Path> paths = net.GetPaths(idsrc, iddst, col-2);
				if (paths!=null)
					System.out.println("\n\nAll Distict Paths from node "+idsrc+" to node "+iddst+" ("+paths.size()+"):");
				PrintListOfPaths(paths);
				
			}else break;
		}while ("Y".equals(option));
		return;
	}
	/*
	public static void FindOutLpaths (Network net){
		String option;
		int idsrc, iddst;
		do{
			System.out.println("\nDo you want to find out all lighpaths established from one origin to one destination (Y/N): ");
			Scanner op = new Scanner (System.in);
			option = op.nextLine();
			if ("Y".equals(option)){
				System.out.println("Enter src node id: ");
				Scanner src = new Scanner (System.in);
				idsrc = src.nextInt();
				System.out.println("Enter dst node id: ");
				Scanner dst = new Scanner (System.in);
				iddst = dst.nextInt();
				ArrayList<Path> paths = net.getLightPaths(idsrc, iddst);
				System.out.println("\n\nFrom node "+idsrc+" to node "+iddst+" there is(are) "+paths.size()+" LightPaths:");
				PrintListOfPaths(paths);
				
			}else break;
		}while ("Y".equals(option));
		return;
	}
	*/
	/*
	public static void findOutWLightPaths (ArrayList<Path> lightPaths){
		String option;
		int idwave;
		do{
			ArrayList<Path> paths = new ArrayList<Path>();
			System.out.println("\nDo you want to find out all lighpaths established using one Wavelenght (Y/N): ");
			Scanner op = new Scanner (System.in);
			option = op.nextLine();
			if ("Y".equals(option)){
				System.out.println("Enter wavelength id: ");
				Scanner src = new Scanner (System.in);
				idwave = src.nextInt();
				for (Path lpath:lightPaths){
					if (lpath.getWavelength().getId()==idwave)
							paths.add(lpath);
				}
				PrintListOfPaths(paths);
				
			}else break;
		}while ("Y".equals(option));
		return;
	}
	*/
	/*
	public static void findOutWLinks (Network net){
		String option;
		int idsrc, iddst;
		do{
			System.out.println("\nDo you want to find-out how many Wavelengths(optical Channels) traverse or are assigned to one specific link (Y/N): ");
			Scanner op = new Scanner (System.in);
			option = op.nextLine();
			if ("Y".equals(option)){
				System.out.println("Enter src node id of the Link: ");
				Scanner src = new Scanner (System.in);
				idsrc = src.nextInt();
				System.out.println("Enter dst node id of the Link: ");
				Scanner dst = new Scanner (System.in);
				iddst = dst.nextInt();
				Link link = net.searchLink(net.GetNode(idsrc), net.GetNode(iddst));
				ArrayList<Wavelength> wavelengths = link.getWavelenghts();
				Collections.sort(wavelengths);//Sort upward optical channels by id
				System.out.println("\n\nLink from node "+idsrc+" to node "+iddst+" has assigned "+link.getNumberOfWavelengths()+" optical channels:");
				for (Wavelength w:wavelengths)
					System.out.println("\n\t Wavelength assigned: "+w.getId());
			}else break;
		}while ("Y".equals(option));
		return;
	}
	*/
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DefaultTableModel adjacencies, demandsTable, modulationFormatsMF, modulationFormatsMCF;
		ArrayList<Path> paths;
		double GB;
		int F, S, delta, maxLPsMIMO;
		boolean fiberType; //1:MF, 0:MCF
		if (args.length>0){
        	adjacencies = ReadFile(args[0],true);
        	demandsTable = ReadFile(args[1],false);
        	fiberType =  args[2].equals("1");
        	modulationFormatsMF = ReadFile(args[3],false);
        	modulationFormatsMCF = ReadFile(args[4],false);
        	F = Integer.parseInt(args[5]);
        	S = Integer.parseInt(args[6]);
        	GB = Double.parseDouble(args[7]);
        	delta = Integer.parseInt(args[8]);
        	maxLPsMIMO = Integer.parseInt(args[9]);
        }
		else {
			adjacencies = ReadFile("/home/platic/AdjacencyMatrix9n26eINT2.txt",true);
			demandsTable = ReadFile("/home/platic/CommLetters/Sources/Demandas/(600)Demands_INT2_FlexGrid.txt",false);  
			modulationFormatsMF = ReadFile("/home/platic/CommLetters/Sources/ModFormats/TR_GN_Model_xMF.txt",false);
			modulationFormatsMCF = ReadFile("/home/platic/CommLetters/Sources/ModFormats/TR_19MCF.txt",false);
			fiberType = true; //1: MF, 0:MCF
			F = 260; //Spectrum Capacity: Total Number of Frequency Slots in the spectrum of the links
			S = 19;
			GB = 7.5;
			delta = 0;//When output is MIN MiMo LPs, delta is the allowed penalty in FSs regarding the number of F (#FSs occupied in MF solution).
			maxLPsMIMO = 0;
		}
		int dim = adjacencies.getColumnCount();
		Network net = BuildNode (dim);
		BuildLink(adjacencies, net);
		PrintListOfNodes(net);
		PrintListOfLinks(net);
		net.CreatePathsTable();
		BuildDemand(demandsTable, net);
		labelColumnNames(modulationFormatsMF, modulationFormatsMCF);
		//PrintListOfDemands(net);
		//computeUserPaths(net);
		computePaths(net, formatsMCF, F, S, GB, fiberType); //according to the demands. Pre-compute all distinct routes (candidate Paths) to serve all demands in order to run ILP optimization -for only TR table either for MF or MCF -
		//computePathsMiMo(net,(F+delta), S, GB); //according to the demands. Pre-compute all distinct routes (candidate Paths) to serve all demands in order to run ILP optimization - for two TR tables, one for MF and other for MCF -
		//FindOutPaths(net);
		
	    /*
	     * Execute k-LinkDisjointPaths Dijkstra
	    
	    KLinkDisjointPaths kspd = new KLinkDisjointPaths(net);
	    paths=kspd.execute(net.GetNode(0), net.GetNode(5),5);
	    System.out.println("\n\nFrom node "+net.GetNode(0).GetId()+" to node "+net.GetNode(5).GetId()+" there is(are) "+paths.size()+"-Link-Disjoint Paths:");
	    PrintListOfPaths(paths);
	    /*
	     * Execute LinkProtection
	   
	    LinkProtection kdisjoint = new LinkProtection(net);
	    paths=kdisjoint.execute(net.GetNode(0), net.GetNode(5));
	    System.out.println("\n\nLink Protection from node "+net.GetNode(0).GetId()+" to node "+net.GetNode(5).GetId()+". Primary and Secondary Paths:");
	    PrintListOfPaths(paths);
	    
		
		/******************************************************************************************************************/
		// General Implementation for Flex-Grid/MCF with JoS FCA 
		// Objective: Minimize the number of frequency slots to serve all demands given a Traffic Matrix (Assume enough spectrum capacity)
		// &  In input parameters, verify if in all cases it does not matter the modulation format (MF or MCF).
	    /*******************************************************************************************************************/
		//ILP1:  Minimize the total number of FSs occupied in any MCF link e
		
				EON_SDM_ILP1_MinFS optimizer = new EON_SDM_ILP1_MinFS();
				optimizer.initialize(net, demands, formatsMCF, F);
				optimizer.solve(net, demands, F, formatsMCF);
		
		
		//ILP2:  New ILP Model for MIN FS given a number of MAX n MIMO LPs
		/*
				EON_SDM_ILP10_MIMO optimizer = new EON_SDM_ILP2_MIMO();
				optimizer.initialize(net, demands, formatsMF, (F+delta), maxLPsMIMO);
				optimizer.solve(net, demands, (F+delta), formatsMF);
		*/
	
	}
}