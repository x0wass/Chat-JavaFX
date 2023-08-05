package models.messagesRunners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import models.MessagesHandler;

/**
 * Helper class to handle text messages between server and GUI clients.
 * to be used in any GUI Client
 * @author x0wass
 */
public class TextMessagesRunner extends AbstractMessagesRunner
{

	/**
	 * Input stream reader to read messages from the {@link #inPipe} and handle
	 * them to {@link AbstractMessagesRunner#messagesHandler}
	 */
	private BufferedReader inBR;

	/**
	 * Constructor
	 * @param messagesHandler The class responsible for didplaying messages anec evt users
	 * @param commonRun The commonRun flag to use with multiple threads
	 * @param parentLogger The caller's logger
	 */
	public TextMessagesRunner(MessagesHandler messagesHandler,
	                          Boolean commonRun,
	                          Logger parentLogger)
	{
		super(messagesHandler, commonRun, parentLogger);
	}

	/**
	 * Run loop:
	 * 	- Creates an {@link BufferedReader} to read text messages from
	 * 	{@link AbstractMessagesRunner#inPipe} then enters loop to
	 * 	- reads a line of text from {@link BufferedReader}
	 * 	- adds message to {@link AbstractMessagesRunner#messagesHandler}
	 */
	@Override
	public void run()
	{
		inBR = new BufferedReader(new InputStreamReader(inPipe));

		String messageIn;

		while (commonRun.booleanValue())
		{
			messageIn = null;
			/*
			 * - Lecture d'une ligne de texte en provenance du serveur avec inBR
			 * Si une exception survient lors de cette lecture on quitte la
			 * boucle.
			 * - Si cette ligne de texte n'est pas nulle on affiche le message
			 * dans le document avec le format voulu en utilisant
			 * #writeMessage(String)
			 * - Après la fin de la boucle on change commonRun à false de
			 * manière synchronisée afin que les autres threads utilisant ce
			 * commonRun puissent s'arrêter eux aussi :
			 * synchronized(commonRun)
			 * {
			 * commonRun = Boolean.FALSE;
			 * }
			 * Dans toutes les étapes si un problème survient (erreur,
			 * exception, ...) on quitte la boucle en ayant au préalable ajouté
			 * un "warning" ou un "severe" au logger (en fonction de l'erreur
			 * rencontrée) et mis le commonRun à false (de manière synchronisé).
			 */
			try
			{
				/*
				 * read from input (blocking call)
				 */
				messageIn = inBR.readLine();
			}
			catch (IOException e)
			{
				logger.warning("ClientFrame: I/O Error reading");
				break;
			}

			if (messageIn != null)
			{
				// handle message to handler
				messagesHandler.addMessage(messageIn);
			}
			else // messageIn == null
			{
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger
			    .info("ClientFrame::cleanup: changing run state at the end ... ");
			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}

		cleanup();
	}


	/**
	 * Cleanup: close streams
	 */
	@Override
	public void cleanup()
	{
		logger.info("closing input buffered reader ... ");
		try
		{
			inBR.close();
		}
		catch (IOException e)
		{
			logger.warning("failed to close object input buffered reader "
			    + e.getLocalizedMessage());
		}

		super.cleanup();
	}

}
