package com.chenxu.leetcode.huisu;

import java.util.*;

public class ziji78 {

    static List<List<Integer>> res = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println(subsets(new int[]{1, 2, 3}));
    }

    public static List<List<Integer>> subsets(int[] nums) {
        backtrack(new ArrayList<>(), nums, 0);
        return res;
    }

    private static void backtrack(List<Integer> path, int[] nums, int start) {
        res.add(new ArrayList<>(path)); // 每个路径都是一个子集

        for (int i = start; i < nums.length; i++) {
            path.add(nums[i]);
            backtrack(path, nums, i + 1);
            path.remove(path.size() - 1); // 撤销选择
        }
    }

}
