package com.chenxu.niuke.huawei;

import java.util.*;

public class testN {
    public static void main(String[] args) {

        String str="bac";
        Set<Character> resSet=new LinkedHashSet<Character>();
        for(char c:str.toCharArray()){
            if(!resSet.add(c)){
                resSet.remove(c);
            }
        }
        if(resSet.isEmpty()){
            System.out.println(-1);
        }else{
            System.out.println(resSet.toArray()[0]);
        }
    }

}
