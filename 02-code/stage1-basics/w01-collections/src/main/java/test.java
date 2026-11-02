import java.util.HashSet;
import java.util.Set;

public class test {
    public static void main(String[] args) {
        String s = "abcabcbb";
        char[] cs = s.toCharArray();
        Set<Character> set = new HashSet<>();
        int len = 0;
        int left = 0;
        int right = 0;
        for (; right < cs.length; right++) {
            char c = cs[right];
            if (set.contains(c)) {
                char c1 = cs[left];
                while(c == c1){
                    c1 = cs[left++];
                }
                set.remove(c);
                if (len<right-left) {
                    len = right-left;
                }
            } else {
                set.add(c);
                right++;
            }
        }
        System.out.println(len);
    }
}
