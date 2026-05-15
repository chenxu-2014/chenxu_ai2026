package com.chenxu.niuke.huawei;

public class zifuchuanjiajiemi29 {
    public static void main(String[] args) {
//        输入：
//        abcdefg1
//        0BCDEFGH
//        输出：
//        BCDEFGH2
//        9abcdefg
        String str1="abcdefg9";
        String str2="0BCDEFGH";
        for(char c:str1.toCharArray()){
            if(Character.isUpperCase(c)){
                if(c=='Z'){
                    System.out.print('a');
                }else{
                    System.out.print( (char)(c-31));
                }
            }
            if(Character.isLowerCase(c)){
                if(c=='z'){
                    System.out.print('A');
                }else{
                    System.out.print((char)(c-31));
                }
            }
             if (c >= '0' && c <= '9') {
                 // 数字 -> 下一个数字（9 -> 0）
                 System.out.print((char) ((c - '0' + 1) % 10 + '0'));
             }
        }
    }
}
