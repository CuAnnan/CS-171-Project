package cs171.project2024.kearns.eamonn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cs171.project2024.kearns.eamonn.HexTile.Direction;
import cs171.project2024.kearns.eamonn.ResourceTile.Resource;

/**
 * A class to encapsulate the bulk of the game's business logic
 */
public class Game
{
	/**
	 * All of the researches that can be learned, whether exposed to the player or not. Read in from a CSV.
	 * @see Game#RESEARCH_FILE_PATH
	 */
	private ArrayList<Research> researches = new ArrayList<Research>();
	/**
	 * The csv file determining the names and costs of the researches.
	 */
	private final static String RESEARCH_FILE_PATH = "./data/research.json";

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
	/**
	 * The diameter of the map hex grid
	 */
	private int diameter;
	
	/**
	 * Whether or not an individual resource type has been discovered
	 */
	private EnumMap<ResourceTile.Resource, Boolean> discoveredResources;
	/**
	 * The amount of each resource that has been mined overall
	 */
	private EnumMap<ResourceTile.Resource, Double> resourcesMined;
	/**
	 * The amount of resources available to the society
	 */
	private EnumMap<ResourceTile.Resource, Double> resourcesRemaining;
	/**
	 * The amount of pollution that mining an individual resource generates
	 */
	private EnumMap<ResourceTile.Resource, Double> pollutionRates;
	
	/**
	 * This is just a helper for setting up the initial conditions
	 */
	private final double BASIC_EXTRACTION_RATE = 0.00125;
	
	/**
	 * The settlment tile gets handled discretely
	 */
	private ResourceTile settlementTile;
	
	/**
	 * This is just used to help figure out how many tiles there are. Not sure if it's actually currently used.
	 */
	private int tileCount;


	
	/**
	 * The game's real constructor. Populates the properties, reads in the researches, sets the intial values, builds the data structure to represent the map and runs the depth-first maze algorithm to determine the geography/topology.
	 * @param radius The radius of the map in tiles 
	 */
	public Game(int radius)
	{
		this.radius = radius;

		this.resourceTiles = new ArrayList<ArrayList<ResourceTile>>();
		this.discoveredTiles = new ArrayList<ResourceTile>();
		
		this.discoveredResources = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesMined = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesRemaining = new EnumMap<>(ResourceTile.Resource.class);
		this.pollutionRates = new EnumMap<>(ResourceTile.Resource.class);
		
		/*
		 *	These may need tweaking 
		 */
		pollutionRates.put(ResourceTile.Resource.WOOD,		0.1);
		pollutionRates.put(ResourceTile.Resource.WATER,		0.001);
		pollutionRates.put(ResourceTile.Resource.OIL,		1.0);
		pollutionRates.put(ResourceTile.Resource.LIVESTOCK,	0.75);
		pollutionRates.put(ResourceTile.Resource.FISSILE,	1.0);
		pollutionRates.put(ResourceTile.Resource.ORE,		1.0);

		for(ResourceTile.Resource r:ResourceTile.Resource.values())
		{
			this.resourcesMined.put(r, 0.0);
			this.discoveredResources.put(r, false);
			this.resourcesRemaining.put(r, 0.0);
		}

		this.discoverResource(ResourceTile.Resource.WOOD);
		this.discoverResource(ResourceTile.Resource.WATER);
		this.discoverResource(ResourceTile.Resource.LIVESTOCK);
		
		
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
		
		// occupy the centre tile
		this.settlementTile = this.resourceTiles.get(radius-1).get(radius-1).occupy();
		// Set every tile to be beside the tiles they should be beside
		this.generateNeighbourConnections();
		

		// build the connections
		this.generateTileConnections();
		
		
		// load the research and "handle" the exceptions
		try
		{
			loadResearch();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("The research data csv could not be found");
		}
		catch(IOException e)
		{
			System.err.println("There was a problem reading the research data.");
		}
	}
	
	/**
	 * Secondary constructor to default the game radius to a fixed predetermined size.
	 */
	public Game()
	{
		this(12);
	}

	/**
	 * Loads the research from the file at RESEARCH_FILE_PATH and throws the exception back to the invoker.
	 * The initial intent was to use JSON, which is better structured than CSV, but understanding the libraries was taking longer than I was willing to devote to something this early in the development cycle.
	 * CSV will do for now
	 * @see Game#RESEARCH_FILE_PATH
	 * @throws IOException
	 */
	public void loadResearch() throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		File file = new File(RESEARCH_FILE_PATH);
		JsonNode node = mapper.readTree(file);
		for(JsonNode researchNode: node)
		{
			this.researches.add(
				Research.fromJsonNode(researchNode)
			);
		}
	}

	public ArrayList<Research> getResearches()
	{
		return this.researches;
	}

	/**
	 * This is where I will implement the functionality to buy research
	 * @param research
	 */
	public boolean buyResearch(Research research)
	{
		// ensuring no race condition prevents us going into debt
		boolean canAfford = true;
		for(Resource resource:research.getCosts().keySet())
		{
			if(this.getResourceAvailable(resource) < research.getCosts().get(resource))
			{
				canAfford = false;
			}
		}
		if(canAfford)
		{
			for(Resource resource:research.getCosts().keySet())
			{
				this.resourcesRemaining.put(resource, this.resourcesRemaining.get(resource) - research.getCosts().get(resource));
			}
			System.out.println(research);
			research.purchase();
			if(research.isRepeatable())
			{
				
			}
			else
			{
				// this is a special case research so handle it
				System.out.println("Special case research");
			}
		}
		return canAfford;
	}

	public ResourceTile getSettlementTile()
	{
		return this.settlementTile;
	}
	
	/**
	 * Iterate through the tiles and provide each tile its neighbours. 
	 * This is to facilitate an easier maze walking algorithm without tightly coupling the Game and Tile classes.
	 * The loop logic for this is a bit confounding because whether a downards neighbour in a two dimensional array is left or right of the cell above it depends on what row you are in.
	 * this is a fallout of representing hexes in the form
	 * ####
	 * #####
	 * ######
	 * #####
	 * ####
	 * 
	 * to mean
	 * 
	 *   # # #
	 *  # # # #
	 * # # # # #
	 *  # # # #
	 *   # # #
	 * @see Game#generateTileConnections()
	 * @see HexTile#addNeighbour()
	 */
	public void generateNeighbourConnections()
	{
		for(ArrayList<ResourceTile> row : this.resourceTiles)
		{
			for(ResourceTile tile: row)
			{
				// we always want to add left right relationships. They're nice and simple.
				if(tile.getX() + 1 < row.size())
				{
					tile.addNeighbour(row.get(tile.getX()+1), ResourceTile.Direction.EAST);
				}
				
				// if we're on a row that is above the centre row, we consider cells in the row below us to be left shifted
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
				// the centre row gets its own considerations as it has to be concerned with the rules for above and below differently. The row above is right shifted as is the row below. It is a unique case.
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
				// the rows below the centre row are right shifted
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

	private ResourceTile initialConnection(Direction direction)
	{
		ResourceTile hex = (ResourceTile)this.settlementTile.getNeighbour(direction);
		hex.blankTile();
		for(ResourceTile.Resource r:this.discoveredResources.keySet())
		{
			if(this.discoveredResources.get(r))
			{
				hex.setAvailableResource(r, 100.0);
				hex.setResourceExtractionRate(r, BASIC_EXTRACTION_RATE);
			}
		}
		this.settlementTile.addConnection(direction);
		this.discoveredTiles.add(hex);
		hex.visit();
		hex.explore();
		return hex;
	}
	
	/**
	 * The method to generate the geography using a depth first map stack algorithm
	 * @see https://en.wikipedia.org/wiki/Maze_generation_algorithm#Iterative_implementation_(with_stack)
	 */
	public void generateTileConnections()
	{
		HexTile nw = this.initialConnection(Direction.NORTHWEST),
				sw = this.initialConnection(Direction.SOUTHWEST),
				e  = this.initialConnection(Direction.EAST);
		
		ArrayList<Stack<HexTile>> searchSpaces = new ArrayList<Stack<HexTile>>();
		searchSpaces.add(new Stack<HexTile>());
		searchSpaces.add(new Stack<HexTile>());
		searchSpaces.add(new Stack<HexTile>());
		searchSpaces.get(0).push(nw);
		searchSpaces.get(1).push(sw);
		searchSpaces.get(2).push(e);

		while(searchSpaces.get(0).size() > 0 || searchSpaces.get(1).size() > 0 || searchSpaces.get(2).size() > 0)
		{
			HexTile searchTile = null;
			for(Stack<HexTile> searchSpace:searchSpaces)
			{
				if(searchSpace.size() > 0)
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
			}
		}
		
	}
	
	/**
	 * Getter for the radius of the game in tiles
	 * @return	The radius of the game in tiles.
	 */
	public int getRadius()
	{
		return radius;
	}
	
	/**
	 * Getter for the diameter of the game in tiles
	 * @return	The radius of the game in tiles
	 */
	public int getDiameter()
	{
		return diameter;
	}

	public ArrayList<Resource> getDiscoveredResources()
	{
		ArrayList<Resource> resources =  new ArrayList<Resource>();
		for(Resource resource:this.discoveredResources.keySet())
		{
			if(this.discoveredResources.get(resource))
			{
				resources.add(resource);
			}
		}
		return resources;
	}
	
	/**
	 * A method to determine if a given resource has been discovered
	 * @param r	The reseource to check
	 * @return	Whether or not the resource has been discovered
	 */
	public boolean isResourceDiscovered(ResourceTile.Resource r)
	{
		return discoveredResources.get(r);
	}
	
	/**
	 * A method to mark a resource as discobvered
	 * @param r	The resource to discover
	 */
	public void discoverResource(ResourceTile.Resource r)
	{
		discoveredResources.put(r, true);
	}
	
	/**
	 * A method to get the 2d array list of tiles
	 * @return
	 */
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
	
	/**
	 * This was useful in early development. I am uncertain if it has any long term benefit but will be leaving it in for the time being.
	 * @return	The count of tiles in the game
	 */
	public int getTileCount()
	{
		return this.tileCount;
	}
	
	/**
	 * Determine the amount of a given resource available for use
	 * @param resiource	The resource to get available amounts of
	 * @return			The amount of the given resource
	 */
	public double getResourceAvailable(ResourceTile.Resource resource)
	{
		return this.resourcesRemaining.get(resource);
	}
	
	/**
	 * This method processes the passage of arbitrary time. What a tick is is largely irrelevant. During a tick, resources get mined and added to the total.
	 * Implementing the movement of resources, rather than the instantaneous acquisition of them, is relatively high priority but needs to come after research.
	 */
	public void processTick()
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
			for(ResourceTile.Resource r:this.discoveredResources.keySet())
			{
				t.setResourceExtractionRate(r, BASIC_EXTRACTION_RATE);
			}
		}
	}
}
