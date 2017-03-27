package com.github.wrdlbrnft.sortedlistadapter;

/**
 * Created with Android Studio<br>
 * User: Xaver<br>
 * Date: 27/03/2017
 */

class RuleUtils {

    public static int getIndexOfClass(Class<?>[] classes, Class<?> item) {
        for (int i = 0; i < classes.length; i++) {
            final Class<?> clazz = classes[i];
            if (clazz.isAssignableFrom(item)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isClassAssignableToOneOf(Class<?>[] classes, Class<?> item) {
        for (Class<?> clazz : classes) {
            if (clazz.isAssignableFrom(item)) {
                return true;
            }
        }
        return false;
    }
}
