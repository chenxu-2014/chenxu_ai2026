package com.chenxu.leetcode.tanxin;

public class zhonghua605 {
    public static void main(String[] args) {
        System.out.println(canPlaceFlowers(new int[]{0,0,1,0,0,0,1,0},3));
    }
        public static  boolean canPlaceFlowers(int[] flowerbed, int n) {
            int len = flowerbed.length;
            for (int i = 0; i < len && n > 0; i++) {
                if (flowerbed[i] == 0) {
                    // 左边是空 or i==0
                    int left = (i == 0) ? 0 : flowerbed[i - 1];
                    // 右边是空 or i==len-1
                    int right = (i == len - 1) ? 0 : flowerbed[i + 1];

                    if (left == 0 && right == 0) {
                        flowerbed[i] = 1; // 种花
                        n--;              // 种了一朵
                    }
                }
            }
            return n <= 0;
        }


}
