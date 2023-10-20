package cs171.project2024.kearns.eamonn;

import java.util.EnumMap;

import cs171.project2024.kearns.eamonn.ResourceTile.Resource;

/**
 * A class to represent the researches which can be repeated indefenitely to increase efficiency.
 */
public class RepeatableResearch extends Research
{
    /**
     * Basic constructor for repeatable researches
     * @param name              The name of the research
     * @param costs             The associated costs
     * @param complete          Whether or not the research has been completed
     * @param effect   
     */
    public RepeatableResearch(String name, EnumMap<Resource, Double> costs, boolean complete, EnumMap<Resource, Double> multiplierAmount)
    {
        super(name, costs, complete);
    }    
}
