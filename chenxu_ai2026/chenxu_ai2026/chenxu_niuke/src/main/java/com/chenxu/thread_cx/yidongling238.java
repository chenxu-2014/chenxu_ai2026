package com.chenxu.thread_cx;

public class yidongling238 {
    public static void main(String[] args) {
        int[] nums= {0,1,0,3,12};//4
        int[] nums1={0,3,7,2,5,8,4,6,0,1};//9
        int[] nums2={9,1,4,7,3,-1,0,5,8,-1,6};//7
        int[] nums3={100,4,200,1,3,2};//4
        int[] nums4={1,0,1,2};//3
        int[] nums5={4,0,-4,-2,2,5,2,0,-8,-8,-8,-8,-1,7,4,5,5,-4,6,6,-3};//5

        System.out.println(yidongling(nums));
    }

    private static int[] yidongling(int[] nums) {
        int n = nums.length;
        int zeroNum = 0;
        for(int i = 0; i < n; i++) {
            if(nums[i] == 0) {
                zeroNum++;
                continue;
            }
            if(zeroNum > 0) {
                nums[i - zeroNum] = nums[i];
                nums[i] = 0;
            }
        }
        return nums;
    }
}
