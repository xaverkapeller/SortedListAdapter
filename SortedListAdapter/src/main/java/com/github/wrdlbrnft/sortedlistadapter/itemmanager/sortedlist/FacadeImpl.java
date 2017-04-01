package com.github.wrdlbrnft.sortedlistadapter.itemmanager.sortedlist;

import java.util.ArrayDeque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 27/03/2017
 */
class FacadeImpl<T> implements SortedListItemManager.Facade<T> {

    private Queue<List<T>> mBacklog = new ArrayDeque<>();
    private List<T> mCurrentState = null;

    @Override
    public synchronized T getItem(int position) {
        if (mCurrentState != null) {
            return mCurrentState.get(position);
        }
        throw new NoSuchElementException();
    }

    @Override
    public synchronized int size() {
        if (mCurrentState != null) {
            return mCurrentState.size();
        }
        return 0;
    }

    @Override
    public synchronized void addState(List<T> data) {
        mBacklog.add(data);
    }

    @Override
    public synchronized void moveToNextState() {
        mCurrentState = mBacklog.poll();
    }
}
