public class Test {

    static boolean Palindrome(String s) {

        String clean_s = "";
        String[] p = s.split(" ");
        for (int i = 0; i < p.length; i++) {
            clean_s += p[i].toLowerCase();
        }

        int l = 0;
        int r = clean_s.length() - 1;

        while (l < r) {
            if (clean_s.charAt(l) != clean_s.charAt(r)) {
                return false;
            }
            l++;
            r--;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(Palindrome("Rise to vote, sir"));
    }
}
