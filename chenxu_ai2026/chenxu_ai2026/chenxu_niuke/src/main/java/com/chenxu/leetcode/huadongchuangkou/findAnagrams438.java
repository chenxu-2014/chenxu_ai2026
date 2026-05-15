package com.chenxu.leetcode.huadongchuangkou;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//https://leetcode.cn/problems/find-all-anagrams-in-a-string/description/
//示例 1:
//
//输入: s = "cbaebabacd", p = "abc"
//输出: [0,6]
//解释:
//起始索引等于 0 的子串是 "cba", 它是 "abc" 的异位词。
//起始索引等于 6 的子串是 "bac", 它是 "abc" 的异位词。
public class findAnagrams438 {
    public static void main(String[] args) {
       System.out.println(findAnagramsCx("abab","ab"));
       System.out.println(findAnagrams("babchdjcbadd","abc"));

    }



    private static List<Integer> findAnagrams(String s, String p) {
        List ans=new ArrayList();

        int sLen=s.length();
        int pLen=p.length();

        if(sLen<pLen){
            return ans;
        }
        //建立两个数组存放字符串中字母出现的词频，并以此作为标准比较
        int[] scount=new int[26];
        int[] pcount=new int[26];

        //当滑动窗口的首位在s[0]处时 （相当于放置滑动窗口进入数组）
        for(int i=0;i<pLen;i++){
            ++scount[s.charAt(i)-'a']; //记录s中前pLen个字母的词频
            ++pcount[p.charAt(i)-'a']; //记录要寻找的字符串中每个字母的词频(只用进行一次来确定)
        }

        //判断放置处是否有异位词     (在放置时只需判断一次)
        if(Arrays.equals(scount,pcount)){
            ans.add(0);
        }

        //开始让窗口进行滑动
        for(int i=0;i<sLen-pLen;i++){ //i是滑动前的首位
            --scount[s.charAt(i) -'a'];       //将滑动前首位的词频删去
            ++scount[s.charAt(i+pLen) -'a'];  //增加滑动后最后一位的词频（以此达到滑动的效果）

            //判断滑动后处，是否有异位词
            if(Arrays.equals(scount,pcount)){
                ans.add(i+1);
            }
        }

        return ans;
    }

    /**
     * 超出时间限制 cx
     * @param s
     * @return
     */
    private static List<Integer> findAnagramsCx(String s, String p) {
        List<Integer> list=new ArrayList<>();
        char[] sArray=s.toCharArray();
        char[] pArray=p.toCharArray();
        Arrays.sort(pArray);
        int pl=pArray.length;
        //外层循环扩展右边界，内层循环扩展左边界
        int sl=sArray.length;
                for (int l = 0, r = 0 ; r <= sl-pl ; r++) {
                    //当前考虑的元素
                    if (checkRule(l,pl,sArray,pArray)) {//区间[left,right]不符合题意
                        list.add(r);
                        l++;
                    }else {
                        l++;
                    }
                    //区间[left,right]符合题意，统计相关信息

                }
        return list;
    }

    private static boolean checkRule(int l, int pl, char[] sArray, char[] pArray) {
        String s = "";
        for(int i=0;i<pl;i++){
            s+=sArray[l+i];
        }
        char[] sChar=s.toCharArray();Arrays.sort(sChar);
        Arrays.sort(s.toCharArray());
        if(String.valueOf(sChar).equals(String.valueOf(pArray))){
            return true;
        }
        return false;
    }
}
