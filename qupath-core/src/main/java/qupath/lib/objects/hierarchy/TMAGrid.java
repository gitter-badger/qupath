/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.objects.hierarchy;

import java.io.Serializable;
import java.util.List;

import qupath.lib.objects.TMACoreObject;

/**
 * Interface defining a (rectangular) TMA grid.
 * 
 * @author Pete Bankhead
 *
 */
public interface TMAGrid extends Serializable {
	
	public int getCoreIndex(String coreName);

	public int nCores();
	
	public int getGridWidth();
	
	public int getGridHeight();
	
	public TMACoreObject getTMACore(String coreName);
	
	public TMACoreObject getTMACoreByUniqueID(String uniqueID);
	
	public TMACoreObject getTMACore(int ind);

	public TMACoreObject getTMACore(int row, int col);

	public List<TMACoreObject> getTMACoreList();
	
	public TMACoreObject getTMACoreForPixel(double x, double y);

}
