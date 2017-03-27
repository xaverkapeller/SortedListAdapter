package com.github.wrdlbrnft.sortedlistadapter;

import java.util.Comparator;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 27/03/2017
 */
class ModelOrderRuleImpl<M extends SortedListAdapter.ViewModel> implements SortedListAdapter.ComparatorRule {

    private final Class<M> mModelClass;
    private final Comparator<M> mComparator;

    ModelOrderRuleImpl(Class<M> modelClass, Comparator<M> comparator) {
        mModelClass = modelClass;
        mComparator = comparator;
    }

    @Override
    public boolean isApplicable(SortedListAdapter.ViewModel a, SortedListAdapter.ViewModel b) {
        final Class<? extends SortedListAdapter.ViewModel> clazzA = a.getClass();
        final Class<? extends SortedListAdapter.ViewModel> clazzB = b.getClass();
        return mModelClass.isAssignableFrom(clazzA)
                && mModelClass.isAssignableFrom(clazzB);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int apply(SortedListAdapter.ViewModel a, SortedListAdapter.ViewModel b) {
        return mComparator.compare((M) a, (M) b);
    }
}
