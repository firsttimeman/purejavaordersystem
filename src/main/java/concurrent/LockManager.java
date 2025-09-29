package concurrent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public List<ReentrantLock> lockAllByIds(Collection<Long> productIds) {

        List<Long> sorted = new ArrayList<>(productIds);
        Collections.sort(sorted);

        List<ReentrantLock> tmp = new ArrayList<>(sorted.size());
        for (Long id : sorted) {
            ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
            lock.lock();
            tmp.add(lock);
        }
        return tmp;
    }

    public void unlockAllByIds(List<ReentrantLock> requested) {
        ListIterator<ReentrantLock> it = requested.listIterator(requested.size());
        while(it.hasPrevious()) {
            it.previous().unlock();
        }
    }
}
