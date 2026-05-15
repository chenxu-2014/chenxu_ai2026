package com.chenxu.thread_cx;



public class lunzhuanshuzu189 {
    public static void main(String[] args) {
        int[] nums= {-1,-100,3,99};
       int[] numsZ= {1,2,3,4,5,6,7};
        System.out.println(rotate(nums,2)[1]);
        System.out.println(rotate(numsZ,3)[1]);
        System.out.println(nums);
    }

    private static int[] rotate(int[] nums, int k) {
        int l=nums.length;
        int nums_re[]=new int[nums.length];
        int n=0;
        for(int j=l-k;j<l;j++){
            nums_re[n]=nums[j];
            n++;
        }
        for(int i=0;i<l-k;i++){
            nums_re[n]=nums[i];
            n++;
        }
        System.arraycopy(nums_re,0,nums,0,l);
//        nums= Arrays.clone(nums_re);
//        nums= Arrays.copyOf(nums_re,l);s
        return nums;
    }
}
