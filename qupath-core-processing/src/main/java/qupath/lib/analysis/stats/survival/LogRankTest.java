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

package qupath.lib.analysis.stats.survival;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.common.GeneralTools;

/**
 * Helper class for applying log-rank test.
 * 
 * @author Pete Bankhead
 *
 */
public class LogRankTest {

	final private static Logger logger = LoggerFactory.getLogger(LogRankTest.class);

	private static ChiSquaredDistribution chi2 = new ChiSquaredDistribution(1);

	/**
	 * Compare KaplanMeier objects using log-rank test.
	 * 
	 * @param km1
	 * @param km2
	 * @return
	 */
	public static LogRankResult computeLogRankTest(final KaplanMeierData km1, final KaplanMeierData km2) {

		if (km1.isEmpty() || km2.isEmpty())
			return new LogRankResult();

		// Get times from both groups
		KaplanMeierData kmMerged = new KaplanMeierData("Temp", km1.getEvents()).addEvents(km2.getEvents());
		double[] timesEventsMerged = kmMerged.getAllTimes();//.getEventTimes();

		// Loop through all the event times
		double d1 = 0;
		double d2 = 0;
		double e1 = 0;
		double e2 = 0;
		for (int j = 0; j < timesEventsMerged.length; j++) {
			double t = timesEventsMerged[j];
			double n1j = km1.getAtRisk(t);
			double n2j = km2.getAtRisk(t);

			double d1j = km1.getEventsAtTime(t);
			double d2j = km2.getEventsAtTime(t);
			d1 += d1j;
			d2 += d2j;

			double pd = (d1j + d2j) / (n1j + n2j);
			e1 += pd * n1j;
			e2 += pd * n2j;
		}
		double stat = (d1-e1)*(d1-e1)/e1 + (d2-e2)*(d2-e2)/e2;
		double logRankPValue;
		if (stat < 0)
			logRankPValue = 1 - chi2.cumulativeProbability(-stat);
		else
			logRankPValue = 1 - chi2.cumulativeProbability(stat);

		double hazardRatio = (d1/e1)/(d2/e2);
		double hazardSE = Math.sqrt(1/e1 + 1/e2);
		double hazardLog = Math.log(hazardRatio);
		double hazardLower = Math.exp(hazardLog - 1.96*hazardSE);
		double hazardUpper = Math.exp(hazardLog + 1.96*hazardSE);
		//			logger.info("Log ranks: {} AND {}; HAZARD: {} ({} - {})", logRankInitial, logRankAgain, hazardRatio, hazardLower, hazardUpper);
		logger.trace(String.format("Log rank: %.4f\tHAZARD: %.3f (%.3f - %.3f)", logRankPValue, hazardRatio, hazardLower, hazardUpper));

		return new LogRankResult(logRankPValue, hazardRatio, hazardLower, hazardUpper);
	}
	
	
	
	
	
	/**
	 * Simple structure used to manage the result of a log rank test.
	 * 
	 * @author Pete Bankhead
	 *
	 */
	public static class LogRankResult {

		final double pValue;
		final double hazardRatio;
		final double hazardRatioLowerConfidence;
		final double hazardRatioUpperConfidence;

		public LogRankResult() {
			this.pValue = Double.NaN;
			this.hazardRatio = Double.NaN;
			this.hazardRatioLowerConfidence = Double.NaN;
			this.hazardRatioUpperConfidence = Double.NaN;
		}

		public LogRankResult(final double pValue, final double hazardRatio, final double hazardRatioLowerConfidence, final double hazardRatioUpperConfidence) {
			this.pValue = pValue;
			this.hazardRatio = hazardRatio;
			this.hazardRatioLowerConfidence = hazardRatioLowerConfidence;
			this.hazardRatioUpperConfidence = hazardRatioUpperConfidence;
		}

		public String getName(final String conditionA, final String conditionB) {
			return String.format("%s vs %s log-rank (HR)", conditionA, conditionB);
		}

		public String getResultString() {
			if (Double.isNaN(pValue))
				return "-";

			String pValueString;
			if (pValue > 1e-3)
				pValueString = GeneralTools.getFormatter(4).format(pValue);					
			else if (pValue > 1e-4)
				pValueString = GeneralTools.getFormatter(5).format(pValue);					
			else if (pValue > 1e-5)
				pValueString = GeneralTools.getFormatter(6).format(pValue);					
			else if (pValue > 1e-6)
				pValueString = GeneralTools.getFormatter(7).format(pValue);					
			else
				pValueString = GeneralTools.getFormatter(8).format(pValue);					
			//		}

			return String.format("%s (%.2f; %.2f-%.2f)", pValueString, hazardRatio, hazardRatioLowerConfidence, hazardRatioUpperConfidence);
		}

		public boolean isValid() {
			return !Double.isNaN(pValue);
		}

		public double getPValue() {
			return pValue;
		}

		public double getHazardRatio() {
			return hazardRatio;
		}

		public double getHazardRatioLowerConfidence() {
			return hazardRatioLowerConfidence;
		}

		public double getHazardRatioUpperConfidence() {
			return hazardRatioUpperConfidence;
		}

	}
	
}