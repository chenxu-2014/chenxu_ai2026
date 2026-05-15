package com.chenxu.leetcode;

import java.util.Arrays;

public class zuichanglianxuxulie128 {
    public static void main(String[] args) {
        int[] nums= {100,4,200,1,3,2};//4
        int[] nums1={0,3,7,2,5,8,4,6,0,1};//9
        int[] nums2={9,1,4,7,3,-1,0,5,8,-1,6};//7
        int[] nums3={100,4,200,1,3,2};//4
        int[] nums4={1,0,1,2};//3
        int[] nums5={4,0,-4,-2,2,5,2,0,-8,-8,-8,-8,-1,7,4,5,5,-4,6,6,-3};//5
       System.out.println(longestConsecutive(nums));
        System.out.println(longestConsecutive(nums1));
        System.out.println(longestConsecutive(nums2));
        System.out.println(longestConsecutive(nums3));
        System.out.println(longestConsecutive(nums4));
        System.out.println(longestConsecutive(nums5));
    }

    private static int longestConsecutive(int[] nums) {
        if(nums.length==0) return 0;
        Arrays.sort(nums);
        int count=1;
        int max=1;
        for (int i=0;i<nums.length-1;i++){
            if(nums[i]+1==nums[i+1]){
                ++count;
                if(count>=max){
                    max=count;
                }
            }else if(nums[i]==nums[i+1]){

            }else{
                count=1;
            }
        }
        return  max;
    }
}
