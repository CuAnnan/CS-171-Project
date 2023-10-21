
import java.util.ArrayList;
import java.util.EnumMap;

import cs171.project2024.kearns.eamonn.*;
import cs171.project2024.kearns.eamonn.HexTile.Direction;
import cs171.project2024.kearns.eamonn.ResourceTile.Resource;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A class to bootstrap the application, extends PApplet to handle all graphics stuff
 * because I doubt whatever native libraries exist are as good
 */
public class ApplicationBootstrapper extends Application
{
	
	/**
	 * A static reference to the game. During various approaches to this, different APIs have required internal static classes so I made game static to make sure it could be accessed
	 */
	private static Game game;
	/**
	 * A class reference to the vbox used to contain the resources. Which is required for when we add more
	 */
	private VBox resourcesVBox;
	/**
	 * An enum map mapping the Resource to the VBox with the text used to display the amount of the resource available.
	 */
	private EnumMap<Resource, Text> resourceTexts = new EnumMap<>(Resource.class);
	private VBox researchesVBox;

	/**
	 * A value to determine the size of the canvas, the size of the scene is derived from that
	 */
	private final int CANVAS_WIDTH = 800;
	/**
	 * A canvas to draw on
	 */
	private Canvas canvas =  new Canvas(CANVAS_WIDTH, CANVAS_WIDTH);
	/**
	 * The graphicsContext to draw on
	 */
	private GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
	

	/**
	 * Since we use text in multiple places, defining the basic font used is smurt
	 */
	private final static Font arial = Font.font("Arial", FontWeight.NORMAL, 14);

	private EnumMap<Resource, Color> colors = new EnumMap<>(Resource.class);

	
	/**
	 * Overriding Application's start method.
	 */
	@Override
	public void start(Stage stage)
	{
		BorderPane border = new BorderPane();
		Scene scene = new Scene(border, CANVAS_WIDTH+200, CANVAS_WIDTH);

		colors.put(Resource.FISSILE,	Color.rgb(255,255,0));
		colors.put(Resource.OIL,		Color.rgb(139,69,19));
		colors.put(Resource.ORE,		Color.rgb(150,25,25));
		colors.put(Resource.WATER,		Color.rgb(0, 150, 255));
		colors.put(Resource.WOOD, 		Color.rgb(0,200,0));
		colors.put(Resource.LIVESTOCK,	Color.rgb(200, 200, 200));
		
		
		VBox right = buildTextOutputUI(scene);
		border.setRight(right);
		border.setCenter(canvas);

		drawMap(true);
		// set the fps to 60 frames per second (ish)
		final double FPS = 1000/60;
		
		Timeline timeline = new Timeline(
			new KeyFrame(Duration.millis(FPS), e->{
				draw();
			})
		);
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();

		
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("CS 171 Éamonn Kearns 60460770");
		stage.show();
	}


	/**
	 * Cleaned this up out of start to make start easier to read
	 * @return
	 */
	private VBox buildTextOutputUI(Scene scene)
	{
		VBox right = new VBox();
		right.setStyle("-fx-background-color: #999999;");
		right.setPadding(new Insets(0, 5, 0, 0));
		right.setPrefWidth(200);
		
		// A box to contain the Title "Resources"
		HBox titleBox = new HBox();
		titleBox.setPadding(new Insets(10,0,5,0));
		Text title = new Text("Resources");
		title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
		titleBox.getChildren().add(title);
		right.getChildren().add(titleBox);

		right.getChildren().add(buildResourcesBox());

		right.getChildren().add(getResearchBox(scene));

		return right;
	}

	/**
	 * Build the research box
	 */
	private VBox getResearchBox(Scene scene)
	{
		VBox researchesContainer = new VBox();
		
		Text researchText = new Text("Research:");
		researchText.setFont(Font.font("Arial", FontWeight.BOLD, 17));
		researchesContainer.getChildren().add(researchText);

		this.researchesVBox = new VBox();
		this.researchesVBox.setPadding(new Insets(0, 0, 0, 10));
		this.researchesVBox.setPrefWidth(200);
		researchesContainer.getChildren().add(this.researchesVBox);
		for(Research research: game.getResearches())
		{
			VBox researchHBox = new VBox();
			researchHBox.setOnMouseClicked(e->{
				if(game.buyResearch(research))
				{
					this.researchesVBox.getChildren().remove(researchHBox);
				}
			});
			researchHBox.setOnMouseEntered(e->{
				scene.setCursor(Cursor.HAND);
			});
			researchHBox.setOnMouseExited(e->{
				scene.setCursor(Cursor.DEFAULT);
			});
			
			VBox rTitleBox = new VBox();
			Text rTitleText = new Text(research.getName());
			rTitleText.setFont(arial);
			rTitleBox.getChildren().add(rTitleText);
			researchHBox.getChildren().add(rTitleBox);

			HBox costBox = new HBox();
				
			for(Resource resource: research.getCosts().keySet())
			{
				VBox resourceBox = new VBox();
				Text costText = new Text(String.format("%.2f",research.getCosts().get(resource)));
				costText.setFont(arial);
				costText.setFill(colors.get(resource));
				resourceBox.getChildren().add(costText);
				costBox.getChildren().add(resourceBox);
			}
			researchHBox.getChildren().add(costBox);

			this.researchesVBox.getChildren().add(researchHBox);
		}

		return researchesContainer;
	}


	private VBox buildResourcesBox() {
		// a box to contain the individual resource outputs.
		// needs to be a class property so as to be able to add to it when things get discovered
		this.resourcesVBox = new VBox();
		this.resourcesVBox.setPrefWidth(200);
		this.resourcesVBox.setPrefHeight(110);
		
		for(Resource resource: game.getDiscoveredResources())
		{
			HBox h = new HBox();
			
			VBox rt = new VBox();
			Text resourceText = new Text(String.format("%s: ",resource.label));
			resourceText.setFont(arial);
			rt.getChildren().add(resourceText);
			rt.setPrefWidth(130);
			rt.setPadding(new Insets(0, 0, 0, 10));
			h.getChildren().add(rt);
			
			VBox at = new VBox();
			Text resourceAmount = new Text(String.format("%.2f",game.getResourceAvailable(resource)));
			resourceAmount.setFont(arial);
			resourceAmount.setFill(colors.get(resource));
			at.getChildren().add(resourceAmount);
			// we need to store the amount as a property so as to be able to update it.
			this.resourceTexts.put(resource, resourceAmount);
			h.getChildren().add(at);
			this.resourcesVBox.getChildren().add(h);
		}
		return this.resourcesVBox;
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
	 * @return			Returns a two dimensional array of doubles representing the points of the polygons vertices for later use.
	 */
	private double[][] polygon(double x, double y, double radius, int numPoints, int rotation)
	{
		// Define and initialise the return value.
		double[][] points= new double[2][numPoints];
		// Convert the initial rotation into radians
		double offset = rotation * Math.PI / 180;
		// use Processing's arbitrary polygon functionality to start a shape
		for(int i = 0; i < numPoints; i++)
		{
			// convert rotation to radians
			double theta = offset + Math.PI * 2 / numPoints * i;
			// unit circle multiplied by the radius and added to the x and y coordinates.
			double vx = x + Math.cos(theta) * radius;
			double vy = y + Math.sin(theta) * radius;
			// add the vertex to the shape to be drawn
			// store the points as a two dimsensional array for later use
			points[0][i] = vx;
			points[1][i] = vy;
		}
		// end the shape
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
	double[][] hexagon(double x, double y, double radius)
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
	double[][] hexagon(double x, double y, double radius, int rotation)
	{
		return polygon(x, y, radius, 6, 30 + rotation);
	}

	/**
	 * This method draws an individual ResourceTile and a crude representation of the resources available on it.
	 * @param tile			The ResourceTile to be drawn
	 * @param x				The x-coordinate of the centre of the ResourceTile
	 * @param y				The x-coordinate of the centre of the ResourceTile
	 * @param radius		The radius at which to draw the ResourceTile
	 * @see	ResourceTile	See the ResourceTile class file for its implementation	
	 */
	public int[][] drawTile(ResourceTile tile, double x, double y, double radius)
	{
		graphicsContext.setLineWidth(1.0);
		/*
		 * We want the points of the outside hexagon (occupied tiles have an internal hexagon).
		 * We use this to draw the state of the walls, which cannot be done with a plain stroked hexagon.
		 */
		double[][] externalPoints = hexagon(x, y, radius);
		if(tile.isOccupied())
		{
			// I could write this this to leverage polymorphism by checking .class, but this property is simpler and more flexible, especially if I intend at a
			// later point to model and represent expansion and the effect that has on the simulated world. Again, it's a design decision which I may end up having
			// to revisit at a later point.
			graphicsContext.setStroke(Color.rgb(120, 120, 120));
			graphicsContext.setFill(Color.rgb(120, 120, 120));
			graphicsContext.fillPolygon(externalPoints[0], externalPoints[1], externalPoints[1].length);
			double[][] internalPoints = hexagon(x, y, radius/4);
			graphicsContext.setStroke(Color.BLACK);
			graphicsContext.setFill(Color.BLACK);
			graphicsContext.fillPolygon(internalPoints[0], internalPoints[1], internalPoints[1].length);
		}
		else
		{
			drawResourceTile(tile, x, y, radius, externalPoints);
		}
		drawTileWalls(tile, externalPoints);
		int[][] pointsAsInts = new int[6][2];
		for(int i = 0; i < 6; i++)
		{
			pointsAsInts[i][0] = (int)externalPoints[0][i];
			pointsAsInts[i][1] = (int)externalPoints[1][i];
		}
		return pointsAsInts;
	}

	/**
	 * A method to draw the walls around a tile depending on whether or not it is connected to its neighbours.
	 * @param tile				The tile to draw
	 * @param externalPoints	The set of points around the tile
	 */
	private void drawTileWalls(ResourceTile tile, double[][] externalPoints) {
		for(Direction direction: Direction.values())
		{
			int index = -1;
			if(direction == HexTile.Direction.NORTHEAST)
			{
				index = 4;
			}
			else if(direction == HexTile.Direction.NORTHWEST)
			{
				index = 3;
			}
			else if(direction == HexTile.Direction.WEST)
			{
				index = 5;
			}
			else if(direction == HexTile.Direction.EAST)
			{
				index = 2;
			}
			else if(direction == HexTile.Direction.SOUTHEAST)
			{
				index = 0;
			}
			else if(direction == HexTile.Direction.SOUTHWEST)
			{
				index = 1;
			}
			if(!tile.isConnected(direction))
			{
				graphicsContext.setLineWidth(2);
				graphicsContext.setStroke(Color.BLACK);
				graphicsContext.beginPath();
				graphicsContext.moveTo(externalPoints[0][index], externalPoints[1][index]);
				graphicsContext.lineTo(externalPoints[0][(index+1)%6], externalPoints[1][(index+1)%6]);
				graphicsContext.stroke();
			}
		}
	}

	/**
	 * A method to draw a resource tile
	 * @param tile				The ResourceTile to draw
	 * @param x					The x coordinate of the tile on the canvas
	 * @param y					The y coordinate of the tile on the canvas
	 * @param radius			The radius of the tile from the centre to the vertex
	 * @param externalPoints	The set of points around the tile
	 */
	private void drawResourceTile(ResourceTile tile, double x, double y, double radius, double[][] externalPoints)
	{
		Color c = tile.isExplored()?Color.rgb(100,200,100):Color.rgb(100, 180, 100);
		graphicsContext.setFill(c);
		graphicsContext.setStroke(c);
		graphicsContext.fillPolygon(externalPoints[0], externalPoints[1], externalPoints[1].length);
		graphicsContext.strokePolygon(externalPoints[0], externalPoints[1], externalPoints[1].length);
		if(tile.isExplored())
		{
			int i = 0;
			for(ResourceTile.Resource r: ResourceTile.Resource.values())
			{
				// only draw the resources if they are discovered and the tile has them
				if(game.isResourceDiscovered(r) && tile.hasResource(r))
				{
					///graphicsContext.setLineWidth(0); // we don't stroke
					graphicsContext.setStroke(Color.BLACK);
					graphicsContext.setFill(colors.get(r));

					// Figure out the centre of the point of the resource indicator by doing some maths
					// I may revisit this position later as I add roads.
					double centreX = (externalPoints[0][i] + externalPoints[0][(i+1)%6] + externalPoints[0][(i+2)%6] - radius/1.8) / 3;
					double centreY = (externalPoints[1][i] + externalPoints[1][(i+1)%6] + externalPoints[1][(i+2)%6] - radius/1.8) / 3;
					
					// System.out.println(String.format("Should be drawing the resource %s at (%.2f, %.2f) with a radius of %.2f, a start angle of 0.00, an ar", r.label, centreX, centreX, radius));
					graphicsContext.fillArc(centreX, centreY, radius/2.5, radius/2.5, 0.0, -360 * tile.getAvailableResource(r), ArcType.ROUND);
					graphicsContext.strokeArc(centreX, centreY, radius/2.5, radius/2.5, 0.0, -360 * tile.getAvailableResource(r), ArcType.ROUND);
				}
				i++;
			}
		}
	}
	
	/**
	 * Method to draw the map
	 * @param firstTime	Whether or not this is the first time we're drawing the map, which is used to determine fi we stick the points in the tile as a property
	 */
	private void drawMap(boolean firstTime)
	{
		int offset = 5;
		// some needed math
		// given the regular hexagon with radius (or side) length r
		// the shortest distance from the center point to the nearest point on the edge (hw) can be
		// derived using the relationship between the sides of the 30, 60, 90 triangle
		// r will be the hypthenuse of the triangle
		// hw will be r * cos(30)
		// the width of the polygon will be twice that distance
		double widthRation = Math.cos(30 * Math.PI / 180);
		int mapBBoxWidth = ((int)Math.min(canvas.getWidth(), canvas.getHeight())) - offset;
		double tileWidth = mapBBoxWidth / game.getDiameter();
		double tileRadius = tileWidth / widthRation / 2;

		double rowY = offset + mapBBoxWidth / 2 - tileRadius * 1.5 * (game.getRadius()-1);

		for(ArrayList<ResourceTile> row:game.getTiles())
		{
			// the center of this row will be the width / 2
			// so we offset the row size by one less than the length of the row
			double columnX = offset + mapBBoxWidth / 2  - tileWidth / 2 * (row.size() - 1);
			for(ResourceTile tile: row)
			{
				int[][] points = drawTile(tile, columnX, rowY, tileRadius);
				if(firstTime)
				{
					tile.setPoints(points);
				}
				columnX += tileWidth;
			}
			
			rowY += tileRadius * 1.5f;
		}
	}

	/**
	 * The draw function which gets called every frame. 
	 */
	public void draw()
	{
		game.processTick();
		drawMap(false);
		// Updates the resources 
		for(Resource resource:game.getDiscoveredResources())
		{
			this.resourceTexts.get(resource).setText(String.format("%.2f", game.getResourceAvailable(resource)));
		}
	}



	
	public static void main(String[] args)
	{
		game = new Game();
		launch(ApplicationBootstrapper.class, args);
	}
}
