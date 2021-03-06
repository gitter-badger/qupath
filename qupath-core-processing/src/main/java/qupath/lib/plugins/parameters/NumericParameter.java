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
 * Abstract parameter to represent a numeric value.
 * 
 * @see DoubleParameter
 * @see IntParameter
 * 
 * @author Pete Bankhead
 *
 * @param <S>
 */
public abstract class NumericParameter<S extends Number> extends AbstractParameter<S> {
	
	private static final long serialVersionUID = 1L;
	
	private String unit = null;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;
	
	NumericParameter(String prompt, S defaultValue, String unit, double minValue, double maxValue, S lastValue, String helpText, boolean isHidden) {
		super(prompt, defaultValue, lastValue, helpText, isHidden);
		this.unit = unit;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public NumericParameter(String prompt, S defaultValue, String unit, double minValue, double maxValue, String helpText) {
		this(prompt, defaultValue, unit, minValue, maxValue, null, helpText, false);
	}

	public NumericParameter(String prompt, S defaultValue, String unit, String helpText) {
		this(prompt, defaultValue, unit, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, helpText);
	}
	
	public boolean hasLowerAndUpperBounds() {
		return hasLowerBound() && hasUpperBound();
	}
	
	public boolean isUnbounded() {
		return !hasLowerBound() && !hasUpperBound();
	}
	
	public double getLowerBound() {
		return minValue;
	}

	public double getUpperBound() {
		return maxValue;
	}
	
	/**
	 * Set the upper and lower bounds.
	 * 
	 * Note: depending on how the parameter is displayed (or if it is displayed) this might not appear
	 * to make a difference.  If shown through a ParameterPanel (in QuPath's JavaFX GUI) it is better to
	 * set limits via the panel rather than directly using this method.
	 * 
	 * @param minValue
	 * @param maxValue
	 */
	public void setRange(double minValue, double maxValue) {
		if (Double.isNaN(minValue))
			minValue = Double.NEGATIVE_INFINITY;
		if (Double.isNaN(maxValue))
			minValue = Double.POSITIVE_INFINITY;
		if (minValue <= maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		} else
			throw new IllegalArgumentException("Invalid range set " + minValue + "-" + maxValue + ": minValue must be <= maxValue");
	}

	public boolean hasLowerBound() {
		return Math.abs(minValue) <= Double.MAX_VALUE;
	}
	
	public boolean hasUpperBound() {
		return Math.abs(maxValue) <= Double.MAX_VALUE;
	}

	public boolean hasUnit() {
		return unit != null;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public abstract boolean setValueWithBoundsCheck(S lastValue);
	
	/**
	 * A class for setting the numeric value as a double (subclasses should convert this as needed).
	 * 
	 * @param val
	 * @return
	 */
	public abstract boolean setDoubleLastValue(double val);
	
	/**
	 * Numbers are considered valid if they are not NaN
	 */
	@Override
	public boolean isValidInput(S value) {
		return !Double.isNaN(value.doubleValue());
	}
	
	@Override
	public boolean setStringLastValue(String value) {
		try {
			double d = Double.parseDouble(value);
			return setDoubleLastValue(d);
		} catch (NumberFormatException e) {}
		return false;
	}
	
}
