package application.listcells;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import models.Message;

/**
 * Custom {@link ListCell} for displaying users names.
 * to be used in {@link application.Controller#authosList}
 * @author x0wass
 */
public class MessageCell extends ListCell<Message>
{

	/**
	 * The root node which will be loaded from FXML
	 */
	private Node graphic;

	/**
	 * The controller to use on this {@link MessageCell}
	 */
	private MessageCellController controller;
	
	
	/**
	 * date message visibility
	 */
	private static boolean dateVisibility = true;

	/**
	 * Default constructor
	 * Loads FXML file to layout the cell and binds controller
	 */
	public MessageCell()
	{
		FXMLLoader loader = new FXMLLoader(MessageCell.class.getResource("MessageCell.fxml"));
		try
		{
			graphic = loader.load();
			controller = loader.getController();
		}
		catch (IOException e)
		{
			System.err.println("Unable to load MessageCell.fxml");
			e.printStackTrace();
		}
	}

	/**
	 * Cell update
	 * @param item message to display in this cell
	 * @param empty indicates if this cell represents data or not
	 */
	protected void updateItem(Message item, boolean empty)
	{
		super.updateItem(item, empty);

		if (empty || (item == null))
		{
			setText(null);
			setGraphic(null);
		}
		else
		{	
			controller.setContentLabel(item,dateVisibility);
			controller.setColor(ColorFromName.getColorFromName(item.getAuthor()));
			setText(null);
			setGraphic(graphic);
		}
	}
	
	public static void setDateVisibility(boolean visible) {
		dateVisibility = visible;
	}
}
