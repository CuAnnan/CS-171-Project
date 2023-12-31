package cs171.project2024.kearns.eamonn;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * A class to extend HexTile to handle the inclusion of minerals and the simulated mining and pollution
 */
public class ResourceTile extends HexTile
{
	/**
	 * An enum to handle resources in a generic sense.
	 * @see https://www.baeldung.com/java-enum-values
	 */
	public enum Resource
	{
		ORE("Ore"),
		WOOD("Wood"),
		WATER("Water"),
		OIL("Oil"),
		LIVESTOCK("Livestock"),
		FISSILE("Fissile Materials");
		public final String label;
		public static final Map<String, Resource> LABEL_MAP = new HashMap<>();

		private Resource(String label)
		{
			this.label = label;
		}

		static {
			for(Resource r: values())
			{
				LABEL_MAP.put(r.label, r);
			}
		}

		public static Resource byLabel(String label)
		{
			return LABEL_MAP.get(label);
		}
	};
	/**
	 * The array to contain the mount of each resource available to a tile
	 */
	private EnumMap<Resource, Double> resources;
	/**
	 * The array to represent the rate at which each resource is being extracted
	 */
	public EnumMap<Resource, Double> extractionRates;
	public static EnumMap<Resource, Double> extractionResearchModifier;
	/**
	 * The remaining resource of each type in the tile
	 */
	private EnumMap<Resource, Double> remainingResources;
	/**
	 * Whether or not this tile has a given resource.
	 */
	private EnumMap<Resource, Boolean> hasResource;
	
	/**
	 * A double to hold the minimum resource value, used in the constructor to create the RNG bounds
	 */
	private final static double MINIMUM_RESOURCE = 500.0;
	/**
	 * A double to hold the maximum resource value, used in the constructor to create the RNG bounds 
	 */
	private final static double MAXIMUM_RESOURCE = 1000.0;

	private boolean depleted = false;
	
	
	/**
	 * Constructor, instantiates the variables
	 * @param x The x coordinate of the tile
	 * @param y The y coordinate of the tile
	 */
	public ResourceTile(int x, int y)
	{
		super(x, y);
		
		this.resources = new EnumMap<>(Resource.class);
		this.remainingResources = new EnumMap<>(Resource.class);
		extractionRates = new EnumMap<>(Resource.class);
		this.hasResource = new EnumMap<>(Resource.class);
		Random rng = new Random();
		for(Resource r:Resource.values())
		{
			Boolean h = rng.nextBoolean();
			this.hasResource.put(r, h);
			if(h)
			{
				Double amount = MINIMUM_RESOURCE + (MAXIMUM_RESOURCE - MINIMUM_RESOURCE) * rng.nextDouble();
				this.remainingResources.put(r, amount);
				this.resources.put(r, amount);
				extractionRates.put(r, 0.0);
			}
			
		}
	}

	public void blankTile()
	{
		for(Resource r:Resource.values())
		{
			this.remainingResources.put(r, 0.0);
			this.resources.put(r, 0.0);
			this.hasResource.put(r, false);
		}
	}

	public void setAvailableResource(Resource resource, double amount)
	{
		this.remainingResources.put(resource, amount);
		this.resources.put(resource, amount);
		this.hasResource.put(resource, true);
	}
	
	/**
	 * Getter for a given resource
	 * @param resourceIndex This value should be one of the indices for the specific resource 
	 * @return The amount remaining of a given resource
	 */
	public double getResource(Resource r)
	{
		return this.resources.get(r);
	}

	public void setResourceExtractionRate(Resource r, double rate)
	{
		this.extractionRates.put(r, rate);
	}
	
	/**
	 * Determine if a tile has a resource or not
	 * @param resourceIndex This value should be one of the indices for the specific resource
	 * @return A boolean representing whether or not the specified resource is available at this tile
	 */
	public boolean hasResource(Resource r)
	{
		return this.hasResource.get(r);
	}
	
	/**
	 * Returns the unmined resource ratio
	 * @param resourceIndex
	 * @return
	 */
	public double getAvailableResource(Resource r)
	{
		return this.remainingResources.get(r) / this.resources.get(r);
	}
	
	/**
	 * Occupy the tile and return a reference to it for easy storage
	 * @return The tile being occupied
	 */
	public ResourceTile occupy()
	{
		this.occupied = true;
		this.explored = true;
		this.visited = true;
		return this;
	}
	
	/**
	 * A method to return the neighbours of this tile
	 * @return An ArrayList containing all of the Neighbours of this tile.
	 */
	public ArrayList<ResourceTile> getNeighbours()
	{
		ArrayList<ResourceTile> list = new ArrayList<ResourceTile>();
		for(HexTile t:this.neighbours.values())
		{
			list.add((ResourceTile)t);
		}
		return list;
	}
	
	/**
	 * @param The resource to process
	 * @return the amount of the given resource that gets processed.
	 */
	public double processResource(Resource r)
	{
		if(!this.hasResource(r))
		{
			return 0.0;
		}
		double amountTaken = Math.min(this.remainingResources.get(r), extractionRates.get(r));
		double amountLeft = this.remainingResources.get(r) - amountTaken;
		if(amountLeft <= 0.0)
		{
			amountLeft = 0.0;
			this.hasResource.put(r, false);
		}
		this.remainingResources.put(r, amountLeft);
		return amountTaken;
	}

	/**
	 * Get the array of all unexplored neighbours.
	 * @return	The list of unexplored neighbours
	 */
	public ArrayList<ResourceTile> getUnexploredNeighbours()
	{
		ArrayList<ResourceTile> neighbours = new ArrayList<ResourceTile>();
		for(HexTile.Direction d: this.connections.keySet())
		{
			if(this.connections.get(d))
			{
				if(!this.neighbours.get(d).explored)
				{
					neighbours.add((ResourceTile)this.neighbours.get(d));
				}
			}
		}
		return neighbours;
	}

	/**
	 * This is a method to mark a cell as being depleted.
	 */
	public void markDepleted()
	{
		this.depleted = true;
	}

	/**
	 * A method to check if a cell is depleted.
	 * @return	Returns true if the cell is depleted, false if it has remaining resources. Only 
	 */
	public boolean isDepleted()
	{
		return this.depleted;
	}
}
