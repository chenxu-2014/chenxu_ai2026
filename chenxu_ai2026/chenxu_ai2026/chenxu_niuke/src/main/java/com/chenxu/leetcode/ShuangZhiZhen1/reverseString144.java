package com.chenxu.leetcode.ShuangZhiZhen1;

//编写一个函数，其作用是将输入的字符串反转过来。输入字符串以字符数组 s 的形式给出。
//
//不要给另外的数组分配额外的空间，你必须原地修改输入数组、使用 O(1) 的额外空间解决这一问题。
//
//
//
//示例 1：
//
//输入：s = ["h","e","l","l","o"]
//输出：["o","l","l","e","h"]
//示例 2：
//
//输入：s = ["H","a","n","n","a","h"]
//输出：["h","a","n","n","a","H"]
public class reverseString144 {
    public static void main(String[] args) {
        Character[] strArr= {'h','e','l','l','o'};
        System.out.println(reverseString(strArr));
        System.out.println(reverseStringleet(strArr));
    }

    private static Character[] reverseString(Character[] strArr) {
        Character[] resArr=new Character[strArr.length];
        int l = 0, r = strArr.length - 1;
        while (l <= strArr.length - 1 ) {
            char tmp=strArr[r];
            resArr[l]=tmp;
            l++;
            r--;
        }
        return resArr;
    }

    private static Character[] reverseStringleet(Character[] strArr) {
        int l = 0, r = strArr.length - 1;
        while (l < r ) {
            char tmp = strArr[l];
            strArr[l] = strArr[r];
            strArr[r] = tmp;
            l++;
            r--;
        }
        return strArr;
    }
}
