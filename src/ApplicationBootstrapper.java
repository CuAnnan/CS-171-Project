
import java.util.ArrayList;

import cs171.project2024.kearns.eamonn.*;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A class to bootstrap the application, extends PApplet to handle all graphics stuff
 * because I doubt whatever native libraries exist are as good
 */
public class ApplicationBootstrapper extends Application
{
	private static Game game;

	public void start(Stage stage)
	{
		
	}
	
	public static void main(String[] args)
	{
		game = new Game();
		
	}

}
