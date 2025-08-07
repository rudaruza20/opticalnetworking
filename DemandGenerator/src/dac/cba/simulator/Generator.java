package dac.cba.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import javax.swing.table.DefaultTableModel;

public class Generator {
	private static DefaultTableModel SetOfProfiles;

	public Generator() {
		// TODO Auto-generated constructor stub
	}
	public static DefaultTableModel ReadProfile (){
		int NumberOfProfiles;
		double SumOfProbabilities=0;
		System.out.println("Enter Number Of Profiles: ");
		Scanner nprofiles = new Scanner (System.in);
		NumberOfProfiles = nprofiles.nextInt();
		SetOfProfiles = new DefaultTableModel(2,NumberOfProfiles);
		for (int column=0;column<NumberOfProfiles;column++){
			System.out.println("Enter Profile "+(column+1)+":");
			int row=0;
			System.out.println("\tEnter BitRate: ");
			Scanner bitrate = new Scanner (System.in);
			double BitRate = bitrate.nextDouble(); // BitRate converted to Double Format
			System.out.println("\tEnter Probability: ");
			Scanner probability = new Scanner (System.in);
			double Probability= probability.nextDouble();
			SetOfProfiles.setValueAt(BitRate, row, column);
			SetOfProfiles.setValueAt(Probability, row+1, column);
			SumOfProbabilities += Probability;
		}
		if (SumOfProbabilities == 1.0)
			System.out.println("OK");
		else{
			System.out.println("ERROR: The sum of probabilities is not equal to 1.0");
			System.exit(0);
		}
		return SetOfProfiles; 
	}
	
	public static DefaultTableModel DemandGen(int NumberOfNodes, int NumberOfDemands){
		int srcnode, dstnode, i;
		double sum=0, BitRate = 0; 
		Double[] Intervals = new Double[SetOfProfiles.getColumnCount()];//Cumulative Probability
		double Probability; //Relative probability, the real probability of occurrence is in SetOfprofiles Table. 
		DefaultTableModel Demands = new DefaultTableModel (NumberOfDemands, 4);
		Random randomsrc = new Random();
		Random randomdst = new Random();
		Random randomprb = new Random();
		for (i=0;i<SetOfProfiles.getColumnCount();i++){
			sum+=(Double)SetOfProfiles.getValueAt(1, i);
			Intervals[i]= sum;
		}
		for (int row=0;row<NumberOfDemands;row++){
			Probability = randomprb.nextDouble();
			srcnode = randomsrc.nextInt(NumberOfNodes);
			do
				dstnode=randomdst.nextInt(NumberOfNodes);	
			while (srcnode == dstnode);
			for (i=0;i<SetOfProfiles.getColumnCount();i++){
				if (Probability <= Intervals[i]){
					BitRate = (Double)SetOfProfiles.getValueAt(0, i);
					break;
				}
			}
			Demands.setValueAt(row, row, 0);
			Demands.setValueAt(srcnode, row, 1);
			Demands.setValueAt(dstnode, row, 2);
			Demands.setValueAt(BitRate, row, 3);
		}
		return Demands; 
		
	}
	public static void WriteFile(String path, DefaultTableModel Demands) throws IOException{
		File file = new File(path);
		BufferedWriter bw;
		bw = new BufferedWriter(new FileWriter(file));
		System.out.println("Writing.... ");
		for (int i=0; i<Demands.getRowCount();i++){
			int id = (Integer)Demands.getValueAt(i, 0);
			int src = (Integer)Demands.getValueAt(i, 1);
			int dst = (Integer)Demands.getValueAt(i, 2);
			double BitRate= (Double)Demands.getValueAt(i, 3); 
			System.out.println(id+","+src+","+dst+","+BitRate);
			bw.write(id+","+src+","+dst+","+BitRate+"\n");
			
		}
	    bw.close();
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		int NumberOfNodes;
		int NumberOfDemands;
		String path = "C:/Users/Ruben/(500)Demands_9ntest9.txt";
		System.out.println("Enter Number Of Nodes: ");
		Scanner nnodes = new Scanner (System.in);
		NumberOfNodes = nnodes.nextInt();
		System.out.println("Enter Number Of Demands: ");
		Scanner ndemands = new Scanner (System.in);
		NumberOfDemands = ndemands.nextInt();
		ReadProfile();
		DefaultTableModel Demands = DemandGen(NumberOfNodes, NumberOfDemands);
		WriteFile (path, Demands);
	}
}

