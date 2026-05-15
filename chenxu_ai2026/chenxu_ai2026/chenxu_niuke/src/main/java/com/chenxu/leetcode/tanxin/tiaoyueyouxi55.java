package com.chenxu.leetcode.tanxin;

public class tiaoyueyouxi55 {
    public static void main(String[] args) {
        System.out.println(canJump(new int[]{3,2,1,0,4}));
        System.out.println(canJump(new int[]{2,3,1,1,4}));
    }
        public static boolean canJump(int[] nums) {
            //因为最大距离内得点都有可以达到 所以直接按最大距离算就行
            int farthest = 0;
            for (int i = 0; i < nums.length; i++) {
                if (i > farthest) return false;
                farthest = Math.max(farthest, i + nums[i]);
            }
            return true;
        }

}
