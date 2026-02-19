package org.sakaiproject.service.gradebook.shared.owl.finalgrades.report;

public class FGChanges
{
	public final int revised, added, removed;

	public FGChanges(int rev, int add, int rem)
	{
		revised = rev;
		added = add;
		removed = rem;
	}
}
