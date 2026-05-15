package com.chenxu.leetcode.tanxin;

import java.util.*;

public class zuichanghuiwenchuan409 {
    public static void main(String[] args) {
        System.out.println(longestPalindrome("aahhdddc"));
        System.out.println(longestPalindrome("aahhdddcefff"));

    }
    public static int longestPalindrome(String s) {
        // res 保存最终的结果，初始值为 0
        int res = 0;
        char[] chs = s.toCharArray();
        // haspMap 用来记录字符串 s 中出现的字符以及对应出现的次数
        Map<Character, Integer> hashMap = new HashMap<>();
        for (char c : chs) {
            hashMap.put(c, hashMap.getOrDefault(c, 0) + 1);
        }
        //遍历 hashMap 中所有的 value
        for (Integer cnt : hashMap.values()) {
            if (cnt % 2 == 0) {
                //当前字符出现的次数 cnt 为偶数，那么它一定是最长回文串的一部分
                res += cnt;
            } else {
                //cx 字符出现为奇数时，-1即为偶数 同时减去了只出现一次得奇数
                //当前字符出现的次数 cnt 为奇数，那么最长回文串一定包括 cnt - 1 个该字符
                res += cnt - 1;
            }
        }
        /*
            上面的 res 计算的是最长回文串长度为偶数的情况，下面再考虑其长度为奇数的情况：
            (1) 当其长度为奇数时，最中间的字符个数一定为奇数，例如 "dccaccd"、"ddaaadd"中的字符 a 分别出现了 1 次和 3 次；
            (2) 如果 s 中存在出现次数为奇数的字符时，任意选择其中的一种，将其放到最长回文串的中间即可，即 res 的值再加一；
            (3) 如果 s 中不存在出现次数为奇数的字符时，最长回文串的长度即为 s 的长度；
        */
        if (res < s.length()) {
            // res < s.length() 说明 s 中一定存在出现次数为奇数的字符
            res++;
        }
        return res;
    }
}
