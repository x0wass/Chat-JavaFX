package models;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

/**
 * Filter allowing to check if a {@link Message} comes from one of the authors
 * registered in this filter. Thus this class contains a set of unique names
 * corresponding to the authors registered in this filter.
 * This filter is created using a {@link ListModel} and a
 * {@link ListSelectionModel} allowing to populate the filter with currently
 * selected names
 * @author x0wass
 */
public class AuthorListFilter implements Predicate<Message>//, ListDataListener
{
	/**
	 * Set of unique authors registered in this filter
	 */
	private Set<String> authors;

	/**
	 * Flag indicating if filtering is active or not
	 */
	private boolean filtering;

	/**
	 * Default constructor
	 * Builds an empty {@link #authors} name set and initialize
	 * {@link #filtering} state to false
	 */
	public AuthorListFilter()
	{
		authors = new TreeSet<String>();
		filtering = false;
	}

	/**
	 * Constructor from a {@link ListModel} and a {@link ListSelectionModel}.
	 * Initialize the {@link #authors} and populate it with names of the
	 * {@link ListModel} selected in the {@link ListSelectionModel}
	 * @param listModel the list model containing the elements
	 * @param selectionModel the list selection model containing the indices of
	 * selected elements
	 */
	public AuthorListFilter(ListModel<String> listModel,
	                        ListSelectionModel selectionModel)
	{
		this();
		int minIndex = 0;
		int maxIndex = 0;

		if (selectionModel != null)
		{
			minIndex = selectionModel.getMinSelectionIndex();
			maxIndex = selectionModel.getMaxSelectionIndex();
		}
		else
		{
			if (listModel != null)
			{
				maxIndex = listModel.getSize() - 1;
			}
		}

		if ((listModel != null) && (selectionModel != null))
		{
			for (int i = minIndex; i <= maxIndex; i++)
			{
				if (selectionModel.isSelectedIndex(i))
				{
					// Fill authors set with selected elements
					add(listModel.getElementAt(i));
				}
			}
		}
	}

	/**
	 * Adds an author to the authors set
	 * @param author the author to add
	 * @return true if the author was not already in the set and has been
	 * successfully added
	 */
	public boolean add(String author)
	{
		if (author != null)
		{
			return authors.add(author);
		}

		return false;
	}

	/**
	 * Removes an author from the authors set
	 * @param author the author to remove from the set
	 * @return true if the author has been successfully removed from the set
	 */
	public boolean remove(String author)
	{
		return authors.remove(author);
	}

	/**
	 * Clears all authors from the authors set
	 */
	public boolean clear()
	{
		if (!authors.isEmpty())
		{
			authors.clear();
			return true;
		}

		return false;
	}

	/**
	 * Filtering state accessor
	 * @return true if filtering is active, false otherwise
	 */
	public boolean isFiltering()
	{
		return filtering;
	}

	/**
	 * Filtering state setter
	 * @param filtering the new filtering state
	 */
	public void setFiltering(boolean filtering)
	{
		this.filtering = filtering;
	}

	/**
	 * Predicate test on a specific message
	 * @param m the message to test
	 * @return true filtering is off. True if the message has an author
	 * registered in this filter when filtering is on. False otherwise.
	 */
	@Override
	public boolean test(Message m)
	{
		if (authors.isEmpty() || !filtering)
		{
			// Il n'y aucun auteur à filtrer
			return true;
		}
		if (m != null)
		{
			for (String author : authors)
			{
				if (m.hasAuthor())
				{
					if (m.getAuthor().equals(author))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * String representation of the filter
	 * @return A String represneting this filter's content
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Filtering ");
		for (Iterator<String> it = authors.iterator(); it.hasNext(); )
		{
			sb.append(it.next());
			if (it.hasNext())
			{
				sb.append(", ");
			}
		}

		return sb.toString();
	}
}
