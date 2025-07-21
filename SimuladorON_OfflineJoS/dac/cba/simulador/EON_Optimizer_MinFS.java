
/*
 * ILP implementation suggested in: Routing and Spectrum Assignment in
Spectrum Sliced Elastic Optical Path Network, Mirosław Klinkowski and Krzysztof Walkowiak
 */

package dac.cba.simulador;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

import ilog.concert.*;
import ilog.cplex.*;

public class EON_Optimizer_MinFS {
	private long t0, t1,t2;
	IloCplex cplex;
	IloNumVar[][] w;
	IloNumVar[][] x;
	IloNumVar[][] y;
	IloNumVar[] z;
	IloNumVar F;
	private long construction_time,solution_time;
	ArrayList<Path> paths =  new ArrayList<Path>();
	
	public EON_Optimizer_MinFS() {
		// TODO Auto-generated constructor stub
		try 
		{
			cplex = new IloCplex();
		} catch (IloException e)
		{
		}
	}
	//C: Capacity of the Spectrum of the links.  
	public void initialize(Network net,ArrayList<Demand> demands, DefaultTableModel formats, int C)
	{
		//C : number of optical Channels (Lambdas)
		t0 = System.currentTimeMillis();
		
		// Initialize Cplex environment
		try
		{			
			IloLinearNumExpr expr = cplex.linearNumExpr();
			//IloLinearNumExpr expr2 = cplex.linearNumExpr();
			int idsrc, iddst;
			// Cplex parameters
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3); //IMPORTANTE:  si se excede el tamaño de la memoria lo guarda en disco
			cplex.setParam(IloCplex.DoubleParam.EpGap, 0.05); //IMPORTANTE: Devuelve la solución cuando haya alcanzado el 95% de ejecución de la optimización
			//cplex.setParam(IloCplex.IntParam.VarSel, 3);
			cplex.setParam(IloCplex.DoubleParam.TiLim, 600); //IMPORTANTE: Tiempo limite en SEG de ejecución y devuelve la solución		
			
			//ERROR no se puede tener parejas s-d únicas porque cada d tiene light-paths (lps) diferentes aunque tengan caminos físicos iguales. Tampoco hay que calcular de todos. Serian lps candidatos todos aquellos que tengan el mismo src-dst-bitrate 
			//Set of demands with not repeated s-d pairs in order to compute not repeated candidate paths 
			ArrayList<Demand> ds = (ArrayList<Demand>)demands.clone();
			int n=ds.size();
			for (int i=0; i<n;i++){
				for (int j=i+1; j<n;j++){
					if ((ds.get(i).GetSrcNode().GetId()==ds.get(j).GetSrcNode().GetId()) && (ds.get(i).GetDstNode().GetId()==ds.get(j).GetDstNode().GetId())){
						ds.remove(j);
						j--;
						n--;	
					}
				}
			}
			
			//fin
			for (Demand d:ds){
				System.out.println("Demand: "+d.GetId()+" ,Src: "+d.GetSrcNode().GetId()+" ,Dst: "+d.GetDstNode().GetId());
			}
			
			
			//Find-out P [All Paths]
				//Set of all candidate paths for demands (with not repeated s-d pairs)
			
			for (int d=0;d<ds.size();d++){
				int idx = formats.findColumn(Double.toString(ds.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(ds.get(d).GetSrcNode().GetId(), ds.get(d).GetDstNode().GetId(), idx-2);
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<(size);index++)
						kshortestPath.remove(3);
				}
				paths.addAll(kshortestPath);
			}
			
			for (Path p:paths){
				System.out.println("Path. src: "+p.getSrcNode().GetId()+", dst: "+p.getDstNode().GetId());
			}
			
			//fin
			// Variables
			//Boolean Variables w(p,f)
			w = new IloNumVar[paths.size()][];
			for (int p=0;p<paths.size();p++)
				w[p] = cplex.boolVarArray(C);
			
			//Boolean Variables x(p,f)
			x = new IloNumVar[paths.size()][];
			for (int p=0;p<paths.size();p++)
				x[p] = cplex.boolVarArray(C);
			
			// Boolean Variables y(e,f)
			y = new IloNumVar[net.getLinks().size()][];
			for (int e=0; e<net.getLinks().size();e++)
				y[e]= cplex.boolVarArray(C);
			
			// Boolean Variables z(f)
			z = cplex.boolVarArray(C);

			// Integer Variable F 
			F = cplex.intVar(1,C);
			
			
			// Constraints
			//(1a)
			for (int d=0;d<demands.size();d++){
				int i=1;
				System.out.println("demand: "+demands.get(d).GetId());
				for (int p=0;p<paths.size();p++){
					if ((paths.get(p).getSrcNode().GetId()==demands.get(d).GetSrcNode().GetId())&&(paths.get(p).getDstNode().GetId()==demands.get(d).GetDstNode().GetId())){
						for (int f=0;f<C;f++)
							expr.addTerm(w[p][f], 1);
					}
				}
				cplex.addEq(expr, 1);
				expr.clear();
			}
			
			//(1b)
			for (int d=0;d<demands.size();d++){
				System.out.println("demand: "+demands.get(d).GetId());
				for (int p=0;p<paths.size();p++){
					if ((paths.get(p).getSrcNode().GetId()==demands.get(d).GetSrcNode().GetId())&&(paths.get(p).getDstNode().GetId()==demands.get(d).GetDstNode().GetId())){
						int numFS =  getnumFS (paths.get(p),demands.get(d).GetBitRate(), formats);
						System.out.println("Path "+p+", NumFS: "+numFS);
						for (int i=0;i<=C-numFS;i++){
							for (int j=i;j<=(i+numFS-1);j++) {
								expr.addTerm(w[p][i], 1);
								expr.addTerm(x[p][j], -1);
								cplex.addLe(expr, 0);
								expr.clear();
							}
						}
					}
				}
			}
			
			//(1c)
			for (int d=0;d<demands.size();d++){
				for (int p=0;p<paths.size();p++){
					if ((paths.get(p).getSrcNode().GetId()==demands.get(d).GetSrcNode().GetId())&&(paths.get(p).getDstNode().GetId()==demands.get(d).GetDstNode().GetId())){
						int numFS=getnumFS(paths.get(p),demands.get(d).GetBitRate(),formats);
						for (int i=(C-numFS+1);i<C;i++){
							expr.addTerm(w[p][i], 1);
							cplex.addEq(expr, 0);
							expr.clear();
						}
					}
				}
			}
			
			//(1d)
			for (int e=0;e<net.getLinks().size();e++){
				Link linkf = net.GetLink(e);
				Link linkr = net.searchLink(net.getLinks().get(e).GetDstNode(), net.getLinks().get(e).GetSrcNode());
				int e1 = net.getIndexOfLink(linkr);
				for (int f=0;f<C;f++){	
					for (int p=0;p<paths.size();p++){
						if (containsLink(paths.get(p),linkf)||containsLink(paths.get(p),linkr))
							expr.addTerm(x[p][f], 1);	
					}
					expr.addTerm(y[e][f], -1);
					expr.addTerm(y[e1][f], -1);
					cplex.addEq(expr, 0);
					expr.clear();
				}
			}
			//(1e)
			for (int f=0;f<C;f++){
				for (int e=0;e<net.getLinks().size();e++){
					expr.addTerm(y[e][f], 1);
				}
				expr.addTerm(z[f], -1*net.getLinks().size());
				cplex.addLe(expr, 0);
				expr.clear();
			}
			//(1f)
			
			for (int f=0;f<C;f++){
				expr.addTerm(z[f], 1);
			}
			expr.addTerm(F, -1);
			cplex.addEq(expr,0);
			expr.clear();
			
			// Objective function
			
			expr.addTerm(F,1);
			IloObjective objective_function = cplex.minimize(expr);
			cplex.add(objective_function);
			
			/*
			for (int f=0;f<C;f++){
				expr.addTerm(z[f],1+(1+f)*10E-4);
			}
			IloObjective objective_function = cplex.minimize(expr);
			cplex.add(objective_function);
			*/
			
		}catch (IloException ev) 
		{
	        //Log.write("Concert exception caught: " + ev);
		}
		
	}
	
	private boolean containsLink(Path path, Link link)
	{
		boolean contains = false;
		
		for(int i = 0; i<path.GetPath().size(); i++)
		{
			if(link.GetSrcNode().GetId() == path.GetPath().get(i).GetSrcNode().GetId() && link.GetDstNode().GetId() == path.GetPath().get(i).GetDstNode().GetId())
			{
				return true;
			}
		}
		return contains;
	}
	
	private int getnumFS (Path p, double bitRate, DefaultTableModel formats){
		String st = null;
		int col = formats.findColumn(Double.toString(bitRate));
		for (int row=1;row<formats.getRowCount();row++){
			if (p.getLength()>Double.parseDouble((String)formats.getValueAt(row-1, 1))&&p.getLength()<=Double.parseDouble((String)formats.getValueAt(row, 1))){
				st = (String)formats.getValueAt(row, col);
			}
		}
		
		return Integer.parseInt(st);
	}
	
	public void solve(Network net, ArrayList<Demand> demands, int C, DefaultTableModel formats)
	{				
		try
		{
			int idsrc, iddst;
			t1 = System.currentTimeMillis();
			
			cplex.solve();
			
			t2 = System.currentTimeMillis();
			
			if(cplex.getStatus()!=IloCplex.Status.Infeasible)
			{	
				construction_time = t1-t0;
				
				solution_time = t2-t1;
				
				System.out.println("\nTime to construct the model is "+(construction_time)/1000.0+" sec.");
				
				System.out.println("Found feasible solution in "+(solution_time)/1000.0+" sec.");
				
				int index = 0;
				
				for(int f = 0; f<C; f++)
				{
					if(cplex.getValue(z[f])>=0.99 && cplex.getValue(z[f])<=1.01)
					{
						System.out.println("FS index assigned: "+f);
						index++;
					}
				}
				System.out.println("Utilized Frequency Slots = "+index);
				
				//System.out.println("Utilized Frequency Slots = "+F);
				
				for(int d = 0; d<demands.size(); d++)
				{
					int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
					idsrc = demands.get(d).GetSrcNode().GetId();
					iddst = demands.get(d).GetDstNode().GetId();
					for(int p=0; p<paths.size();p++)
					{
						if ((paths.get(p).getSrcNode().GetId()==demands.get(d).GetSrcNode().GetId())&&(paths.get(p).getDstNode().GetId()==demands.get(d).GetDstNode().GetId())){
							int i=0;
							for (int f=0;f<C;f++){
								if(cplex.getValue(x[p][f])>=0.999 && cplex.getValue(x[p][f])<=1.001)
								{
									if (i==0){
										System.out.print("Demand "+d+" utilizes candidate lightpath "+p+" : {");
										System.out.print(paths.get(p).getSrcNode().GetId());
										for (Link link:paths.get(p).GetPath()){
											System.out.print(","+link.GetDstNode().GetId());
										}
										System.out.print("}, with following ("+paths.get(p).getnumFS()+") frequency slots (id): {"+f);
									}
									else System.out.print(","+f);
									i++;
								}
							}
						}
					}
					System.out.println("}");
				}
			}
			else
			{
					System.out.println("No solution found");
			}
		}catch (IloException ev) 
		{
			//Log.write("Concert exception caught: " + ev);
		}
	}
}
