# SortedListAdapter

The `RecyclerView.Adapter` that makes your life easy!

 - **Based on the SortedList**: Uses a `Comparator` to sort the elements in your `RecyclerView`. By using a `Comparator` the `SortedList` takes care of managing the models in the `Adapter` in an efficient way and triggers all view animations for you!
 - **Reduces boilerplate**: All you have to do is implement your `ViewHolder` based on `SortedListAdapter.ViewHolder`. The `SortedListAdapter` takes care of binding data and calls to the notify methods when you add or remove models to the `SortedListAdapter`.

[![Build Status](https://travis-ci.org/Wrdlbrnft/SortedListAdapter.svg?branch=master)](https://travis-ci.org/Wrdlbrnft/SortedListAdapter)

# How do I add it to my project?

Just add this dependency to your build.gradle file:

```
compile 'com.github.wrdlbrnft:sorted-list-adapter:0.2.0.1'
```

## For more information about this library visit the [**project homepage**](https://wrdlbrnft.github.io/SortedListAdapter/)

# Example Project

You can find a simple example project in this [**GitHub Repository**](https://github.com/Wrdlbrnft/Searchable-RecyclerView-Demo).
