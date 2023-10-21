package cs171.project2024.kearns.eamonn;

import java.util.EnumMap;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

import cs171.project2024.kearns.eamonn.ResourceTile.Resource;

/**
 * A class to encapsulate the costs associated with Research.
 */
public class Research
{
    /**
     * The name of the research item
     */
    protected final String name;
    /**
     * A map of the resource to cost, such that each research will have one or more resource cost.
     */
    protected final EnumMap<Resource, Double> costs;
    /**
     * A boolean to determine if the research has been completed
     */
    protected boolean complete;
    protected boolean repeatable;

    /**
     * A helper constructor for new researches that are not complete.
     * @param name  The name of the research item.
     * @param costs The costs associated with it as an EnumMap of Resources to Doubles
     */
    public Research(String name, EnumMap<Resource, Double> costs)
    {
        this(name, costs, false, false);
    }
    
    /**
     * A more complete constructor for the creation of Researches
     * @param name      The name of the research item.
     * @param costs     The costs associated with it as an EnumMap of Resources to Doubles
     * @param complete  Whether or not the research has already been complete
     */
    public Research(String name, EnumMap<Resource, Double> costs, boolean complete, boolean repeatable)
    {
        this.name = name;
        this.costs = costs;
        this.complete = complete;
        this.repeatable = repeatable;
    }

    /**
     * Getter for the research name
     * @return  The name of the research
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * A method to determine, given the amount of each resource available, whether this research can be afforded
     * @param availableResources    AN enum of the resources required.
     * @return                      Whether or not the research can be afforded given the amount of resources presented.
     */
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

    public EnumMap<Resource, Double> getCosts()
    {
        return this.costs;
    }

    /**
     * Getter for whether the research has been concluded.
     * @return
     */
    public boolean isResearched()
    {
        return this.complete;
    }

    /**
     * Set the complete field to true
     */
    public void purchase()
    {
        this.complete = true;
    }

    public boolean isRepeatable()
    {
        return this.repeatable;
    }

    /**
     * Convert a Jackson JsonNode to a Research Object
     * @param json  The Jackson JsonNode object representing a Research.
     * @return      The Research Object
     */
    public static Research fromJsonNode(JsonNode json)
    {
        
        EnumMap<Resource, Double> costs = new EnumMap<>(Resource.class);
        JsonNode costsNode = json.get("costs");
        Iterator<String> iterator = costsNode.fieldNames();
        boolean complete = json.has("complete")?json.get("complete").asBoolean():false;
        boolean repeatable = json.has("repeatable")?json.get("repeatable").asBoolean():false;
        iterator.forEachRemaining(e->{
            costs.put(
                Resource.byLabel(e),
                Double.parseDouble(costsNode.get(e).asText())
            );
        });
        Research r = new Research(json.get("name").asText(), costs, complete, repeatable);
        return r;
    }

    /**
     * A helper function that allows the Research be printed to the screen.
     */
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