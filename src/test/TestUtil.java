package test;

public class TestUtil {

    private static long t1;

    public static void initTime() {

        t1 = System.currentTimeMillis();
    }

    public static long timeDelta() {

        return System.currentTimeMillis() - t1;
    }

    public static void printTimeDelta() {

        System.out.println(System.currentTimeMillis() - t1);
    }

}
