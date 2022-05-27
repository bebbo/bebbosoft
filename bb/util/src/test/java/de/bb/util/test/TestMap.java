package de.bb.util.test;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import junit.framework.TestCase;
import de.bb.util.MultiMap;
import de.bb.util.SingleMap;

public class TestMap extends TestCase {

	@Test
	public void testMultiMap() {
		MultiMap<Integer, Integer> multi = new MultiMap<Integer, Integer>();

		for (int j = 0; j <= 10; j += 2) {
			for (int i = j; i <= 20; i += 2) {
				multi.put(i, i);
			}
		}

		int N = 0;
		for (Iterator<Entry<Integer, Integer>> i = multi.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(51, N);
	}

	@Test
	public void testMultiHeadMap() {
		MultiMap<Integer, Integer> multi = new MultiMap<Integer, Integer>();

		for (int j = 0; j <= 10; j += 2) {
			for (int i = j; i <= 20; i += 2) {
				multi.put(i, i);
			}
		}

		int N = 0;
		Map<Integer, Integer> map = multi.headMap(11);
		for (Iterator<Entry<Integer, Integer>> i = map.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(21, N);
	}

	@Test
	public void testMultiTailMap() {
		MultiMap<Integer, Integer> multi = new MultiMap<Integer, Integer>();

		for (int j = 0; j <= 10; j += 2) {
			for (int i = j; i <= 20; i += 2) {
				multi.put(i, i);
			}
		}

		int N = 0;
		Map<Integer, Integer> map = multi.tailMap(11);
		for (Iterator<Entry<Integer, Integer>> i = map.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(30, N);
	}

	@Test
	public void testMultiSubMap() {
		MultiMap<Integer, Integer> multi = new MultiMap<Integer, Integer>();

		for (int j = 0; j <= 10; j += 2) {
			for (int i = j; i <= 20; i += 2) {
				multi.put(i, i);
			}
		}

		int N = 0;
		Map<Integer, Integer> map = multi.subMap(5,
				15);
		for (Iterator<Entry<Integer, Integer>> i = map.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(27, N);
	}

	@Test
	public void testSingleMap() {
		SingleMap<Integer, Integer> single = new SingleMap<Integer, Integer>();

		for (int i = 0; i <= 20; i += 2) {
			single.put(i, i);
		}

		int N = 0;
		for (Iterator<Entry<Integer, Integer>> i = single.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(11, N);
	}

	@Test
	public void testSingleHeadMap() {
		SingleMap<Integer, Integer> single = new SingleMap<Integer, Integer>();

		for (int i = 0; i <= 20; i += 2) {
			single.put(i, i);
		}

		int N = 0;
		Map<Integer, Integer> head = single.headMap(11);
		for (Iterator<Entry<Integer, Integer>> i = head.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(6, N);
	}

	@Test
	public void testSingleTailMap() {
		SingleMap<Integer, Integer> single = new SingleMap<Integer, Integer>();

		for (int i = 0; i <= 20; i += 2) {
			single.put(i, i);
		}

		int N = 0;
		Map<Integer, Integer> head = single.tailMap(9);
		for (Iterator<Entry<Integer, Integer>> i = head.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(6, N);
	}

	@Test
	public void testSingleTailMapEmpty() {
		SingleMap<Integer, Integer> single = new SingleMap<Integer, Integer>();

		for (int i = 0; i <= 20; i += 2) {
			single.put(i, i);
		}

		int N = 0;
		Map<Integer, Integer> tail = single.tailMap(21);
		for (Iterator<Entry<Integer, Integer>> i = tail.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(0, N);
	}

	@Test
	public void testSingleSubMap() {
		SingleMap<Integer, Integer> single = new SingleMap<Integer, Integer>();

		for (int i = 0; i <= 20; i += 2) {
			single.put(i, i);
		}

		int N = 0;
		Map<Integer, Integer> head = single.subMap(5, 15);
		for (Iterator<Entry<Integer, Integer>> i = head.entrySet().iterator(); i.hasNext();) {
			Entry<Integer, Integer> e = i.next();
			Integer j = (Integer) e.getKey();
			assertEquals(j, e.getValue());
			++N;
		}
		assertEquals(5, N);
	}

	@Test
	public void testMultiMap_bug_1() {
		MultiMap<String, String> multi = new MultiMap<String, String>();
		multi.put("esp", "1");
		multi.put("ebp", "2");
		Map<String, String> tail = multi.tailMap("0");
		int N = 0;
		for (Iterator<Entry<String, String>> i = tail.entrySet().iterator(); i.hasNext();) {
			Entry<String, String> e = i.next();
			e.getKey();
			++N;
		}
		assertEquals(2, N);
	}
}
