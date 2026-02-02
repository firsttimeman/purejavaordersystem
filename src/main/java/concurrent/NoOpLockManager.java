package concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class NoOpLockManager implements ProductLockManager{
    @Override
    public List<Lock> lockAllByIds(Collection<Long> ids) {
        return List.of();
    }

    @Override
    public void unlockAllByIds(List<Lock> locks) {

    }
}
