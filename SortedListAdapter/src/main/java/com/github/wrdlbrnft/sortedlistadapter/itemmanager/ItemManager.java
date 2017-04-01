package com.github.wrdlbrnft.sortedlistadapter.itemmanager;

import android.support.annotation.NonNull;

import java.util.Collection;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 01/04/2017
 */

public interface ItemManager<T> {

    interface Transaction<T> {
        Transaction<T> add(@NonNull T item);
        Transaction<T> add(@NonNull Collection<T> items);
        Transaction<T> remove(@NonNull T item);
        Transaction<T> remove(@NonNull Collection<T> items);
        Transaction<T> replaceAll(@NonNull Collection<T> items);
        Transaction<T> removeAll();
        void commit();
    }

    interface TransactionCallback {
        void onTransactionsInProgress();
        void onTransactionsFinished();
        void onChangeSetAvailable(ChangeSet changeSet);
    }

    T getItem(int position);
    int getItemCount();

    Transaction<T> newTransaction();
}
