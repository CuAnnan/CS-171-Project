package cs171.project2024.kearns.eamonn;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Stack;


public class Game
{
	/**
	 * A two dimensional array list to hold the ResourceTiles 
	 */
	private ArrayList<ArrayList<ResourceTile>> resourceTiles;
	
	/**
	 * A list of discovered tiles. Since we don't care about their positions, we can just handle them simply.
	 */
	private ArrayList<ResourceTile> discoveredTiles;
	
	/**
	 * The radius of the map hex grid in tiles.
	 */
	private int radius;
	private int diameter;
	
	private EnumMap<ResourceTile.Resource, Boolean> resourceDiscovered;
	private EnumMap<ResourceTile.Resource, Double> resourcesMined;
	private EnumMap<ResourceTile.Resource, Double> resourcesRemaining;
	
	public static EnumMap<ResourceTile.Resource, String> resourceNames = new EnumMap<>(ResourceTile.Resource.class);
	
	private final double BASIC_EXTRACTION_RATE = 0.1;
	
	
	private ResourceTile settlementTile;
	
	private int tileCount;
	
	
	/**
	 * 
	 * @param radius The radius of the map in tiles 
	 */
	public Game(int radius)
	{
		this.radius = radius;
		
		
		this.resourceTiles = new ArrayList<ArrayList<ResourceTile>>();
		this.discoveredTiles = new ArrayList<ResourceTile>();
		
		this.resourceDiscovered = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesMined = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesRemaining = new EnumMap<>(ResourceTile.Resource.class);
		
		resourceNames.put(ResourceTile.Resource.WOOD, 		"Wood");
		resourceNames.put(ResourceTile.Resource.WATER, 		"Water");
		resourceNames.put(ResourceTile.Resource.OIL, 		"Oil");
		resourceNames.put(ResourceTile.Resource.LIVESTOCK,	"Livestock");
		resourceNames.put(ResourceTile.Resource.FISSILE,	"Fissile Material");
		resourceNames.put(ResourceTile.Resource.ORE,		"Ore");
		
		
		for(ResourceTile.Resource r:ResourceTile.Resource.values())
		{
			this.resourcesMined.put(r, 0.0);
			this.resourceDiscovered.put(r, false);
			this.resourcesRemaining.put(r, 0.0);
		}
		
		
		// the middle row will have the map radius radius to one side, and that many minus one to the other.
		this.diameter = this.radius * 2 - 1;
		// the length of the row will go up and down depending on whether you've passed the center row
		int rowLength = radius;
	
		for(int i = 0; i < this.diameter; i++)
		{
			ArrayList<ResourceTile> row = new ArrayList<ResourceTile>();

			for(int j= 0; j < rowLength; j++)
			{
				row.add(new ResourceTile(j, i));
				tileCount++;
			}
			rowLength += i<this.radius-1?1:-1;
			
			this.resourceTiles.add(row);
		}
		
		this.settlementTile = this.resourceTiles.get(radius-1).get(radius-1).occupy();
		this.generateNeighbourConnections();
		this.generateTileConnections();
		this.discoverResource(ResourceTile.Resource.WOOD);
		this.discoverResource(ResourceTile.Resource.WATER);
		this.discoverResource(ResourceTile.Resource.LIVESTOCK);
		for(ResourceTile t: this.settlementTile.getNeighbours())
		{
			t.explore();
			this.discoveredTiles.add(t);
			for(ResourceTile.Resource r:this.resourceDiscovered.keySet())
			{
				t.setResourceExtractionRate(r, BASIC_EXTRACTION_RATE);
			}
		}
	}
	
	public Game()
	{
		this(3);
	}
	
	public ResourceTile getSettlementTile()
	{
		return this.settlementTile;
	}
	
	public void generateNeighbourConnections()
	{
		for(ArrayList<ResourceTile> row : this.resourceTiles)
		{
			for(ResourceTile tile: row)
			{
				if(tile.getX() + 1 < row.size())
				{
					tile.addNeighbour(row.get(tile.getX()+1), ResourceTile.Direction.WEST);
				}
				
				if(tile.getY() < this.resourceTiles.size()/2)
				{
					ArrayList<ResourceTile> below = this.resourceTiles.get(tile.getY()+1);
					if(tile.getX() >= 0 && tile.getX() < below.size())
					{
						tile.addNeighbour(below.get(tile.getX()), ResourceTile.Direction.SOUTHWEST);
					}
					
					if(tile.getY() > 0)
					{
						ArrayList<ResourceTile> above = this.resourceTiles.get(tile.getY() - 1);
						if(tile.getX() > 0 && tile.getX() < above.size()+1)
						{
							tile.addNeighbour(above.get(tile.getX() - 1), ResourceTile.Direction.NORTHWEST);
						}
					}
				}
				else if(tile.getY() == this.resourceTiles.size()/2)
				{
					ArrayList<ResourceTile> below = this.resourceTiles.get(tile.getY()+1);
					ArrayList<ResourceTile> above = this.resourceTiles.get(tile.getY() - 1);
					
					if(tile.getX() >= 0 && tile.getX() < below.size())
					{
						tile.addNeighbour(below.get(tile.getX()), ResourceTile.Direction.SOUTHEAST);
					}
					if(tile.getX() >0 && tile.getX() < above.size() + 1)
					{
						tile.addNeighbour(above.get(tile.getX()-1), ResourceTile.Direction.NORTHWEST);
					}
				}
				else
				{
					if(tile.getY() < this.resourceTiles.size() - 1)
					{
						ArrayList<ResourceTile> below = this.resourceTiles.get(tile.getY()+1);
						if(tile.getX() >= 0 && tile.getX() < below.size())
						{
							tile.addNeighbour(below.get(tile.getX()), ResourceTile.Direction.SOUTHEAST);							
						}
					}
					ArrayList<ResourceTile> above = this.resourceTiles.get(tile.getY() - 1);
					if(tile.getX() >= 0 && tile.getX() + 1 < above.size())
					{
						tile.addNeighbour(above.get(tile.getX() + 1), ResourceTile.Direction.NORTHEAST);
					}
				}
			}
		}
		
	}
	
	/**
	 * The method to generate the geography using a depth first map stack algorithm
	 * @see https://en.wikipedia.org/wiki/Maze_generation_algorithm#Iterative_implementation_(with_stack)
	 */
	public void generateTileConnections()
	{
		HexTile searchTile = this.settlementTile;
		searchTile.visit();
		
		Stack<HexTile> searchSpace = new Stack<HexTile>();
		searchSpace.push(searchTile);
		while(searchSpace.size() > 0)
		{
			searchTile = searchSpace.pop();
			// pick a random direction
			HexTile.Direction d = searchTile.getRandomUnvisitedNeighbourDirection();
			// if it's not null, there is an unvisited neighbour in that direction
			if(d != null)
			{
				searchTile.addConnection(d);
				searchSpace.push(searchTile);
				HexTile neighbour = searchTile.getNeighbour(d);
				neighbour.visit();
				searchSpace.push(neighbour);
			}
		}
		
		for(HexTile.Direction d:HexTile.Direction.values())
		{
			this.settlementTile.addConnection(d);
		}
	}
	
	public int getRadius()
	{
		return radius;
	}
	
	public int getDiameter()
	{
		return diameter;
	}
	
	public boolean isResourceDiscovered(ResourceTile.Resource r)
	{
		return resourceDiscovered.get(r);
	}
	
	public void discoverResource(ResourceTile.Resource r)
	{
		resourceDiscovered.put(r, true);
	}
	
	public ArrayList<ArrayList<ResourceTile>> getTiles()
	{
		return this.resourceTiles;
	}
	
	/**
	 * This is used for debugging purposes, but prints out the hex assuming all goes well.
	 */
	public String toString()
	{
		String[] output = new String[this.diameter];
		
		int i = 0;
		int padding = this.radius -1;
		
		for(ArrayList<ResourceTile> row:this.resourceTiles)
		{
			String[] rowStringArray = new String[row.size()];
			
			int j = 0;
			for(ResourceTile t: row)
			{
				rowStringArray[j] = t.toString();
				j++;
			}
			String rowString =String.join(" ", rowStringArray);
			
			for(int p = 0; p < padding; p++)
			{
				rowString = " "+rowString;
			}
			padding += i < this.radius - 1?-1:1;
			
			output[i] = rowString;
			i++;
		}
		return String.join("\n", output);
	}
	
	public int getTileCount()
	{
		return this.tileCount;
	}
	
	public double getResourceAvailable(ResourceTile.Resource r)
	{
		return this.resourcesRemaining.get(r);
	}
	
	void processTick()
	{
		ArrayList<ResourceTile> newlyDiscoveredNeighbouringTiles = new ArrayList<ResourceTile>();
		for(ResourceTile t: this.discoveredTiles)
		{
			boolean allKnownResourcesDepleted = true;
			for(ResourceTile.Resource r:ResourceTile.Resource.values())
			{
				if(this.isResourceDiscovered(r) && t.hasResource(r))
				{
					double amountProcessed = t.processResource(r);
					this.resourcesMined.put(r, this.resourcesMined.get(r) + amountProcessed);
					this.resourcesRemaining.put(r,  this.resourcesRemaining.get(r) + amountProcessed);
					if(t.getAvailableResource(r) > 0.0)
					{
						allKnownResourcesDepleted = false;
					}
				}
			}
			if(allKnownResourcesDepleted && !t.isDepleted())
			{
				t.markDepleted();
				for(ResourceTile neighbour: t.getUnexploredNeighbours())
				{
					newlyDiscoveredNeighbouringTiles.add(neighbour);
				}
			}
		}
		for(ResourceTile t: newlyDiscoveredNeighbouringTiles)
		{
			t.explore();
			this.discoveredTiles.add(t);
			for(ResourceTile.Resource r:this.resourceDiscovered.keySet())
			{
				t.setResourceExtractionRate(r, BASIC_EXTRACTION_RATE);
			}
		}
	}
}
