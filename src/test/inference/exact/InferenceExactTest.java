package test.inference.exact;

import domain.data.AbstractDouble;
import inference.exact.EliminationAsk;
import inference.exact.EnumerationAsk;
import org.junit.Test;
import test.inference.InferenceTest.TestBayesianNetwork;

import static test.inference.InferenceTest.alarmTest;

public class InferenceExactTest {



    @Test
    public void alarmTestEnumerationAsk() {

        System.out.println();
        System.out.println("====================");
        System.out.println("ENUMERATION ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = EnumerationAsk.ask(test.request, test.obs, test.bayesianNetwork);

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestEliminationAsk() {

        System.out.println();
        System.out.println("====================");
        System.out.println("ELIMINATION ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = EliminationAsk.ask(test.request,test.obs,test.bayesianNetwork);

        System.out.println("RESULTAT : " + rs);
    }

}
