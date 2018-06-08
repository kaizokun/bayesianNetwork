package test.inference.approximative;

import domain.data.AbstractDouble;
import inference.approximation.GibbsAsk;
import inference.approximation.SamplingAsk;
import inference.approximation.SimilaritySamplingAsk;
import network.MarkovCoverDistributionComputeStatic;
import org.junit.Test;
import test.inference.InferenceTest.TestBayesianNetwork;

import static test.inference.InferenceTest.alarmTest;

public class InferenceApproximativeTest {

    @Test
    public void alarmTestSampleAsk(){

        System.out.println();
        System.out.println("====================");
        System.out.println("SAMPLE ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = SamplingAsk.ask(test.request, test.obs, test.bayesianNetwork, 500000);

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestSimilarSampleAsk(){

        System.out.println();
        System.out.println("====================");
        System.out.println("SIMILAR ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = SimilaritySamplingAsk.ask(test.request, test.obs, test.bayesianNetwork, 500000);

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestGibbsAsk(){

        System.out.println();
        System.out.println("====================");
        System.out.println("GIBBS ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = GibbsAsk.ask(test.request, test.obs, test.bayesianNetwork, 20000, new MarkovCoverDistributionComputeStatic());

        System.out.println("RESULTAT : " + rs);
    }



}
