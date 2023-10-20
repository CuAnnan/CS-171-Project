package cs171.project2024.kearns.eamonn;
import java.util.EnumMap;
import java.util.Random;
import java.awt.Polygon;

/**
 * A class to handle the relative positions of tiles to each other and allow tile walking for the creation of maze like structures.
 */
public class HexTile
{
	/**
	 * An Enum to handle directions. This will prevent people putting in incorrect valuse.
	 */
	public enum Direction
	{
		NORTHWEST, NORTHEAST, EAST, SOUTHEAST, SOUTHWEST, WEST;
	};
	
	/**
	 * Any tile that is within 1 tile of this tile in a given Direction is considered its neighbour. We'll store the Direction enum as the key and the 
	 */
	protected EnumMap<Direction, HexTile> neighbours;

	/**
	 * In order to simulate geography, in a very very abstract sense, a tile is considered 
	 */
	protected EnumMap<Direction, Boolean> connections;
	
	protected EnumMap<Direction, Direction> opposites;

	
	/**
	 * Whether or not the tile is occupied
	 */
	protected boolean occupied;
	/**
	 * Whether or not the tile is explored for the purposes of mining
	 */
	protected boolean explored;
	/**
	 * A boolean to track whether or not the tile has been tile walked during the initial set up.
	 */
	protected boolean visited;
	

	
	/**
	 * The tile's x position
	 */
	protected int x;
	/**
	 * The tile's y position
	 */
	protected int y;

	/**
	 * This is the easiest way to check if a point is inside a hextile, figuring out the calculations myself is possible, but would be reinventing the wheel.
	 */
	Polygon polygon;
	
	/**
	 * The topMostPoint lets you tell if a row could conceivably contain the hextile that contains a Hextile that has been clicked. When iterating through the rows of tiles, this allows us to check the top and bottom bounds of the row by checking
	 * the y coordinate against said bounds.
	 */
	int[] topMostPoint = new int[2];
	/**
	 * The topMostPoint lets you tell if a row could conceivably contain the hextile that contains a Hextile that has been clicked. When iterating through the rows of tiles, this allows us to check the top and bottom bounds of the row by checking
	 * the y coordinate against said bounds.
	 */
	int[] bottomMostPoint = new int[2];
	
	
	
	/**
	 * Default constructor. Instantiates all needed values
	 * @param x the x coordinate of the tile
	 * @param y the y coordinate of the tile
	 */
	public HexTile(int x, int y)
	{
		
		this.x = x;
		this.y = y;
		this.occupied = false;
		this.explored = false;
		this.visited = false;
		this.connections = new EnumMap<>(Direction.class);
		this.neighbours = new EnumMap<>(Direction.class);
		this.opposites = new EnumMap<>(Direction.class);
		this.opposites.put(Direction.NORTHEAST, Direction.SOUTHWEST);
		this.opposites.put(Direction.NORTHWEST, Direction.SOUTHEAST);
		this.opposites.put(Direction.SOUTHEAST, Direction.NORTHWEST);
		this.opposites.put(Direction.SOUTHWEST, Direction.NORTHEAST);
		this.opposites.put(Direction.EAST, Direction.WEST);
		this.opposites.put(Direction.WEST, Direction.EAST);
	}
	
	/**
	 * Occupy the tile and return a reference to it for easy storage
	 * @return The tile being occupied
	 */
	public HexTile occupy()
	{
		this.occupied = true;
		return this;
	}
	
	/**
	 * Add a neighbour to a cell and store its direction relative to this tile. This is done for thee neighbouring cell in the opposite direction
	 * @param neighbour	The neighbouring cell
	 * @param direction	The direction of the relationship
	 */
	public void addNeighbour(HexTile neighbour, Direction direction)
	{
		this.neighbours.put(direction, neighbour);
		Direction d = this.opposites.get(direction);
		neighbour.neighbours.put(d, this);
	}

	/**
	 * Set the points the hextile is drawn to. The exact geometry used by any drawing library is irrelevant.
	 * @param points	A two-d array of integers representing the points
	 */
	public void setPoints(int points[][])
	{
		int smallest = Integer.MAX_VALUE;
		int biggest = Integer.MIN_VALUE;
		this.polygon = new Polygon();

		for(int[] point: points)
		{
			this.polygon.addPoint(point[0], point[1]);
			if(point[1] > biggest)
			{
				biggest = point[1];
				this.bottomMostPoint = point;
			}
			if(point[1] < smallest)
			{
				smallest = point[1];
				this.topMostPoint = point;
			}
		}
	}

	/**
	 * A method to determine if a point (x, y) is within the hextile. Allows us to tell if a cell has been clicked
	 * @param 	x	The x coord of the point
	 * @param 	y	The y coord of the point
	 * @return		True if the hextile contains the point, false otherwise.
	 */
	public boolean isPointInside(int x, int y)
	{
		return this.polygon.contains(x, y);
	}

	/**
	 * Check if a point clicked is within the y bounds of the hex. In abstract terms, what this lets us do is check if a row could conceivably contain the point, to reduce the number of checks that are requried.
	 * This is needed because a y point could be part of one of two rows if the point is in the ^v^v part of the hex intersections
	 * @param 	y		The y coord of the point
	 * @return			True if the point is within the vertical bounds
	 */
	public boolean isWithinVerticalBounds(int y)
	{
		return y < this.bottomMostPoint[1] && y > this.topMostPoint[1];
	}
	
	/**
	 * A method to check if a tile has a neighbour in a given direction
	 * @param	direction	The direction to check
	 * @return				Returns true if there is a hextile in the given direction. Otherwise false.
	 */
	public boolean hasNeighbour(Direction direction)
	{
		return (this.neighbours.containsKey(direction));
	}
	
	/**
	 * Check if the tile is occupied
	 * @return
	 */
	public boolean isOccupied()
	{
		return this.occupied;
	}
	
	/**
	 * Check whether the tile is explored
	 * @return
	 */
	public boolean isExplored()
	{
		return this.explored;
	}
	
	/**
	 * Mark the cell as explored
	 */
	public void explore()
	{
		this.explored = true;
	}
	
	public void visit()
	{
		this.visited = true;
	}
	
	public boolean isVisited()
	{
		return this.visited;
	}
	
	public int getX()
	{
		return this.x;
	}
	
	public int getY()
	{
		return this.y;
	}
	
	/**
	 * A method to determine whether or not there are any tiles adjacent to this taht have not been tileWalked
	 * @return
	 */
	public Direction getRandomUnvisitedNeighbourDirection()
	{
		// initialise to null
		Direction neighbourDirection = null;
		// set an array
		Direction[] neighbourDirections = new Direction[neighbours.size()];
		
		// shuffle the directions
		int i = 0;
		for(Direction d: neighbours.keySet())
		{
			neighbourDirections[i] = d;
			i++;
		}
			
		Random r = new Random();
		// shuffle the array of neighbours using a random swap sort, fast but not cryptographically solid, but that's fine.
		for(i = 0; i < neighbourDirections.length; i++)
		{
			int randomIndex = r.nextInt(neighbourDirections.length);
			Direction d = neighbourDirections[randomIndex];
			neighbourDirections[randomIndex] = neighbourDirections[i];
			neighbourDirections[i] = d;
		}
		
		i = 0;
		// get a the first of the unvisited neighbouring cells (or null if none)
		while(neighbourDirection == null && i < neighbourDirections.length)
		{
			HexTile t = this.neighbours.get(neighbourDirections[i]);
			if(t.visited == false)
			{
				neighbourDirection = neighbourDirections[i];
			}
			i++;
		}
		return neighbourDirection;
	}
	
	/**
	 * Get a specific neighbour given the direction, or null if nonoe
	 * @param 	direction	The direction to find the neighbour at, if such a neighbour exists
	 * @return				The neighbouring Tile or null if none
	 */
	public HexTile getNeighbour(Direction direction)
	{
		if(this.neighbours.containsKey(direction))
		{
			return this.neighbours.get(direction);
		}
		return null;
	}
	
	/**
	 * Check whether or not a tile is connected to its neighbour. Connectedness is what determines whether there is a wall between two cells.
	 * If they are connected there is no wall
	 * @param	direction	The direction to check for connectedness
	 * @return				Returns true if the cells are connected by the given direction, false otherwise.
	 */
	public boolean isConnected(Direction direction)
	{
		if(this.connections.containsKey(direction))
		{
			return this.connections.get(direction);
		}
		return false;
	}
	
	/**
	 * Add a connection between two tiles by the direction. This is reflective so the opposite connection is also added.
	 * eg  a cell connected to its northwest neighbour, the neighbour's connected to this cell at the southeast
	 * @param direction	The direction to establish a connection between
	 */
	public void addConnection(Direction direction)
	{
		this.connections.put(direction, true);
		this.neighbours.get(direction).connections.put(this.opposites.get(direction), true);
	}
	
	/**
	 * This is just a primitive helper file to allow the printing of Tiles and, by extension, the entire map.
	 */
	public String toString()
	{
		return this.occupied?"+":this.visited?"#":"*";
	}
	
}