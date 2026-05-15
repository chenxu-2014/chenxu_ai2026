package com.chenxu.leetcode.huadongchuangkou;

import java.util.*;

public class wuchongfuzifudezuichangzichuan3 {
    public static void main(String[] args) {
        System.out.println(lengthOfLongestSubstring1("aasddaddvv"));
        System.out.println(lengthOfLongestSubstring("aasddaddvv"));
    }
    public static  int lengthOfLongestSubstring(String s) {
        HashMap<Character,Integer> map=new HashMap<>();

        int left=0,right=0,res=0;
        while (right < s.length()) {
            char c = s.charAt(right++);
            map.put(c, map.getOrDefault(c, 0) + 1);
            while(map.get(c)>0){
                char d = s.charAt(left++);
                map.put(d, map.getOrDefault(d, 0) - 1);

            }
            res=Math.max(res,right-left);
        }
        return res;
    }


    public static  int lengthOfLongestSubstring1(String s) {
        //滑动窗口
        Map<Character, Integer> window = new HashMap<>();
        int left = 0, right = 0, res = 0;

        while (right < s.length()) {
            char c = s.charAt(right++);
            window.put(c, window.getOrDefault(c, 0) + 1);

            while (window.get(c) > 1) {  // 出现重复
                char d = s.charAt(left++);
                window.put(d, window.get(d) - 1);
            }
            res = Math.max(res, right - left);
        }
        return res;
    }
}
