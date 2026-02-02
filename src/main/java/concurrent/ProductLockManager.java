package concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

public interface ProductLockManager {
    List<Lock> lockAllByIds(Collection<Long> ids);
    void unlockAllByIds(List<Lock> locks);
}
