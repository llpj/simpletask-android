/**
 * This file is part of Simpletask.
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 */
package nl.mpcjanssen.simpletask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;


public class AddTask extends ThemedActivity {

    private final static String TAG = AddTask.class.getSimpleName();

    private List<Task> m_backup;
    private TodoApplication m_app;

    private String share_text;

    private EditText textInputField;
    private BroadcastReceiver m_broadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;

    public boolean hasWordWrap() {
        return ((CheckBox) findViewById(R.id.cb_wrap)).isChecked();
    }

    public void setWordWrap(boolean bool) {
        ((CheckBox) findViewById(R.id.cb_wrap)).setChecked(bool);
        if (textInputField!=null) {
            textInputField.setHorizontallyScrolling(!bool);
        }
    }

    public boolean hasCloneTags() {
        return ((CheckBox) findViewById(R.id.cb_clone)).isChecked();
    }

    public void setCloneTags(boolean bool) {
        ((CheckBox) findViewById(R.id.cb_clone)).setChecked(bool);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
         MenuInflater inflater = getMenuInflater();
        if (m_app.isDarkActionbar()) {
            inflater.inflate(R.menu.add_task, menu);
        } else {
            inflater.inflate(R.menu.add_task_light, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (m_app.isBackSaving()) {
                    saveTasksAndClose();
                }
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                finish();
                startActivity(upIntent);
                return true;
            case R.id.menu_save_task:
                saveTasksAndClose();
                return true;
            case R.id.menu_cancel_task:
                finish();
                return true;
            case R.id.menu_help:
                showHelp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelp() {
        Intent i = new Intent(this, HelpScreen.class);
        i.putExtra(Constants.EXTRA_HELP_PAGE,Constants.HELP_ADD_TASK);
        startActivity(i);
    }


    private void saveTasksAndClose() {
        // save clone CheckBox state
        m_app.setAddTagsCloneTags(hasCloneTags());
        m_app.setWordWrap(hasWordWrap());
        
        // strip line breaks
        textInputField = (EditText) findViewById(R.id.taskText);
        String input;
                if (textInputField!=null) {
                    input = textInputField.getText().toString();
                } else {
                    input = "";
                }

        // Don't add empty tasks
        if (input.trim().equals("")) {
             finish();
             return;
        }

        ArrayList<String> tasks = new ArrayList<String>();
        tasks.addAll(Arrays.asList(input.split("\\r\\n|\\r|\\n")));
        ArrayList<String> originalLines = new ArrayList<String>();
        ArrayList<Task> updatedTasks = new ArrayList<Task>();
        if (m_backup != null && tasks.size()>0) {
            int numTasks = tasks.size();
            int numBackup = m_backup.size();
            Log.v("...","tasks " + tasks.size() + "  backup " + m_backup.size());

            for (int i = 0 ; i < numTasks && i < numBackup  ; i++) {
                originalLines.add(m_backup.get(0).inFileFormat());
                m_backup.get(0).init(tasks.get(0),null);
                updatedTasks.add(m_backup.get(0));
                tasks.remove(0);
                m_backup.remove(0);
            }
        }
        // Append new tasks
        ArrayList<Task> addedTasks = new ArrayList<Task>();
        if (tasks.size()>0) {
            for (String task: tasks) {
                if (m_app.hasPrependDate()) {
                    addedTasks.add(new Task(0,task, DateTime.today(TimeZone.getDefault())));
                } else {
                    addedTasks.add(new Task(0,task,null));
                }
            }
        }
        m_app.getFileStore().modify(m_backup, updatedTasks, addedTasks, null);
        finish();
    }

    private void noteToSelf(@NotNull Intent intent) {
        String task = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            Log.v(TAG,"Voice note added.");
        }
        addBackgroundTask(task);
    }

    @Override
    public void onBackPressed() {
        if (m_app.isBackSaving()) {
            saveTasksAndClose();
        }
        super.onBackPressed();
    }

    private void addBackgroundTask(String sharedText) {
        ArrayList<Task> bgTask = new ArrayList<Task>();
        for (String taskText : sharedText.split("\n|\r\n")) {
            if (m_app.hasPrependDate()) {   
                bgTask.add(new Task(0,taskText, DateTime.today(TimeZone.getDefault())));
            } else {
                bgTask.add(new Task(0,taskText));
            }
        }
        m_app.getFileStore().modify(null,null,bgTask,null);
        m_app.updateWidgets();
        Util.showToastShort(m_app, R.string.task_added);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");

        m_app = (TodoApplication) getApplication();
        m_app.setActionBarStyle(getWindow());
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI);
        intentFilter.addAction(Constants.BROADCAST_SYNC_START);
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE);

        localBroadcastManager = m_app.getLocalBroadCastManager();

        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NotNull Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_SYNC_START)) {
                    setProgressBarIndeterminateVisibility(true);
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_DONE)) {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        };
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter);


        ActionBar actionBar = getActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        final Intent intent = getIntent();
        ActiveFilter mFilter = new ActiveFilter();
        mFilter.initFromIntent(intent);
        final String action = intent.getAction();
        // create shortcut and exit
        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Log.d(TAG, "Setting up shortcut icon");
            setupShortcut();
            finish();
            return;
        } else if (Intent.ACTION_SEND.equals(action)) {
            Log.d(TAG, "Share");
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                share_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString();
            } else {
                share_text = "";
            }
            if (!m_app.hasShareTaskShowsEdit()) {
                if (!share_text.equals("")) {
                    addBackgroundTask(share_text);
                }
                finish();
                return;
            }
        } else if ("com.google.android.gm.action.AUTO_SEND".equals(action)) {
            // Called as note to self from google search/now
            noteToSelf(intent);
            finish();
            return;
        } else if (Constants.INTENT_BACKGROUND_TASK.equals(action)) {
            Log.v(TAG, "Adding background task");
            if (intent.hasExtra(Constants.EXTRA_BACKGROUND_TASK)) {
                addBackgroundTask(intent.getStringExtra(Constants.EXTRA_BACKGROUND_TASK));
            } else {
                Log.w(TAG, "Task was not in extras");
            }
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.add_task);

        // text
        textInputField = (EditText) findViewById(R.id.taskText);
        m_app.setEditTextHint(textInputField, R.string.tasktexthint);

        if (share_text != null) {
            textInputField.setText(share_text);
        }

        Task iniTask = null;
        setTitle(R.string.addtask);

        // m_backup = m_app.getFileStore().getTasksToUpdate();
        if (m_backup!=null && m_backup.size()>0) {
            ArrayList<String> prefill = new ArrayList<String>();
            for (Task t : m_backup) {
                prefill.add(t.inFileFormat());
            }
            String sPrefill = Util.join(prefill,"\n");
            textInputField.setText(sPrefill);
            setTitle(R.string.updatetask);
        } else {
            if (textInputField.getText().length() == 0) {
                iniTask = new Task(1, "");
                iniTask.initWithFilter(mFilter);
            }

            if (iniTask != null && iniTask.getTags().size() == 1) {
                List<String> ps = iniTask.getTags();
                String project = ps.get(0);
                if (!project.equals("-")) {
                    textInputField.append(" +" + project);
                }
            }


            if (iniTask != null && iniTask.getLists().size() == 1) {
                List<String> cs = iniTask.getLists();
                String context = cs.get(0);
                if (!context.equals("-")) {
                    textInputField.append(" @" + context);
                }
            }
        }
        // Listen to enter events, use IME_ACTION_NEXT for soft keyboards
        // like Swype where ENTER keyCode is not generated.

        int inputFlags = InputType.TYPE_CLASS_TEXT;

        if (m_app.hasCapitalizeTasks()) {
            inputFlags |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }
        textInputField.setRawInputType(inputFlags);
        textInputField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        textInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, @Nullable KeyEvent keyEvent) {

                boolean hardwareEnterUp = keyEvent!=null &&
                        keyEvent.getAction() == KeyEvent.ACTION_UP &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean hardwareEnterDown = keyEvent!=null &&
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean imeActionNext = (actionId == EditorInfo.IME_ACTION_NEXT);

                if (imeActionNext || hardwareEnterUp ) {
                    // Move cursor to end of line
                    int position = textInputField.getSelectionStart();
                    String remainingText = textInputField.getText().toString().substring(position);
                    int endOfLineDistance = remainingText.indexOf('\n');
                    int endOfLine;
                    if (endOfLineDistance == -1) {
                        endOfLine = textInputField.length();
                    } else {
                        endOfLine = position + endOfLineDistance;
                    }
                    textInputField.setSelection(endOfLine);
                    replaceTextAtSelection("\n", false);

                    if (hasCloneTags()) {
                        String precedingText = textInputField.getText().toString().substring(0, endOfLine);
                        int lineStart = precedingText.lastIndexOf('\n');
                        String line;
                        if (lineStart != -1) {
                            line = precedingText.substring(lineStart, endOfLine);
                        } else {
                            line = precedingText;
                        }
                        Task t = new Task(0, line);
                        LinkedHashSet<String> tags = new LinkedHashSet<String>();
                        for (String ctx : t.getLists()) {
                            tags.add("@" + ctx);
                        }
                        for (String prj : t.getTags()) {
                            tags.add("+" + prj);
                        }
                        replaceTextAtSelection(Util.join(tags, " "), true);
                    } 
                    endOfLine++;
                    textInputField.setSelection(endOfLine);
                }
                return (imeActionNext || hardwareEnterDown || hardwareEnterUp);
            }
        });

        setCloneTags(m_app.isAddTagsCloneTags());
        setWordWrap(m_app.isWordWrap());
        
        findViewById(R.id.cb_wrap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWordWrap(hasWordWrap());
            }
        });

        int textIndex = 0;
        textInputField.setSelection(textIndex);

        // Set button callbacks
        findViewById(R.id.btnContext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContextMenu();
            }
        });
        findViewById(R.id.btnProject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagMenu();
            }
        });
        findViewById(R.id.btnPrio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrioMenu();
            }
        });

        findViewById(R.id.btnDue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDate(Task.DUE_DATE);
            }
        });
        findViewById(R.id.btnThreshold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDate(Task.THRESHOLD_DATE);
            }
        });

        if (m_backup!=null && m_backup.size()>0) {
            textInputField.setSelection(textInputField.getText().length());
        }
    }

    private void insertDate(final int dateType) {
        Dialog d = Util.createDeferDialog(this, dateType, false, new Util.InputDialogListener() {
            @Override
            public void onClick(@NotNull String selected) {
                if (selected.equals("pick")) {
                    /* Note on some Android versions the OnDateSetListener can fire twice
                     * https://code.google.com/p/android/issues/detail?id=34860
                     * With the current implementation which replaces the dates this is not an
                     * issue. The date is just replaced twice
                     */
                    final DateTime today = DateTime.today(TimeZone.getDefault());
                    DatePickerDialog dialog = new DatePickerDialog(AddTask.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            month++;

                            DateTime date = DateTime.forDateOnly(year,month,day);
                            insertDateAtSelection(dateType, date);
                        }
                    },
                            today.getYear(),
                            today.getMonth()-1,
                            today.getDay());

                    boolean showCalendar = m_app.showCalendar();

                    dialog.getDatePicker().setCalendarViewShown(showCalendar);
                    dialog.getDatePicker().setSpinnersShown(!showCalendar);
                    dialog.show();
                } else {
                    insertDateAtSelection(dateType, Util.addInterval(DateTime.today(TimeZone.getDefault()), selected));
                }
            }
        });
        d.show();
    }

    private void replaceDate(int dateType, @NotNull String date) {
           if (dateType==Task.DUE_DATE) {
               replaceDueDate(date);
           } else {
               replaceThresholdDate(date);
           }
    }

    private void insertDateAtSelection(int dateType, @NotNull DateTime date) {
        replaceDate(dateType, date.format("YYYY-MM-DD"));
    }

    private void showTagMenu() {
        Set<String> items = new TreeSet<String>();
        items.addAll(m_app.getFileStore().getProjects());
        // Also display contexts in tasks being added
        Task t = new Task(0,textInputField.getText().toString());
        items.addAll(t.getTags());
        final ArrayList<String> projects = Util.sortWithPrefix(items, m_app.sortCaseSensitive(),null);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        builder.setView(view);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        final EditText ed = (EditText) view.findViewById(R.id.editText);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
                projects.toArray(new String[projects.size()])));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        m_app.setEditTextHint(ed,R.string.new_tag_name);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<String>();
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.equals("")) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    replaceTextAtSelection("+" + item + " ", true);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.project_prompt);
        dialog.show();
    }

    private void showPrioMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Priority[] priorities = Priority.values();
        ArrayList<String> priorityCodes = new ArrayList<String>();

        for (Priority prio : priorities) {
            priorityCodes.add(prio.getCode());
        }

        builder.setItems(priorityCodes.toArray(new String[priorityCodes.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replacePriority(priorities[which].getCode());
                    }
                });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.priority_prompt);
        dialog.show();
    }


    private void showContextMenu() {
        Set<String> items = new TreeSet<String>();
        items.addAll(m_app.getFileStore().getContexts());
        // Also display contexts in tasks being added
        Task t = new Task(0,textInputField.getText().toString());
        items.addAll(t.getLists());
        final ArrayList<String> contexts = Util.sortWithPrefix(items, m_app.sortCaseSensitive(),null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        builder.setView(view);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        final EditText ed = (EditText) view.findViewById(R.id.editText);
        String [] choices = contexts.toArray(new String[contexts.size()]);
                lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
                choices ));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        m_app.setEditTextHint(ed,R.string.new_list_name);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<String>();
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.equals("")) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    replaceTextAtSelection("@" + item + " ", true);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.context_prompt);
        dialog.show();
    }

    public int getCurrentCursorLine(@NotNull EditText editText) {
        int selectionStart = Selection.getSelectionStart(editText.getText());
        Layout layout = editText.getLayout();

        if (selectionStart != -1) {
            return layout.getLineForOffset(selectionStart);
        }

        return -1;
    }

    private void replaceDueDate(@NotNull CharSequence newDueDate) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<String>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(0, lines.get(currentLine));
            t.setDueDate(newDueDate.toString());
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
        // restore selection
        int newLength = textInputField.getText().length();
        sizeDelta = newLength - length;
        int newStart = Math.max(0, start + sizeDelta);
        int newEnd = Math.min(end + sizeDelta, newLength);
        newEnd = Math.max(newStart, newEnd);
        textInputField.setSelection(newStart, newEnd);
    }

    private void replaceThresholdDate(@NotNull CharSequence newThresholdDate) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<String>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(0, lines.get(currentLine));
            t.setThresholdDate(newThresholdDate.toString());
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
        // restore selection
        int newLength = textInputField.getText().length();
        sizeDelta = newLength - length;
        int newStart = Math.max(0, start + sizeDelta);
        int newEnd = Math.min(end + sizeDelta, newLength);
        newEnd = Math.max(newStart, newEnd);
        textInputField.setSelection(start, end);
    }

    private void replacePriority(@NotNull CharSequence newPrio) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        Log.v(TAG, "Current selection: " + start + "-" + end);
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<String>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(0, lines.get(currentLine));
            Log.v(TAG,"Changing prio from " + t.getPriority().toString() + " to " + newPrio.toString()); 
            t.setPriority(Priority.toPriority(newPrio.toString()));
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
        // restore selection
        int newLength = textInputField.getText().length();
        sizeDelta = newLength - length;
        int newStart = Math.max(0, start + sizeDelta);
        int newEnd = Math.min(end + sizeDelta, newLength);
        newEnd = Math.max(newStart, newEnd);
        Log.v(TAG, "New selection (" + sizeDelta + "): " + newStart + "-" + newEnd);
        textInputField.setSelection(newStart, newEnd);

    }

    private void replaceTextAtSelection(CharSequence title, boolean spaces) {
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        if (start == end && start != 0 && spaces) {
            // no selection prefix with space if needed
            if (!(textInputField.getText().charAt(start - 1) == ' ')) {
                title = " " + title;
            }
        }
        textInputField.getText().replace(Math.min(start, end), Math.max(start, end),
                title, 0, title.length());
    }

    private void setupShortcut() {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this, this.getClass().getName());

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getString(R.string.shortcut_addtask_name));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
    }

    public void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver);
    }
}
