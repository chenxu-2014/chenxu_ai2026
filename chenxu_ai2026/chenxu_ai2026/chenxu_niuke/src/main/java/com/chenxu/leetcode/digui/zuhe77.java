package com.chenxu.leetcode.digui;
import java.util.*;

/**
 * 输入：n = 4, k = 2
 * 输出：
 * [
 *   [2,4],
 *   [3,4],
 *   [2,3],
 *   [1,2],
 *   [1,3],
 *   [1,4],
 * ]
 */
public class zuhe77 {

    public static void main(String[] args) {
        System.out.println(combine(4,3));
    }
        public static List<List<Integer>> combine(int n, int k) {
            List<List<Integer>> result = new ArrayList<>();
            // 调用回溯函数，从数字1开始选择
            backtrack(n, k, 1, new ArrayList<>(), result);
            return result;
        }

        private static void backtrack(int n, int k, int start, List<Integer> path, List<List<Integer>> result) {
            // 1. 终止条件：当前路径长度等于k
            if (path.size() == k) {
                result.add(new ArrayList<>(path)); // 注意要新建一个List
                return;
            }

            // 2. 遍历选择：从start开始，到n结束
            for (int i = start; i <= n; i++) {
                path.add(i);          // 做出选择：将当前数字i加入路径
                // 3. 递归：从i+1开始选择，避免重复
                backtrack(n, k, i + 1, path, result);
                path.remove(path.size() - 1); // 撤销选择：回溯，移除最后加入的数字
            }
        }

}
