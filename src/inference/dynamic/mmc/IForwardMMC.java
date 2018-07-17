package inference.dynamic.mmc;

import math.Matrix;

public interface IForwardMMC {
    Matrix forward();

    Matrix forward(boolean saveForwards);

    Matrix forward(int t);

    Matrix forward(int t, boolean saveForwards);
}
