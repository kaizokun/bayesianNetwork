package domain;

import java.util.*;

public class Domain implements IDomain {

    protected List<DomainValue> values;

    protected Hashtable<Object, DomainValue> index = new Hashtable<>();

    public Domain(Object... values) {

        this.values = new ArrayList(Arrays.asList(new Object[values.length]));

        for(int i = 0 ; i < values.length ; i ++){

            DomainValue domainValue = new DomainValue(values[i],i);

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
    public Object getValue(int j) {

        return this.values.get(j);
    }

    @Override
    public Object getObjectValue(int j) {

        return this.values.get(j).getValue();
    }

    @Override
    public DomainValue getDomainValue(Object object){

        return this.index.get(object);
    }

    public static class DomainValue{

        private Object value;

        private int index;

        public DomainValue() {
        }

        public DomainValue(Object value, int index) {

            this.value = value;

            this.index = index;
        }

        public Object getValue() {

            return value;
        }

        public void setValue(Object value) {

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
    }
}
