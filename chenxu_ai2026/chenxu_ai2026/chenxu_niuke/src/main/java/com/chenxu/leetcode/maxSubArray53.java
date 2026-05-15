package com.chenxu.leetcode;

public class maxSubArray53 {
    public static void main(String[] args) {
        int[] nums= {1,-3,4,-1,2,1,-5,4};
        int[] nums1= {5,4,-1,7,8};
        int[] nums2= {1,3};
        System.out.println(maxSubArray(nums));
        System.out.println(maxSubArray(nums1));
        System.out.println(maxSubArray(nums2));
    }

    private static int maxSubArray(int[] nums) {
        int res=0,tmp;
        if(nums.length>0){
           res = nums[0];
        }
        for(int i=0;i<nums.length;i++) {
            tmp = nums[i];

            for (int j=i; j < nums.length-1; j++) {
                if (nums[j] + nums[j+1] < 0 && nums[j] > 0) {
                    tmp += nums[j+1];
                    break;
                }
                tmp += nums[j+1];
//                if(j+2==nums.length && nums[j+1]>0){
//                    tmp += nums[j+1];
//                }

            }
            res=Math.max(res,tmp);
        }

        return res;

    }
}
