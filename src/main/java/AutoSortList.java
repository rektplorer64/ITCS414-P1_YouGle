
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A simple Wrapper Class that mimics the idea of SortedList
 * @param <T>
 */
public class AutoSortList<T> extends ArrayList<T> {

    private Comparator<T> comparator;

    public AutoSortList(Comparator<T> comparator){
        this(new ArrayList<>(), comparator);
    }

    public AutoSortList(@NotNull Collection<? extends T> c, Comparator<T> comparator) {
        super(c);
        this.comparator = comparator;
        this.sort(comparator);
    }

    @Override
    public boolean add(T t) {
        final boolean outcome = super.add(t);
        this.sort(comparator);
        return outcome;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        this.sort(comparator);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        final boolean outcome = super.addAll(c);
        this.sort(comparator);
        return outcome;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        final boolean outcome = super.addAll(index, c);
        this.sort(comparator);
        return outcome;
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        super.replaceAll(operator);
        this.sort(comparator);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        final boolean outcome = super.removeIf(filter);
        this.sort(comparator);
        return outcome;
    }

    @Override
    public AutoSortList<T> clone() {
        return new AutoSortList<T>((Collection<? extends T>) super.clone(), comparator);
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

}
