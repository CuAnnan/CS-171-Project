
import java.util.ArrayList;

import cs171.project2024.kearns.eamonn.*;

/**
 * A class to bootstrap the application, extends PApplet to handle all graphics stuff
 * because I doubt whatever native libraries exist are as good
 */
public class ApplicationBootstrapper
{
	private static Game game;

	
	public static void main(String[] args)
	{
		game = new Game();
		System.out.println(game);
	}

}
