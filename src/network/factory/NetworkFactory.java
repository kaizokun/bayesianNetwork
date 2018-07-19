package network.factory;

import network.dynamic.DynamicBayesianNetwork;

public  interface NetworkFactory {

    DynamicBayesianNetwork initNetwork();

}
