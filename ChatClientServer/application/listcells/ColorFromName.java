package application.listcells;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javafx.scene.paint.Color;

/**
 * Helper class to get a {@link Color} from a name
 * @author x0wass
 */
public class ColorFromName
{
	/**
	 * Map associating names to colors so that each message from specific users
	 * can be displayed with a specific color
	 * This map is updated with calls to {@link #getColorForName(String)}
	 * @see models.messagesRunners.AbstractMessagesRunner#getColorFromName(String)
	 */
	private static Map<String, Color> colorMap = new TreeMap<String, Color>();

	/**
	 * Get color from name
	 * @param name the name to generate color from
	 * @return the color for this name based on the {@link #hashCode()} of the
	 * name, or {@link Color#BLACK} if the name is null or blank.
	 */
	public static Color getColorFromName(String name)
	{
		if ((name != null) && !name.isBlank())
		{
			if (!colorMap.containsKey(name))
			{
				Random rand = new Random(name.hashCode());
				int intColor = rand.nextInt();
				int bitMask = 255;
				// Blue is bits 0-7 of intColor
				double blue = (intColor & bitMask) / 255.0;
				// Green is bits 8-15 of intColor
				double green = ((intColor >>> 8) & bitMask) / 255.0;
				// Red is bits 16-23 of intColor
				double red = ((intColor >>> 16) & bitMask) / 255.0;
				Color color = new Color(red, green, blue, 1.0).darker();
				colorMap.put(name, color);
			}
			return colorMap.get(name);
		}
		else
		{
			return Color.BLACK;
		}
	}
}
