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

package qupath.lib.plugins.parameters;

/**
 * Parameter that doesn't actually store any value, but might contain some 
 * useful text (or a title) that may need to be displayed.
 * 
 * @author Pete Bankhead
 *
 */
public class EmptyParameter extends AbstractParameter<String> {
	
	private static final long serialVersionUID = 1L;

	protected boolean isTitle = false;
	
	EmptyParameter(String prompt, boolean isTitle, boolean isHidden) {
		super(prompt, null, null, null, isHidden);
		this.isTitle = isTitle;
	}
	
	/**
	 * An empty parameter, which does not take any input, always returning null.
	 * It is useful to store a prompt that should be displayed.
	 * 
	 * The isTitle parameter identifies whether the prompt corresponds to a title, 
	 * so that it might be displayed differently (e.g. in bold)
	 * 
	 * @param prompt
	 * @param defaultValue
	 */
	public EmptyParameter(String prompt, boolean isTitle) {
		this(prompt, isTitle, false);
	}
	
	public EmptyParameter(String prompt) {
		this(prompt, false);
	}
	
	public boolean isTitle() {
		return isTitle;
	}

	/**
	 * Always returns false
	 */
	@Override
	public boolean isValidInput(String value) {
		return false;
	}

	@Override
	public boolean setStringLastValue(String value) {
		return false;
	}
	
	@Override
	public String toString() {
		return getPrompt();
	}

	@Override
	public Parameter<String> duplicate() {
		return new EmptyParameter(getPrompt(), isTitle);
	}

}
