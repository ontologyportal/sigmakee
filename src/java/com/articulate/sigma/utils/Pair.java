package com.articulate.sigma.utils;

import java.util.Comparator;

/**
 * This code is copyright Infosys Ltd 2017.
  This software is released under the GNU Public License.
 */

/**
 * 
 * @author mohit.gupta
 *
 */
public class Pair implements Comparator<Pair> {

	public int count = 0;
	public String str = null;

	public Pair() {

	}

	public Pair(int count, String str) {
		this.count = count;
		this.str = str;
	}

	@Override
	public int compare(Pair o1, Pair o2) {
		return o1.count - o2.count;
	}
}
