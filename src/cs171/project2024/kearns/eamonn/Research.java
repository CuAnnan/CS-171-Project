package cs171.project2024.kearns.eamonn;

import java.util.EnumMap;
import java.util.Set;

import cs171.project2024.kearns.eamonn.ResourceTile.Resource;


public class Research
{
    protected final String name;
    protected final EnumMap<Resource, Double> costs;

    public Research(String name, EnumMap<Resource, Double> costs)
    {
        this.name = name;
        this.costs = costs;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean canAfford(EnumMap<Resource, Double> availableResources)
    {
        boolean canAfford = true;
        int i = 0;
        Set<Resource> resources = costs.keySet();
        while(i < resources.size() && canAfford)
        {
            canAfford = false;
        }
        return canAfford;
    }
}