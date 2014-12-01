package cn.clickwise.math.random;

public class DIGAMMAFUNC {
	/** Actually the negative Euler-Mascheroni constant */
	public static final double EULER_MASCHERONI = -0.5772156649015328606065121;
	public static final double PI_SQUARED_OVER_SIX = Math.PI * Math.PI / 6;
	public static final double HALF_LOG_TWO_PI = Math.log(2 * Math.PI) / 2;

	public static final double DIGAMMA_COEF_1 = 1 / 12;
	public static final double DIGAMMA_COEF_2 = 1 / 120;
	public static final double DIGAMMA_COEF_3 = 1 / 252;
	public static final double DIGAMMA_COEF_4 = 1 / 240;
	public static final double DIGAMMA_COEF_5 = 1 / 132;
	public static final double DIGAMMA_COEF_6 = 691 / 32760;
	public static final double DIGAMMA_COEF_7 = 1 / 12;
	public static final double DIGAMMA_COEF_8 = 3617 / 8160;
	public static final double DIGAMMA_COEF_9 = 43867 / 14364;
	public static final double DIGAMMA_COEF_10 = 174611 / 6600;
	public static final double DIGAMMA_LARGE = 9.5;
	public static final double DIGAMMA_SMALL = .000001;

	/**
	 * Calculate digamma using an asymptotic expansion involving Bernoulli
	 * numbers.
	 */

	public static double digamma(double z) {
		// This is based on matlab code by Tom Minka
		// if (z < 0) { System.out.println(" less than zero"); }
		double psi = 0;
		if (z < DIGAMMA_SMALL) {
			psi = EULER_MASCHERONI - (1 / z); // + (PI_SQUARED_OVER_SIX * z);
			/*
			 * for (int n=1; n<100000; n++) { psi += z / (n * (n + z)); }
			 */
			return psi;
		}
		while (z < DIGAMMA_LARGE) {
			psi -= 1 / z;
			z++;
		}
		double invZ = 1 / z;
		double invZSquared = invZ * invZ;
		psi += Math.log(z)
				- .5
				* invZ
				- invZSquared
				* (DIGAMMA_COEF_1 - invZSquared
						* (DIGAMMA_COEF_2 - invZSquared
								* (DIGAMMA_COEF_3 - invZSquared
										* (DIGAMMA_COEF_4 - invZSquared
												* (DIGAMMA_COEF_5 - invZSquared
														* (DIGAMMA_COEF_6 - invZSquared
																* DIGAMMA_COEF_7))))));
		return psi;
	}

	public static double trigamma(double z) {
		int shift = 0;
		while (z < 2) {
			z++;
			shift++;
		}

		double oneOverZ = 1.0 / z;
		double oneOverZSquared = oneOverZ * oneOverZ;

		double result = oneOverZ + 0.5 * oneOverZSquared + 0.1666667
				* oneOverZSquared * oneOverZ - 0.03333333 * oneOverZSquared
				* oneOverZSquared * oneOverZ + 0.02380952 * oneOverZSquared
				* oneOverZSquared * oneOverZSquared * oneOverZ - 0.03333333
				* oneOverZSquared * oneOverZSquared * oneOverZSquared
				* oneOverZSquared * oneOverZ;

		System.out.println(z + " -> " + result);

		while (shift > 0) {
			shift--;
			z--;
			result += 1.0 / (z * z);
			System.out.println(z + " -> " + result);
		}

		return result;
	}
	
	/** Currently aliased to <code>logGammaStirling</code> */
	//这个应该相当于
	public static double logGamma(double z) {
		return logGammaStirling(z);
	}

	/** Use a fifth order Stirling's approximation.
	 * 
	 *	@param z Note that Stirling's approximation is increasingly unstable as <code>z</code> approaches 0. If <code>z</code> is less than 2, we shift it up, calculate the approximation, and then shift the answer back down.
	 */
	public static double logGammaStirling(double z) {
		int shift = 0;
		while (z < 2) {
			z++;
			shift++;
		}

		double result = HALF_LOG_TWO_PI + (z - 0.5) * Math.log(z) - z +
		1/(12 * z) - 1 / (360 * z * z * z) + 1 / (1260 * z * z * z * z * z);

		while (shift > 0) {
			shift--;
			z--;
			result -= Math.log(z);
		}

		return result;
	}
}
