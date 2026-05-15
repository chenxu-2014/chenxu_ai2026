package com.chenxu.leetcode.erfenchazhao;

import java.util.*;

public class aichixiangjiaodekk875 {
    public static void main(String[] args) {
        System.out.println(minEatingSpeed(new int[]{30,11,23,4,20},8));
    }
    public static int minEatingSpeed(int[] piles, int h) {
        //maxK: 吃香蕉的最大速度
        int maxK = Arrays.stream(piles).max().getAsInt();
        //吃香蕉的最小速度为 1 根/小时，最大速度为 maxK 根/小时
        int left = 1;
        int right = maxK;
        //查找左边界的二分搜索，计算吃完香蕉的最小速度
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (getTime(piles, mid) > h) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        //由于本题一定有解，故不需要做边界判断
        return left;
    }

    /*
    	实现 f(x)
        k:吃香蕉的速度（单位：根/小时）
        返回值:吃完所有香蕉所需的时间（单位：小时）
    */
    public static long getTime(int[] piles, int k) {
        //防止整数溢出
        long hour = 0;
        for (int i = 0; i < piles.length; i++) {
            hour += piles[i] / k;
            if (piles[i] % k > 0) {
                hour++;
            }
        }
        return hour;
    }
}
