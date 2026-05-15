package com.chenxu.niuke.huawei;

import java.util.*;

public class shujufenzu93 {
        public static void main(String[] args) {
                int[] num=new int[]{1,0,2,3,-2};
                int sum5 = 0;
                int sum3 = 0;
                List<Integer> otherList = new ArrayList<>();

                for (int i = 0; i < 5; i++) {
                    if (num[i] % 5 == 0) {
                        sum5 += num[i];
                    } else if (num[i] % 3 == 0) {
                        sum3 += num[i];
                    } else {
                        otherList.add(num[i]);
                    }
                }

                // 初始调用DFS，从otherList的第0个元素开始，当前组A(sum5)和组B(sum3)的差值
                boolean result = dfs(otherList, 0, sum5, sum3);
                System.out.println(result);
        }

        private static boolean dfs(List<Integer> list, int index, int sumA, int sumB) {
            // 递归终止条件：已经处理完剩余列表中的所有数字
            if (index == list.size()) {
                return sumA == sumB;
            }
            // 对于当前数字，有两种选择：放入组A 或 放入组B
            int num = list.get(index);
            // 递归搜索两种可能性：放入组A 或 放入组B
            return dfs(list, index + 1, sumA + num, sumB) || dfs(list, index + 1, sumA, sumB + num);
        }
}
