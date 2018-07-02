package inference.dynamic.mmc;

import math.Matrix;

public interface IForward {
    Matrix forward();

    Matrix forward(int t);

    Matrix forward(int t, boolean saveForwards);
}
