package voldemort.store.serialized;

import java.util.ArrayList;
import java.util.List;

import voldemort.VoldemortException;
import voldemort.serialization.Serializer;
import voldemort.store.Store;
import voldemort.versioning.VectorClock;
import voldemort.versioning.Version;
import voldemort.versioning.Versioned;

import com.google.common.base.Objects;

/**
 * A store that transforms requests to a Store<byte[],byte[]> to a Store<K,V>
 * 
 * @author jay
 * 
 * @param <K> The type of the key being stored
 * @param <V> The type of the value being stored
 */
public class SerializingStore<K, V> implements Store<K, V> {

    private final Store<byte[], byte[]> store;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

    public SerializingStore(Store<byte[], byte[]> store,
                            Serializer<K> keySerializer,
                            Serializer<V> valueSerializer) {
        this.store = Objects.nonNull(store);
        this.keySerializer = Objects.nonNull(keySerializer);
        this.valueSerializer = Objects.nonNull(valueSerializer);
    }

    public boolean delete(K key, Version version) throws VoldemortException {
        return store.delete(keySerializer.toBytes(key), version);
    }

    public List<Versioned<V>> get(K key) throws VoldemortException {
        List<Versioned<byte[]>> found = store.get(keySerializer.toBytes(key));
        List<Versioned<V>> results = new ArrayList<Versioned<V>>(found.size());
        for (Versioned<byte[]> versioned : found)
            results.add(new Versioned<V>(valueSerializer.toObject(versioned.getValue()),
                                         (VectorClock) versioned.getVersion()));
        return results;
    }

    public String getName() {
        return store.getName();
    }

    public void put(K key, Versioned<V> value) throws VoldemortException {
        store.put(keySerializer.toBytes(key),
                  new Versioned<byte[]>(valueSerializer.toBytes(value.getValue()),
                                        (VectorClock) value.getVersion()));
    }

    public void close() {
        store.close();
    }

    protected Serializer<V> getValueSerializer() {
        return valueSerializer;
    }

    protected Serializer<K> getKeySerializer() {
        return keySerializer;
    }

}