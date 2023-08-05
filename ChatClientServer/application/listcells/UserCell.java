package application.listcells;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;

/**
 * Custom {@link ListCell} for displaying users names.
 * to be used in {@link application.Controller#authosList}
 * @author x0wass
 */
public class UserCell extends ListCell<String>
{

	/**
	 * The root node which will be loaded from FXML
	 */
	private Node graphic;

	/**
	 * The controller to use on this {@link UserCell}
	 */
	private UserCellController controller;

	/**
	 * Default constructor
	 * Loads FXML file to layout the cell and binds controller
	 */
	public UserCell()
	{
		FXMLLoader loader = new FXMLLoader(UserCell.class.getResource("UserCell.fxml"));
		try
		{
			graphic = loader.load();
			controller = loader.getController();
		}
		catch (IOException e)
		{
			System.err.println("Unable to load UserCell.fxml");
			e.printStackTrace();
		}
	}

	/**
	 * Cell update
	 * @param item user name to display in this cell
	 * @param empty indicates if this cell represents data or not
	 */
	@Override
	protected void updateItem(String item, boolean empty)
	{
		super.updateItem(item, empty);

		if (empty || (item == null))
		{
			setText(null);
			setGraphic(null);
		}
		else
		{
			controller.setContentLabel(item);
			controller.setColor(ColorFromName.getColorFromName(item));
			setText(null);
			setGraphic(graphic);
		}
	}
}
