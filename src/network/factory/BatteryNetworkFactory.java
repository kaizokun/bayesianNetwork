package network.factory;

import domain.Domain;
import domain.IDomain;

import java.util.ArrayList;
import java.util.List;

public abstract class BatteryNetworkFactory implements NetworkFactory {


    protected Double[][] initBatteryTransitionTable(IDomain batteryLevelDomain) {

        //La batterie à 100% de change de passer d'un niveau supérieur à un niveau strictement inférieur
        //et aucune pour le reste

        Double[][] tab = new Double[batteryLevelDomain.getSize()][batteryLevelDomain.getSize()];

        List<Domain.DomainValue> previousBatteryLevels = batteryLevelDomain.getValues();

        List<Domain.DomainValue> batteryLevels = new ArrayList<>(batteryLevelDomain.getValues());

        int row = 0;

        for (Domain.DomainValue previousLevel : previousBatteryLevels) {

            Integer previousLevelInt = (Integer) previousLevel.getValue();

            int col = 0;

            for (Domain.DomainValue level : batteryLevels) {

                Integer levelInt = (Integer) level.getValue();

                int cmp = previousLevelInt.compareTo(levelInt);

                if (cmp == 0) {
                    // 4 chance sur 5 de conserver le niveau de batterie
                    tab[row][col] = 0.8;
                    //le niveau précédent est superieur au suivant et la difference est de 1
                } else if (cmp > 0 && previousLevelInt - levelInt == 1) {
                    // 1chance sur  5 de passer à un niveau inferieur de battery
                    tab[row][col] = 0.2;

                    //le niveau précédent est inférieur ou superieur mais de plus de 1
                } else {

                    tab[row][col] = 0.0;
                }

                col++;
            }

            row++;
        }

        return tab;
    }

    public enum BATTERY_VARS {

        BATTERY, GAUGE, BROKEN_GAUGE
    }

}
