package com.chenxu.leetcode.huadongchuangkou;

import java.util.HashSet;

//给你一个整数数组 nums 和一个整数 k ，判断数组中是否存在两个 不同的索引 i 和 j ，满足 nums[i] == nums[j] 且 abs(i - j) <= k 。如果存在，返回 true ；否则，返回 false 。
//
//示例 1：
//
//输入：nums = [1,2,3,1], k = 3
//输出：true
//示例 2：
//
//输入：nums = [1,0,1,1], k = 1
//输出：true
//示例 3：
//
//输入：nums = [1,2,3,1,2,3], k = 2
//输出：false
public class cunzaichongfuyuansu219 {
    public static void main(String[] args) {
        System.out.println(containsNearbyDuplicate(new int[]{1,2,3,1},3));
        System.out.println(containsNearbyDuplicate(new int[]{1,2,3,1,2,3},2));
    }
//    核心思路：维护一个长为 min(k+1,n) 的滑动窗口，
//    用哈希集合维护窗口内的元素。在元素 x 进入窗口之前，判断 x 是否在哈希集合中，如果在，则说明把 x 加入窗口之后，窗口内有重复元素。
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
