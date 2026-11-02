import java.util.HashSet;
import java.util.Set;

public class test {
    public static void main(String[] args) {
        System.out.println(minSubArrayLen(3, new int[]{1,2,2,3,4}));
    }
    public static int minSubArrayLen(int target, int[] nums) {
        int left = 0;
        int right = 0;
        int result = 0;
        Set<Integer> set = new HashSet<>();
        while (right < nums.length) {
            if (set.contains(nums[right])) {
                while (nums[left]!=nums[right]) {
                    set.remove(nums[left]);
                    left++;
                }
                left++;
            }
            set.add(nums[right]);
            if (right-left+1>=target&&right-left+1>result) {
                result = right-left+1;
            }
            right++;
        }
        return result;
    }
}
