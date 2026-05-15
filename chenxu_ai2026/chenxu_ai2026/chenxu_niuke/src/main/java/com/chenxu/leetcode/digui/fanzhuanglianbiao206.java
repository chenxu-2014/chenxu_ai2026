package com.chenxu.leetcode.digui;



public class fanzhuanglianbiao206 {
    //思路1————递归
    public static void main(String[] args) {
        ListNode listNode=new ListNode(1,new ListNode(2,new ListNode(3,new ListNode(4))));
        ListNode listNode1=new ListNode(1,new ListNode(2,new ListNode(3,new ListNode(4))));

        listNode= reverseList(listNode);
        listNode1= reverseList1(listNode1);
        System.out.println(listNode);
    }
     public static class ListNode {
          int val;
          ListNode next;
          ListNode(int x) { val = x; }

         ListNode(int val, ListNode next) { this.val = val; this.next = next; }
      }

    public static ListNode reverseList1(ListNode head) {
        ListNode cur = head, pre = null;
        while(cur != null) {
            ListNode tmp = cur.next;     // 暂存后继节点 cur.next
            cur.next = pre;          // 修改 next 引用指向
            pre = cur;               // pre 暂存 cur
            cur = tmp;               // cur 访问下一节点

        }
        return pre;

    }
        public static ListNode reverseList(ListNode head) {
            //递归终止条件
            if (head == null || head.next == null) {
                return head;
            }
	    /*
	    	以 head = [1, 2, 3, 4, 5] 为例
	    	(1) 先将以 head.next 为头节点的链表进行反转，得到 newHead = [5, 4, 3, 2]
			(2) 此时 head.next = 2
		*/
            ListNode newHead = reverseList(head.next);
	    /*
	    	以 head->p2->null 为例：
			经过 head.next.next = head 之后，得到 head->p2->head
			经过 head.next = null 之后，得到 p2->head->null
			这样便将 head 与 p2 进行了反转
		*/
            head.next.next = head;
            head.next = null;
            return newHead;
        }
    }


