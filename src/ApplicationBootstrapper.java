
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

	private void drawMap()
	{
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
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
