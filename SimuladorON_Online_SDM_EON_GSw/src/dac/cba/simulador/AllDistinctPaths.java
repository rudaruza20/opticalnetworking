package dac.cba.simulador;

import java.util.ArrayList;
import java.util.Iterator;

public class AllDistinctPaths {
	 private Node source;
	 private Node destination;
	 private ArrayList<Path> paths;
	 private ArrayList<Node> currentPath;

	public AllDistinctPaths() {
		// TODO Auto-generated constructor stub
		this.source = null;
        this.destination = null;
        this.currentPath = null;
        this.paths = null;
	}
	public ArrayList<Path> ComputeAllDistinctPaths (Node src, Node dst){
		this.source = src;
        this.destination = dst;
        this.paths = new ArrayList<Path>();
        this.currentPath = new ArrayList<Node>();
        this.stepAhead(source);
        return this.paths;  
	}
	private void stepAhead (Node node)
    {
        this.currentPath.add(node);

        if (node.equals(this.destination))
        {
             this.paths.add(this.buildPath(this.currentPath));
        }
        else
        {
            Iterator<Link> it = node.GetOutLinks().iterator();

            while (it.hasNext())
            {
                Link link = it.next();
                Node destination = link.GetDstNode();
                
                if (!this.currentPath.contains(destination))
                {
                	this.stepAhead(destination);
                }
            }
        }

        this.currentPath.remove(this.currentPath.size()-1);
    }

    private Path buildPath (ArrayList<Node> sequence)
    {  
    	double length=0;
    	if (sequence.size() == 0)
    	{
    		System.out.println("ERROR: No path to build.");
    		System.exit(0);
    	}
    	
    	ArrayList<Link> linkList = new ArrayList<Link>();
        
        for (int i = 1; i < sequence.size(); i++)
        {
        	Node endPoint1 = sequence.get(i-1);
        	Node endPoint2 = sequence.get(i);
        	Link link = endPoint1.GetLinkToNeighbor(endPoint2);
        	length+=link.GetWeight();
        	linkList.add(link); 	
        }
        
        Path path = new Path (this.source, this.destination, linkList, sequence.size()-1, length);
        return path;  
    }
}


