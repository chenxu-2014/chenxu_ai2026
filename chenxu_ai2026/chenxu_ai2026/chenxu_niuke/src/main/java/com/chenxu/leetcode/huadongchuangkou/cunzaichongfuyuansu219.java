package com.chenxu.leetcode.huadongchuangkou;

import java.util.HashSet;

public class cunzaichongfuyuansu219 {
    public static void main(String[] args) {
        System.out.println(containsNearbyDuplicate(new int[]{1,2,3,1},3));
        System.out.println(containsNearbyDuplicate(new int[]{1,2,3,1,2,3},2));
    }
    public static boolean containsNearbyDuplicate(int[] nums, int k) {
        HashSet<Integer> hashSet=new HashSet<>();
        for(int i=0;i<nums.length;i++){
            if(i>k){
                hashSet.remove(nums[i-k-1]);
            }
            if(!hashSet.add(nums[i])){
                return true;
            }
        }
        return false;
    }
}
