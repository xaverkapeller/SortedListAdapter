package com.github.wrdlbrnft.sortedlistadapter.itemmanager;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 01/04/2017
 */
public interface ChangeSet {

    interface MoveCallback {
        void move(int fromPosition, int toPosition);
    }

    interface AddCallback {
        void add(int index, int count);
    }

    interface RemoveCallback {
        void remove(int index, int count);
    }

    interface ChangeCallback {
        void change(int index, int count);
    }

    void applyTo(
            MoveCallback moveCallback,
            AddCallback addCallback,
            RemoveCallback removeCallback,
            ChangeCallback changeCallback
    );
}
