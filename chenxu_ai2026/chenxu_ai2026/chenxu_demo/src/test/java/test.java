public class test {
    public static void main(String[] args) {
        int h = 0b1010_1100_0011_1001_1111_0000_0101_1010; // 随便写一个32位数
        int shifted = h >>> 16;
        int mixed = h ^ shifted;

        System.out.println("原始 h       : " + toBinary32(h));
        System.out.println("h >>> 16     : " + toBinary32(shifted));
        System.out.println("h ^ (h>>>16) : " + toBinary32(mixed));
        System.out.println("\n观察低16位：");
        System.out.println("原始低16位   : " + toBinary16(h));
        System.out.println("混合后低16位 : " + toBinary16(mixed));
    }

    static String toBinary32(int x) {
        return String.format("%32s", Integer.toBinaryString(x)).replace(' ', '0');
    }

    static String toBinary16(int x) {
        return String.format("%16s", Integer.toBinaryString(x & 0xFFFF)).replace(' ', '0');
    }
}