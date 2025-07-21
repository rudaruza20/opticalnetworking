package dac.cba.simulador;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

import ilog.concert.*;
import ilog.cplex.*;

public class EON_ILP_MinFS {
	private long t0, t1,t2;
	IloCplex cplex;
	IloNumVar[][][] w;
	IloNumVar[][][] x;
	IloNumVar[][] y;
	IloNumVar[] z;
	IloNumVar F;
	private long construction_time,solution_time;
	ArrayList<Path> paths =  new ArrayList<Path>();
	
	public EON_ILP_MinFS() {
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
			cplex.setParam(IloCplex.DoubleParam.EpGap, 0.01); //IMPORTANTE: Devuelve la solución cuando haya alcanzado el 95% de ejecución de la optimización
			//cplex.setParam(IloCplex.IntParam.VarSel, 3);
			cplex.setParam(IloCplex.DoubleParam.TiLim, 600); //IMPORTANTE: Tiempo limite en SEG de ejecución y devuelve la solución		
			
			
			//fin
			// Variables
			//Boolean Variables w(d,p,f)
			w = new IloNumVar[demands.size()][3][];//k=3 (OJO)
			for (int d=0; d<demands.size();d++){
				/*
				idsrc = demands.get(d).GetSrcNode().GetId();
				iddst = demands.get(d).GetDstNode().GetId();
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				w[d]= new IloNumVar[net.GetPaths(idsrc, iddst, idx0)-2).size()][];
				*/
				//for (int p=0; p<net.GetPaths(idsrc, iddst, idx-2).size();p++){
				for (int p=0;p<3;p++){
					w[d][p]= cplex.boolVarArray(C);
				}
			}
			
			//Boolean Variables x(d,p,f)
			x = new IloNumVar[demands.size()][3][];//k=3 (OJO)
			for (int d=0; d<demands.size();d++){
				/*
				idsrc = demands.get(d).GetSrcNode().GetId();
				iddst = demands.get(d).GetDstNode().GetId();
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				x[d]= new IloNumVar[net.GetPaths(idsrc, iddst, idx-2).size()][];
				*/
				//for (int p=0; p<net.GetPaths(idsrc, iddst, idx-2).size();p++){
				for (int p=0;p<3;p++){
					x[d][p]= cplex.boolVarArray(C);
				}
			}
			
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
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<size;index++)
						kshortestPath.remove(3);
				}
				for (int p=0;p<kshortestPath.size();p++){
					for (int f=0;f<C;f++)
						expr.addTerm(w[d][p][f], 1);
				}
				cplex.addEq(expr, 1);
				expr.clear();
			}
			//(1b)
			for (int d=0;d<demands.size();d++){
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<size;index++)
						kshortestPath.remove(3);
				}
				for (int p=0;p<kshortestPath.size();p++){
					for (int i=0;i<=(C-kshortestPath.get(p).getnumFS());i++){
						for (int j=i;j<=(i+kshortestPath.get(p).getnumFS()-1);j++) {
							expr.addTerm(w[d][p][i], 1);
							expr.addTerm(x[d][p][j], -1);
							cplex.addLe(expr, 0);
							expr.clear();
						}
					}
				}
			}
			//(1c)
			for (int d=0;d<demands.size();d++){
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<size;index++)
						kshortestPath.remove(3);
				}
				for (int p=0;p<kshortestPath.size();p++){
					for (int i=(C-kshortestPath.get(p).getnumFS()+1);i<C;i++){
						expr.addTerm(w[d][p][i], 1);
						cplex.addEq(expr, 0);
						expr.clear();
					}
				}
			}
			//(1d)
			//OCH are full duplex, so this constraint should assure that the channel c in link e (forward) and e' (reverse) is assigned only once.
			for (int e=0;e<net.getLinks().size();e++){
				Link linkf = net.GetLink(e);
				Link linkr =  net.searchLink(net.getLinks().get(e).GetDstNode(), net.getLinks().get(e).GetSrcNode());
				for (int f=0;f<C;f++){	
					for (int d=0;d<demands.size();d++){
						int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
						ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
						//k=3, 3 primeros(mejores) caminos
						ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
						int size= allshortestPath.size();
						if (size>3){
							for (int index=3;index<size;index++)
								kshortestPath.remove(3);
						}
						for (int p=0;p<kshortestPath.size();p++){
							if (containsLink(kshortestPath.get(p),linkf)||containsLink(allshortestPath.get(p),linkr)){ 
								expr.addTerm(x[d][p][f], 1);
							}
						}
					}
					expr.addTerm(y[e][f], -1);
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
				
				int fsi = 0;
				
				for(int f = 0; f<C; f++)
				{
					if(cplex.getValue(z[f])>=0.99 && cplex.getValue(z[f])<=1.01)
					{
						fsi++;
					}
				}
				System.out.println("Utilized Frequency Slots = "+fsi);
				
				//System.out.println("Utilized Frequency Slots = "+F);
				
				
				for(int d = 0; d<demands.size(); d++)
				{
					idsrc = demands.get(d).GetSrcNode().GetId();
					iddst = demands.get(d).GetDstNode().GetId();
					int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
					ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
					//k=3, 3 primeros(mejores) caminos
					ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
					int size= allshortestPath.size();
					if (size>3){
						for (int index=3;index<size;index++)
							kshortestPath.remove(3);
					}
					for(int p=0; p<kshortestPath.size();p++)
					{	
						int i=0;
						for (int f=0;f<C;f++){
							if(cplex.getValue(x[d][p][f])>=0.999 && cplex.getValue(x[d][p][f])<=1.001)
							{	
								if (i==0){
									System.out.print("Demand "+d+" utilizes candidate lightpath "+p+" : {");
									System.out.print(net.GetPaths(idsrc, iddst, idx-2).get(p).getSrcNode().GetId());
									for (Link link:net.GetPaths(idsrc, iddst,idx-2).get(p).GetPath()){
										System.out.print(","+link.GetDstNode().GetId());
									}
									System.out.print("}, with following ("+kshortestPath.get(p).getnumFS()+") frequency slots (id): {"+f);	
								}else System.out.print(","+f);
								i++;
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
