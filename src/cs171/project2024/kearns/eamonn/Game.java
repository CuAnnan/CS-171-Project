package cs171.project2024.kearns.eamonn;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Stack;

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
	private final static String RESEARCH_FILE_PATH = "./data/research.csv";
	/**
	 * The available researches. This is populated by a somewhat expensive loop and so it is advisable to throttle its upkeep or develop a better algorithm.
	 * @see Game#updateAvailableResearches()
	 */
	private ArrayList<Research> availableResearches;

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
	private EnumMap<ResourceTile.Resource, Boolean> resourceDiscovered;
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
	private final double BASIC_EXTRACTION_RATE = 0.25;
	
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
		
		this.resourceDiscovered = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesMined = new EnumMap<>(ResourceTile.Resource.class);
		this.resourcesRemaining = new EnumMap<>(ResourceTile.Resource.class);
		this.pollutionRates = new EnumMap<>(ResourceTile.Resource.class);

		this.availableResearches = new ArrayList<Research>();
		
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
		
		// occupy the centre tile
		this.settlementTile = this.resourceTiles.get(radius-1).get(radius-1).occupy();
		// Set every tile to be beside the tiles they should be beside
		this.generateNeighbourConnections();
		// build the connections
		this.generateTileConnections();
		
		// mark wood, water and livestock as discovered
		this.discoverResource(ResourceTile.Resource.WOOD);
		this.discoverResource(ResourceTile.Resource.WATER);
		this.discoverResource(ResourceTile.Resource.LIVESTOCK);
		// for(ResourceTile.Resource r: ResourceTile.Resource.values())
		// {
		// 	this.discoverResource(r);
		// }

		
		// reveal the initial neighbours of the settlement tile
		for(ResourceTile t: this.settlementTile.getNeighbours())
		{
			t.explore();
			this.discoveredTiles.add(t);
			for(ResourceTile.Resource r:this.resourceDiscovered.keySet())
			{
				t.setResourceExtractionRate(r, BASIC_EXTRACTION_RATE);
			}
		}

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
		this(10);
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
		BufferedReader reader = new BufferedReader(new FileReader(RESEARCH_FILE_PATH));
		String currentLine = reader.readLine();
		// we don't actually do anything with this line
		boolean firstLine = true;
		ArrayList<String> researchTitles = new ArrayList<String>();
		while(currentLine != null)
		{
			String[] lineParts = currentLine.split(",");
			if(firstLine)
			{
				for(String part:lineParts)
				{
					researchTitles.add(part);
				}
			}
			else
			{
				EnumMap<Resource, Double> resources = new EnumMap<>(Resource.class);
				for(int i = 1; i < lineParts.length; i++)
				{
					if(lineParts[i] != "")
					{
						resources.put(Resource.byLabel(researchTitles.get(i)), Double.parseDouble(lineParts[i]));
					}
				}
				Research r = new Research(lineParts[0], resources);
				this.researches.add(r);
			}
			firstLine = false;
			currentLine = reader.readLine();
		}
		reader.close();
	}

	/**
	 * Return the researches that are available to be performed
	 * @return	An ArrayList of Research objects
	 * @see		Research
	 */
	public ArrayList<Research> getAvailableResearches()
	{
		return this.availableResearches;
	}

	/**
	 * Iterates through all of the researches and, if they can be afforded, adds them to the available researches property.
	 * This has been separated from getAvailableResearches because, for one reason or another, clicks were not registering on it. It may have been a race condition between the draw and the update of the variable.
	 * @see Research#canAfford()
	 */
	public void updateAvailableResearches()
	{
		ArrayList<Research> researches = new ArrayList<Research>();
		for(Research r:this.researches)
		{
			if(!r.isResearched() && r.canAfford(this.resourcesMined))
			{
				researches.add(r);
			}
		}
		this.availableResearches = researches;
	}

	/**
	 * This is where I will implement the functionality to buy research
	 * @param r
	 */
	public void buyResearch(Research r)
	{
		System.out.println(r);
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
		/*
		 * 
		 * 
		 */
		for(ArrayList<ResourceTile> row : this.resourceTiles)
		{
			for(ResourceTile tile: row)
			{
				// we always want to add left right relationships. They're nice and simple.
				if(tile.getX() + 1 < row.size())
				{
					tile.addNeighbour(row.get(tile.getX()+1), ResourceTile.Direction.WEST);
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
	
	/**
	 * A method to determine if a given resource has been discovered
	 * @param r	The reseource to check
	 * @return	Whether or not the resource has been discovered
	 */
	public boolean isResourceDiscovered(ResourceTile.Resource r)
	{
		return resourceDiscovered.get(r);
	}
	
	/**
	 * A method to mark a resource as discobvered
	 * @param r	The resource to discover
	 */
	public void discoverResource(ResourceTile.Resource r)
	{
		resourceDiscovered.put(r, true);
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
	 * 
	 */
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
