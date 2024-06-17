package com.cookandroid.todocalnote;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI 요소 선언: CalendarView, TextView 2개, ListView, Button 3개
    private CalendarView calendarView;
    private TextView tvSelectedDate, tvDateDiff;
    private ListView lvMemoList;
    private Button btnAddMemo, btnDeleteSelectedMemos, btnFilter;

    // 메모 데이터를 저장할 ArrayList와 어댑터
    private ArrayList<Memo> memoList;
    private MemoAdapter memoAdapter;

    // 선택된 날짜를 저장할 변수
    private String selectedDate;

    // 날짜 형식을 지정하는 SimpleDateFormat 객체
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

    // 데이터베이스 헬퍼 객체
    private MemoDatabaseHelper dbHelper;

    // 로그 태그
    private static final String TAG = "MainActivity";

    // 필터 타입 열거형
    private enum FilterType {
        ALL, DEADLINE_ONLY, TODAY_ONLY
    }

    // 현재 적용된 필터를 저장하는 변수
    private FilterType currentFilter = FilterType.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 초기화
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tv_selectedDate);
        tvDateDiff = findViewById(R.id.tv_dateDiff);
        lvMemoList = findViewById(R.id.lv_memoList);
        btnAddMemo = findViewById(R.id.btn_addMemo);
        btnDeleteSelectedMemos = findViewById(R.id.btn_deleteSelectedMemos);
        btnFilter = findViewById(R.id.btn_filter);

        // 데이터베이스 헬퍼 객체 초기화
        dbHelper = new MemoDatabaseHelper(this);

        // 메모 리스트와 어댑터 초기화
        memoList = new ArrayList<>();
        memoAdapter = new MemoAdapter();
        lvMemoList.setAdapter(memoAdapter);

        // 현재 날짜를 기본 선택 날짜로 설정하고 텍스트뷰에 표시
        Calendar calendar = Calendar.getInstance();
        selectedDate = sdf.format(calendar.getTime());
        tvSelectedDate.setText(selectedDate + " (" + dayFormat.format(calendar.getTime()) + ")");
        tvDateDiff.setText("오늘");

        // 메모를 로드하여 화면에 표시
        loadMemos();

        // 캘린더 날짜 변경 리스너 설정
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // 선택된 날짜를 캘린더에서 가져와서 포맷팅하여 변수에 저장하고 텍스트뷰에 표시
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(selectedCalendar.getTime());
            tvSelectedDate.setText(selectedDate + " (" + dayFormat.format(selectedCalendar.getTime()) + ")");
            // 선택된 날짜와 현재 날짜의 차이를 계산하여 텍스트뷰에 표시
            updateDateDiff(selectedCalendar);
            // 선택된 날짜에 해당하는 메모를 로드하여 화면에 표시
            loadMemos();
        });

        // 메모 추가 버튼 클릭 리스너 설정
        btnAddMemo.setOnClickListener(v -> {
            // 메모 추가 화면(MemoActivity)으로 이동
            Intent intent = new Intent(MainActivity.this, MemoActivity.class);
            intent.putExtra("date", selectedDate);
            startActivity(intent);
        });

        // 메모 리스트 아이템 클릭 리스너 설정
        lvMemoList.setOnItemClickListener((parent, view, position, id) -> {
            // 클릭한 메모의 ID를 가져와서 메모 수정 화면(MemoActivity)으로 이동
            Memo memo = memoList.get(position);
            Intent intent = new Intent(MainActivity.this, MemoActivity.class);
            intent.putExtra("date", selectedDate);
            intent.putExtra("memoId", memo.getId());
            startActivity(intent);
        });

        // 선택된 메모 삭제 버튼 클릭 리스너 설정
        btnDeleteSelectedMemos.setOnClickListener(v -> deleteSelectedMemos());

        // 필터 버튼 클릭 리스너 설정
        btnFilter.setOnClickListener(v -> toggleFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때 메모를 로드하여 화면에 표시
        loadMemos();
    }

    // 메모 로드 메서드
    private void loadMemos() {
        // 메모 리스트 초기화
        memoList.clear();

        // 읽기 가능한 데이터베이스 객체를 가져옴
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 선택된 날짜 또는 마감일이 있는 메모를 쿼리
        Cursor cursor = dbHelper.getMemosByDateOrDeadline(selectedDate);

        // 컬럼 인덱스를 가져옴
        int idIndex = cursor.getColumnIndex("id");
        int titleIndex = cursor.getColumnIndex("title");
        int dateIndex = cursor.getColumnIndex("date");
        int deadlineIndex = cursor.getColumnIndex("deadline");

        // 커서를 통해 메모 데이터를 메모 리스트에 추가
        if (idIndex != -1 && titleIndex != -1 && dateIndex != -1 && deadlineIndex != -1) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(idIndex);
                String title = cursor.getString(titleIndex);
                String date = cursor.getString(dateIndex);
                String deadline = cursor.getString(deadlineIndex);
                Log.d(TAG, "Loaded memo: id=" + id + ", title=" + title + ", date=" + date + ", deadline=" + deadline);
                memoList.add(new Memo(id, title, date, deadline));
            }
        }

        // 커서와 데이터베이스 객체를 닫음
        cursor.close();
        db.close();

        // 필터를 적용하여 메모 리스트를 업데이트
        applyFilter();
    }

    // 필터 적용 메서드
    private void applyFilter() {
        // 필터링된 메모 리스트를 초기화
        ArrayList<Memo> filteredList = new ArrayList<>();
        for (Memo memo : memoList) {
            // 필터 타입에 따라 메모를 필터링하여 추가
            switch (currentFilter) {
                case DEADLINE_ONLY:
                    if (memo.getDeadline() != null && !memo.getDeadline().isEmpty()) {
                        filteredList.add(memo);
                    }
                    break;
                case TODAY_ONLY:
                    if (memo.getDeadline() == null || memo.getDeadline().isEmpty()) {
                        filteredList.add(memo);
                    }
                    break;
                case ALL:
                default:
                    filteredList.add(memo);
                    break;
            }
        }

        // 정렬: 1. 마감일과 가까울수록 상위에 정렬 2. 일반 메모는 최초 저장순으로 표시
        Collections.sort(filteredList, new Comparator<Memo>() {
            @Override
            public int compare(Memo m1, Memo m2) {
                if (m1.getDeadline() != null && !m1.getDeadline().isEmpty() &&
                        m2.getDeadline() != null && !m2.getDeadline().isEmpty()) {
                    return m1.getDeadline().compareTo(m2.getDeadline());
                } else if (m1.getDeadline() != null && !m1.getDeadline().isEmpty()) {
                    return -1;
                } else if (m2.getDeadline() != null && !m2.getDeadline().isEmpty()) {
                    return 1;
                } else {
                    return m1.getDate().compareTo(m2.getDate());
                }
            }
        });

        // 어댑터를 통해 필터링된 메모 리스트를 업데이트하고 화면에 반영
        memoAdapter.updateMemos(filteredList);
    }

    // 날짜 차이 업데이트 메서드
    private void updateDateDiff(Calendar selectedCalendar) {
        // 오늘 날짜와 선택된 날짜를 비교하여 차이를 계산
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
        selectedCalendar.set(Calendar.MINUTE, 0);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);

        long diff = selectedCalendar.getTimeInMillis() - today.getTimeInMillis();
        long daysDiff = diff / (24 * 60 * 60 * 1000);

        // 날짜 차이에 따라 텍스트뷰에 표시
        if (daysDiff == 0) {
            tvDateDiff.setText("오늘");
        } else if (daysDiff == 1) {
            tvDateDiff.setText("내일");
        } else if (daysDiff == -1) {
            tvDateDiff.setText("어제");
        } else if (daysDiff > 1) {
            tvDateDiff.setText(daysDiff + "일 후");
        } else {
            tvDateDiff.setText(Math.abs(daysDiff) + "일 전");
        }
    }

    // 필터 토글 메서드
    private void toggleFilter() {
        // 필터 타입을 순차적으로 변경
        switch (currentFilter) {
            case ALL:
                currentFilter = FilterType.DEADLINE_ONLY;
                btnFilter.setText("오늘만");
                break;
            case DEADLINE_ONLY:
                currentFilter = FilterType.TODAY_ONLY;
                btnFilter.setText("전체");
                break;
            case TODAY_ONLY:
                currentFilter = FilterType.ALL;
                btnFilter.setText("마감만");
                break;
        }
        // 필터를 적용하여 메모 리스트를 업데이트
        applyFilter();
    }

    private class MemoAdapter extends BaseAdapter {

        private ArrayList<Memo> filteredMemos;

        public MemoAdapter() {
            filteredMemos = new ArrayList<>();
        }

        // 필터링된 메모 리스트를 업데이트하는 메서드
        public void updateMemos(ArrayList<Memo> newMemos) {
            filteredMemos.clear();
            filteredMemos.addAll(newMemos);
            notifyDataSetChanged(); // 어댑터에 데이터 변경을 알림
        }

        @Override
        public int getCount() {
            return filteredMemos.size(); // 필터링된 메모 리스트의 크기를 반환
        }

        @Override
        public Object getItem(int position) {
            return filteredMemos.get(position); // 특정 위치의 메모를 반환
        }

        @Override
        public long getItemId(int position) {
            return position; // 메모의 위치를 ID로 반환
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // 레이아웃을 인플레이트하여 뷰를 생성
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_memo, parent, false);
            }

            // 메모 아이템의 UI 요소를 초기화
            TextView tvMemoTitle = convertView.findViewById(R.id.tv_memoTitle);
            Button btnMemoDeadline = convertView.findViewById(R.id.btn_memoDeadline);
            CheckBox cbMemo = convertView.findViewById(R.id.cb_memo);

            // 현재 위치의 메모를 가져옴
            final Memo memo = filteredMemos.get(position);
            tvMemoTitle.setText(memo.getTitle()); // 메모 제목 설정

            // 마감일이 있는 경우 마감일을 표시, 그렇지 않으면 "마감 설정" 버튼 텍스트 설정
            if (memo.getDeadline() != null && !memo.getDeadline().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                    Calendar deadlineCalendar = Calendar.getInstance();
                    deadlineCalendar.setTime(sdf.parse(memo.getDeadline()));

                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.setTime(sdf.parse(selectedDate));

                    long diff = deadlineCalendar.getTimeInMillis() - selectedCalendar.getTimeInMillis();
                    long daysDiff = diff / (24 * 60 * 60 * 1000);

                    if (daysDiff > 0) {
                        btnMemoDeadline.setText("D-" + daysDiff);
                    } else if (daysDiff == 0) {
                        btnMemoDeadline.setText("D-DAY");
                    } else {
                        btnMemoDeadline.setText("D+" + Math.abs(daysDiff));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    btnMemoDeadline.setText("");
                }
            } else {
                btnMemoDeadline.setText("마감 설정");
            }

            // 마감일 설정 버튼 클릭 리스너 설정
            btnMemoDeadline.setOnClickListener(v -> showDeadlinePickerDialog(memo));

            // 메모 제목 클릭 리스너 설정
            tvMemoTitle.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MemoActivity.class);
                intent.putExtra("date", selectedDate);
                intent.putExtra("memoId", memo.getId());
                startActivity(intent);
            });

            // 체크박스 상태 변경 리스너 설정
            cbMemo.setOnCheckedChangeListener(null);
            cbMemo.setChecked(memo.isChecked());
            cbMemo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                memo.setChecked(isChecked);
                dbHelper.updateMemoCheckStatus(memo.getId(), isChecked);
            });

            return convertView; // 뷰 반환
        }
    }

    // 마감일 설정 다이얼로그 표시 메서드
    private void showDeadlinePickerDialog(Memo memo) {
        // 다이얼로그 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_date_picker, null);
        builder.setView(dialogView);

        // 다이얼로그 내부 요소 초기화
        DatePicker datePicker = dialogView.findViewById(R.id.datePicker);
        Button btnRemoveDeadline = dialogView.findViewById(R.id.btn_removeDeadline);

        // 마감일 해제 버튼 표시 및 동작 설정
        if (memo.getDeadline() == null || memo.getDeadline().isEmpty()) {
            btnRemoveDeadline.setVisibility(View.GONE);
        } else {
            btnRemoveDeadline.setVisibility(View.VISIBLE);
            btnRemoveDeadline.setOnClickListener(v -> {
                memo.setDeadline(null);
                dbHelper.updateMemoDeadline(memo.getId(), null);
                Toast.makeText(MainActivity.this, "마감일이 해제되었습니다.", Toast.LENGTH_SHORT).show();
                loadMemos(); // 메모를 다시 로드하여 업데이트된 정보를 반영함
            });
        }

        // 오늘 이후의 날짜만 선택 가능하도록 설정
        Calendar today = Calendar.getInstance();
        datePicker.setMinDate(today.getTimeInMillis());

        // 다이얼로그 확인 버튼 클릭 리스너 설정
        builder.setPositiveButton("확인", (dialog, which) -> {
            int year = datePicker.getYear();
            int month = datePicker.getMonth();
            int day = datePicker.getDayOfMonth();

            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, day);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            String deadline = sdf.format(selectedDate.getTime());

            memo.setDeadline(deadline);
            dbHelper.updateMemoDeadline(memo.getId(), deadline);
            Toast.makeText(MainActivity.this, "마감일이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            loadMemos(); // 메모를 다시 로드하여 업데이트된 정보를 반영함
        });

        // 다이얼로그 취소 버튼 클릭 리스너 설정
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        // 다이얼로그 생성 및 표시
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // 선택된 메모 삭제 메서드
    private void deleteSelectedMemos() {
        // 삭제 확인 다이얼로그 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("삭제 확인");
        builder.setMessage("정말로 선택한 메모들을 삭제하시겠습니까?");
        builder.setPositiveButton("예", (dialog, which) -> {
            ArrayList<Integer> idsToDelete = new ArrayList<>();
            for (Memo memo : memoList) {
                if (memo.isChecked()) {
                    idsToDelete.add(memo.getId());
                }
            }

            // 선택된 메모들을 삭제
            for (int id : idsToDelete) {
                dbHelper.deleteMemo(id);
            }

            // 메모를 다시 로드하여 업데이트된 정보를 반영
            loadMemos();

            // 삭제 완료 메시지 표시
            String toastMessage = idsToDelete.size() + "개의 메모가 정상적으로 삭제되었습니다.";
            Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("아니요", null);
        builder.show();
    }

    // Memo 클래스 정의
    private static class Memo {
        private int id;
        private String title;
        private String date;
        private String deadline;
        private boolean isChecked;

        // Memo 생성자
        public Memo(int id, String title, String date, String deadline) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.deadline = deadline;
            this.isChecked = false;
        }

        // Getter 및 Setter 메서드
        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDate() {
            return date;
        }

        public String getDeadline() {
            return deadline;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }
    }
}
