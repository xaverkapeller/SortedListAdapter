# SortedListAdapter

The `RecyclerView.Adapter` that makes your life easy!

 - **Based on the SortedList**: Uses a `Comparator` to sort the elements in your `RecyclerView`. By using a `Comparator` the `SortedList` takes care of managing the models in the `Adapter` in an efficient way and triggers all view animations for you!
 - **Reduces boilerplate**: All you have to do is implement your `ViewHolder` based on `SortedListAdapter.ViewHolder`. The `SortedListAdapter` takes care of binding data and calls to the notify methods when you add or remove models to the `SortedListAdapter`.
 - **High Performance**: `SortedListAdapter` also works effortlessly with large lists up to 100.000 items!

[![Build Status](https://travis-ci.org/Wrdlbrnft/SortedListAdapter.svg?branch=master)](https://travis-ci.org/Wrdlbrnft/SortedListAdapter)
[![BCH compliance](https://bettercodehub.com/edge/badge/Wrdlbrnft/SortedListAdapter)](https://bettercodehub.com/)

SortedListAdapter is based on the [**ModularAdapter**](https://wrdlbrnft.github.io/ModularAdapter/) you can find the GitHub project [**here**](https://github.com/Wrdlbrnft/ModularAdapter).

If you want to use SortedListAdapter then [**ModularAdapter**](https://wrdlbrnft.github.io/ModularAdapter/) is probably the library you are really looking for. All features of SortedListAdapter are developed in and for ModularAdapter and then included in this project.

# How do I add it to my project?

Just add this dependency to your build.gradle file:

```
compile 'com.github.wrdlbrnft:sorted-list-adapter:0.3.0.27'
```

## Example Project

You can find a simple example project in this [**GitHub Repository**](https://github.com/Wrdlbrnft/Searchable-RecyclerView-Demo).

Or if you first want to see what this library can do for yourself then download the example app from the Play Store here:

[![Get it on Google Play](https://developer.android.com/images/brand/en_generic_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=com.github.wrdlbrnft.searchablerecyclerviewdemo)

# How do I implement it?

## Implementing the Adapter

There are two ways to create a `SortedListAdapter` in your project. First you can subclass it like any other `Adapter`:

```java
public class ExampleAdapter extends SortedListAdapter<ExampleModel> {

    public ExampleAdapter(Context context, Comparator<ExampleModel> comparator) {
        super(context, ExampleModel.class, comparator);
    }

    @Override
    protected ViewHolder<? extends ExampleModel> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        final View itemView = inflater.inflate(R.layout.item_example, parent, false);
        return new ExampleViewHolder(itemView);
    }
}
```

As you can see above in that case all you have to do is implement the `onCreateViewHolder()` method. The `SortedListAdapter` takes care of binding the data.

However you can also quickly create a `SortedListAdapter` using the `SortedListAdapter.Builder` class:

```java
final SortedListAdapter<ExampleModel> adapter = new SortedListAdapter.Builder<>(context, ExampleModel.class, comparator)
        .add(ExampleModel.class, new SortedListAdapter.ViewHolderFactory<SortedListAdapter.ViewHolder<ExampleModel>>() {
            @Override
            public SortedListAdapter.ViewHolder<ExampleModel> create(LayoutInflater layoutInflater, ViewGroup viewGroup) {
                final View itemView = inflater.inflate(R.layout.item_example, parent, false);
                return new ExampleViewHolder(itemView);
            }
        })
        .build();
```

Both ways are equivalent, however when you have lists with many simple models the second method is usually favourable. If you are reusing the Adapter in many places and/or want to implement complex behavior you should go with the first method.

The `Comparator` instance you see in the constructor of both cases is used to determine the order of the items in your `RecyclerView`. This is the trade off the `SortedListAdapter` offers you: You gain high performance and seamless item animations even when dealing with hundreds of thousands of items in your `RecyclerView` and pay the price with the added complexity of having to implement a `Comparator` which sorts the items in your list in exactly the way you want. 

However the `SortedListAdapter` comes with a handy tool to make this job a little easier. You can use the `SortedListAdapter.ComparatorBuilder` which greatly simplifies your work when dealing with multiple different models in your list. It is used like this:

```java
final Comparator<SortedListAdapter.ViewModel> comparator = new SortedListAdapter.ComparatorBuilder<>()
        .setGeneralOrder(SomeModel.class, AnotherModel.class)
        .setOrderForModel(SomeModel.class, new Comparator<SomeModel>() {
            @Override
            public int compare(SomeModel a, SomeModel b) {
                return a.getText().compareTo(b.getText());
            }
        })
        .setOrderForModel(AnotherModel.class, new Comparator<AnotherModel>() {
            @Override
            public int compare(AnotherModel a, AnotherModel b) {
                return Integer.signum(a.getRank() - b.getRank());
            }
        })
        .build();
```

In the above example `setGeneralOrder()` is used to set the order of models based on type. In this specific example it means all `SomeModel` models will appear before `AnotherModel` models. The `setOrderForModel()` calls below it are used to set how each type of model should be ordered specifically. In this case it means that `SomeModel` instances are ordered by comparing their text field and `AnotherModel` instances are ordered based on their rank field.

## Implementing the View Models

All models used in a `SortedListAdapter` have to implement the `SortedListAdapter.ViewModel` interface. This interface requires you to implement two methods in your model:

 1. **`isSameModelAs()`**: This method is used to determine if two models refer to the same thing. Imagine it like this: If you have a list of movies and you update the list (for example when the user is filtering the list) `isSameModelAs()` will be used by the `SortedListAdapter` to determine if the same model is still present in the list.
 2. **`isContentTheSameAs()`**: This method is used to determine if the content of a model is equal to some other model. For example after two "same" model have been found using `isSameModelAs()` then `isContentTheSameAs()` will be used to check if data in the model has been modified and if a change animation has to be played in the `RecylerView`.
 
The canonical way to implement the above methods is like this:

```java
public class ExampleModel implements SortedListAdapter.ViewModel {

    private final long mId;
    private final String mValue;

    public ExampleModel(long id, String value) {
        mId = id;
        mValue = value;
    }

    public long getId() {
        return mId;
    }

    public String getValue() {
        return mValue;
    }

    @Override
    public <T> boolean isSameModelAs(T item) {
        if (item instanceof ExampleModel) {
            final ExampleModel other = (ExampleModel) item;
            return other.mId == mId;
        }
        return false;
    }

    @Override
    public <T> boolean isContentTheSameAs(T item) {
        if (item instanceof ExampleModel) {
            final ExampleModel other = (ExampleModel) item;
            return mValue != null ? mValue.equals(other.mValue) : other.mValue == null;
        }
        return false;
    }
}
```

## Implementing the ViewHolders

ViewHolders used in a `SortedListAdapter` have to be subclasses of `SortedListAdapter.ViewHolder`. They are responsible for binding the data to the `View`s in your `RecyclerView`. An example implementation looks like this:

```java
public class ExampleViewHolder extends SortedListAdapter.ViewHolder<ExampleModel> {

    private final TextView mValueView;

    public ExampleViewHolder(View itemView) {
        super(itemView);
        
        mValueView = itemView.findViewById(R.id.value);
    }

    @Override
    protected void performBind(ExampleModel item) {
        mValueView.setText(item.getValue());
    }
}
```

The method `performBind()` is called by the `SortedListAdapter` when it is necessary to bind new data to the `View` managed by your `ViewHolder`.

# Using the Adapter

Once you have implemented the `Adapter` you can add and remove models to it with an `Editor` object which can be accessed through the `edit()` method. All changes you make with this `Editor` object are batched and executed together once you call `commit()` on the `Editor` instance:

```java
final Comparator<ExampleModel> alphabeticalComparator = new Comparator<ExampleModel>() {
    @Override
    public int compare(ExampleModel a, ExampleModel b) {
        return a.getText().compareTo(b.getText());
    }
};

final ExampleAdapter adapter = new ExampleAdapter(context, alphabeticalComparator);
recyclerView.setAdapter(adapter);

adapter.edit()
        .add(modelsToAdd)
        .commit();
```

As you can see above you can add models to the `Adapter` with `add()`. The same way you can remove models with `remove()`:

```
adapter.edit()
        .remove(modelsToRemove)
        .commit();
```

You can also completely replace all models in the `Adapter` with another `List` of models with the `replaceAll()` method:

```java
adapter.edit()
        .replaceAll(newModels)
        .commit();
```

Or you can remove all models with `removeAll()`:

```java
adapter.edit()
        .removeAll()
        .commit();
```

**All** changes you make this way will automatically be animated in the RecyclerView!

### Best practices

Usually all you need is the `replaceAll()` method. If you have a `List` of models that you want to show you can add them to the `Adapter` and if you later get an updated `List` of models through some network call you can just use `replaceAll()` to swap them out. The `SortedListAdapter` will automatically figure out the difference between the two `Lists` and animate the difference!
