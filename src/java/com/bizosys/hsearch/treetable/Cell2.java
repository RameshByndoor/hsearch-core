package com.bizosys.hsearch.treetable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bizosys.hsearch.byteutils.ISortedByte;
import com.bizosys.hsearch.byteutils.SortedBytesArray;
import com.bizosys.hsearch.hbase.ObjectFactory;

public class Cell2<K1, V> {
	
	public ISortedByte<K1> k1Sorter = null;
	public ISortedByte<V> vSorter = null;
	private List<CellKeyValue<K1, V>> sortedList = null;
	public byte[] data = null;
	
	public Cell2(ISortedByte<K1> k1Sorter, ISortedByte<V> vSorter) {
		this.k1Sorter = k1Sorter;
		this.vSorter = vSorter;
	}

	public Cell2(ISortedByte<K1> k1Sorter, ISortedByte<V> vSorter, List<CellKeyValue<K1, V>> sortedList) {
		this(k1Sorter, vSorter);
		this.sortedList = sortedList;
	}

	
	public Cell2 (ISortedByte<K1> k1Sorter, ISortedByte<V> vSorter, byte[] data) {
		this(k1Sorter, vSorter);
		this.data = data;
	}
	
	public void add(K1 k, V v) {
		if ( null == sortedList) sortedList = new ArrayList<CellKeyValue<K1,V>>();
		sortedList.add(new CellKeyValue<K1, V>(k, v) );
	}
	
	public void sort(Comparator<CellKeyValue<K1, V>> comp) {
		if ( null == sortedList) return;
		Collections.sort(sortedList, comp);
	}
	
	public List<CellKeyValue<K1, V>> getMap(byte[] data) throws IOException {
		this.data = data;
		parseElements();
		return sortedList;
	}
	
	public List<CellKeyValue<K1, V>> getMap() throws IOException {
		if ( null != sortedList) return sortedList;
		if ( null != this.data) {
			parseElements();
			return sortedList;
		}
		throw new IOException("Cell is not initialized");
	}
	
	public void populate(Map<K1,V> map) throws IOException {
		ISortedByte<byte[]> kvB = SortedBytesArray.getInstance().parse(data);
		
		byte[] allKeysB = kvB.getValueAt(0);
		if ( null == allKeysB ) return;

		byte[] allValuesB = kvB.getValueAt(1);
		if ( null == allValuesB ) return;
		
		int sizeK = k1Sorter.parse(allKeysB).getSize();
		int sizeV = vSorter.parse(allValuesB).getSize();
		
		if ( sizeK != sizeV) throw new IOException("Mismatch keys : " + sizeK + " , and values = " + sizeK); 
		
		for ( int i=0; i<sizeK; i++) {
			map.put(k1Sorter.getValueAt(i), vSorter.getValueAt(i));
		}
	}	
	
	
	public byte[] toBytesOnSortedData() throws IOException {

		if ( null == sortedList) return null;
		if ( sortedList.size() == 0 ) return null;
		
		Collection<K1> keys = new ArrayList<K1>();
		Collection<V> values = new ArrayList<V>();
		
		for (CellKeyValue<K1, V> entry : sortedList) {
			keys.add(entry.getKey());
			values.add(entry.getValue());
		}

		List<byte[]> bytesElems = new ArrayList<byte[]>();
		bytesElems.add(k1Sorter.toBytes(keys)); keys.clear();
		bytesElems.add(vSorter.toBytes(values)); values.clear();
		
		byte[] cellB = SortedBytesArray.getInstance().toBytes(bytesElems);
		bytesElems.clear();
		return cellB;
	}
	
	public byte[] toBytes(V minValue, V maximumValue, boolean leftInclusize, boolean rightInclusize, Comparator<V> vComp) throws IOException {
		
		if ( sortedList.size() == 0 ) return null;
		Collection<K1> keys = new ArrayList<K1>();
		Collection<V> values = new ArrayList<V>();
		
		for (CellKeyValue<K1, V> entry : sortedList) {
			if ( leftInclusize ) if ( vComp.compare(entry.getValue(), minValue) < 0 ) continue;
			else if ( vComp.compare(entry.getValue(), minValue) <= 0 ) continue;
			
			if ( rightInclusize ) if ( vComp.compare(entry.getValue(), maximumValue) >= 0 ) continue;
			else if ( vComp.compare(entry.getValue(), maximumValue) > 0 ) continue;
						
			keys.add(entry.getKey());
			values.add(entry.getValue());
		}
		
		if ( keys.size() == 0 ) return null;

		
		List<byte[]> bytesElems = ObjectFactory.getInstance().getByteArrList();
		bytesElems.add(k1Sorter.toBytes(keys));
		keys.clear();
		bytesElems.add(vSorter.toBytes(values));
		values.clear();
		
		byte[] cellB = SortedBytesArray.getInstance().toBytes(bytesElems);
		
		ObjectFactory.getInstance().putByteArrList(bytesElems);
		
		return cellB;
	}		
	
	public Collection<Integer> indexOf(V exactValue) throws IOException {
		List<Integer> foundPositions = new ArrayList<Integer>();
		findMatchingPositions(exactValue, null, null, foundPositions);
		return foundPositions;
	}
	
	public Collection<Integer> indexOf(V minimumValue, V maximumValue) throws IOException {
		List<Integer> foundPositions = new ArrayList<Integer>();
		findMatchingPositions(null, minimumValue, maximumValue, foundPositions);
		return foundPositions;
	}

	public Set<K1> keySet(V exactValue) throws IOException {
		Set<K1> keys = new HashSet<K1>();
		keySet(exactValue, null, null, keys);
		return keys;
	}
	
	public void keySet(V exactValue, Collection<K1> keys) throws IOException {
		keySet(exactValue, null, null, keys);
	}

	public Set<K1> keySet(V minimumValue, V maximumValue) throws IOException {
		Set<K1> keys = new HashSet<K1>();
		keySet(minimumValue, maximumValue, keys);
		return keys;
	}
	
	public void keySet(V minimumValue, V maximumValue, Collection<K1> keys) throws IOException {
		keySet(null, minimumValue, maximumValue, keys);
	}
	
	private void keySet( V exactValue, V minimumValue, V maximumValue, Collection<K1> foundKeys) throws IOException {
		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		List<Integer> foundPositions = new ArrayList<Integer>();
		findMatchingPositions(exactValue, minimumValue, maximumValue, foundPositions);
		
		k1Sorter.parse(allKeysB);
		//System.out.println("Size :" + k1Sorter.getSize());
		for (int position : foundPositions) {
			foundKeys.add( k1Sorter.getValueAt(position) );
		}
	}
	
	public Collection<V> values(V exactValue) throws IOException {
		Collection<V> values = new ArrayList<V>();
		matchValues(exactValue, null, null, values);
		return values;
	}
	
	public Collection<V> values(V minimumValue, V maximumValue) throws IOException {
		Collection<V> values = new ArrayList<V>();
		matchValues(null, minimumValue, maximumValue, values);
		return values;
	}

	public void values(V exactValue, Collection<V> foundValues) throws IOException {
		matchValues(exactValue, null, null, foundValues);
	}
	
	public void values(V minimumValue, V maximumValue, Collection<V> foundValues) throws IOException {
		matchValues(null, minimumValue, maximumValue, foundValues);
	}	
	
	private void matchValues( V exactValue, V minimumValue, V maximumValue, Collection<V> foundValues) throws IOException {
		List<Integer> foundPositions = new ArrayList<Integer>();
		byte[] allValuesB = findMatchingPositions(exactValue, minimumValue, maximumValue, foundPositions);

		vSorter.parse(allValuesB);
		for (int position : foundPositions) {
			foundValues.add( vSorter.getValueAt(position) );
		}
	}

	public Collection<V> values() throws IOException {

		Collection<V> values = new ArrayList<V>();
		values(values);
		return values;
	}
	
	public void values( Collection<V> values) throws IOException {
		
		byte[] allValuesB = SortedBytesArray.getInstance().parse(data).getValueAt(1);
		if ( null == allValuesB) return;
		
		vSorter.parse(allValuesB);
		int size = vSorter.getSize();
		for ( int i=0; i<size; i++) {
			values.add(vSorter.getValueAt(i));
		}
	}
		
	public Collection<V> valuesAt(Collection<Integer> foundPositions) throws IOException {
		List<V> foundValues = new ArrayList<V>();
		valuesAt(foundValues, foundPositions );
		return foundValues;
	}

	public void valuesAt(Collection<V> foundValues, Collection<Integer> foundPositions) throws IOException {
		
		byte[] allValuesB = SortedBytesArray.getInstance().parse(data).getValueAt(1);
		for (int position : foundPositions) {
			foundValues.add( vSorter.parse(allValuesB).getValueAt(position));
		}
	}
	
	private byte[] findMatchingPositions( V exactValue, V minimumValue, V maximumValue, Collection<Integer> foundPositions) throws IOException {
			
		
		byte[] allValsB = SortedBytesArray.getInstance().parse(data).getValueAt(1);
		if ( null == allValsB) return null;
		vSorter.parse(allValsB);	
		
		if ( null != exactValue || null != minimumValue || null != maximumValue ) {
				
			if ( null != exactValue ) {
				vSorter.getEqualToIndexes(exactValue, foundPositions);
			} else {
				if ( null != minimumValue && null != maximumValue ) {
					vSorter.getRangeIndexesInclusive(minimumValue, maximumValue, foundPositions);
				} else if ( null != minimumValue) {
					vSorter.getGreaterThanEqualToIndexes(minimumValue, foundPositions);
				} else {
					vSorter.getLessThanEqualToIndexes(maximumValue, foundPositions);
				}
			}
		}
		return allValsB;
	}

	public Collection<K1> keySet() throws IOException {
		List<K1> keys = new ArrayList<K1>();
		keySet(keys);
		return keys;
	}
	
	public void keySet( Collection<K1> keys) throws IOException {
		
		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		if ( null == allKeysB ) return;
		
		int size = k1Sorter.parse(allKeysB).getSize();
		for ( int i=0; i<size; i++) {
			keys.add(k1Sorter.getValueAt(i));
		}
	}
	
	public void parseElements() throws IOException {
		if ( null == this.sortedList) this.sortedList = new ArrayList<CellKeyValue<K1, V>>();
		else this.sortedList.clear();
		
		List<K1> allKeys = new ArrayList<K1>();
		keySet(allKeys);
		
		List<V> allValues = new ArrayList<V>(); 
		values(allValues);
		
		int allKeysT = allKeys.size();
		if ( allKeysT != allValues.size() ) throw new IOException( 
			"Keys and Values tally mismatched : keys(" + allKeysT + ") , values(" + allValues.size() + ")");
		
		for ( int i=0; i<allKeysT; i++) {
			sortedList.add( new CellKeyValue<K1, V>(allKeys.get(i), allValues.get(i)));
		}
	}
	
	public void remove(K1 key) {
		if ( null == this.sortedList) return;
		int elemIndex = this.sortedList.indexOf(key);
		if ( -1 == elemIndex) return;
		this.sortedList.remove(elemIndex);
	}
}
