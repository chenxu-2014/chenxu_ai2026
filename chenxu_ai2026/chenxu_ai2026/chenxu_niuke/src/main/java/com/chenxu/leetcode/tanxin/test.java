package com.chenxu.leetcode.tanxin;

import java.util.Arrays;
import java.util.Collections;

public class test {
    public static void main(String[] args) {
        int[][] arr2 = new int[][]{{1,2},{2,3},{3,4},{1,3}};
        Integer[] arr1 = {3, 1, 2};
        Arrays.sort(arr1, Collections.reverseOrder());
        Arrays.sort(arr2,(a,b)->{
            return Integer.compare(a[0],b[0]);
        });
        System.out.println(arr2);
    }
}
