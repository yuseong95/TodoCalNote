package com.cookandroid.todocalnote;

import android.graphics.Color;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MemoFormatter {

    // 체크박스를 토글하는 메서드
    public static void toggleCheckbox(EditText etMemoContent, MemoActivity memoActivity) {
        // 현재 커서 위치 가져옴
        int start = etMemoContent.getSelectionStart();
        // 커서가 위치한 라인의 시작과 끝 인덱스 계산
        int line = etMemoContent.getLayout().getLineForOffset(start);
        int lineStart = etMemoContent.getLayout().getLineStart(line);
        int lineEnd = findLineEnd(etMemoContent.getText(), lineStart);

        // 에디터블 객체 가져옴
        Editable editable = etMemoContent.getText();
        // 현재 라인 텍스트 가져옴
        String lineText = editable.subSequence(lineStart, lineEnd).toString();

        // 체크박스가 있는지 확인하고 토글
        if (lineText.startsWith("☐ ") || lineText.startsWith("☑ ")) {
            // 체크박스가 있으면 삭제
            editable.delete(lineStart, lineStart + 2);
            // 취소선 및 색상 제거
            StrikethroughSpan[] spans = editable.getSpans(lineStart + 2, lineEnd, StrikethroughSpan.class);
            for (StrikethroughSpan span : spans) {
                editable.removeSpan(span);
            }
            ForegroundColorSpan[] colorSpans = editable.getSpans(lineStart + 2, lineEnd, ForegroundColorSpan.class);
            for (ForegroundColorSpan colorSpan : colorSpans) {
                editable.removeSpan(colorSpan);
            }
        } else {
            // 체크박스가 없으면 추가
            String checkbox = "☐ ";
            editable.insert(lineStart, checkbox);
            int spanStart = lineStart;
            int spanEnd = lineStart + checkbox.length();

            // 클릭 가능 스팬 추가
            ClickableSpan clickableSpan = createClickableSpan(editable, false, spanStart, spanEnd, memoActivity);
            editable.setSpan(clickableSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // 메모 저장
        memoActivity.saveMemo();
    }

    // 불릿 포인트 적용 메서드
    public static void applyBullet(EditText etMemoContent) {
        int start = etMemoContent.getSelectionStart();
        Editable editable = etMemoContent.getText();
        int line = etMemoContent.getLayout().getLineForOffset(start);
        int lineStart = etMemoContent.getLayout().getLineStart(line);
        int lineEnd = etMemoContent.getLayout().getLineEnd(line);

        String lineText = editable.subSequence(lineStart, lineEnd).toString().trim();

        // 체크박스가 있으면 라인 시작 위치 조정
        if (lineText.startsWith("☐ ") || lineText.startsWith("☑ ")) {
            lineStart += 2;
        }

        // 기존 번호나 불릿 제거
        if (lineText.matches("\\d+\\. .*")) {
            int index = lineText.indexOf(' ');
            editable.delete(lineStart, lineStart + index + 1);
        }

        if (lineText.startsWith("• ")) {
            editable.delete(lineStart, lineStart + 2);
        } else {
            editable.insert(lineStart, "• ");
        }
    }

    // 번호 매기기 적용 메서드
    public static void applyNumbering(EditText etMemoContent) {
        int start = etMemoContent.getSelectionStart();
        Editable editable = etMemoContent.getText();
        int line = etMemoContent.getLayout().getLineForOffset(start);
        int lineStart = etMemoContent.getLayout().getLineStart(line);
        int lineEnd = etMemoContent.getLayout().getLineEnd(line);

        String lineText = editable.subSequence(lineStart, lineEnd).toString().trim();

        // 체크박스가 있으면 라인 시작 위치 조정
        if (lineText.startsWith("☐ ") || lineText.startsWith("☑ ")) {
            lineStart += 2;
        }

        // 기존 불릿 제거
        if (lineText.startsWith("• ")) {
            editable.delete(lineStart, lineStart + 2);
        }

        // 기존 번호 제거 및 새로운 번호 추가
        if (lineText.matches("\\d+\\. .*")) {
            int index = lineText.indexOf(' ');
            editable.delete(lineStart, lineStart + index + 1);
        } else {
            int lineNumber = getNextNumber(etMemoContent, line);
            editable.insert(lineStart, lineNumber + ". ");
        }
    }

    // 텍스트 정렬 적용 메서드
    public static void applyAlignment(EditText etMemoContent, Layout.Alignment alignment) {
        int start = etMemoContent.getSelectionStart();
        int end = etMemoContent.getSelectionEnd();
        Editable editable = etMemoContent.getText();
        editable.setSpan(new AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 텍스트 스타일 적용 메서드 (굵게, 기울임 등)
    public static void applyStyle(EditText etMemoContent, int style, boolean apply) {
        int start = etMemoContent.getSelectionStart();
        int end = etMemoContent.getSelectionEnd();
        Editable editable = etMemoContent.getText();

        if (apply) {
            editable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    editable.removeSpan(span);
                }
            }
        }
    }

    // 밑줄 토글 메서드
    public static void toggleUnderline(EditText etMemoContent, boolean apply) {
        int start = etMemoContent.getSelectionStart();
        int end = etMemoContent.getSelectionEnd();
        Editable editable = etMemoContent.getText();

        if (apply) {
            editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            UnderlineSpan[] spans = editable.getSpans(start, end, UnderlineSpan.class);
            for (UnderlineSpan span : spans) {
                editable.removeSpan(span);
            }
        }
    }

    // 취소선 토글 메서드
    public static void toggleStrikethrough(EditText etMemoContent, boolean apply) {
        int start = etMemoContent.getSelectionStart();
        int end = etMemoContent.getSelectionEnd();
        Editable editable = etMemoContent.getText();

        if (apply) {
            editable.setSpan(new StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
            for (StrikethroughSpan span : spans) {
                editable.removeSpan(span);
            }
        }
    }

    // 들여쓰기 및 내어쓰기 적용 메서드
    public static void applyIndent(EditText etMemoContent, boolean indent) {
        int start = etMemoContent.getSelectionStart();
        Editable editable = etMemoContent.getText();
        int line = etMemoContent.getLayout().getLineForOffset(start);
        int lineStart = etMemoContent.getLayout().getLineStart(line);

        if (indent) {
            editable.insert(lineStart, "\t");
        } else {
            if (editable.subSequence(lineStart, lineStart + 1).toString().equals("\t")) {
                editable.delete(lineStart, lineStart + 1);
            }
        }
    }

    // 텍스트 크기 적용 메서드
    public static void applyTextSize(EditText etMemoContent, int textSize) {
        int start = etMemoContent.getSelectionStart();
        int end = etMemoContent.getSelectionEnd();
        Editable editable = etMemoContent.getText();
        editable.setSpan(new AbsoluteSizeSpan(textSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 체크박스 클릭 가능한 스팬 생성 메서드
    public static ClickableSpan createClickableSpan(Editable editable, boolean isChecked, int start, int end, MemoActivity memoActivity) {
        return new ClickableSpan() {
            boolean checked = isChecked;

            @Override
            public void onClick(View widget) {
                Log.d("MemoActivity", "Checkbox clicked");
                checked = !checked;

                int line = ((EditText) widget).getLayout().getLineForOffset(start);
                int lineStart = ((EditText) widget).getLayout().getLineStart(line);
                int lineEnd = findLineEnd(editable, lineStart);

                if (checked) {
                    editable.replace(start, start + 1, "☑");
                    editable.setSpan(new StrikethroughSpan(), start + 2, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    editable.setSpan(new ForegroundColorSpan(Color.GRAY), start + 2, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    editable.replace(start, start + 1, "☐");
                    StrikethroughSpan[] spans = editable.getSpans(start + 2, lineEnd, StrikethroughSpan.class);
                    for (StrikethroughSpan span : spans) {
                        editable.removeSpan(span);
                    }
                    ForegroundColorSpan[] colorSpans = editable.getSpans(start + 2, lineEnd, ForegroundColorSpan.class);
                    for (ForegroundColorSpan colorSpan : colorSpans) {
                        editable.removeSpan(colorSpan);
                    }
                }

                // 체크박스 상태 변경 후 메모 저장
                memoActivity.saveMemo();
            }
        };
    }

    // 라인의 끝 인덱스 찾는 메서드
    public static int findLineEnd(Spannable spannable, int start) {
        int end = spannable.length();
        for (int i = start; i < spannable.length(); i++) {
            if (spannable.charAt(i) == '\n') {
                end = i;
                break;
            }
        }
        return end;
    }

    // 다음 번호 매기기 위한 번호 찾는 메서드
    private static int getNextNumber(EditText etMemoContent, int line) {
        Editable editable = etMemoContent.getText();
        int count = 1;
        for (int i = 0; i < line; i++) {
            int lineStart = etMemoContent.getLayout().getLineStart(i);
            int lineEnd = etMemoContent.getLayout().getLineEnd(i);
            String lineText = editable.subSequence(lineStart, lineEnd).toString().trim();
            if (lineText.matches("\\d+\\. .*")) {
                count++;
            }
        }
        return count;
    }
}
