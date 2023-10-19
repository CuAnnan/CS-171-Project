package cs171.project2024.kearns.eamonn;

import java.util.EnumMap;

import cs171.project2024.kearns.eamonn.ResourceTile.Resource;


public class Research
{
    protected final String name;
    protected final EnumMap<Resource, Double> costs;
    protected boolean complete;

    public Research(String name, EnumMap<Resource, Double> costs)
    {
        this(name, costs, false);
    }
    
    public Research(String name, EnumMap<Resource, Double> costs, boolean complete)
    {
        this.name = name;
        this.costs = costs;
        this.complete = complete;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean canAfford(EnumMap<Resource, Double> availableResources)
    {
        boolean canAfford = true;
        for(Resource r: this.costs.keySet())
        {
            if(availableResources.get(r) < this.costs.get(r))
            {
                canAfford = false;
            }
        }
        return canAfford;
    }

    public boolean isResearched()
    {
        return this.complete;
    }

    public String toString()
    {
        String out = String.format("[%s:\n", this.name);
        for(Resource r : this.costs.keySet())
        {
            out+= String.format("\t%s: %.2f\n", r.label, this.costs.get(r));
        }
        out += "]";
        return out;
    }
}