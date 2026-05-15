package com.chenxu.leetcode.huadongchuangkou;

import java.util.ArrayList;
import java.util.List;

public class shuzuduidapingjunshu643 {

    public static void main(String[] args) {
       // System.out.println(findMaxAverage(new int[]{1,12,-5,-6,50,3},4));
        System.out.println(findMaxAverage(new int[]{-1},1));

    }
    public static double findMaxAverage(int[] nums, int k) {//负数不对
        double res=0 ;double av = 0.0;
        int temp = 0;
        List<Integer> winList=new ArrayList<>();
        for(int i=0;i<nums.length;i++){
            winList.add(nums[i]);
            if(winList.size()>=k){
                for(int j=0;j<k;j++){
                    temp += winList.get(j);
                }
                System.out.println(temp);
                av=(double) temp/k;
                res=Math.max( res,av);
                temp=0;
                winList.remove(0);
            }
        }
        return  res;
    }

    public static double findMaxAverage1(int[] nums, int k) {
        int sum = 0;
        for (int i = 0; i < k; i++){ sum += nums[i];}
        int maxSum = sum;
        for (int i = k; i < nums.length; i++) {
            sum += nums[i] - nums[i - k];  // 滑动窗口
            maxSum = Math.max(maxSum, sum);
        }
        return maxSum * 1.0 / k;
    }
}
