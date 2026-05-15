package com.chenxu.leetcode.huisu;

import java.util.*;

public class kuohaoshengcheng22 {

    public static void main(String[] args) {
        System.out.println(generateParenthesisDigui(2));
        System.out.println( generateParenthesishuisu(2));
    }
    public static List<String> generateParenthesisDigui(int n) {
        if (n == 1) {
            return Arrays.asList("()");
        }
        Set<String> hashSet = new HashSet<>();
        for (String str : generateParenthesisDigui(n - 1)) {
            for (int i = 0; i <= str.length() / 2; i++) {
                hashSet.add(str.substring(0, i) + "()" + str.substring(i, str.length()));
            }
        }
        return new ArrayList<>(hashSet);
    }

    // res 用于保存最终结果
    static List<String> res = new ArrayList<>();
    // builder 保存中间结果
    static StringBuilder builder = new StringBuilder();

    public static List<String> generateParenthesishuisu(int n) {
        backtrack(0, 0, n);
        return res;
    }

    /*
        leftCnt: 左括号的数量
        rightCnt: 右括号的数量
        n: 括号的对数
    */
    public static void backtrack(int leftCnt, int rightCnt, int n) {
        //如果 builder 的长度 = 2 * n，说明已经找到了一种组合
        if (builder.length() == n * 2) {
            res.add(builder.toString());
            return;
        }
        //如果左括号数量小于 n，可以放一个左括号
        if (leftCnt < n) {
            builder.append('(');
            backtrack(leftCnt + 1, rightCnt, n);
            builder.deleteCharAt(builder.length() - 1);
        }
        //如果右括号数量小于左括号的数量，可以放一个右括号
        if (rightCnt < leftCnt) {
            builder.append(')');
            backtrack(leftCnt, rightCnt + 1, n);
            builder.deleteCharAt(builder.length() - 1);
        }
    }
}
