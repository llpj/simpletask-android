package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class FilterListFragment extends Fragment {

    final static String TAG = FilterListFragment.class.getSimpleName();
    private ListView lv;
    private Switch invertSwitch;
    private Switch orSwitch;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;
    private ArrayList<String> selectedItems;
    private boolean not;
    private boolean isOr;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState() this:" + this);
        outState.putStringArrayList("selectedItems", getSelectedItems());
        outState.putBoolean("not", getNot());
        outState.putBoolean("isOr", getOr());

    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        ArrayList<String> items = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS);
        actionbar = getActivity().getActionBar();

        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getStringArrayList("selectedItems");
            not = savedInstanceState.getBoolean("not");
	    isOr = savedInstanceState.getBoolean("isOr");
        } else {
            selectedItems = arguments.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS);
            not = arguments.getBoolean(FilterActivity.INITIAL_NOT);
            isOr = arguments.getBoolean(FilterActivity.INITIAL_OR);	    
        }

        Log.v(TAG, "Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.multi_filter,
                container, false);

        invertSwitch = (Switch) layout.findViewById(R.id.invertSwitch);
        orSwitch = (Switch) layout.findViewById(R.id.orSwitch);

        lv = (ListView) layout.findViewById(R.id.listview);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        lv.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.simple_list_item_multiple_choice, items));

        for (int i = 0; i < items.size(); i++) {
            if (selectedItems!=null && selectedItems.contains(items.get(i))) {
                lv.setItemChecked(i, true);
            }
        }

        invertSwitch.setChecked(not);
	orSwitch.setChecked(isOr);

        gestureDetector = new GestureDetector(TodoApplication.getAppContext(),
                new FilterGestureDetector());
        OnTouchListener gestureListener = new OnTouchListener() {
            @Override
            public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    return true;
                }
                return false;
            }
        };

        lv.setOnTouchListener(gestureListener);
        return layout;
    }

    public boolean getNot() {
        if (invertSwitch == null) {
            return not;
        } else {
            return invertSwitch.isChecked();
        }
    }

    public boolean getOr() {
        if (orSwitch == null) {
            return isOr;
        } else {
            return orSwitch.isChecked();
        }
    }

    public ArrayList<String> getSelectedItems() {

        ArrayList<String> arr = new ArrayList<String>();
        if (lv == null) {
            // Tab was not displayed so no selections were changed
            return selectedItems;
        }
        int size = lv.getCount();
        for (int i = 0; i < size; i++) {
            if (lv.isItemChecked(i)) {
                arr.add((String) lv.getAdapter().getItem(i));
            }
        }
        return arr;
    }

    class FilterGestureDetector extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(@NotNull MotionEvent e1, @NotNull MotionEvent e2, float velocityX,
                               float velocityY) {

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            int index = actionbar.getSelectedNavigationIndex();
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Log.v(TAG, "Fling left");
                if (index < actionbar.getTabCount() - 1)
                    index++;
                actionbar.setSelectedNavigationItem(index);
                return true;
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // left to right swipe
                Log.v(TAG, "Fling right");
                if (index > 0)
                    index--;
                actionbar.setSelectedNavigationItem(index);
                return true;
            }
            return false;
        }
    }
}
