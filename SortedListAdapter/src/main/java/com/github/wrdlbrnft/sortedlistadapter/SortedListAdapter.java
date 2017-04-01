package com.github.wrdlbrnft.sortedlistadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.wrdlbrnft.proguardannotations.KeepClass;
import com.github.wrdlbrnft.proguardannotations.KeepClassMembers;
import com.github.wrdlbrnft.proguardannotations.KeepMember;
import com.github.wrdlbrnft.proguardannotations.KeepSetting;
import com.github.wrdlbrnft.sortedlistadapter.itemmanager.ChangeSet;
import com.github.wrdlbrnft.sortedlistadapter.itemmanager.ItemManager;
import com.github.wrdlbrnft.sortedlistadapter.itemmanager.sortedlist.SortedListItemManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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
        Editor<T> add(@NonNull T item);
        Editor<T> add(@NonNull Collection<T> items);
        Editor<T> remove(@NonNull T item);
        Editor<T> remove(@NonNull Collection<T> items);
        Editor<T> replaceAll(@NonNull Collection<T> items);
        Editor<T> removeAll();
        void commit();
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public abstract static class ViewHolder<T extends ViewModel> extends RecyclerView.ViewHolder {

        private T mCurrentItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public final void bind(T item) {
            mCurrentItem = item;
            performBind(item);
        }

        @KeepMember
        protected abstract void performBind(@NonNull T item);

        public final T getCurrentItem() {
            return mCurrentItem;
        }
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public interface ViewModel {
        <T> boolean isSameModelAs(@NonNull T model);
        <T> boolean isContentTheSameAs(@NonNull T model);
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public static class ComparatorBuilder<T extends ViewModel> {

        private final List<ComparatorRule> mComparatorRules = new ArrayList<>();

        @SafeVarargs
        public final ComparatorBuilder<T> setGeneralOrder(@NonNull Class<? extends T>... modelClasses) {
            if (modelClasses.length > 1) {
                mComparatorRules.add(new GeneralOrderRuleImpl(modelClasses));
            }
            return this;
        }

        public final <M extends T> ComparatorBuilder<T> setOrderForModel(@NonNull Class<M> modelClass, @NonNull Comparator<M> comparator) {
            mComparatorRules.add(new ModelOrderRuleImpl<>(modelClass, comparator));
            return this;
        }

        public final Comparator<T> build() {
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
        VH create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);
    }

    @KeepClass
    @KeepClassMembers(KeepSetting.PUBLIC_MEMBERS)
    public static class Builder<T extends ViewModel> {

        private final List<ModularSortedListAdapterImpl.Module<?, ?>> mModules = new ArrayList<>();

        private final Context mContext;
        private final Class<T> mItemClass;
        private final Comparator<T> mComparator;

        public Builder(@NonNull Context context, @NonNull Class<T> itemClass, @NonNull Comparator<T> comparator) {
            mContext = context;
            mItemClass = itemClass;
            mComparator = comparator;
        }

        public <M extends T, VH extends ViewHolder<M>> Builder<T> add(@NonNull Class<M> modelClass, @NonNull ViewHolderFactory<VH> holderFactory) {
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

    interface ComparatorRule {
        boolean isApplicable(ViewModel a, ViewModel b);
        int apply(ViewModel a, ViewModel b);
    }

    private final ItemManager.TransactionCallback mTransactionCallback = new ItemManager.TransactionCallback() {
        @Override
        public void onTransactionsInProgress() {
            for (Callback callback : mCallbacks) {
                callback.onEditStarted();
            }
        }

        @Override
        public void onTransactionsFinished() {
            for (Callback callback : mCallbacks) {
                callback.onEditFinished();
            }
        }

        @Override
        public void onChangeSetAvailable(ChangeSet changeSet) {
            changeSet.applyTo(
                    SortedListAdapter.this::notifyItemMoved,
                    SortedListAdapter.this::notifyItemRangeInserted,
                    SortedListAdapter.this::notifyItemRangeRemoved,
                    SortedListAdapter.this::notifyItemRangeChanged
            );
        }
    };

    private final List<Callback> mCallbacks = new ArrayList<>();
    private final ItemManager<T> mItemManager;
    private final LayoutInflater mInflater;

    public SortedListAdapter(@NonNull Context context, @NonNull Class<T> itemClass, @NonNull Comparator<T> comparator) {
        mInflater = LayoutInflater.from(context);
        mItemManager = new SortedListItemManager<>(itemClass, comparator, mTransactionCallback);
    }

    public void addCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    @Override
    public final ViewHolder<? extends T> onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateViewHolder(mInflater, parent, viewType);
    }

    @KeepMember
    @NonNull
    protected abstract ViewHolder<? extends T> onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int viewType);

    @Override
    public final void onBindViewHolder(ViewHolder<? extends T> holder, int position) {
        final T item = getItem(position);
        ((ViewHolder<T>) holder).bind(item);
    }

    @NonNull
    public final Editor<T> edit() {
        final ItemManager.Transaction<T> transaction = mItemManager.newTransaction();
        return new EditorImpl<>(transaction);
    }

    @Override
    public final int getItemCount() {
        return mItemManager.getItemCount();
    }

    @NonNull
    public T getItem(int position) {
        return mItemManager.getItem(position);
    }
}