package com.cookandroid.todocalnote;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MemoActivity extends AppCompatActivity {

    // 메모 제목과 내용 입력 필드
    private EditText etMemoTitle, etMemoContent;
    private MemoDatabaseHelper dbHelper; // 데이터베이스 헬퍼 객체
    private String selectedDate, deadlineDate; // 선택된 날짜와 마감 날짜
    private int memoId = -1; // 메모 ID, -1은 새로운 메모를 의미
    private boolean isSaved = false; // 메모가 저장되었는지 여부
    private boolean isContentChanged = false; // 메모 내용이 변경되었는지 여부
    private boolean isInitialized = false; // 에딧텍스트가 초기화되었는지 여부
    private boolean hasContent = false; // 메모에 내용이 있는지 여부
    private static final String TAG = "MemoActivity"; // 로그 태그

    // 텍스트 스타일 상태 변수
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private boolean isStrikethrough = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo);

        // UI 요소 초기화
        etMemoTitle = findViewById(R.id.et_memoTitle);
        etMemoContent = findViewById(R.id.et_memoContent);
        Button btnToolbox = findViewById(R.id.btn_toolbox);
        Button btnBold = findViewById(R.id.btn_bold);
        Button btnItalic = findViewById(R.id.btn_italic);
        Button btnUnderline = findViewById(R.id.btn_underline);
        Button btnStrikethrough = findViewById(R.id.btn_strikethrough);
        Button btnTextSize = findViewById(R.id.btn_textSize);
        Button btnSetDeadline = findViewById(R.id.btn_setDeadline);

        dbHelper = new MemoDatabaseHelper(this);

        // 인텐트에서 전달된 날짜와 메모 ID를 가져옴
        selectedDate = getIntent().getStringExtra("date");
        memoId = getIntent().getIntExtra("memoId", -1);

        // 메모 ID가 -1이 아니면 기존 메모 로드
        if (memoId != -1) {
            loadMemo();
        } else {
            // 새 메모의 경우 초기 텍스트 설정
            StringBuilder initialText = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                initialText.append("\n");
            }
            etMemoContent.setText(initialText.toString());
        }

        // 텍스트 변경 감지 리스너 설정
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isContentChanged = true; // 내용이 변경되었음을 표시
                if (s.length() > 0 && !s.toString().trim().isEmpty()) {
                    hasContent = true; // 내용이 존재함을 표시
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isContentChanged) {
                    saveMemo(); // 메모를 저장
                    isContentChanged = false; // 변경 상태 초기화
                }
            }
        };

        // 제목과 내용에 텍스트 변경 감지 리스너 설정
        etMemoTitle.addTextChangedListener(textWatcher);
        etMemoContent.addTextChangedListener(textWatcher);

        // 에딧텍스트 터치 리스너 설정
        etMemoContent.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && !isInitialized) {
                isInitialized = true; // 초기화 상태 표시
            }
            return false;
        });

        // 각 버튼에 대한 클릭 리스너 설정
        btnToolbox.setOnClickListener(v -> showToolbox(v));
        btnBold.setOnClickListener(v -> {
            isBold = !isBold;
            MemoFormatter.applyStyle(etMemoContent, android.graphics.Typeface.BOLD, isBold);
        });
        btnItalic.setOnClickListener(v -> {
            isItalic = !isItalic;
            MemoFormatter.applyStyle(etMemoContent, android.graphics.Typeface.ITALIC, isItalic);
        });
        btnUnderline.setOnClickListener(v -> {
            isUnderline = !isUnderline;
            MemoFormatter.toggleUnderline(etMemoContent, isUnderline);
        });
        btnStrikethrough.setOnClickListener(v -> {
            isStrikethrough = !isStrikethrough;
            MemoFormatter.toggleStrikethrough(etMemoContent, isStrikethrough);
        });
        btnTextSize.setOnClickListener(v -> showTextSizeDialog());
        btnSetDeadline.setOnClickListener(v -> showDatePickerDialog());

        etMemoContent.setMovementMethod(LinkMovementMethod.getInstance()); // 링크 이동 메서드 설정
    }

    // 날짜 선택 다이얼로그 표시 메서드
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            deadlineDate = sdf.format(calendar.getTime());

            if (memoId != -1) {
                ContentValues values = new ContentValues();
                values.put("deadline", deadlineDate);
                dbHelper.updateMemo(memoId, values);
            }

            // 마감일 설정 및 변경에 따라 토스트 메시지 표시
            if (isSaved) {
                Toast.makeText(MemoActivity.this, "마감일이 변경되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MemoActivity.this, "마감일이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // 이전 날짜 선택 방지
        datePickerDialog.show(); // 다이얼로그 표시
    }

    // 도구 상자 표시 메서드
    private void showToolbox(View anchorView) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_toolbox, null);

        final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true); // 포커스를 받을 수 있도록 설정
        popupWindow.setOutsideTouchable(true); // 외부 영역 터치 시 닫히도록 설정

        // 도구 상자 버튼 초기화 및 클릭 리스너 설정
        Button btnAddCheckbox = popupView.findViewById(R.id.btn_addCheckbox);
        Button btnBullet = popupView.findViewById(R.id.btn_bullet);
        Button btnNumber = popupView.findViewById(R.id.btn_number);
        Button btnIndent = popupView.findViewById(R.id.btn_indent);
        Button btnOutdent = popupView.findViewById(R.id.btn_outdent);

        btnAddCheckbox.setOnClickListener(v -> {
            MemoFormatter.toggleCheckbox(etMemoContent, MemoActivity.this);
            popupWindow.dismiss();
        });
        btnBullet.setOnClickListener(v -> {
            MemoFormatter.applyBullet(etMemoContent);
            popupWindow.dismiss();
        });
        btnNumber.setOnClickListener(v -> {
            MemoFormatter.applyNumbering(etMemoContent);
            popupWindow.dismiss();
        });
        btnIndent.setOnClickListener(v -> {
            MemoFormatter.applyIndent(etMemoContent, true);
            popupWindow.dismiss();
        });
        btnOutdent.setOnClickListener(v -> {
            MemoFormatter.applyIndent(etMemoContent, false);
            popupWindow.dismiss();
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location); // 앵커 뷰의 위치를 가져옴
        int x = location[0] + anchorView.getWidth() - popupWindow.getWidth(); // 오른쪽 정렬
        int y = location[1] - popupWindow.getHeight() - 200; // 200dp 낮춰서 나타나도록 설정
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y); // 팝업 윈도우 표시
    }

    // 텍스트 크기 설정 다이얼로그 표시 메서드
    private void showTextSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("텍스트 사이즈 입력:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            int textSize = Integer.parseInt(input.getText().toString());
            MemoFormatter.applyTextSize(etMemoContent, textSize); // 입력된 텍스트 크기 적용
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel()); // 취소 버튼 설정

        builder.show(); // 다이얼로그 표시
    }

    // 기존 메모 로드 메서드
    private void loadMemo() {
        Cursor cursor = dbHelper.getMemo(memoId);

        int titleIndex = cursor.getColumnIndex("title");
        int contentIndex = cursor.getColumnIndex("content");
        int deadlineIndex = cursor.getColumnIndex("deadline");

        if (cursor.moveToFirst()) {
            // 제목 설정
            if (titleIndex != -1) {
                String title = cursor.getString(titleIndex);
                etMemoTitle.setText(title);
            }
            // 내용 설정 및 체크박스 처리
            if (contentIndex != -1) {
                String content = cursor.getString(contentIndex);
                Editable ssb = Editable.Factory.getInstance().newEditable(content);

                for (int i = 0; i < ssb.length(); i++) {
                    if (ssb.charAt(i) == '☐' || ssb.charAt(i) == '☑') {
                        final int start = i;
                        final int end = MemoFormatter.findLineEnd(ssb, i);
                        boolean isChecked = ssb.charAt(i) == '☑';
                        ClickableSpan clickableSpan = MemoFormatter.createClickableSpan(ssb, isChecked, start, end, this);
                        ssb.setSpan(clickableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (isChecked) {
                            ssb.setSpan(new StrikethroughSpan(), start + 2, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ssb.setSpan(new ForegroundColorSpan(Color.GRAY), start + 2, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }

                etMemoContent.setText(ssb);
                etMemoContent.setMovementMethod(LinkMovementMethod.getInstance());
                if (!content.trim().isEmpty()) {
                    hasContent = true; // 내용이 있음을 표시
                }
            }
            // 마감일 설정
            if (deadlineIndex != -1) {
                deadlineDate = cursor.getString(deadlineIndex);
            }
        }

        cursor.close(); // 커서 닫기
    }

    // 메모 저장 메서드
    public void saveMemo() {
        if (isOnlyWhitespace()) {
            return; // 내용이 공백만 있는 경우 저장하지 않음
        }

        String title = etMemoTitle.getText().toString();
        String content = etMemoContent.getText().toString();

        if (title.isEmpty() && content.isEmpty()) {
            return; // 제목과 내용이 모두 비어있는 경우 저장하지 않음
        }

        // 제목이 비어있는 경우 내용에서 제목을 자동 설정
        if (title.isEmpty()) {
            int start = 0;
            int endOfLineIndex;
            while (true) {
                endOfLineIndex = content.indexOf('\n', start);
                if (endOfLineIndex == -1) {
                    endOfLineIndex = content.length();
                }
                title = content.substring(start, endOfLineIndex).trim();
                if (!title.isEmpty()) {
                    break; // 공백이 아닌 제목을 찾으면 루프 종료
                }
                if (endOfLineIndex == content.length()) {
                    title = content.trim(); // 모든 내용을 탐색해도 제목이 없으면 전체 내용을 제목으로 설정
                    break;
                }
                start = endOfLineIndex + 1;
            }
        }

        // 메모 데이터베이스에 저장할 값 설정
        ContentValues values = new ContentValues();
        values.put("date", selectedDate);
        values.put("title", title);
        values.put("content", content);
        values.put("deadline", deadlineDate);

        // 새 메모인 경우 삽입, 기존 메모인 경우 업데이트
        if (memoId == -1) {
            memoId = (int) dbHelper.insertMemo(values);
        } else {
            dbHelper.updateMemo(memoId, values);
        }

        isSaved = true; // 저장 상태 표시
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 메모가 저장되지 않았고, 내용이 변경되었으며, 공백이 아닌 경우 저장
        if (!isSaved && isContentChanged && !isOnlyWhitespace()) {
            saveMemo();
        }
    }

    // 메모가 공백인지 여부를 확인하는 메서드
    private boolean isOnlyWhitespace() {
        String title = etMemoTitle.getText().toString().trim();
        String content = etMemoContent.getText().toString().trim();
        return title.isEmpty() && content.isEmpty();
    }
}
