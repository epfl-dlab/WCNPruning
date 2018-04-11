package ch.epfl.dlab.wikipedia.wcnpruning;

import java.util.Map;

/**
 * 
 * @author Tiziano Piccardi tiziano.piccardi@epfl.ch
 *
 */
public interface PurityScoreFunction {
	/**
	 * Get the score of one types distribution
	 * @param tfd Types Frequency Distribution in the format type-count
	 * @return
	 */
	public double getScore(Map<String, Integer> tfd);
	
	/**
	 * Get the alias of the scoring function
	 * @return
	 */
	public String getName();
}
