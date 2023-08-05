package application.listcells;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import models.Message;

/**
 * Controller for customized message
 * @author x0wass
 *
 */
public class MessageCellController
{
	/**
	 * Message label
	 */
	@FXML
	private Label messageLabel;
	
	@FXML
	private Label authorLabel;
	
	@FXML
	private Label dateLabel;
	
	/**
	 * Set {@link #messageLabel}, {@link #dateLabel}, {@link #authorLabel}from text
	 * @param mess the new message text to set in {@link #messageLabel}, {@link #dateLabel}, {@link #authorLabel}
	 */
	public void setContentLabel(Message mess, boolean dateVisibility)
	{
		String date = dateVisibility ? "["+mess.getFormattedDate() +"]" : "";
		dateLabel.setText(date);
		authorLabel.setText(mess.getAuthor());
		messageLabel.setText(mess.getContent());
	}

	/**
	 * Set entete message color in {@link #messageLabel}
	 * @param color the new color to set
	 */
	public void setColor(Color color)
	{
		authorLabel.setTextFill(color);
	}
}
