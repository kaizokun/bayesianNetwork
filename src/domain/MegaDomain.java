package domain;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MegaDomain extends Domain<List<Domain.DomainValue>> {

    public MegaDomain(List<List<DomainValue>> values) {

        this.values = Arrays.asList(new MegaDomainValue[values.size()]);

        for (int i = 0; i < values.size(); i++) {

            List<DomainValue> domainValueList = values.get(i);

            MegaDomainValue domainValue = new MegaDomainValue(domainValueList, i);

            this.values.set(i, domainValue);

            this.index.put(domainValueList, domainValue);
        }
    }

    public static class MegaDomainValue extends DomainValue<List<DomainValue>> {

        public MegaDomainValue(List<DomainValue> value) {

            super(value);
        }

        public MegaDomainValue(List<DomainValue> value, int index) {

            super(value, index);
        }

        @Override
        public Iterator<DomainValue> iterator() {

            return this.value.iterator();
        }
    }

}
