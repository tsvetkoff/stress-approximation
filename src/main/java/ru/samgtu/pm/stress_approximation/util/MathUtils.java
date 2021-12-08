package ru.samgtu.pm.stress_approximation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

    private MathUtils() {

    }

    /**
     * Определенный интеграл [R1, r] по переменному верхнему пределу r (R1<=r<=R2), вычесленный методом трапеций.
     *
     * @param function Подынтегральная функция
     * @param step     Шаг интегрирования
     * @param result   Значение интеграла
     */
    public static void varIntegral(double[] function, double step, double[] result) {
        result[0] = 0;
        for (int k = 1; k < function.length; k++) {
            result[k] = result[k - 1] + step / 2 * (function[k - 1] + function[k]);
        }
    }

    /**
     * Определенный интеграл [R1, R2].
     *
     * @param function Подынтегральная функция
     * @param step     Шаг интегрирования
     * @return Значение интеграла
     */
    public static double defIntegral(double[] function, double step) {
        if (function.length % 2 == 1) {
            return defIntegralSimpson(function, step);
        } else {
            // если N четно, то разбиваем интеграл по отрезку [r0..rN] на два интеграла:
            // для интегрирования по отрезку [r0..r1] используем метод трапеций
            // для интегрирования по отрезку [r1..rN] используем метод Симпсона
            double sum = 0;
            for (int k = 2; k < function.length - 1; k = k + 2) {
                sum += function[k - 1] + 4 * function[k] + function[k + 1];
            }
            return step / 3 * sum + (function[0] + function[1]) / 2 * step;
        }
    }

    /**
     * Определенный интеграл [R1, R2], вычесленный по методу Симпсона.
     *
     * @param function Подынтегральная функция
     * @param step     Шаг интегрирования
     * @return Значение интеграла
     */
    private static double defIntegralSimpson(double[] function, double step) {
        double sum = 0;
        for (int k = 1; k < function.length - 1; k = k + 2) {
            sum += function[k - 1] + 4 * function[k] + function[k + 1];
        }
        return step / 3 * sum;
    }

//    /**
//     * Определенный интеграл [R1, R2], вычесленный по методу трапеций.
//     *
//     * @param function Подынтегральная функция
//     * @param step     Шаг интегрирования
//     * @return Значение интеграла
//     */
//    private static double defIntegralTrapeze(double[] function, double step) {
//        double sum = (function[0] + function[function.length - 1]) / 2;
//        for (int k = 1; k < function.length - 1; k++) {
//            sum += function[k];
//        }
//        return step * sum;
//    }

    /**
     * Округление числа для избежания неточности представления дробных числе при арифметических операциях.
     *
     * @param value Число для округления
     * @return Округленное значение
     */
    public static double round(double value) {
        return new BigDecimal(value).setScale(7, RoundingMode.HALF_UP).doubleValue();
    }

}
