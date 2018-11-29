package kvstore;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class KVCache   {
	
	private int numSets = 100;
	private int numElem = 10;
	
	class Entry {
		private String key;
		private String value;
		private boolean refer;
		public Entry(String key,String value,boolean refer){
			this.key=key;this.value=value;this.refer=refer;
		}
		public String getKey(){return key;}
		public String getValue(){return value;}
		public boolean getRefer(){return refer;}
		public void setValue(String value){this.value=value;}
		public void setRefer(boolean refer){this.refer=refer;}
	}
	
	Lock[] locks;
	LinkedList<Entry>[] sets;
	
    /**
     * Constructs a second-chance-replacement cache.
     *
     * @param numSets the number of sets this cache will have
     * @param maxElemsPerSet the size of each set
     */
   
    public KVCache(int numSets, int maxElemsPerSet) {
    	this.numSets = numSets;
    	this.numElem = maxElemsPerSet;
    	locks = new ReentrantLock[numSets];
    	sets = new LinkedList[numSets];
    	for(int i=0;i<numSets;++i) {
    		locks[i] = new ReentrantLock();
    		sets[i] = new LinkedList<Entry>();
    	}
    }

    /**
     * Retrieves an entry from the cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method.
     *
     * @param  key the key whose associated value is to be returned.
     * @return the value associated to this key or null if no value is
     *         associated with this key in the cache
     */
   
    public String get(String key) {
        int k = getSetId(key);
        for(Entry e : sets[k]) {
        	if(e.getKey().equals(key)) {
        		e.setRefer(true);
        		return e.getValue();
        	}
        }
        return null;
    }

    /**
     * Adds an entry to this cache.
     * If an entry with the specified key already exists in the cache, it is
     * replaced by the new entry. When an entry is replaced, its reference bit
     * will be set to True. If the set is full, an entry is removed from
     * the cache based on the eviction policy. If the set is not full, the entry
     * will be inserted behind all existing entries. For this policy, we suggest
     * using a LinkedList over an array to keep track of entries in a set since
     * deleting an entry in an array will leave a gap in the array, likely not
     * at the end. More details and explanations in the spec. Assumes access to
     * the corresponding set has already been locked by the caller of this
     * method.
     *
     * @param key the key with which the specified value is to be associated
     * @param value a value to be associated with the specified key
     */
    
    public void put(String key, String value) {
        int k = getSetId(key);
        // check if key exists
        for(Entry e: sets[k]) {
        	if(e.getKey().equals(key)) {
        		e.setValue(value);
        		e.setRefer(true);
        		return ;
        	}
        }
        // does not exist
        if(sets[k].size() == numElem) { // remove one element
        	while(sets[k].getFirst().getRefer()) {
        		Entry t = sets[k].remove();
        		t.setRefer(false);
        		sets[k].add(t);
        	}
        	sets[k].remove(); // remove the first element marked
        }
        // add a new entry
        sets[k].add(new Entry(key, value, false));
    }

    /**
     * Removes an entry from this cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method. Does nothing if called on a key not in the cache.
     *
     * @param key key with which the specified value is to be associated
     */
    
    public void del(String key) {
    	int k = getSetId(key);
        for(Entry e : sets[k]) {
        	if(e.getKey().equals(key)) {
        		sets[k].remove(e);
        		return ;
        	}
        }
    }

    /**
     * Get a lock for the set corresponding to a given key.
     * The lock should be used by the caller of the get/put/del methods
     * so that different sets can be modified in parallel.
     *
     * @param  key key to determine the lock to return
     * @return lock for the set that contains the key
     */
    public Lock getLock(String key) {
        return locks[getSetId(key)];
    }

    /**
     * Get the id of the set for a particular key.
     *
     * @param  key key of interest
     * @return set of the key
     */
    private int getSetId(String key) {
        return Math.abs(key.hashCode()) % numSets;
    }

    /**
     * Serialize this store to XML. See spec for details on output format.
     * This method is best effort. Any exceptions that arise can be dropped.
     */
   

}