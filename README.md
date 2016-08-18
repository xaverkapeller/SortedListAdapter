# SortedListAdapter

The `RecyclerView.Adapter` that makes your life easy!

 - **Based on the SortedList**: Uses a `Comparator` to sort the elements in your `RecyclerView`. By using a `Comparator` the `SortedList` takes care of managing the models in the `Adapter` in an efficient way and triggers all view animations for you!
 - **Reduces boilerplate**: All you have to do is implement your `ViewHolder` based on `SortedListAdapter.ViewHolder`. The `SortedListAdapter` takes care of binding data and calls to the notify methods when you add or remove models to the `SortedListAdapter`.

# How do I add it to my project?

Just add this dependency to your build.gradle file:

```
compile 'com.github.wrdlbrnft:sorted-list-adapter:0.1.0.5'
```

# Simple Example with Data Binding

## Models

All models you want to use in a `SortedListAdapter` have to implement `SortedListAdapter.ViewModel` like this:

```java
public class ExampleModel implements SortedListAdapter.ViewModel {

    private final long mId;
    private final String mText;

    public ExampleModel(long id, String text) {
        mId = id;
        mText = text;
    }
    
    public long getId() {
        return mId;
    }

    public String getText() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExampleModel that = (ExampleModel) o;

        return mText != null ? mText.equals(that.mText) : that.mText == null;

    }

    @Override
    public int hashCode() {
        return mText != null ? mText.hashCode() : 0;
    }
}
```

It is also advisable that all models override `equals()` and `hashCode()`. However don't include any id field that might be in your models in `equals()` or `hashCode()`. `equals()` and `hashCode()` should only compare the content of a model - in the example above, just the text. 

<sub>Most IDE's can generate `equals()` and `hashCode()` implementations for you.</sub>

## ViewHolders

When you implement a `ViewHolder` use the base class `SortedListAdapter.ViewHolder`, the type parameter of `SortedListAdapter.ViewHolder` should be the model which you want to bind to that `ViewHolder`:

```java
public class ExampleViewHolder extends SortedListAdapter.ViewHolder<ExampleModel> {

    private final ItemExampleBinding mBinding;

    public ExampleViewHolder(ItemExampleBinding binding) {
        super(binding.getRoot());
        mBinding = binding;
    }

    @Override
    protected void performBind(ExampleModel item) {
        mBinding.setModel(item);
    }
}
```

Bind data in the `performBind()` method.

## The Adapter

```java
public class ExampleAdapter extends SortedListAdapter<ExampleModel> {

    public ExampleAdapter(Context context, Comparator<ExampleModel> comparator) {
        super(context, ExampleModel.class, comparator);
    }

    @Override
    protected ViewHolder<? extends ExampleModel> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        final ItemExampleBinding binding = ItemExampleBinding.inflate(inflater, parent, false);
        return new ExampleViewHolder(binding);
    }

    @Override
    protected boolean areItemsTheSame(ExampleModel item1, ExampleModel item2) {
        return item1.getId() == item2.getId();
    }

    @Override
    protected boolean areItemContentsTheSame(ExampleModel oldItem, ExampleModel newItem) {
        return oldItem.equals(newItem);
    }
}
```

In `onCreateViewHolder()` just create instances of your `ViewHolder` implementations like usual. TWo extra methods have to be implemented for the `SortedListAdapter`: `areItemsTheSame()` and `areItemContentsTheSame()`.

 - **`areItemsTheSame()`**: Should check if two models refer to the same thing. This method should in most cases just compare the id of a model. In simplest terms the `SortedListAdapter` uses this method to determine if a model is already contained in the `SortedListAdater` or if a new model has been added.
 - **`areItemContentsTheSame()`**: Should check if the content of two models is the same. If your models have implemented `equals()` correctly you can just use `equals()` here to compare them. The `SortedListAdapter` uses this to determine if a model has changed and if a change animation should be triggered in the `RecyclverView`.
 
## Using the Adapter

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
        .remove(modelToRemove)
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

All changes you make will automatically be properly animated!

# Example Project

You can find a simple example project in this [**GitHub Repository**](https://github.com/Wrdlbrnft/Searchable-RecyclerView-Demo).
 
