package pkg;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class WithDependency {
    public List<String> getList() {
        return ImmutableList.of("a", "b", "c");
    }
}
