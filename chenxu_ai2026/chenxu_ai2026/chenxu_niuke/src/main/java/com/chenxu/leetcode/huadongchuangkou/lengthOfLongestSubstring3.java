package com.chenxu.leetcode.huadongchuangkou;

import java.util.HashSet;
import java.util.Set;

public class lengthOfLongestSubstring3 {
    public static void main(String[] args) {
        System.out.println(lengthOfLongestSubstring("pwwkew"));
        System.out.println(lengthOfLongestSubstringLC("pwwkew"));
    }

    private static int lengthOfLongestSubstringLC(String s) {
            //        模板：
            ////外层循环扩展右边界，内层循环扩展左边界
            //        for (int l = 0, r = 0 ; r < n ; r++) {
            //            //当前考虑的元素
            //            while (l <= r && check()) {//区间[left,right]不符合题意
            //                //扩展左边界
            //            }
            //            //区间[left,right]符合题意，统计相关信息
            //        }
            //本题：
            //滑动窗口
            char[] ss = s.toCharArray();
            Set<Character> set = new HashSet<>();//去重
            int res = 0;//结果
            for(int left = 0, right = 0; right < s.length(); right++) {//每一轮右端点都扩一个。
                char ch = ss[right];//right指向的元素，也是当前要考虑的元素
                while(set.contains(ch)) {//set中有ch，则缩短左边界，同时从set集合出元素
                    set.remove(ss[left]);
                    left++;
                }
                set.add(ss[right]);//别忘。将当前元素加入。
                res = Math.max(res, right - left + 1);//计算当前不重复子串的长度。
            }
            return res;

    }

    /**
     * cx
     * @param s
     * @return
     */
    private static Integer lengthOfLongestSubstring(String s) {
        if(0==s.length()){return 0;}
        int max=1,temp=1;
        boolean flag=false;
        HashSet<Character> charSet=new HashSet<>();
        for(int i=0;i<s.length();i++){
            charSet.clear();
            temp=1;//每次进循环都是新开始
            char a=s.charAt(i);
            charSet.add(a);
            for(int j=i+1;j<s.length();j++){
                if(charSet.add(s.charAt(j))){//不存在 数据+1 已存在则跳出循环
                    temp++;
                }else{
                    break;
                }
                //max=temp>max?temp:max;
                max=Math.max(temp,max);
            }

        }

        return max;
    }
}
