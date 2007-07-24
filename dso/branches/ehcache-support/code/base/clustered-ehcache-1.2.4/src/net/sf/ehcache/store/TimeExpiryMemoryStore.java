package net.sf.ehcache.store;

import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcclient.ehcache.TimeExpiryMap;

public class TimeExpiryMemoryStore extends MemoryStore {
	private static final Log LOG = LogFactory
			.getLog(TimeExpiryMemoryStore.class.getName());

	public TimeExpiryMemoryStore(Ehcache cache, DiskStore diskStore) {
		super(cache, diskStore);

		try {
			map = loadMapInstance();
		} catch (CacheException e) {
			LOG
					.error(
							cache.getName()
									+ "Cache: Cannot start TimeExpiryMemoryStore. Initial cause was "
									+ e.getMessage(), e);
		}
	}

	private Map loadMapInstance() throws CacheException {
		try {
			Class.forName("com.tcclient.ehcache.TimeExpiryMap");
			Map candidateMap = new SpoolingTimeExpiryMap(1 /* 1 sec */, cache
					.getTimeToLiveSeconds());
			if (LOG.isDebugEnabled()) {
				LOG.debug(cache.getName()
						+ " Cache: Using SpoolingTimeExpiryMap implementation");
			}
			return candidateMap;
		} catch (Exception e) {
			// Give up
			throw new CacheException(cache.getName()
					+ "Cache: Cannot find com.tcclient.ehcache.TimeExpiryMap.");
		}
	}

	public final void evictExpiredElements() {
		((SpoolingTimeExpiryMap) map).evictExpiredElements();
	}

	public final int getHitCount() {
		return ((SpoolingTimeExpiryMap) map).getHitCount();
	}

	public final int getMissCountExpired() {
		return ((SpoolingTimeExpiryMap) map).getMissCountExpired();
	}

	public final int getMissCountNotFound() {
		return ((SpoolingTimeExpiryMap) map).getMissCountNotFound();
	}
	
	public final boolean isExpired(final Object key) {
		return ((SpoolingTimeExpiryMap) map).isExpired(key);
	}

	public final class SpoolingTimeExpiryMap extends TimeExpiryMap {

		public SpoolingTimeExpiryMap(long timeToIdleSec, long timeToLiveSec) {
			super(timeToIdleSec, timeToLiveSec);
		}

		protected final void processExpired(Object key, Object value) {
			// Already removed from the map at this point
			Element element = (Element) value;

			// When max size is 0
			if (element == null) {
				return;
			}

			// check for expiry before going to the trouble of spooling
			if (element.isExpired()) {
				notifyExpiry(element);
			} else {
				evict(element);
			}
		}

		public final void evictExpiredElements() {
			timeExpiryDataStore.evictExpiredElements();
		}
	}

}
