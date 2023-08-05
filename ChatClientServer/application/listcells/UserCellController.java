package application.listcells;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * Controller for customized
 * @author x0wass
 *
 */
public class UserCellController
{
	/**
	 * User label
	 */
	@FXML
	private Label userLabel;

	/**
	 * Set {@link #userLabel} from text
	 * @param userText the new text to set in {@link #userLabel}
	 */
	public void setContentLabel(String userText)
	{
		userLabel.setText(userText);
	}

	/**
	 * Set message color in {@link #userLabel}
	 * @param color the new color to set
	 */
	public void setColor(Color color)
	{
		userLabel.setTextFill(color);
	}
}
