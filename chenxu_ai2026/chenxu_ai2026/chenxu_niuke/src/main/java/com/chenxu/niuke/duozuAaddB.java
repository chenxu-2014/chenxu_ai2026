package com.chenxu.niuke;

import java.util.*;

public class duozuAaddB {

        public static void main(String[] args) {
            Scanner in = new Scanner(System.in);
            int n=in.nextInt();

            int count=0;
            for(int i=0;i<=n;i++){
                if(i%7 == 0 || contain(String.valueOf(i))){
                    count++;
                }
            }
            System.out.println(count);
        }


    private static boolean contain(String str) {
            boolean flag=false;
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)=='7'){
                    flag=true;
                }
            }
            return flag;

    }

}
