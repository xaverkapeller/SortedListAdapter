package com.github.wrdlbrnft.sortedlistadapter;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 27/03/2017
 */
class GeneralOrderRuleImpl implements SortedListAdapter.ComparatorRule {

    private final Class<? extends SortedListAdapter.ViewModel>[] mModelClasses;

    GeneralOrderRuleImpl(Class<? extends SortedListAdapter.ViewModel>[] modelClasses) {
        mModelClasses = modelClasses;
    }

    @Override
    public boolean isApplicable(SortedListAdapter.ViewModel a, SortedListAdapter.ViewModel b) {
        final Class<? extends SortedListAdapter.ViewModel> clazzA = a.getClass();
        final Class<? extends SortedListAdapter.ViewModel> clazzB = b.getClass();
        return !clazzA.equals(clazzB)
                && RuleUtils.isClassAssignableToOneOf(mModelClasses, clazzA)
                && RuleUtils.isClassAssignableToOneOf(mModelClasses, clazzB);
    }

    @Override
    public int apply(SortedListAdapter.ViewModel a, SortedListAdapter.ViewModel b) {
        final Class<? extends SortedListAdapter.ViewModel> clazzA = a.getClass();
        final Class<? extends SortedListAdapter.ViewModel> clazzB = b.getClass();
        return Integer.signum(RuleUtils.getIndexOfClass(mModelClasses, clazzA) - RuleUtils.getIndexOfClass(mModelClasses, clazzB));
    }
}
