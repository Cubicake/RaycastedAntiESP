package games.cubi.raycastedantiesp.core.utils;

import games.cubi.logs.Logger;

/**
 * Static utility class for operations on int arrays annotated with {@link IntArrayListMarker}, which are intended to be used as lists of integers without the overhead of autoboxing.
 * <p>
 * {@link IntArrayListMarker} fields may be null or non-initialised to save memory, {@code IntArrayList} methods will treat null or non-initialised arrays as empty lists.
 * <p>
 * Arrays expand and contract as needed such that the length of the backing array is always equal to the number of elements in the list, so there is no extra capacity. This means that adding or removing elements from the list will involve creating a new array and copying the existing elements, which can be inefficient for large lists. However, this design choice was made to save memory and avoid the overhead of maintaining a separate size field and capacity management logic.
 */
public class IntArrayList {
    private IntArrayList() {}

    public static int size(int@IntArrayListMarker[] array) {
        return array == null ? 0 : array.length;
    }

    public static boolean isEmpty(int@IntArrayListMarker[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(int@IntArrayListMarker[] array, int value) {
        if (array == null) return false;
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }

    /**
     * Caller of this method <b>must</b> reassign the returned array to the original array reference, as the original array will not be modified and a new array with the added element will be returned.
     */
    public static int@IntArrayListMarker[] add(int@IntArrayListMarker[] array, int value) {
        if (array == null) {
            return new int[]{value};
        } else {
            int[] newArray = new int[array.length + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[array.length] = value;
            return newArray;
        }
    }

    /**
     * Caller of this method <b>must</b> reassign the returned array to the original array reference, as the original array will not be modified and a new array with the added element will be returned.
     */
    public static int@IntArrayListMarker[] remove(int@IntArrayListMarker[] array, int value) {
        if (array == null) return null;
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                index = i;
                break;
            }
        }
        if (index == -1) return array; // value not found, return original array
        int[] newArray = new int[array.length - 1];
        System.arraycopy(array, 0, newArray, 0, index);
        System.arraycopy(array, index + 1, newArray, index, array.length - index - 1);
        return newArray;
    }

    /**
     * Caller of this method <b>must</b> reassign the returned array to the original array reference, as the original array will not be modified and a new array with the added element will be returned.
     */
    public static int@IntArrayListMarker[] clear(int@IntArrayListMarker[] array) {
        return null;
    }

    public static int get(int@IntArrayListMarker[] array, int index) {
        if (array == null) Logger.errorAndReturn(new IndexOutOfBoundsException("Attempted to find value at index: " + index + ", for IntArrayList with size: 0"), 3, IntArrayList.class);
        if (index < 0 || index >= array.length) Logger.errorAndReturn(new IndexOutOfBoundsException("Attempted to find value at index: " + index + ", for IntArrayList with size: " + size(array)), 3, IntArrayList.class);
        return array[index];
    }

    public static int@IntArrayListMarker[] set(int@IntArrayListMarker[] array, int index, int value) {
        array[index] = value;
        return array;
    }

    public static int[] getCopyOrNull(int@IntArrayListMarker[] array) {
        if (size(array) == 0) return null;
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    public static String toString(int@IntArrayListMarker[] array) {
        if (array == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

