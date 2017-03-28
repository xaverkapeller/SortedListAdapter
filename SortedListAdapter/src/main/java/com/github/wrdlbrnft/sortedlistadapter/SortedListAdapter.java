package com.github.wrdlbrnft.sortedlistadapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.wrdlbrnft.proguardannotations.KeepClass;
import com.github.wrdlbrnft.proguardannotations.KeepClassMembers;
import com.github.wrdlbrnft.proguardannotations.KeepMember;
import com.github.wrdlbrnft.proguardannotations.KeepSetting;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with Android Studio
 * User: Xaver
 * Date: 13/08/16
 */
@KeepClass
@KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
public abstract class SortedListAdapter<T extends SortedListAdapter.ViewModel> extends RecyclerView.Adapter<SortedListAdapter.ViewHolder<? extends T>> {

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface Callback {
        void onEditStarted();
        void onEditFinished();
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface Editor<T extends ViewModel> {
        Editor<T> add(T item);
        Editor<T> add(List<T> items);
        Editor<T> remove(T item);
        Editor<T> remove(List<T> items);
        Editor<T> replaceAll(List<T> items);
        Editor<T> removeAll();
        void commit();
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface Filter<T> {
        boolean test(T item);
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public abstract static class ViewHolder<T extends ViewModel> extends RecyclerView.ViewHolder {

        private T mCurrentItem;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public final void bind(T item) {
            mCurrentItem = item;
            performBind(item);
        }

        @KeepMember
        protected abstract void performBind(T item);

        public final T getCurrentItem() {
            return mCurrentItem;
        }
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface ViewModel {
        <T> boolean isSameModelAs(T model);
        <T> boolean isContentTheSameAs(T model);
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public static class ComparatorBuilder<T extends ViewModel> {

        private final List<ComparatorRule> mComparatorRules = new ArrayList<>();

        public ComparatorBuilder<T> setGeneralOrder(Class<? extends T>... modelClasses) {
            if (modelClasses.length > 1) {
                mComparatorRules.add(new GeneralOrderRuleImpl(modelClasses));
            }
            return this;
        }

        public <M extends T> ComparatorBuilder<T> setOrderForModel(Class<M> modelClass, Comparator<M> comparator) {
            mComparatorRules.add(new ModelOrderRuleImpl<>(modelClass, comparator));
            return this;
        }

        public Comparator<T> build() {
            return (a, b) -> {
                for (ComparatorRule comparatorRule : mComparatorRules) {
                    if (comparatorRule.isApplicable(a, b)) {
                        return comparatorRule.apply(a, b);
                    }
                }
                return 0;
            };
        }
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface ViewHolderFactory<VH extends ViewHolder<?>> {
        VH create(LayoutInflater inflater, ViewGroup parent, boolean attachToRoot);
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public static class Builder<T extends ViewModel> {

        private final List<ModularSortedListAdapterImpl.Module<?, ?>> mModules = new ArrayList<>();

        private final Context mContext;
        private final Class<T> mItemClass;
        private final Comparator<T> mComparator;

        public Builder(Context context, Class<T> itemClass, Comparator<T> comparator) {
            mContext = context;
            mItemClass = itemClass;
            mComparator = comparator;
        }

        public <M extends T, VH extends ViewHolder<M>> Builder<T> add(Class<M> modelClass, ViewHolderFactory<VH> holderFactory) {
            mModules.add(new ModularSortedListAdapterImpl.Module<M, VH>(
                    mModules.size(),
                    modelClass,
                    holderFactory
            ));
            return this;
        }

        public SortedListAdapter<T> build() {
            return new ModularSortedListAdapterImpl<>(mContext, mItemClass, mComparator, mModules);
        }
    }

    interface Facade<T> {
        T getItem(int position);
        int size();
        void addState(List<T> data);
        void moveToNextState();
    }

    private interface Change {
        void apply();
    }

    interface ComparatorRule {
        boolean isApplicable(ViewModel a, ViewModel b);
        int apply(ViewModel a, ViewModel b);
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final ChangeCache mChangeCache = new ChangeCache();
    private final BlockingDeque<List<Action<T>>> mCommitQueue = new LinkedBlockingDeque<>();
    private final AtomicBoolean mCommitInProgress = new AtomicBoolean(false);
    private final Facade<T> mFacade = new FacadeImpl<>();
    private final List<Callback> mCallbacks = new ArrayList<>();

    private final LayoutInflater mInflater;
    private final SortedList<T> mSortedList;
    private final Class<T> mItemClass;
    private final Comparator<T> mComparator;

    public SortedListAdapter(Context context, Class<T> itemClass, Comparator<T> comparator) {
        mInflater = LayoutInflater.from(context);
        mItemClass = itemClass;
        mComparator = comparator;
        mSortedList = new SortedList<>(itemClass, mChangeCache);
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    @Override
    public final ViewHolder<? extends T> onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateViewHolder(mInflater, parent, viewType);
    }

    @KeepMember
    protected abstract ViewHolder<? extends T> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

    @Override
    public final void onBindViewHolder(ViewHolder<? extends T> holder, int position) {
        final T item = getItem(position);
        ((ViewHolder<T>) holder).bind(item);
    }

    public final Editor<T> edit() {
        return new EditorImpl();
    }

    @Override
    public final int getItemCount() {
        return mFacade.size();
    }

    public T getItem(int position) {
        return mFacade.getItem(position);
    }

    private interface Action<T extends ViewModel> {
        void perform(SortedList<T> list);
    }

    private class EditorImpl implements Editor<T> {

        private final List<Action<T>> mActions = new ArrayList<>();

        @Override
        public Editor<T> add(final T item) {
            mActions.add(list -> mSortedList.add(item));
            return this;
        }

        @Override
        public Editor<T> add(final List<T> items) {
            mActions.add(list -> {
                Collections.sort(items, mComparator);
                mSortedList.addAll(items);
            });
            return this;
        }

        @Override
        public Editor<T> remove(final T item) {
            mActions.add(list -> mSortedList.remove(item));
            return this;
        }

        @Override
        public Editor<T> remove(final List<T> items) {
            mActions.add(list -> {
                for (T item : items) {
                    mSortedList.remove(item);
                }
            });
            return this;
        }

        @Override
        public Editor<T> replaceAll(final List<T> items) {
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
                mSortedList.addAll(items);
            });
            return this;
        }

        @Override
        public Editor<T> removeAll() {
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
                startEdit();
            }
        }

        private void startEdit() {
            final Thread updateThread = new Thread(this::performEdit);
            updateThread.start();
            notifyEditStarted();
        }

        private void notifyEditStarted() {
            for (Callback callback : mCallbacks) {
                callback.onEditStarted();
            }
        }

        private void performEdit() {
            try {
                while (!mCommitQueue.isEmpty()) {
                    final List<Action<T>> actions = mCommitQueue.pollFirst();
                    if (actions == null) {
                        return;
                    }
                    mChangeCache.startCaching();
                    mSortedList.beginBatchedUpdates();
                    for (Action<T> action : actions) {
                        action.perform(mSortedList);
                    }
                    mSortedList.endBatchedUpdates();
                    final List<T> currentState = new ArrayList<>();
                    for (int i = 0, count = mSortedList.size(); i < count; i++) {
                        currentState.add(mSortedList.get(i));
                    }
                    mFacade.addState(currentState);
                    MAIN_HANDLER.post(this::applyCommit);
                }
            } finally {
                mCommitInProgress.set(false);
                MAIN_HANDLER.post(this::notifyEditFinished);
            }
        }

        private void applyCommit() {
            mFacade.moveToNextState();
            mChangeCache.flushChanges();
        }

        private void notifyEditFinished() {
            for (Callback callback : mCallbacks) {
                callback.onEditFinished();
            }
        }
    }

    private class ChangeCache extends SortedList.Callback<T> {

        private final List<List<Change>> mChangeQueue = new ArrayList<>();

        private List<Change> mCurrentChanges;

        void startCaching() {
            synchronized (mChangeQueue) {
                mCurrentChanges = new ArrayList<>();
                mChangeQueue.add(mCurrentChanges);
            }
        }

        void flushChanges() {
            synchronized (mChangeQueue) {
                if (mChangeQueue.isEmpty()) {
                    return;
                }
                final List<Change> changes = mChangeQueue.remove(0);
                for (Change change : changes) {
                    change.apply();
                }
            }
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
            synchronized (mChangeQueue) {
                mCurrentChanges.add(() -> notifyItemRangeInserted(position, count));
            }
        }

        @Override
        public void onRemoved(int position, int count) {
            synchronized (mChangeQueue) {
                mCurrentChanges.add(() -> notifyItemRangeRemoved(position, count));
            }
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            synchronized (mChangeQueue) {
                mCurrentChanges.add(() -> notifyItemMoved(fromPosition, toPosition));
            }
        }

        @Override
        public void onChanged(int position, int count) {
            synchronized (mChangeQueue) {
                mCurrentChanges.add(() -> notifyItemRangeChanged(position, count));
            }
        }
    }
}