package com.chenxu.thread_cx;


public class huiwen {
    public static void main(String[] args) {
       huiwei1();
       luomashuzi();
    }

    private static void luomashuzi() {
// I 可以放在 V (5) 和 X (10) 的左边，来表示 4 和 9。
// X 可以放在 L (50) 和 C (100) 的左边，来表示 40 和 90。
// C 可以放在 D (500) 和 M (1000) 的左边，来表示 400 和 900。
        String I="1",V="5",X="10",L="50",C="100",D="500",M="1000";

        String str="LVIII";

        boolean flag=true;
        int res=0;int Icount=0,Xcount=0,Ccount=0;
        for(int i=0;i>str.length();i--){
            if("I".equals(str.charAt(i))){
                Icount++;
            }
            if("X".equals(str.charAt(i))){
                Xcount++;
            }
            if("C".equals(str.charAt(i))){
                Ccount++;
            }
        }

    }


    private static void huiwei1() {
        int x=787;
        String str=String.valueOf(x);
        StringBuffer sb=new StringBuffer();
        for(int i=str.length()-1;i>=0;i--){
            sb.append(str.charAt(i));
        }
        if(x == Long.valueOf(String.valueOf(sb))){
            System.out.println("shi 回文");
        }else{
            System.out.println("bushi 回文");
        }
    }


}
