package com.bizosys.hsearch.treetable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bizosys.hsearch.byteutils.ISortedByte;
import com.bizosys.hsearch.byteutils.SortedBytesArray;

public abstract class CellBase<K1> {
	public byte[] data;
	public ISortedByte<K1> k1Sorter = null;
	
	public abstract void parseElements() throws IOException;
	
	public void parseElements(byte[] data) throws IOException {
		this.data = data;
		if ( null == this.data) return;
		parseElements();
	}
	
	public int indexOf(K1 exactKey) throws IOException{
		if ( null == exactKey ) return -1; //Nulls not allowed
		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		if ( null == allKeysB) return -1;
		return k1Sorter.parse(allKeysB).getEqualToIndex(exactKey);
	}
	
	/**
	 * Find the index of matching range of keys
	 * @param keyMinimum
	 * @param keyMaximum
	 * @return
	 * @throws IOException
	 */
	public Collection<Integer> indexOf(K1 keyMinimum, K1 keyMaximum) throws IOException{
		Collection<Integer> indexes = new ArrayList<Integer>();
		findMatchingPositions(null, keyMinimum, keyMaximum, indexes);
		return indexes;
	}
	
	protected byte[] findMatchingPositions(K1 exactValue, K1 minimumValue, K1 maximumValue, Collection<Integer> foundPositions) throws IOException {
		
		if ( null == this.data) return null;
		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		if ( null == allKeysB) return null;
			
		if ( null != exactValue || null != minimumValue || null != maximumValue ) {
				
			if ( null != exactValue ) {
				k1Sorter.parse(allKeysB).getEqualToIndexes(exactValue, foundPositions);
			} else {
				if ( null != minimumValue && null != maximumValue ) {
					k1Sorter.parse(allKeysB).getRangeIndexesInclusive(minimumValue, maximumValue, foundPositions);
				} else if ( null != minimumValue) {
					k1Sorter.parse(allKeysB).getGreaterThanEqualToIndexes(minimumValue, foundPositions);
				} else {
					k1Sorter.parse(allKeysB).getLessThanEqualToIndexes(maximumValue, foundPositions);
				}
			}
		}
		return allKeysB;
	}

	public Collection<K1> get(K1 exactValue) throws IOException {
		List<K1> foundKeys = new ArrayList<K1>();
		get(exactValue, null ,null, foundKeys);
		return foundKeys;
	}

	public void get(K1 exactValue, Collection<K1> foundKeys ) throws IOException {
		get(exactValue, null ,null, foundKeys);
	}
	
	public Collection<K1> get(K1 minimumValue, K1 maximumValue) throws IOException {
		List<K1> foundKeys = new ArrayList<K1>();
		get(null, minimumValue ,maximumValue, foundKeys);
		return foundKeys;
	}
	
	public void get(K1 minimumValue, K1 maximumValue, Collection<K1> foundKeys) throws IOException {
		get(null, minimumValue ,maximumValue, foundKeys);
	}

	private void get(K1 exactValue, K1 minimumValue,
			K1 maximumValue, Collection<K1> foundKeys) throws IOException {

		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		List<Integer> foundPositions = new ArrayList<Integer>();
		findMatchingPositions(exactValue, minimumValue, maximumValue, foundPositions);

		k1Sorter.parse(allKeysB);
		for (int position : foundPositions) {
			foundKeys.add( k1Sorter.getValueAt(position) );
		}
	}

	public Set<K1> keySet() throws IOException {
		Set<K1> keys = new HashSet<K1>();
		keySet(keys);
		return keys;
	}
	
	public void keySet(Collection<K1> keys) throws IOException {
		
		if ( null == data) {
			throw new IOException("Null Data - Use sortedList to get Keys directly");
		}
		
		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		if ( null == allKeysB) return;
		k1Sorter.parse(allKeysB);
		int size = k1Sorter.getSize();
		for ( int i=0; i<size; i++) {
			keys.add(k1Sorter.getValueAt(i));
		}
	}
	
	protected abstract Collection<byte[]> getEmbeddedCellBytes() throws IOException;
	protected abstract byte[] getKeyBytes() throws IOException;
	
	public byte[] toBytes() throws IOException {

		List<byte[]> bytesElems = new ArrayList<byte[]>();

		bytesElems.add(getKeyBytes());
		byte[] valB = SortedBytesArray.getInstance().toBytes(getEmbeddedCellBytes());
		
		bytesElems.add(valB);
		byte[] cellB = SortedBytesArray.getInstance().toBytes(bytesElems);
		bytesElems.clear();
		return cellB;
	}
	
	public byte[] remove(K1 exactKey) throws IOException {
		return remove(exactKey, null, null);
	}
	
	public byte[] remove(K1 minimumValue, K1 maximumValue) throws IOException {
		return remove(null, minimumValue, maximumValue);
	}

	private byte[] remove(K1 exactValue, K1 minimumValue, K1 maximumValue) throws IOException {

		byte[] allKeysB = SortedBytesArray.getInstance().parse(data).getValueAt(0);
		Set<Integer> foundPositions = new HashSet<Integer>();
		findMatchingPositions(exactValue, minimumValue, maximumValue, foundPositions);
	
		k1Sorter.parse(allKeysB);
		int totalSize = k1Sorter.getSize();

		List<K1> allKeys = new ArrayList<K1>(totalSize);
		List<byte[]> allValues = new ArrayList<byte[]>(totalSize);
		ISortedByte<byte[]> sba = SortedBytesArray.getInstance();
		byte[] allValuesB = SortedBytesArray.getInstance().parse(data).getValueAt(1);
		
		sba.parse(allValuesB);
		for (int position = 0 ; position< totalSize; position++) {
			if ( foundPositions.contains(position)) continue;
			allKeys.add(k1Sorter.getValueAt(position) );
			allValues.add(sba.getValueAt(position));
		}
		
		List<byte[]> bytesElems = new ArrayList<byte[]>();
		bytesElems.add(k1Sorter.toBytes(allKeys));
		allKeys.clear();
		bytesElems.add(sba.toBytes(allValues));
		allValues.clear();
		
		byte[] cellB = SortedBytesArray.getInstance().toBytes(bytesElems);
		return cellB;
	}
	
	public static byte[] serializeKV(byte[] keys, byte[] values) throws IOException {
		List<byte[]> bytesElems = new ArrayList<byte[]>();
		bytesElems.add(keys);
		bytesElems.add(values);
		byte[] cellB = SortedBytesArray.getInstance().toBytes(bytesElems);
		bytesElems.clear();
		return cellB;		
	}


}
