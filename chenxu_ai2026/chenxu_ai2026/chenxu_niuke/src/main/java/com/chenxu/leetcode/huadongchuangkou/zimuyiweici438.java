package com.chenxu.leetcode.huadongchuangkou;

import java.util.*;

public class zimuyiweici438 {
    public static void main(String[] args) {
        System.out.println(findAnagrams("advsdada", "sdaf"));
    }

    public static List<Integer> findAnagrams(String s, String p) {
        List<Integer> res = new ArrayList<>();
        if (s.length() < p.length()) {
            return res;
        }

        int[] need = new int[26];   // 模式串 p 的频率
        int[] window = new int[26]; // 当前窗口的频率

        for (char c : p.toCharArray()) {
            need[c - 'a']++;
        }
        int left=0,right=0,len=p.length();
        while(right<s.length()){
            char c=s.charAt(right++);
            window[c-'a']++;
            if(right-left==len){
                if (Arrays.equals(window, need)) {
                    res.add(left);
                }
                char d=s.charAt(left);
                window[d-'a']--;
                left++;
            }
        }

        return res;
    }
}