package cs171.project2024.kearns.eamonn;
import java.util.EnumMap;
import java.util.Random;

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
	private int x;
	/**
	 * The tile's y position
	 */
	private int y;
	
	
	
	
	
	
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
	
	public void addNeighbour(HexTile neighbour, Direction direction)
	{
		this.neighbours.put(direction, neighbour);
		Direction d = this.opposites.get(direction);
		neighbour.neighbours.put(d, this);
	}
	
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
		Direction neighbourDirection = null;
		Direction[] neighbourDirections = new Direction[neighbours.size()];
		
		// shuffle the directions
		int i = 0;
		for(Direction d: neighbours.keySet())
		{
			neighbourDirections[i] = d;
			i++;
		}
			
		Random r = new Random();
		// shuffle the array of 
		for(i = 0; i < neighbourDirections.length; i++)
		{
			int randomIndex = r.nextInt(neighbourDirections.length);
			Direction d = neighbourDirections[randomIndex];
			neighbourDirections[randomIndex] = neighbourDirections[i];
			neighbourDirections[i] = d;
		}
		
		i = 0;
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
	
	public HexTile getNeighbour(Direction d)
	{
		if(this.neighbours.containsKey(d))
		{
			return this.neighbours.get(d);
		}
		return null;
	}
	
	public boolean isConnected(Direction d)
	{
		if(this.connections.containsKey(d))
		{
			return this.connections.get(d);
		}
		return false;
	}
	
	public void addConnection(Direction d)
	{
		this.connections.put(d, true);
		this.neighbours.get(d).connections.put(this.opposites.get(d), true);
	}
	
	public String toString()
	{
		return this.occupied?"+":this.visited?"#":"*";
	}
	
}