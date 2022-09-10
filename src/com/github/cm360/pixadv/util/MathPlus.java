package com.github.cm360.pixadv.util;

public class MathPlus {

	public static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

}
