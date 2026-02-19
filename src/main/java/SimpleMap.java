import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A simple hash-map implementation of the {@link Map} interface backed by an
 * array of chained {@link Entry} buckets.
 *
 * <p>This implementation supports {@code null} keys and {@code null} values,
 * provides O(1) amortised {@code get}/{@code put}/{@code remove} operations,
 * and automatically rehashes when the load factor exceeds 0.75.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class SimpleMap<K, V> extends AbstractMap<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Node<K, V>[] table;
    private int size;
    private int modCount;
    private final float loadFactor;

    /** Single linked-list node used for chaining within a bucket. */
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V old = this.value;
            this.value = newValue;
            return old;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    /** Constructs an empty map with default initial capacity and load factor. */
    @SuppressWarnings("unchecked")
    public SimpleMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        this.table = new Node[DEFAULT_INITIAL_CAPACITY];
    }

    /**
     * Constructs an empty map with the specified initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    @SuppressWarnings("unchecked")
    public SimpleMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 1) throw new IllegalArgumentException("initialCapacity must be >= 1");
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("loadFactor must be positive");
        this.loadFactor = loadFactor;
        this.table = new Node[roundUpToPowerOfTwo(Math.min(initialCapacity, MAXIMUM_CAPACITY))];
    }

    /** Constructs a new map containing all mappings from the supplied map. */
    public SimpleMap(Map<? extends K, ? extends V> m) {
        this();
        putAll(m);
    }

    // ------------------------------------------------------------------
    // Core Map operations
    // ------------------------------------------------------------------

    @Override
    public int size() {
        return size;
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = getNode(key);
        return node == null ? null : node.value;
    }

    @Override
    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    @Override
    public V put(K key, V value) {
        int h = hash(key);
        int idx = indexFor(h, table.length);
        for (Node<K, V> n = table[idx]; n != null; n = n.next) {
            if (n.hash == h && Objects.equals(n.key, key)) {
                V old = n.value;
                n.value = value;
                return old;
            }
        }
        table[idx] = new Node<>(h, key, value, table[idx]);
        size++;
        modCount++;
        if (size > table.length * loadFactor) {
            resize(table.length >= MAXIMUM_CAPACITY ? table.length : table.length * 2);
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        int h = hash(key);
        int idx = indexFor(h, table.length);
        Node<K, V> prev = null;
        for (Node<K, V> n = table[idx]; n != null; prev = n, n = n.next) {
            if (n.hash == h && Objects.equals(n.key, key)) {
                if (prev == null) {
                    table[idx] = n.next;
                } else {
                    prev.next = n.next;
                }
                size--;
                modCount++;
                return n.value;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
        modCount++;
    }

    // ------------------------------------------------------------------
    // entrySet – required by AbstractMap
    // ------------------------------------------------------------------

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private Node<K, V> getNode(Object key) {
        int h = hash(key);
        int idx = indexFor(h, table.length);
        for (Node<K, V> n = table[idx]; n != null; n = n.next) {
            if (n.hash == h && Objects.equals(n.key, key)) {
                return n;
            }
        }
        return null;
    }

    private static int hash(Object key) {
        if (key == null) return 0;
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private static int indexFor(int hash, int capacity) {
        return hash & (capacity - 1);
    }

    /** Returns the smallest power of 2 that is >= {@code n}. */
    private static int roundUpToPowerOfTwo(int n) {
        if (n <= 1) return 1;
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Node<K, V>[] newTable = new Node[newCapacity];
        for (Node<K, V> head : table) {
            for (Node<K, V> n = head; n != null; ) {
                Node<K, V> next = n.next;
                int idx = indexFor(n.hash, newCapacity);
                n.next = newTable[idx];
                newTable[idx] = n;
                n = next;
            }
        }
        table = newTable;
    }

    // ------------------------------------------------------------------
    // EntrySet view
    // ------------------------------------------------------------------

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Node<K, V> node = getNode(e.getKey());
            return node != null && Objects.equals(node.value, e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Node<K, V> node = getNode(e.getKey());
            if (node != null && Objects.equals(node.value, e.getValue())) {
                SimpleMap.this.remove(e.getKey());
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            SimpleMap.this.clear();
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }
    }

    // ------------------------------------------------------------------
    // Iterator over all entries
    // ------------------------------------------------------------------

    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {

        private int bucketIndex = 0;
        private Node<K, V> next;
        private Node<K, V> current;
        private int expectedModCount = modCount;

        EntryIterator() {
            advance();
        }

        private void advance() {
            while (next == null && bucketIndex < table.length) {
                next = table[bucketIndex++];
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (modCount != expectedModCount) throw new java.util.ConcurrentModificationException();
            if (next == null) throw new NoSuchElementException();
            current = next;
            next = current.next;
            if (next == null) advance();
            return current;
        }

        @Override
        public void remove() {
            if (current == null) throw new IllegalStateException();
            if (modCount != expectedModCount) throw new java.util.ConcurrentModificationException();
            SimpleMap.this.remove(current.key);
            expectedModCount = modCount;
            current = null;
        }
    }
}
