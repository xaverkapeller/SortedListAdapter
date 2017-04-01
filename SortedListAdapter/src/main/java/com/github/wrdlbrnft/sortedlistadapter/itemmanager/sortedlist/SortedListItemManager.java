package com.github.wrdlbrnft.sortedlistadapter.itemmanager.sortedlist;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.github.wrdlbrnft.sortedlistadapter.itemmanager.ItemManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 01/04/2017
 */
public class SortedListItemManager<T extends SortedListAdapter.ViewModel> implements ItemManager<T> {

    private interface Action<T extends SortedListAdapter.ViewModel> {
        void perform(SortedList<T> list);
    }

    interface Facade<T> {
        T getItem(int position);
        int size();
        void addState(List<T> data);
        void moveToNextState();
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final ChangeCache mChangeCache = new ChangeCache();
    private final BlockingDeque<List<Action<T>>> mCommitQueue = new LinkedBlockingDeque<>();
    private final AtomicBoolean mCommitInProgress = new AtomicBoolean(false);

    private final Class<T> mItemClass;
    private final Comparator<T> mComparator;
    private final SortedList<T> mSortedList;
    private final TransactionCallback mCallback;

    public SortedListItemManager(Class<T> itemClass, Comparator<T> comparator, TransactionCallback callback) {
        mItemClass = itemClass;
        mComparator = comparator;
        mCallback = callback;
        mSortedList = new SortedList<>(mItemClass, mChangeCache);
    }

    @Override
    public T getItem(int position) {
        return mChangeCache.getItem(position);
    }

    @Override
    public int getItemCount() {
        return mChangeCache.getItemCount();
    }

    @Override
    public Transaction<T> newTransaction() {
        return new TransactionImpl();
    }

    private class TransactionImpl implements Transaction<T> {

        private final List<Action<T>> mActions = new ArrayList<>();

        @Override
        public Transaction<T> add(@NonNull T item) {
            mActions.add(list -> mSortedList.add(item));
            return this;
        }

        @Override
        public Transaction<T> add(@NonNull Collection<T> items) {
            mActions.add(list -> mSortedList.addAll(items));
            return this;
        }

        @Override
        public Transaction<T> remove(@NonNull T item) {
            mActions.add(list -> mSortedList.remove(item));
            return this;
        }

        @Override
        public Transaction<T> remove(@NonNull Collection<T> items) {
            mActions.add(list -> {
                @SuppressWarnings("unchecked")
                final T[] array = items.toArray((T[]) Array.newInstance(mItemClass, items.size()));
                Arrays.sort(array, mComparator);
                for (T item : array) {
                    mSortedList.remove(item);
                }
            });
            return this;
        }

        @Override
        public Transaction<T> replaceAll(@NonNull Collection<T> items) {
            mActions.add(list -> {
                @SuppressWarnings("unchecked")
                final T[] array = items.toArray((T[]) Array.newInstance(mItemClass, items.size()));
                Arrays.sort(array, mComparator);
                for (int i = mSortedList.size() - 1; i >= 0; i--) {
                    final T currentItem = mSortedList.get(i);
                    final int index = Arrays.binarySearch(array, currentItem, mComparator);
                    if (index < 0) {
                        mSortedList.remove(currentItem);
                    }
                }
                mSortedList.addAll(array, true);
            });
            return this;
        }

        @Override
        public Transaction<T> removeAll() {
            mActions.add(list -> mSortedList.clear());
            return this;
        }

        @Override
        public void commit() {
            final List<Action<T>> actions = new ArrayList<>(mActions);
            mActions.clear();
            MAIN_HANDLER.post(() -> initializeCommit(actions));
        }

        private void initializeCommit(List<Action<T>> actions) {
            mCommitQueue.add(actions);
            if (!mCommitInProgress.getAndSet(true)) {
                startTransaction();
            }
        }

        private void startTransaction() {
            final Thread updateThread = new Thread(this::performTransactions);
            updateThread.start();
            notifyTransactionsStarted();
        }

        private void notifyTransactionsStarted() {
            mCallback.onTransactionsInProgress();
        }

        private void performTransactions() {
            try {
                while (!mCommitQueue.isEmpty()) {
                    final List<Action<T>> actions = mCommitQueue.pollFirst();
                    if (actions == null) {
                        return;
                    }
                    mSortedList.beginBatchedUpdates();
                    for (Action<T> action : actions) {
                        action.perform(mSortedList);
                    }
                    mSortedList.endBatchedUpdates();
                    mChangeCache.applyChanges();
                }
            } finally {
                mCommitInProgress.set(false);
                MAIN_HANDLER.post(this::notifyTransactionsFinished);
            }
        }

        private void notifyTransactionsFinished() {
            mCallback.onTransactionsFinished();
        }
    }

    private interface Change {
        void apply(ChangeConsumer consumer);
    }

    private class ChangeCache extends SortedList.Callback<T> {

        private final List<Change> mCurrentChanges = new ArrayList<>();
        private final Facade<T> mFacade = new FacadeImpl<>();

        void applyChanges() {
            final List<Change> changes = new ArrayList<>(mCurrentChanges);
            mCurrentChanges.clear();

            final List<T> currentState = captureState();
            mFacade.addState(currentState);

            MAIN_HANDLER.post(() -> mCallback.onChangeSetAvailable((moveCallback, addCallback, removeCallback, changeCallback) -> {
                final ChangeConsumer consumer = new ChangeConsumerImpl(moveCallback, addCallback, removeCallback, changeCallback);
                mFacade.moveToNextState();
                for (Change change : changes) {
                    change.apply(consumer);
                }
            }));
        }

        public T getItem(int position) {
            return mFacade.getItem(position);
        }

        public int getItemCount() {
            return mFacade.size();
        }

        @NonNull
        private List<T> captureState() {
            final List<T> currentState = new ArrayList<>();
            for (int i = 0, count = mSortedList.size(); i < count; i++) {
                currentState.add(mSortedList.get(i));
            }
            return currentState;
        }

        @Override
        public int compare(T a, T b) {
            return mComparator.compare(a, b);
        }

        @Override
        public boolean areContentsTheSame(T oldItem, T newItem) {
            return oldItem.isContentTheSameAs(newItem);
        }

        @Override
        public boolean areItemsTheSame(T item1, T item2) {
            return item1.isSameModelAs(item2);
        }

        @Override
        public void onInserted(int position, int count) {
            mCurrentChanges.add(consumer -> consumer.add(position, count));
        }

        @Override
        public void onRemoved(int position, int count) {
            mCurrentChanges.add(consumer -> consumer.remove(position, count));
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mCurrentChanges.add(consumer -> consumer.move(fromPosition, toPosition));
        }

        @Override
        public void onChanged(int position, int count) {
            mCurrentChanges.add(consumer -> consumer.change(position, count));
        }
    }
}
