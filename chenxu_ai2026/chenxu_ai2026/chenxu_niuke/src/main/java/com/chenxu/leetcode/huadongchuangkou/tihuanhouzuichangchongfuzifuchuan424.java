package com.chenxu.leetcode.huadongchuangkou;

import java.util.HashSet;
import java.util.*;

public class tihuanhouzuichangchongfuzifuchuan424 {
    public static void main(String[] args) {
        System.out.println(gptcharacterReplacement("CABDBBACCBBD",1));
    }
    public static int characterReplacement(String s, int k) {
        int res=0,change=0,left=0,right=0,len=s.length();
        Set<Character> set=new HashSet<>();
        Map<Character,Integer> map=new HashMap<>();
        while(right<len){
            char c = s.charAt(right++);
            //map.put(c,map.getOrDefault(c,0)+1);
            if(!set.add(c)){
                change++;
            }
            while(change>k){
                res=Math.max(res,set.size());
                set.clear();
            }

        }
        return res;
    }

    public static int gptcharacterReplacement(String s, int k) {
        int[] count = new int[26]; // 记录窗口内每个字符的出现次数
        int left = 0, right = 0, maxCount = 0, res = 0;

        while (right < s.length()) {
            char c = s.charAt(right);
            count[c - 'A']++;
            maxCount = Math.max(maxCount, count[c - 'A']); // 窗口内最多字符数
            right++;

            // 如果替换次数超过 k，就收缩窗口
            while (right - left - maxCount > k) {
                count[s.charAt(left) - 'A']--;
                left++;
            }

            res = Math.max(res, right - left);
        }
        return res;
    }
}
