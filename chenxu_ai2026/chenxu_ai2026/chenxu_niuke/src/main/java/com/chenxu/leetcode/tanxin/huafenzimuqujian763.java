package com.chenxu.leetcode.tanxin;

import java.util.*;

public class huafenzimuqujian763 {
    public static void main(String[] args) {
        System.out.println(partitionLabels("adsadasd"));
        System.out.println(partitionLabels("ababcbacadefegdehijhklij"));
        System.out.println(partitionLabels("eccbbbbdec"));

    }


    public static  List<Integer> partitionLabels(String s) {
        List<Integer> res=new ArrayList<>();
        int[] locArr=new int[26];
        for(int i=0;i<s.length();i++){
            locArr[s.charAt(i)-'a']=Math.max(i,locArr[s.charAt(i)-'a']);
        }
        int start=0;int end=0;
        for (int i = 0; i < s.length(); i++) {
            end = Math.max(end, locArr[s.charAt(i) - 'a']);
            //当前字母在 s 中最后一次出现的下标等于 i，则说明片段 s[start..end] 可以被划分出来
            if (i == end) {
                res.add(end - start + 1);
                start = i + 1;
            }
        }
        return  res;
    }
}
