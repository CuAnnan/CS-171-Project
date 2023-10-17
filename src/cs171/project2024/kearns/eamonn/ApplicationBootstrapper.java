package cs171.project2024.kearns.eamonn;
import java.util.ArrayList;

import processing.core.PApplet;

public class ApplicationBootstrapper extends PApplet
{
	static float MAP_BBOX_WIDTH;
	static Game game;
	int timesDrawn = 0;
	
	public void settings()
	{
		smooth(8);
		size(1200, 800);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param radius
	 * @param numPoints
	 * @param rotation An angle in degrees
	 */
	float[][] polygon(float x, float y, float radius, int numPoints, int rotation)
	{
		float points[][] = new float[numPoints][2];
		float offset = rotation * PI / 180;
		beginShape(); 
		
		for(int i = 0; i < numPoints; i++)
		{
			float angle = offset + TWO_PI / numPoints * i;
			float vx = x + cos(angle) * radius;
			float vy = y + sin(angle) * radius;
			vertex(vx, vy);
			points[i][0] = vx;
			points[i][1] = vy;
		}
		
		endShape(CLOSE);
		return points;
	}
	
	float[][] hexagon(float x, float y, float radius)
	{
		return polygon(x, y, radius, 6, 30);
	}
	
	float[][] hexagon(float x, float y, float radius, int rotation)
	{
		return polygon(x, y, radius, 6, 30 + rotation);
	}
	
	public void setup()
	{
		surface.setTitle("CS 171 Project (Eamonn Kearns)");
		drawUI();
	}
	
	public void drawUI()
	{
		drawMap();
		fill(255);
		rect(40 + MAP_BBOX_WIDTH, 10, width - MAP_BBOX_WIDTH - 50, MAP_BBOX_WIDTH);
		fill(0);
		textSize(20);
		text("Resources: ", 40 + MAP_BBOX_WIDTH, 50);
		ResourceTile.Resource[] resources = {ResourceTile.Resource.WOOD, ResourceTile.Resource.LIVESTOCK, ResourceTile.Resource.WATER, ResourceTile.Resource.ORE, ResourceTile.Resource.OIL, ResourceTile.Resource.FISSILE};
		int texty = 70;
		for(ResourceTile.Resource r:resources)
		{
			if(game.isResourceDiscovered(r))
			{
				text(Game.resourceNames.get(r)+": "+String.format("%.1f", game.getResourceAvailable(r)), 60 + MAP_BBOX_WIDTH, texty);
				texty+= 30;
			}
		}
	}
	
	public void draw()
	{
		game.processTick();
		drawUI();
	}
	
	public void drawHexTile(ResourceTile tile, float x, float y, float radius)
	{
		strokeWeight(1);
		
		float[][] externalPoints;
		if(tile.isOccupied())
		{
			stroke(120);
			fill(120);
			externalPoints = hexagon(x, y, radius);
			fill(0);
			stroke(0);
			hexagon(x, y, radius/4);
		}
		else
		{
//			stroke(100,180,100);
			noStroke();
			if(tile.isExplored())
			{
				fill(100,200,100);
				float[][] points = hexagon(x, y, radius);
				int i = 0;
				for(ResourceTile.Resource r: ResourceTile.Resource.values())
				{
					if(game.isResourceDiscovered(r) && tile.hasResource(r))
					{
						stroke(0);
						switch(r)
						{
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
						float centreX = (x + points[i][0] + points[(i+1)%6][0]) / 3;
						float centreY = (y + points[i][1] + points[(i+1)%6][1]) / 3;
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
				fill(100,180,100);
			}
			externalPoints = hexagon(x, y, radius);
		}
		
		stroke(0);
		for(HexTile.Direction d: HexTile.Direction.values())
		{
			int index = -1;
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

			strokeWeight(2);
			
			if(!tile.isConnected(d))
			{
				stroke(0);
				line(externalPoints[index][0], externalPoints[index][1], externalPoints[(index + 1) % 6][0], externalPoints[(index+1)%6][1]);
			}
		}
	}
	
	static float[] averagePoints(float[][] pointsArray, int index)
	{
		float[] results = {(pointsArray[index][0] + pointsArray[(index+1)%pointsArray.length][0])/2,(pointsArray[index][1] + pointsArray[(index+1)%pointsArray.length][1])/2};
		return results;
	}
	
	
	private void drawMap() {
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
				drawHexTile(tile, columnX, rowY, tileRadius);
				columnX += tileWidth;
			}
			
			rowY += tileRadius * 1.5f;
		}
	}
	
	public static void main(String[] args)
	{
		game = new Game(10);
		// TODO Auto-generated method stub
		String[] processingArgs = {"My Game"};
		ApplicationBootstrapper myGame = new ApplicationBootstrapper();
		PApplet.runSketch(processingArgs, myGame);
		// modifying to try to force a compile
	}

}
