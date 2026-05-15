package com.chenxu.leetcode.tanxin;

import java.util.Arrays;
import java.util.Collections;

public class wuchongdianqujian435 {
    public static void main(String[] args) {
       System.out.println(eraseOverlapIntervals(new int[][]{{1,2},{2,3},{3,4},{1,3}}));


    }
    public static int eraseOverlapIntervals(int[][] intervals) {
        Arrays.sort(intervals,(a,b)->{
            return Integer.compare(a[0],b[0]);
        });
        int count = 0; // 统计需要移除的个数
        for(int i = 0; i < intervals.length - 1; i++){
            if(intervals[i][1] > intervals[i+1][0]){
                // 代表存在重复
                count++;
                intervals[i+1][1] = Math.min(intervals[i][1], intervals[i+1][1]); // 修改下一段区间起点位置
            }
        }
        return count;
    }
}
