package com.chenxu.leetcode;


import java.util.HashSet;

public class singleNumber136 {
    public static void main(String[] args) {
        int[] nums= {1,1,2,2,4,4,3};
        System.out.println(singleNumberGf(nums));
        System.out.println(singleNumberCx(nums));
    }

    /**
     * 答案是使用位运算。对于这道题，可使用异或运算 ⊕。异或运算有以下三个性质。
     *
     * 任何数和 0 做异或运算，结果仍然是原来的数，即 a⊕0=a。
     * 任何数和其自身做异或运算，结果是 0，即 a⊕a=0。
     * 异或运算满足交换律和结合律，即 a⊕b⊕a=b⊕a⊕a=b⊕(a⊕a)=b⊕0=b。
     * @param nums
     * @return
     */
    private static int singleNumberGf(int[] nums) {
        int single = 0;
        for (int num : nums) {
            single ^= num;
        }
        return single;


    }

    private static int singleNumberCx(int[] nums) {
        if(nums.length==1){return nums[0];}
        HashSet<Integer> set=new HashSet<>();
        for(int num:nums){
            if(!set.add(num)){
                set.remove(num);
            }
        }
        return set.iterator().next();
    }
}
