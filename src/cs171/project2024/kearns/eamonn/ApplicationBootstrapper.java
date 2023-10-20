package cs171.project2024.kearns.eamonn;
import java.util.ArrayList;

import processing.core.PApplet;

/**
 * A class to bootstrap the application, extends PApplet to handle all graphics stuff
 * because I doubt whatever native libraries exist are as good
 */
public class ApplicationBootstrapper extends PApplet
{
	static float MAP_BBOX_WIDTH;

	static final int RESEARCH_START = 260;
	static final int TEXT_PADDING = 30;

	static Game game;
	int timesDrawn = 0;
	
	public void settings()
	{
		smooth(8);
		size(1200, 800);
	}
	
	/**
	 * A class method to draw an arbitrary regular polygon at point (x, y), with a radius of r, a number of points numPoints with an initial rotation of rotation.
	 * @implNote		Derived from first principles. This may well look identical to any other solution for this, because there's really only one way to solve it.
	 * 					This could be further abstracted to map all polygons (so all n-sided "stars")
	 * @param x			The x-Coordinate of the centre of the regular polygon
	 * @param y			The y-Coordinate of the centre of the regular polygon
	 * @param radius	The radius of the regular polygon
	 * @param numPoints	The number of points in the regular polygon
	 * @param rotation	An angle in degrees expressed as the counter-clockwise rotation about the origin where 0 degrees (which equals 0 radians) is the angle
	 * 					such that cos(0) = 1. While the standard for cos and sin and all functions are radians, they are harder 
	 * 					to express in code, both in terms of legiblity and in terms of reusability. This is a design decision that
	 * 					I think I can justify
	 * @return			Returns a two dimensional array of floats representing the points of the polygons vertices for later use.
	 */
	float[][] polygon(float x, float y, float radius, int numPoints, int rotation)
	{
		// Define and initialise the return value.
		float points[][] = new float[numPoints][2];
		// Convert the initial rotation into radians
		float offset = rotation * PI / 180;
		// use Processing's arbitrary polygon functionality to start a shape
		beginShape(); 
		for(int i = 0; i < numPoints; i++)
		{
			// convert rotation to radians
			float theta = offset + TWO_PI / numPoints * i;
			// unit circle multiplied by the radius and added to the x and y coordinates.
			float vx = x + cos(theta) * radius;
			float vy = y + sin(theta) * radius;
			// add the vertex to the shape to be drawn
			vertex(vx, vy);
			// store the points as a two dimsensional array for later use
			points[i][0] = vx;
			points[i][1] = vy;
		}
		// end the shape
		endShape(CLOSE);
		// return the points
		return points;
	}
	
	/**
	 * A helper that calls polygon specifically for a hexagon where we only want the hex to be "pointed up"
	 * @see   	ApplicationBootstrapper#polygon(float, float, float, int, int)	for the actual implementation
	 * @param	x		The x-coordinate of the hexagon
	 * @param	y		The y-coordinate of the hexagon
	 * @param	radius	The radius of the hexagon
	 * @return	Returns the set of points of the hexagon
	 */
	float[][] hexagon(float x, float y, float radius)
	{
		return polygon(x, y, radius, 6, 30);
	}
	
	/**
	 * A helper that calls polygon specifically for a hexagon where the angle of rotation, relative to "up" is defined in degrees 
	 * @param	x			The x-coordinate of the hexagon
	 * @param	y			The y-coordinate of the hexagon
	 * @param	radius		The radius of the hexagon
	 * @param 	rotation	The rotation of the hexagon around the centre. I believe this is actually unused, but I'm not sure.
	 * @return	Returns the set of points of the hexagon
	 */
	float[][] hexagon(float x, float y, float radius, int rotation)
	{
		return polygon(x, y, radius, 6, 30 + rotation);
	}
	
	/**
	 * Overloading PApplet's setup
	 */
	public void setup()
	{
		surface.setTitle("CS 171 Project (Eamonn Kearns)");
		drawUI(true);
	}

	public void drawUI()
	{
		drawUI(false);
	}
	
	/**
	 * A function to draw the entirety of the UI
	 * Draws a rectangle for the resources and writes out the current values. This is very much a work in progress.
	 * @see ApplicationBootstrapper#drawMap()
	 */
	public void drawUI(boolean firstTime)
	{
		// draw the map
		drawMap(firstTime);
		// draw the rest
		fill(255);
		rect(40 + MAP_BBOX_WIDTH, 10, width - MAP_BBOX_WIDTH - 50, MAP_BBOX_WIDTH);
		fill(0);
		textSize(20);
		int textY = 50;
		
		text("Resources:", 45 + MAP_BBOX_WIDTH, textY);
		ResourceTile.Resource[] resources = {ResourceTile.Resource.WOOD, ResourceTile.Resource.LIVESTOCK, ResourceTile.Resource.WATER, ResourceTile.Resource.ORE, ResourceTile.Resource.OIL, ResourceTile.Resource.FISSILE};
		for(ResourceTile.Resource r:resources)
		{
			if(game.isResourceDiscovered(r))
			{
				textY += TEXT_PADDING;
				text(r.label+": "+String.format("%.1f", game.getResourceAvailable(r)), 60 + MAP_BBOX_WIDTH, textY);
			}
		}

		textY=RESEARCH_START;
		text("Research:", 45 + MAP_BBOX_WIDTH, textY);
		for(Research r: game.getAvailableResearches())
		{
			textY += TEXT_PADDING;
			
			text(r.getName(), 60 + MAP_BBOX_WIDTH, textY);
		}
		timesDrawn++;
		timesDrawn %= 50;
		if(timesDrawn == 0)
		{
			game.updateAvailableResearches();
		}
	}
	
	/**
	 * Overloading PApplet's draw method.
	 * @see Game#processTick()
	 * @see ApplicationBootstrapper#drawUI()
	 */
	public void draw()
	{
		game.processTick();
		drawUI();
	}

	public void mouseClicked()
	{
		if(mouseX < MAP_BBOX_WIDTH && mouseY < MAP_BBOX_WIDTH)
		{
			ArrayList<ArrayList<ResourceTile>> rows = game.getTiles();
			for(ArrayList<ResourceTile> row:rows)
			{
				ResourceTile firstCell = row.get(0);
				// I need to figure out if a click event is occuring within the area defined by a tile
				if(firstCell.isWithinVerticalBounds(mouseY))
				{
					boolean found = false;
					int i = 0;
					while(!found && i < row.size())
					{
						ResourceTile r = row.get(i);
						if(r.isPointInside(mouseX, mouseY))
						{
							found = true;
						}
						i++;
					}
				}
			}
		}
		else
		{
			// this is hasty and may need revisiting given time.
			if(mouseY > RESEARCH_START)
			{
				ArrayList<Research> researches = game.getAvailableResearches();
				int relevantMouseY = (mouseY - RESEARCH_START -5)/30;
				if(relevantMouseY < researches.size())
				{
					game.buyResearch(researches.get(relevantMouseY));
				}
			}
		}
	}
	
	/**
	 * This method draws an individual ResourceTile and a crude representation of the resources available on it.
	 * @param tile			The ResourceTile to be drawn
	 * @param x				The x-coordinate of the centre of the ResourceTile
	 * @param y				The x-coordinate of the centre of the ResourceTile
	 * @param radius		The radius at which to draw the ResourceTile
	 * @see	ResourceTile	See the ResourceTile class file for its implementation	
	 */
	public int[][] drawResourceTile(ResourceTile tile, float x, float y, float radius)
	{
		strokeWeight(1);
		/*
		 * We want the points of the outside hexagon (occupied tiles have an internal hexagon).
		 * We use this to draw the state of the walls, which cannot be done with a plain stroked hexagon.
		 */
		float[][] externalPoints; 
		if(tile.isOccupied())
		{
			// I could write this this to leverage polymorphism by checking .class, but this property is simpler and more flexible, especially if I intend at a
			// later point to model and represent expansion and the effect that has on the simulated world. Again, it's a design decision which I may end up having
			// to revisit at a later point.
			stroke(120);
			fill(120);
			externalPoints = hexagon(x, y, radius);
			fill(0);
			stroke(0);
			hexagon(x, y, radius/4);
		}
		else
		{
			drawResources(tile, x, y, radius);
			externalPoints = hexagon(x, y, radius);
		}
		
		drawTileWalls(tile, externalPoints);
		int[][] pointsAsInts = new int[externalPoints.length][2];
		for(int i = 0; i < externalPoints.length; i++)
		{
			pointsAsInts[i][0] = (int)externalPoints[i][0];
			pointsAsInts[i][1] = (int)externalPoints[i][1];
		}
		return pointsAsInts;
	}

	private void drawTileWalls(ResourceTile tile, float[][] externalPoints) {
		stroke(0);
		for(HexTile.Direction d: HexTile.Direction.values())
		{
			int index = -1;
			// I could absolutely map the set of directions to the indexes of the points returned. This may be a todo thing later.
			// at the moment this works and it is not inefficient. I think a switch might look nicer. As it's only one line statements.
			if(d == HexTile.Direction.NORTHEAST)
			{
				index = 4;
			}
			else if(d == HexTile.Direction.NORTHWEST)
			{
				index = 3;
			}
			else if(d == HexTile.Direction.WEST)
			{
				index = 5;
			}
			else if(d == HexTile.Direction.EAST)
			{
				index = 2;
			}
			else if(d == HexTile.Direction.SOUTHEAST)
			{
				index = 0;
			}
			else if(d == HexTile.Direction.SOUTHWEST)
			{
				index = 1;
			}
			// draw the walls thick so they are always obvious
			strokeWeight(2);
			// only draw the wall if the two tiles have not been connected.
			if(!tile.isConnected(d))
			{
				stroke(0);
				// 
				line(externalPoints[index][0], externalPoints[index][1], externalPoints[(index + 1) % 6][0], externalPoints[(index+1)%6][1]);
			}
		}
	}

	private void drawResources(ResourceTile tile, float x, float y, float radius) {
		// draw the map tile
		noStroke();
		if(tile.isExplored())
		{
			// we only draw the resources if the tile is explored
			fill(100,200,100);
			float[][] points = hexagon(x, y, radius);
			int i = 0;
			for(ResourceTile.Resource r: ResourceTile.Resource.values())
			{
				// only draw the resources if they are discovered and the tile has them
				// TODO: Add a step to ignore this if the tile's resource is exhausted, wasted clock cycles.
				if(game.isResourceDiscovered(r) && tile.hasResource(r))
				{
					stroke(0); // we don't stroke
					switch(r)
					{
						// figure out the color
						case FISSILE:
							fill(255,255,0);
							break;
						case OIL:
							fill(139,69,19);
							break;
						case ORE:
							fill(150,25,25);
							break;
						case WATER:
							fill(0, 150, 255);
							break;
						case WOOD:
							fill(0,200,0);
							break;
						case LIVESTOCK:
							fill(200);
							break;
					}
					// Figure out the centre of the point of the resource indicator by doing some maths
					// I may revisit this position later as I add roads.
					float centreX = (x + points[i][0] + points[(i+1)%6][0]) / 3;
					float centreY = (y + points[i][1] + points[(i+1)%6][1]) / 3;
					// Draw a decreasing arc around it using the PIE flag. I've tried both with and without, the PIE looks slightly better
					// but this is an aesthetic choice.
					arc(centreX, centreY, radius/2, radius/2, 0, TWO_PI * (float)tile.getAvailableResource(r), PIE);
				}
				i++;
			}
			noFill();
			stroke(100,180,100);
			strokeWeight(2);
		}
		else
		{
			// if the cell hasn't been discovered, apply fog of war instead
			fill(100,180,100);
		}
	}
	
	/**
	 * A method to draw the map to the screen. The position of the centre point of the drawing area is determined by a hypothetical bounding box derived from the 
	 * width and height of the draw screen.
	 */
	private void drawMap(boolean firstTime) {
		// this is to determine how much of the UI in general the map will use
		float scale = 1f;
		
		int offset = 10;
		
		// some needed math
		// given the regular hexagon with radius (or side) length r
		// the shortest distance from the center point to the nearest point on the edge (hw) can be
		// derived using the relationship between the sides of the 30, 60, 90 triangle
		// r will be the hypthenuse of the triangle
		// hw will be r * cos(30)
		// the width of the polygon will be twice that distance
		float widthRation = (float)Math.cos(30 * Math.PI / 180);
		// figure out the bounding box of the map
		MAP_BBOX_WIDTH = Math.min(width, height) * scale;
		// figure out the width of individual hexes based by dividing the width by the number of tiles across the centre
		float tileWidth = MAP_BBOX_WIDTH / game.getDiameter();
		// derive the radius from the width
		float tileRadius = tileWidth / widthRation / 2;
		 
		// half the height, because we want the center point of the center row
		// each row up you go is 1.5 times the radius, because of how hexagons stack
		// the game's radius needs to be offset by 1 because it's not a circle, it's a hexagon
		float rowY = offset + MAP_BBOX_WIDTH / 2 - tileRadius * 1.5f * (game.getRadius()-1);
		
		for(ArrayList<ResourceTile> row:game.getTiles())
		{
			// the center of this row will be the width / 2
			// so we offset the row size by one less than the length of the row
			float columnX = offset + MAP_BBOX_WIDTH / 2  - tileWidth / 2 * (row.size() - 1);
			for(ResourceTile tile: row)
			{
				int[][] points = drawResourceTile(tile, columnX, rowY, tileRadius);
				if(firstTime)
				{
					tile.setPoints(points);
				}
				columnX += tileWidth;
			}
			
			rowY += tileRadius * 1.5f;
		}
	}
	
	public static void main(String[] args)
	{
		game = new Game();
		String[] processingArgs = {"My Game"};
		ApplicationBootstrapper myGame = new ApplicationBootstrapper();
		PApplet.runSketch(processingArgs, myGame);
		// modifying to try to force a compile
	}

}
