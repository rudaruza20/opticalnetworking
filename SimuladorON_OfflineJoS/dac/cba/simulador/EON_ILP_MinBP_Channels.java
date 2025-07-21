/*
 * ILP implementation suggested in: Modeling the routing and spectrum allocation problem for flexgrid optical networks, Luis Velasco, et al.
 */

package dac.cba.simulador;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.DefaultTableModel;

import ilog.concert.*;
import ilog.cplex.*;

public class EON_ILP_MinBP_Channels {
	private long t0, t1,t2;
	IloCplex cplex;
	IloNumVar[] x;
	IloNumVar[][][] y;
	
	private long construction_time,solution_time;
	
	
	public EON_ILP_MinBP_Channels() {
		// TODO Auto-generated constructor stub
		try 
		{
			cplex = new IloCplex();
		} catch (IloException e)
		{
		}
	}
	//C: Capacity of the Spectrum of the links.  
	public void initialize(Network net,ArrayList<Demand> demands, DefaultTableModel formats, int S)
	{
		//C : number of optical Channels (Lambdas)
		t0 = System.currentTimeMillis();
		
		// Initialize Cplex environment
		try
		{			
			IloLinearNumExpr expr = cplex.linearNumExpr();
			
			
			int idsrc, iddst;
			// Cplex parameters
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3); //IMPORTANTE:  si se excede el tamaño de la memoria lo guarda en disco
			cplex.setParam(IloCplex.DoubleParam.EpGap, 0.05); //IMPORTANTE: Devuelve la solución cuando haya alcanzado el 95% de ejecución de la optimización
			//cplex.setParam(IloCplex.IntParam.VarSel, 3);
			cplex.setParam(IloCplex.DoubleParam.TiLim, 280); //IMPORTANTE: Tiempo limite en SEG de ejecución y devuelve la solución		
			
			
			//fin
			// Variables
			//Boolean Variables x(d)
			//x = new IloNumVar[demands.size()];//
			x = cplex.boolVarArray(demands.size());
			
			
			//Boolean Variables y(d,p,c)
			y = new IloNumVar[demands.size()][][];
			for (int d=0;d<demands.size();d++){
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				idsrc = demands.get(d).GetSrcNode().GetId();
				iddst = demands.get(d).GetDstNode().GetId();
				y[d]= new IloNumVar[net.GetPaths(idsrc, iddst, idx-2).size()][];
				for (int p=0; p<net.GetPaths(idsrc, iddst, idx-2).size();p++){
				//for (int p=0;p<;p++){ //max p=3
					y[d][p]= cplex.boolVarArray(319); // max c=319 (with 2 FS)
				}	
			}
			
			
			// Constraints
			//(1a)
			//All demands 
			for (int d=0;d<demands.size();d++){
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
				/*
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<size;index++)
						kshortestPath.remove(3);
				}
				System.out.println("Kshortest path size: "+kshortestPath.size());
				*/
				//for (int p=0;p<kshortestPath.size();p++){
				for (int p=0;p<allshortestPath.size();p++){
					for (int c=0;c<allshortestPath.get(p).getNumberOfSetOfChannels();c++)
						expr.addTerm(y[d][p][c], 1);
				}
				expr.addTerm(x[d], 1);
				cplex.addEq(expr, 1);
				expr.clear();
			}
			// (1b)
			for (int e=0;e<net.getLinks().size();e++){
				Link linkf = net.GetLink(e);
				Link linkr = net.searchLink(net.getLinks().get(e).GetDstNode(), net.getLinks().get(e).GetSrcNode());
				for (int s=0;s<S;s++){	
					for (int d=0;d<demands.size();d++){
						int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
						ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
						/*
						//k=3, 3 primeros(mejores) caminos
						ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
						int size= allshortestPath.size();
						if (size>3){
							for (int index=3;index<size;index++)
								kshortestPath.remove(3);
						}
						*/
						//for (int p=0;p<kshortestPath.size();p++){
						for (int p=0;p<allshortestPath.size();p++){
							if (containsLink(allshortestPath.get(p),linkf)||containsLink(allshortestPath.get(p),linkr)){ //OCH are full duplex, so this constraint should ensure that the channel c in link e (forward and reverse) is assigned only once. 
								for (int c=0;c<allshortestPath.get(p).getNumberOfSetOfChannels();c++){
									if (containsSlot(allshortestPath.get(p).getChannels().get(c),s)){
										expr.addTerm(y[d][p][c], 1);
									}
								}
							}			
						}
					}
					cplex.addLe(expr, 1);
					expr.clear();
				}
			}
			
			// Objective function
			
			
			for (int d=0;d<demands.size();d++){
				expr.addTerm(x[d],1+(1+d)*10E-4);
			}
			
			IloObjective objective_function = cplex.minimize(expr);
			cplex.add(objective_function);
			
			
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
	
	private boolean containsSlot (Channel c, int s){
		
		
		if (s>=c.getstartFS() && s<=c.getendFS()){
			return true;
		}
		else {
			return false;
		}
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
				
				int d_blocked=0;
				for (int d=0;d<demands.size();d++){
					if (cplex.getValue(x[d])>=0.99 && cplex.getValue(x[d])<=1.01){
						d_blocked++;
					}
				}
					
				
				ArrayList<Integer> join = new ArrayList<Integer>(); //Set of active fs (frequency slots)
				int activeFS =0;
				int reqFS = 0;
				for (int d=0;d<demands.size();d++){
					int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
					ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
					idsrc = demands.get(d).GetSrcNode().GetId();
					iddst = demands.get(d).GetDstNode().GetId();
					/*
					//k=3, 3 primeros(mejores) caminos
					ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
					int size= allshortestPath.size();
					if (size>3){
						for (int index=3;index<size;index++)
							kshortestPath.remove(3);
					}
					*/
					//for (int p=0;p<kshortestPath.size();p++){
					for (int p=0;p<allshortestPath.size();p++){
						for (int c=0;c<allshortestPath.get(p).getNumberOfSetOfChannels();c++){
							if(cplex.getValue(y[d][p][c])>=0.99 && cplex.getValue(y[d][p][c])<=1.01){
								System.out.print("Demand "+d+" utilizes candidate lightpath "+p+" : {");
								System.out.print(net.GetPaths(idsrc, iddst, idx-2).get(p).getSrcNode().GetId());
								for (Link link:net.GetPaths(idsrc, iddst, idx-2).get(p).GetPath()){
									System.out.print(","+link.GetDstNode().GetId());
								}
								System.out.println("}, with following frequency slots (id): {"+allshortestPath.get(p).getChannels().get(c).getstartFS()+".."+allshortestPath.get(p).getChannels().get(c).getendFS()+"}");
								reqFS+=allshortestPath.get(p).getnumFS();
								for (int i=allshortestPath.get(p).getChannels().get(c).getstartFS();i<=allshortestPath.get(p).getChannels().get(c).getendFS();i++){
									join.add(i);									
								}
							}
						}
					}
				}
				
				Collections.sort(join);
				int n=join.size();
				for (int i=0; i<n;i++){
					for (int j=i+1; j<n;j++){
						if (join.get(i).equals(join.get(j))){
							join.remove(j);
							j--;
							n = join.size();
						}
					}
				}
				
				
				System.out.println("First index of active FS: "+join.get(0));
				System.out.println("Last index of active FS: "+join.get(join.size()-1));
			
				
				System.out.println("Total Number of Require Frequency Slot: "+reqFS);
				System.out.println("Total Number of Active(Used) Frequency Slots: "+join.size());
				System.out.printf("Reuse Factor: %.2f",((double)((double)reqFS/(double)join.size()))*100);	
				System.out.println("%");
				System.out.println("Total number of blocked (unserved)  Demands: "+d_blocked);
				System.out.printf("Blocking Factor: %.2f", ((double)d_blocked/(double)demands.size())*100);
				System.out.println("%");
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
