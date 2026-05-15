package com.chenxu.niuke.huawei;

import java.util.*;

public class zifuchuanjiami36 {
    public static void main(String[] args) {
        String str1="trailblazers";
        String str2="attackatdawn";
        System.out.println(strEncode(str1,str2));
    }

    private static String strEncode(String str1, String str2) {
        Set<Character> set=new HashSet<>();
        StringBuffer sb=new StringBuffer();
        StringBuffer res=new StringBuffer();
        for(char c:str1.toCharArray()){
            if(set.add(c)){
                sb.append(c);
            }
        }
        for(int i=0;i<26;i++){
            if(set.add((char)('a'+i))){
                sb.append((char)('a'+i));
            }
        }
        char[] let=new char[]{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
        Map<Character,Character> map=new HashMap<>();
        int i=0;
        for (char c:sb.toString().toCharArray() ) {
            map.put(let[i++],c);
        }
        for(char c:str2.toCharArray()){
            res.append(map.get(c));
        }
        return res.toString();
    }
}
