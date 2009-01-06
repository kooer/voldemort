package voldemort.client;

import voldemort.serialization.Serializer;
import voldemort.store.Store;
import voldemort.store.memory.InMemoryStorageEngine;
import voldemort.store.serialized.SerializingStore;
import voldemort.store.versioned.InconsistencyResolvingStore;
import voldemort.store.versioned.VersionIncrementingStore;
import voldemort.utils.SystemTime;
import voldemort.utils.Time;
import voldemort.versioning.ChainedResolver;
import voldemort.versioning.InconsistencyResolver;
import voldemort.versioning.TimeBasedInconsistencyResolver;
import voldemort.versioning.VectorClockInconsistencyResolver;
import voldemort.versioning.Versioned;

/**
 * A store client that produces fake, in-memory stores
 * 
 * @author jay
 * 
 */
@SuppressWarnings("unchecked")
public class MockStoreClientFactory<K, V> implements StoreClientFactory {

    private final int nodeId;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;
    private final Time time;

    public MockStoreClientFactory(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this(keySerializer, valueSerializer, 0, SystemTime.INSTANCE);
    }

    public MockStoreClientFactory(Serializer<K> keySerializer,
                                  Serializer<V> valueSerializer,
                                  int nodeId,
                                  Time time) {
        this.nodeId = nodeId;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.time = time;
    }

    public <K, V> StoreClient<K, V> getStoreClient(String storeName) {
        return getStoreClient(storeName, new TimeBasedInconsistencyResolver<V>());
    }

    public <K, V> StoreClient<K, V> getStoreClient(String storeName,
                                                   InconsistencyResolver<Versioned<V>> inconsistencyResolver) {
        // Add inconsistency resolving decorator, using their inconsistency
        // resolver (if they gave us one)
        InconsistencyResolver<Versioned<V>> secondaryResolver = new TimeBasedInconsistencyResolver();
        if(inconsistencyResolver != null)
            secondaryResolver = inconsistencyResolver;

        Store store = new VersionIncrementingStore(new InMemoryStorageEngine(storeName),
                                                   nodeId,
                                                   time);
        if(isSerialized())
            store = new SerializingStore(store, keySerializer, valueSerializer);
        Store<K, V> consistentStore = new InconsistencyResolvingStore<K, V>(store,
                                                                            new ChainedResolver<Versioned<V>>(new VectorClockInconsistencyResolver(),
                                                                                                              secondaryResolver));
        return new DefaultStoreClient(consistentStore, keySerializer, valueSerializer, null);
    }

    private boolean isSerialized() {
        return keySerializer != null && valueSerializer != null;
    }

}