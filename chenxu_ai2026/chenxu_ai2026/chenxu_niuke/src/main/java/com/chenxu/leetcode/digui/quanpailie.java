package com.chenxu.leetcode.digui;
import java.util.ArrayList;
import java.util.List;

/**
 * abc
 * 输出格式：
 *
 * text
 *
 * 复制
 *
 * 下载
 * abc
 * acb
 * bac
 * bca
 * cab
 * cba
 */
public class quanpailie {
        public static void main(String[] args) {
            // 示例输入，假设输入是数组 [1,2,3]
            int[] nums = {1, 2, 3};
            List<List<Integer>> result = permute(nums);
            for (List<Integer> list : result) {
                System.out.println(list);
            }
        }

        public static List<List<Integer>> permute(int[] nums) {
            List<List<Integer>> res = new ArrayList<>();
            backtrack(res, new ArrayList<>(), nums);
            return res;
        }

        private static void backtrack(List<List<Integer>> res, List<Integer> tempList, int[] nums) {
            if (tempList.size() == nums.length) {
                res.add(new ArrayList<>(tempList));
            } else {
                for (int i = 0; i < nums.length; i++) {
                    if (tempList.contains(nums[i])) {
                        continue;
                    } // 跳过已经选择的元素
                    tempList.add(nums[i]);
                    backtrack(res, tempList, nums);
                    tempList.remove(tempList.size() - 1); // 回溯
                }
            }
        }

}
