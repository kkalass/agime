package de.kalass.android.common.util;

import android.content.ContentProviderOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by klas on 27.01.14.
 */
public final class Arrays2 {

    private Arrays2() {

    }

    public static <E>ArrayList<E> asArrayList(E[] array) {
        final List<E> opsList = Arrays.asList(array);
        final ArrayList<E> opsArrayList;

        if (opsList instanceof  ArrayList) {
            opsArrayList = ( ArrayList<E> )opsList;
        } else {
            opsArrayList = new ArrayList<E>(opsList.size());
            opsArrayList.addAll(opsList);
        }
        return opsArrayList;
    }

}
