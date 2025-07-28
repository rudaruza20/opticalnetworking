
/*
 * ILP implementation suggested in: Routing and Spectrum Assignment in
Spectrum Sliced Elastic Optical Path Network, Mirosław Klinkowski and Krzysztof Walkowiak
 */

package dac.cba.simulador;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

import ilog.concert.*;
import ilog.cplex.*;

public class EON_SDM_ILP_MinFS {
	private long t0, t1,t2;
	IloCplex cplex;
	IloNumVar[] x;
	IloNumVar[][] y;
	IloNumVar[] z;
	private long construction_time,solution_time;
	ArrayList<Path> paths =  new ArrayList<Path>();
	
	public EON_SDM_ILP_MinFS() {
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
			IloLinearNumExpr expr2 = cplex.linearNumExpr();
			// Cplex parameters
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3); //IMPORTANTE:  si se excede el tamaño de la memoria lo guarda en disco
			cplex.setParam(IloCplex.DoubleParam.EpGap, 0.02); //IMPORTANTE: Devuelve la solución cuando haya alcanzado el 95% de ejecución de la optimización
			//cplex.setParam(IloCplex.IntParam.VarSel, 3);
			cplex.setParam(IloCplex.DoubleParam.TiLim, 43200); //IMPORTANTE: Tiempo limite en SEG de ejecución y devuelve la solución		
			
			//Find-out total number of candidate lightpaths
			int nL = 0; //number of total candidate lightpaths U(d\D L_d)
			for (int d=0;d<demands.size();d++){
				int idx = formats.findColumn(Double.toString(demands.get(d).GetBitRate()));
				ArrayList<Path> allshortestPath =  net.GetPaths(demands.get(d).GetSrcNode().GetId(), demands.get(d).GetDstNode().GetId(), idx-2);
				//k=3, 3 primeros(mejores) caminos
				ArrayList<Path> kshortestPath = (ArrayList<Path>)allshortestPath.clone();
				int size= allshortestPath.size();
				if (size>3){
					for (int index=3;index<(size);index++)
						kshortestPath.remove(3);
				}
				if (size==0){
					System.out.println("Demand "+demands.get(d).GetId()+" is outside max. reach");
					demands.remove(d); //remove demands outside from max. reach.  
				}
				for (Path p:kshortestPath){
					for (Channel c:p.getChannels()){
						nL++;
					}
				}
			}
			
			System.out.println("Total number of L_d: "+nL);	
			//end
			
			// Variables
			// Boolean Variables x(l)
			x = cplex.boolVarArray(nL);
			// Boolean Variables y(e,s)
			y = new IloNumVar[net.getLinks().size()][];		
			for(int e = 0; e<net.getLinks().size(); e++)
			{
				y[e] = cplex.boolVarArray(S);
			}			
			// Boolean Variables z(s)			
			z = cplex.boolVarArray(S);
			// Constraints
			//(1a)
			int l=0;
			for (Demand d:demands){
				//System.out.println("demand: "+demands.get(d).GetId());
				int pos = formats.findColumn(Double.toString(d.GetBitRate()));
				int j=1;
				for (Path p: net.GetPaths(d.GetSrcNode().GetId(), d.GetDstNode().GetId(), pos-2)){
					for (Channel c:p.getChannels()){
						expr.addTerm(x[l], 1);
						l++;
					}
					if (j>=3) break; //k=3
					j++;
				}
				cplex.addEq(expr, 1);
				expr.clear();
			}
			
			//(1b)
			int shift=0;
			int e_ = 0;
			for (Link e:net.getLinks()){
				for (int s=0;s<S;s++){
					l=0;
					for (Demand d:demands){
						//System.out.println("demand: "+demands.get(d).GetId());
						int pos = formats.findColumn(Double.toString(d.GetBitRate()));
						int j=1;
						for (Path p: net.GetPaths(d.GetSrcNode().GetId(), d.GetDstNode().GetId(), pos-2)){
							if (containsLink(p,e)){
								for (Channel c:p.getChannels()){
									if (c.containsSlot(s)){
										expr.addTerm(x[l], 1);
									}
									l++;
								}
							}
							else{
								shift = p.getChannels().size();
								l+=shift;
							}
							if (j>=3) break; //k=3
							j++;
						}
					}
					expr2.addTerm(y[e_][s], 1);
					cplex.addLe(expr, expr2);
					expr.clear();
					expr2.clear();
				}
				e_++;
			}
			//(1c)
			for(int s = 0; s<S; s++)
			{
				expr2.addTerm(z[s], net.getLinks().size());
				
				for(int e = 0; e<net.getLinks().size(); e++)
				{
					expr.addTerm(y[e][s], 1);
				}
				
				cplex.addLe(expr, expr2);
				expr.clear();
				expr2.clear();
			}
			
			// Objective function
			for(int s = 0; s<S; s++)
			{
				expr.addTerm(z[s],1+(1+s)*10E-4);
				//expr.addTerm(z[s],1);
			}
			
			l=0;
			for (Demand d:demands){
				//System.out.println("demand: "+demands.get(d).GetId());
				int pos = formats.findColumn(Double.toString(d.GetBitRate()));
				int j=1;
				for (Path p: net.GetPaths(d.GetSrcNode().GetId(), d.GetDstNode().GetId(), pos-2)){
					for (Channel c:p.getChannels()){
						expr.addTerm(x[l], p.getHops()*p.getnumFS()*10E-4);
						l++;
					}
					if (j>=3) break;
					j++;
				}
			}
			
			IloObjective objective_function = cplex.minimize(expr);
			cplex.add(objective_function);
			expr.clear();
			
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
	
	public void solve(Network net, ArrayList<Demand> demands, int S, DefaultTableModel formats)
	{				
		try
		{
			t1 = System.currentTimeMillis();
			
			cplex.solve();
			
			t2 = System.currentTimeMillis();
			
			if(cplex.getStatus()!=IloCplex.Status.Infeasible)
			{	
				construction_time = t1-t0;
				
				solution_time = t2-t1;
				
				System.out.println("\nTime to construct the model is "+(construction_time)/1000.0+" sec.");
				
				System.out.println("Found feasible solution in "+(solution_time)/1000.0+" sec.");
				
				
				int index = 0, maxIndex=0;
				for(int f = 0; f<S; f++)
				{
					if(cplex.getValue(z[f])>=0.99 && cplex.getValue(z[f])<=1.01)
					{
						//System.out.println("FS index assigned: "+f);
						index++;
						if (f>maxIndex) maxIndex=f;
					}
				}
				
				//System.out.println("Utilized Frequency Slots = "+F);
				
				int l=0, t_slots=0;
				for (Demand d:demands){
					//System.out.println("demand: "+demands.get(d).GetId());
					int pos = formats.findColumn(Double.toString(d.GetBitRate()));	
					int j=1;
					for (Path p: net.GetPaths(d.GetSrcNode().GetId(), d.GetDstNode().GetId(), pos-2)){
						for (Channel c:p.getChannels()){
							if(cplex.getValue(x[l])>=0.99 && cplex.getValue(x[l])<=1.01){
								System.out.print("Demand "+d.GetId()+" utilizes candidate path "+j+": "+p.GetPath().get(0).GetSrcNode().GetId());
								for (Link e : p.GetPath()){
									System.out.print("-"+e.GetDstNode().GetId());
								}
								System.out.println(" and FSs(id) from "+c.getstartFS()+" to "+c.getendFS());
								t_slots+=p.getnumFS()*p.getHops();
							}
							l++;
						}
						if (j>=3) break;
						j++;
					}
				}
				System.out.println("Utilized Frequency Slots = "+index);
				System.out.println("max FS(id) used in any MF/MCF link = "+maxIndex);
				System.out.println("Total nFSs network-wide: "+t_slots*22);
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


