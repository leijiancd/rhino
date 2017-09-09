package org.mozilla.javascript;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Hashing {

  public static final EmptyHashMap<?, ?> EMPTY_MAP = new EmptyHashMap<Object, Object>();

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> emptyMap() {
    return (Map<K, V>)EMPTY_MAP;
  }

  public static final class EmptyHashMap<K, V>
    extends AbstractMap<K, V> {

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }

    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public V get(Object key) {
      return null;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public V put(K key, V value) {
      throw new IllegalArgumentException();
    }
  }

  public static final class SmallHashMap<K, V>
    extends AbstractMap<K, V> {

    public static final int SIZE = 128;

    private final HashSlot<K, V>[] slots;
    private int size;
    private HashSlot<K, V> firstAdded;
    private HashSlot<K, V> lastAdded;

    protected static final class HashSlot<K, V> {
      protected K key;
      protected V val;
      protected HashSlot<K, V> next;
      protected HashSlot<K, V> orderedNext;

      HashSlot(K key, V val) {
        this.key = key;
        this.val = val;
      }
    }

    private final class Entries
      extends AbstractSet<Entry<K, V>> {

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new Iter();
      }

      @Override
      public int size() {
        return SmallHashMap.this.size();
      }
    }

    private final class Iter
      implements Iterator<Entry<K, V>>
    {
      private HashSlot<K, V> next;

      Iter() {
        next = firstAdded;
      }

      @Override
      public boolean hasNext() {
        return (next != null);
      }

      @Override
      public Entry<K, V> next() {
        HashSlot<K, V> ret = next;
        next = next.orderedNext;
        return new SimpleEntry<K, V>(ret.key, ret.val);
      }
    }


    public SmallHashMap() {
      slots = new HashSlot[SIZE];
    }

    private int hash(Object key) {
      return (key == null ? 0 : Math.abs(key.hashCode()));
    }

    @Override
    public V put(K key, V val) {
      if (size == SIZE) {
        throw new IllegalArgumentException("Hash table larger than maximum size");
      }

      int hc = hash(key) % SIZE;
      HashSlot<K, V> prev = slots[hc];

      HashSlot<K, V> slot = prev;
      while (slot != null) {
        if (key == slot.key || key.equals(slot.key)) {
          V ret = slot.val;
          slot.val = val;
          return ret;
        }

        prev = slot;
        slot = slot.next;
      }

      HashSlot<K, V> newSlot = new HashSlot<K, V>(key, val);
      if (slots[hc] == prev) {
        slots[hc] = newSlot;
      } else {
        prev.next = newSlot;
      }

      if (lastAdded != null) {
        lastAdded.orderedNext = newSlot;
      }
      if (firstAdded == null) {
        firstAdded = newSlot;
      }
      lastAdded = newSlot;

      size++;

      return null;
    }

    @Override
    public V remove(Object key) {
      int hc = hash(key) % SIZE;
      HashSlot<K, V> prev = slots[hc];

      HashSlot<K, V> slot = prev;
      while (slot != null) {
        if (key == slot.key || key.equals(slot.key)) {
          break;
        }

        prev = slot;
        slot = slot.next;
      }

      if (slot == null) {
        return null;
      }

      V ret = slot.val;
      if (prev == slot) {
        slots[hc] = slot.next;
      } else {
        prev.next = slot.next;
      }

      if (slot == firstAdded) {
        prev = null;
        firstAdded = slot.orderedNext;
      } else {
        prev = firstAdded;
        while (prev.orderedNext != slot) {
          prev = prev.orderedNext;
        }
        prev.orderedNext = slot.orderedNext;
      }
      if (slot == lastAdded) {
        lastAdded = prev;
      }

      size--;

      return ret;
    }

    @Override
    public V get(Object key) {
      final HashSlot<K, V> slot = getSlot(key);
      if (slot == null) {
        return null;
      }
      return slot.val;
    }

    @Override
    public boolean containsKey(Object key) {
      final HashSlot<K, V> slot = getSlot(key);
      return (slot != null);
    }

    private HashSlot<K, V> getSlot(Object key) {
      HashSlot<K, V> slot;
      int hc = hash(key) % SIZE;

      for (slot = slots[hc]; slot != null; slot = slot.next) {
        if (key == slot.key || (key != null && key.equals(slot.key))) {
          return slot;
        }
      }
      return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new Entries();
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean isEmpty() {
      return size == 0;
    }
  }
}
