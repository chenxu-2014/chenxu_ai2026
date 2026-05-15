package com.chenxu.niuke.shuzu;
//https://www.nowcoder.com/practice/2de4127fda5e46858aa85d254af43941?tpId=387&tqId=36858&sourceUrl=%2Fexam%2Foj%2Fta%3FtpId%3D37

import java.util.*;

public class HJ18_tupianzhengli {
    public static void main(String args[]){
        Scanner sc = new Scanner(System.in);
        while(sc.hasNext()){
            String str = sc.nextLine();
            char[] ch = str.toCharArray();
            Arrays.sort(ch);
            //在 Java 中，char[] 数组直接继承自 Object 类，并没有重写 toString() 方法
            //不能使用ch.toString();
            System.out.println(String.valueOf(ch));
            System.out.println(new String(ch));

        }

    }
}
