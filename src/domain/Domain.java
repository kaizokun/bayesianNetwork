package domain;

import java.util.*;

public class Domain<T> implements IDomain {

    protected List<DomainValue> values;

    protected Hashtable<T, DomainValue> index = new Hashtable<>();

    public Domain(Set<T> set){

        this((T[]) set.toArray());
    }

    public Domain(T... values) {

        this.values = Arrays.asList(new DomainValue[values.length]);

        for (int i = 0; i < values.length; i++) {

            DomainValue domainValue = new DomainValue(values[i], i);

            this.values.set(i, domainValue);

            this.index.put(values[i], domainValue);
        }
    }

    @Override
    public List<DomainValue> getValues() {

        return this.values;
    }

    @Override
    public int getSize() {

        return this.values.size();
    }

    @Override
    public DomainValue getDomainValue(int j) {

        return this.values.get(j);
    }

    @Override
    public DomainValue getDomainValue(Object object) {

        return this.index.get(object);
    }

    public static class DomainValue<T> implements Iterable<DomainValue> {

        protected T value;

        protected int index;

        public DomainValue() {
        }

        public DomainValue(T value) {
            this.value = value;
        }

        public DomainValue(T value, int index) {

            this.value = value;

            this.index = index;
        }

        public Object getValue() {

            return value;
        }

        public void setValue(T value) {

            this.value = value;
        }

        public int getIndex() {

            return index;
        }

        public void setIndex(int index) {

            this.index = index;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DomainValue that = (DomainValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }


        @Override
        public Iterator<DomainValue> iterator() {

            return Arrays.asList(new DomainValue[]{this}).iterator();
        }


    }

}
