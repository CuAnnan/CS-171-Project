
import java.util.ArrayList;
import java.util.EnumMap;

import cs171.project2024.kearns.eamonn.*;
import cs171.project2024.kearns.eamonn.ResourceTile.Resource;
import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

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
	private EnumMap<Resource, VBox> resourceVboxes = new EnumMap<>(Resource.class);

	private final Canvas canvas = new Canvas(600, 600);
	private final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

	/**
	 * Overriding Application's start method.
	 */
	@Override
	public void start(Stage stage)
	{
		BorderPane border = new BorderPane();
		
		VBox right = buildTextOutputUI();
		border.setRight(right);
		border.setCenter(canvas);

		drawMap();
		
		Scene scene = new Scene(border, 800, 600);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("CS 171 Éamonn Kearns 60460770");
		stage.show();
	}


	/**
	 * Cleaned this up out of start to make start easier to read
	 * @return
	 */
	private VBox buildTextOutputUI() {
		Font arial = Font.font("Arial", FontWeight.BOLD, 14);

		VBox right = new VBox();
		right.setPrefWidth(200);
		
		// A box to contain the Title "Resources"
		HBox titleBox = new HBox();
		titleBox.setPadding(new Insets(0,0,5,0));
		Text title = new Text("Resources");
		title.setFont(arial);
		titleBox.getChildren().add(title);
		right.getChildren().add(titleBox);

		// a box to contain the individual resource outputs.
		// needs to be a class property so as to be able to add to it when things get discovered
		this.resourcesVBox = new VBox();
		this.resourcesVBox.setPrefWidth(200);
		for(Resource resource: game.getDiscoveredResources())
		{
			HBox h = new HBox();

			VBox rt = new VBox();
			Text resourceText = new Text(String.format("%s: ",resource.label));
			resourceText.setFont(arial);
			rt.getChildren().add(resourceText);
			rt.setPrefWidth(100);
			rt.setPadding(new Insets(0, 0, 0, 10));
			h.getChildren().add(rt);
			
			VBox at = new VBox();
			Text resourceAmount = new Text(String.format("%.2f",game.getResourceAvailable(resource)));
			resourceAmount.setFont(arial);
			at.getChildren().add(resourceAmount);
			// we need to store the amount as a property so as to be able to update it.
			this.resourceVboxes.put(resource, at);
			h.getChildren().add(at);
			this.resourcesVBox.getChildren().add(h);
		}
		
		right.getChildren().add(this.resourcesVBox);
		return right;
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
	private double[][] polygon(float x, float y, float radius, int numPoints, int rotation)
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
	double[][] hexagon(float x, float y, float radius)
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
	double[][] hexagon(float x, float y, float radius, int rotation)
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
	public void drawResourceTile(ResourceTile tile, float x, float y, float radius)
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
			graphicsContext.fillPolygon(externalPoints[0], externalPoints[1], externalPoints[1].length);
		}
		else
		{
			
		}
	}
	

	private void drawMap()
	{
		
		
	}

	public void draw()
	{

	}



	
	public static void main(String[] args)
	{
		game = new Game();
		System.out.println(game);
		launch(ApplicationBootstrapper.class, args);
	}
}
