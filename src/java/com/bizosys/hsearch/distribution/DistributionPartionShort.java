/*
* Copyright 2010 Bizosys Technologies Limited
*
* Licensed to the Bizosys Technologies Limited (Bizosys) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The Bizosys licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bizosys.hsearch.distribution;
import java.util.Arrays;
import java.util.Collection;

public final class DistributionPartionShort {

	protected short[] distributes(Collection<Short> inputs, int parts) {
		
		short[] output = new short[parts - 1];
		short[] ranges = new short[]{Short.MIN_VALUE, Short.MAX_VALUE};
		GravityCenter gc = distribute(inputs, ranges);
		output[0] = gc.avgValue;
		System.out.println(output[0]);
		ranges = new short[]{Short.MIN_VALUE, gc.avgValue};
		for ( int i=1; i< (parts/2); i++) {
			GravityCenter mingc = distribute(inputs, ranges);
			output[i] = mingc.avgValue;
			ranges = new short[]{Short.MIN_VALUE, mingc.avgValue};
		}

		ranges = new short[]{gc.avgValue, Short.MAX_VALUE};
		for ( int i=parts/2; i< parts - 1; i++) {
			GravityCenter maxgc = distribute(inputs, ranges);
			output[i] = maxgc.avgValue;
			ranges = new short[]{maxgc.avgValue, Short.MAX_VALUE};
		}
		
		Arrays.sort(output);
		return output;
	}

	private GravityCenter distribute(Collection<Short> inputs, short[] ranges) {
		GravityCenter wts = new GravityCenter();

		getAverage(inputs, wts, ranges);
		int loops = 0;
		while (true) {
			loops++;
			if ( loops > 30) break;
			/**
			System.out.println("Min:" + wts.minValue + " Avg:" + wts.avgValue
					+ " Max:" + wts.maxValue + " LeftW:" + wts.leftWeight
					+ " RightW:" + wts.rightWeight);
			*/
			getWeights(inputs, wts, ranges);

			if (wts.leftWeight > wts.rightWeight) {
				short avgValue = wts.avgValue;
				wts.avgValue = (short) (avgValue - (avgValue/2 - wts.minValue/2));
				wts.maxValue = avgValue;
			} else {
				short avgValue = wts.avgValue;
				wts.avgValue = (short) (avgValue + (wts.maxValue/2 - avgValue/2) );
				wts.minValue = avgValue;
			}
			
			int diff = (wts.leftWeight > wts.rightWeight) ? (wts.leftWeight - wts.rightWeight) : (wts.rightWeight - wts.leftWeight);
			if ( diff <= 1 ) {
				/**
				System.out.println("Min:" + wts.minValue + " Avg:" + wts.avgValue
						+ " Max:" + wts.maxValue + " LeftW:" + wts.leftWeight
						+ " RightW:" + wts.rightWeight);
				*/
				break;
			}
		}
		return wts;
		
	}

	
	
	private void getAverage(Collection<Short> inputs, GravityCenter wts, short[] ranges) {
		short min = Short.MAX_VALUE;
		short max = Short.MIN_VALUE;
		for (short i : inputs) {
			if ( i < ranges[0] || i > ranges[1] ) continue;
			if (i < min) min = i;
			else if (i > max) max = i;
		}
		wts.minValue = min;
		wts.maxValue = max;
		wts.avgValue = (short) (max/2 - min/2);
	}

	private void getWeights(Collection<Short> inputs, GravityCenter weights, short[] ranges) {
		int left=0, right=0;
		short avgValue = weights.avgValue;
		for (short i : inputs) {
			if ( i < ranges[0] || i > ranges[1] ) continue;
			if (i >= avgValue) right++;
			else left++;
		}
		weights.leftWeight = left;
		weights.rightWeight = right;
	}
	
	
	private class GravityCenter {
		public short minValue;
		public short maxValue;
		public short avgValue;
		public int leftWeight;
		public int rightWeight;
		
		public GravityCenter() {
		}
	}	

	
}
