package concurrent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SortedReentrantLockManager implements ProductLockManager{


    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public List<Lock> lockAllByIds(Collection<Long> productIds) {
        List<Long> sorted = new ArrayList<>(productIds);
        Collections.sort(sorted);

        List<Lock> acquired = new ArrayList<>(sorted.size());
        for (Long id : sorted) {
            ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
            lock.lock();
            acquired.add(lock);
        }
        return acquired;
    }

    @Override
    public void unlockAllByIds(List<Lock> acquired) {
        ListIterator<Lock> it = acquired.listIterator(acquired.size());
        while (it.hasPrevious()) it.previous().unlock();
    }
}
