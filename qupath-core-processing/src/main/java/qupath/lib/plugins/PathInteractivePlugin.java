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

package qupath.lib.plugins;

import java.util.Collection;

import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.plugins.parameters.ParameterList;

/**
 * Interface to define a PathPlugin that requires a list of (user-adjustable) parameters.
 * 
 * By using this interface, QuPath is able to show a standardized dialog box within which 
 * the parameters are set.
 * 
 * @author Pete Bankhead
 *
 * @param <T>
 */
public interface PathInteractivePlugin<T> extends PathPlugin<T> {
	
	/**
	 * This should return a default ParameterList.
	 * 
	 * Each time the method is invoked, a new ParameterList should be created.
	 * 
	 * @return
	 */
	public abstract ParameterList getDefaultParameterList(final ImageData<T> imageData);

	
	/**
	 * Get a collection of possible parent objects that the plugin could have.
	 * This may be used, for example, to specified that analysis may be applied to any/all TMA cores or annotations.
	 * If no parent objects are required, PathRootObject.class should be returned in the list.
	 * 
	 * @return
	 */
	public Collection<Class<? extends PathObject>> getSupportedParentObjectClasses();

}
