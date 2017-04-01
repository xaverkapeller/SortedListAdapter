package com.github.wrdlbrnft.sortedlistadapter.itemmanager.sortedlist;

import com.github.wrdlbrnft.sortedlistadapter.itemmanager.ChangeSet;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 01/04/2017
 */
class ChangeConsumerImpl implements ChangeConsumer {
    private final ChangeSet.MoveCallback mMoveCallback;
    private final ChangeSet.AddCallback mAddCallback;
    private final ChangeSet.RemoveCallback mRemoveCallback;
    private final ChangeSet.ChangeCallback mChangeCallback;

    ChangeConsumerImpl(ChangeSet.MoveCallback moveCallback, ChangeSet.AddCallback addCallback, ChangeSet.RemoveCallback removeCallback, ChangeSet.ChangeCallback changeCallback) {
        mMoveCallback = moveCallback;
        mAddCallback = addCallback;
        mRemoveCallback = removeCallback;
        mChangeCallback = changeCallback;
    }

    @Override
    public void move(int fromPosition, int toPosition) {
        mMoveCallback.move(fromPosition, toPosition);
    }

    @Override
    public void add(int index, int count) {
        mAddCallback.add(index, count);
    }

    @Override
    public void remove(int index, int count) {
        mRemoveCallback.remove(index, count);
    }

    @Override
    public void change(int index, int count) {
        mChangeCallback.change(index, count);
    }
}
