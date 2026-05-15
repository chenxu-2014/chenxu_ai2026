package com.chenxu.leetcode.digui;

public class rotateRight61 {
    public static void main(String[] args) {
        ListNode head=new ListNode(1);
        ListNode head2=new ListNode(2);
        ListNode head3=new ListNode(3);
        ListNode head4=new ListNode(4);
        ListNode head5=new ListNode(5);
        head.next=head2;
        head2.next=head3;
        head3.next=head4;
        head4.next=head5;
        ListNode reshead=rotateRight(head,2);
        System.out.println(reshead);

    }
    public static ListNode rotateRight(ListNode head, int k) {
        // 边界情况：空链表或只有一个节点，旋转后结果不变
        if (head == null || head.next == null) {
            return head;
        }

        // 1. 计算链表长度并找到尾节点
        ListNode tail = head;
        int len = 1;  // 长度初始为1，因为head不为空
        while (tail.next != null) {
            tail = tail.next;
            len++;
        }

        // 2. 实际需要移动的步数（取模）
        k = k % len;
        // 如果k为0，说明移动后链表不变，直接返回原头
        if (k == 0) {
            return head;
        }
        tail.next=head;
        ListNode resH = head;
        for(int i=1;i<len-k;i++){
            resH=resH.next;
        }

        // 5. 新头节点就是新尾节点的下一个节点
        ListNode newHead = resH.next;

        // 6. 断开环，新尾节点成为真正的尾节点
        resH.next = null;

        return newHead;



    }
    public static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }
}
