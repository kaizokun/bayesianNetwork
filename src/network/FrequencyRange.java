package network;

import domain.Domain;
import domain.data.AbstractDouble;

import java.util.List;
import java.util.Map;

/**
 * -------------------------SUB CLASSES--------------------------
 */


public class FrequencyRange {

    private AbstractDouble min, max;

    public FrequencyRange() {
    }

    public FrequencyRange(AbstractDouble min, AbstractDouble max) {

        this.min = min;

        this.max = max;
    }

    public int compareTo(AbstractDouble search) {

        //inférieur au minimum rang inférieur
        if (search.compareTo(min) < 0) {

            return -1;
        }

        //inférieur au minimum rang supérieur
        if (search.compareTo(max) > 0) {

            return 1;
        }

        //range[0,1]
        //rdm [0,0.99999]
        //supérieur ou égal au min et inférieur au max dans le range
        //return search.compareTo(min) >= 0 && search.compareTo(max) < 0;

        return 0;
    }

    public AbstractDouble getMin() {
        return min;
    }

    public void setMin(AbstractDouble min) {
        this.min = min;
    }

    public AbstractDouble getMax() {
        return max;
    }

    public void setMax(AbstractDouble max) {
        this.max = max;
    }

    @Override
    public String toString() {

        return "[" + this.min + " - " + this.max + "]";
    }

    public static Domain.DomainValue dichotomicSearch(List<Map.Entry<Domain.DomainValue, FrequencyRange>> rangeEntries,
                                                       AbstractDouble search) {

        return dichotomicSearch(rangeEntries, search, 0 , rangeEntries.size());

    }

    private static Domain.DomainValue dichotomicSearch(List<Map.Entry<Domain.DomainValue, FrequencyRange>> rangeEntries,
                                                AbstractDouble search, int s, int e) {

        int middle = s + ((e - s) / 2);

        Map.Entry<Domain.DomainValue, FrequencyRange> rangeEntry = rangeEntries.get(middle);

        switch (rangeEntry.getValue().compareTo(search)) {

            case 0:

                return rangeEntry.getKey();

            case -1:

                return dichotomicSearch(rangeEntries, search, s, middle);

            default:

                return dichotomicSearch(rangeEntries, search, middle + 1, e);
        }
    }


}
