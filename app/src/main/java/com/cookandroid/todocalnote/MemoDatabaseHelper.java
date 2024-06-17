package com.cookandroid.todocalnote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MemoDatabaseHelper extends SQLiteOpenHelper {
    // 데이터베이스 이름과 버전 상수 선언
    private static final String DATABASE_NAME = "memo.db"; // 데이터베이스 이름
    private static final int DATABASE_VERSION = 4; // 데이터베이스 버전
    private static final String TAG = "MemoDatabaseHelper"; // 로그 태그

    // 생성자
    public MemoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION); // SQLiteOpenHelper 생성자 호출
    }

    // 데이터베이스 생성시 호출됨
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 메모 테이블 생성 SQL 쿼리
        String createTable = "CREATE TABLE memos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 자동 증가 기본 키
                "date TEXT, " + // 메모 날짜
                "title TEXT, " + // 메모 제목
                "content TEXT, " + // 메모 내용
                "deadline TEXT, " + // 마감일
                "isChecked INTEGER DEFAULT 0)"; // 체크 상태 (기본값 0)
        db.execSQL(createTable); // SQL 쿼리 실행
    }

    // 데이터베이스 업그레이드 시 호출됨
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 버전 2로 업그레이드: 제목 칼럼 추가
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE memos ADD COLUMN title TEXT");
        }
        // 버전 3으로 업그레이드: 마감일과 체크 상태 칼럼 추가
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE memos ADD COLUMN deadline TEXT");
            db.execSQL("ALTER TABLE memos ADD COLUMN isChecked INTEGER DEFAULT 0");
        }
        // 버전 4로 업그레이드: 새로운 칼럼 추가 (현재는 사용되지 않음)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE memos ADD COLUMN newColumn TEXT");
        }
    }

    // 새 메모 삽입 메서드
    public long insertMemo(ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase(); // 쓰기 가능한 데이터베이스 객체 얻기
        return db.insert("memos", null, values); // 메모 삽입 후 새 메모의 ID 반환
    }

    // 기존 메모 업데이트 메서드
    public int updateMemo(int id, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase(); // 쓰기 가능한 데이터베이스 객체 얻기
        return db.update("memos", values, "id=?", new String[]{String.valueOf(id)}); // 메모 업데이트 후 변경된 행 수 반환
    }

    // 메모의 체크 상태 업데이트 메서드
    public int updateMemoCheckStatus(int id, boolean isChecked) {
        SQLiteDatabase db = this.getWritableDatabase(); // 쓰기 가능한 데이터베이스 객체 얻기
        ContentValues values = new ContentValues(); // ContentValues 객체 생성
        values.put("isChecked", isChecked ? 1 : 0); // 체크 상태 설정
        return db.update("memos", values, "id=?", new String[]{String.valueOf(id)}); // 메모 체크 상태 업데이트 후 변경된 행 수 반환
    }

    // 메모의 마감일 업데이트 메서드
    public int updateMemoDeadline(int id, String deadline) {
        SQLiteDatabase db = this.getWritableDatabase(); // 쓰기 가능한 데이터베이스 객체 얻기
        ContentValues values = new ContentValues(); // ContentValues 객체 생성
        values.put("deadline", deadline); // 마감일 설정
        return db.update("memos", values, "id=?", new String[]{String.valueOf(id)}); // 메모 마감일 업데이트 후 변경된 행 수 반환
    }

    // 특정 메모 조회 메서드
    public Cursor getMemo(int id) {
        SQLiteDatabase db = this.getReadableDatabase(); // 읽기 가능한 데이터베이스 객체 얻기
        return db.query("memos", null, "id=?", new String[]{String.valueOf(id)}, null, null, null); // 메모 조회 후 커서 반환
    }

    // 특정 날짜 또는 마감일 기준 메모 조회 메서드
    public Cursor getMemosByDateOrDeadline(String date) {
        SQLiteDatabase db = this.getReadableDatabase(); // 읽기 가능한 데이터베이스 객체 얻기
        Log.d(TAG, "Querying memos by date or deadline: " + date); // 로그 출력
        Cursor cursor = db.rawQuery("SELECT * FROM memos WHERE date=? OR (deadline IS NOT NULL AND deadline >= ?)", new String[]{date, date}); // SQL 쿼리 실행 후 커서 반환
        Log.d(TAG, "Memos found: " + cursor.getCount()); // 조회된 메모 수 로그 출력
        return cursor;
    }

    // 메모 삭제 메서드
    public void deleteMemo(int id) {
        SQLiteDatabase db = this.getWritableDatabase(); // 쓰기 가능한 데이터베이스 객체 얻기
        db.delete("memos", "id=?", new String[]{String.valueOf(id)}); // 메모 삭제
    }
}
