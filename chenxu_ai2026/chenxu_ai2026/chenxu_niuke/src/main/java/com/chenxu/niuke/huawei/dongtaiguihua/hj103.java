package com.chenxu.niuke.huawei.dongtaiguihua;

public class hj103 {
    public static void main(String[] args) {
        int n=6;
        int[] arr=new int[]{2,5,1,5,4,5};
        int max=0;
        int[] DP=new int[n];
        for(int i=0;i<n;i++){
            DP[i]=1;
            for(int j=0;j<i;j++){
                if(arr[i]>arr[j]){
                    DP[i]=Math.max(DP[i],DP[j]+1);
                }
            }
            max=Math.max(max,DP[i]);
        }
        System.out.println(max);
    }
}
