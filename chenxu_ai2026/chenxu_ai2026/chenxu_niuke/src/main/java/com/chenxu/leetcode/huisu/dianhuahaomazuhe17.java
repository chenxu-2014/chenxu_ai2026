package com.chenxu.leetcode.huisu;

import java.util.ArrayList;
import java.util.List;

public class dianhuahaomazuhe17 {
    static String[] dic = {"", "*", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
    // res 用来保存结果
    static List<String> res = new ArrayList<>();
    // builder 用于保存中间结果
    static StringBuilder builder = new StringBuilder();

    public static void main(String[] args) {
        res=letterCombinations("234");
        System.out.println(res);
    }
    public static List<String> letterCombinations(String digits) {
        int length = digits.length();
        //考虑特殊情况
        if (length == 0) {
            return res;
        }
        backtrack(digits, 0);
        return res;
    }

    public static void backtrack(String digits, int start) {
        if (builder.length() == digits.length()) {
            //达到回溯树的叶子节点
            res.add(builder.toString());
            return;
        }
        for (int i = start; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            for (char c : dic[digit].toCharArray()) {
                //做选择
                builder.append(c);
                //递归下一层回溯树
                backtrack(digits, i + 1);
                //撤销选择
                builder.deleteCharAt(builder.length() - 1);
            }
        }
    }
}
