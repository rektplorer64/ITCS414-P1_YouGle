
import java.util.*;

/**
 * A simple Wrapper Class that mimics the idea of SortedList
 * @param <T>
 */
public class AutoSortList<T> implements Collection<T> {

    private ArrayList<T> list;
    private Comparator<T> comparator;

    public AutoSortList(Comparator<T> comparator){
        this(new ArrayList<>(), comparator);
    }

    public AutoSortList(ArrayList<T> list, Comparator<T> comparator){
        this.list = new ArrayList<>(list);
        this.comparator = comparator;
        sort();
    }

    public AutoSortList(AutoSortList<T> autoSortListA) {
        this(autoSortListA.list, autoSortListA.comparator);
    }

    @Override
    public boolean add(T item){
        boolean isSuccessful = list.add(item);
        sort();
        return isSuccessful;
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public T get(int index){
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean isSuccessful = list.addAll(c);
        sort();
        return isSuccessful;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean isSuccessful = list.removeAll(c);
        sort();
        return isSuccessful;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    private void sort(){
        this.list.sort(this.comparator);
    }

    @Override
    public String toString() {
        return list.toString();
    }

    public ArrayList<T> getList() {
        return list;
    }

    public AutoSortList<T> clone(){
        return new AutoSortList<T>((ArrayList<T>) this.list.clone(), this.comparator);
    }
}
