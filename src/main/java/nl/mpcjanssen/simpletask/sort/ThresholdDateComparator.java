package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;

import nl.mpcjanssen.simpletask.task.Task;

public class ThresholdDateComparator extends Ordering<Task> {

    @Override
    public int compare(@NotNull Task a, @NotNull Task b) {
        int result;
        if (a.getThresholdDate() == null && b.getThresholdDate() == null) {
            result = 0;
        } else if (a.getThresholdDate() == null) {
            result = 1;
        } else if (b.getThresholdDate() == null) {
            result = -1;
        } else {
            result = a.getThresholdDate().compareTo(b.getThresholdDate());
        }
        return result;
    }
}
