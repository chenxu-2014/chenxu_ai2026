package com.chenxu.thread_cx;

public class maxArea11 {
    public static void main(String[] args) {
        int[] nums= {1,8,6,2,5,4,8,3,7};//4
        System.out.println(maxAreaCx(nums));
        System.out.println(maxArea(nums));
    }

    private static int maxArea(int[] height) {
        int i = 0, j = height.length - 1, res = 0;
        while(i < j) {
            res = height[i] < height[j] ?
                    Math.max(res, (j - i) * height[i++]):
                    Math.max(res, (j - i) * height[j--]);
        }
        return res;

    }

    /**
     * 会导致超时 上边还是牛的
     * @param height
     * @return
     */
    private static int maxAreaCx(int[] height) {
        int  maxArea=0;
        int tempArea=0;
        int tempHeight=0;
        if(1>height.length) return 0;
        for(int i=0;i<height.length-1;i++){
            for(int j=1;j<height.length;j++){
                tempHeight=height[i]>height[j]?height[j]:height[i];
                tempArea=tempHeight*(j-i);
                maxArea=maxArea>=tempArea?maxArea:tempArea;
            }
        }
        return maxArea;

    }
}
