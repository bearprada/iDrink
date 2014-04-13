package lab.prada.android.app.idrink;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class LogProvider extends ContentProvider {

    private static final String AUTHORITY = "lab.prada.android.app.idrink";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" + LogDbHelper.DB_TABLE_NAME);
    public static final String DEFAULT_SORT_ORDER = "timestamp DESC";

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0; // we should not support delete log now
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd."+AUTHORITY+"."+LogDbHelper.DB_TABLE_NAME;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LogDbHelper helper = new LogDbHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(LogDbHelper.DB_TABLE_NAME, null, values);
        Uri result = Uri.parse("content://" + AUTHORITY + "/" + LogDbHelper.DB_TABLE_NAME + "/" + id);
        getContext().getContentResolver().notifyChange(URI, null);
        return result;
    }

    @Override
    public boolean onCreate() {
        return true; // FIXME
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        LogDbHelper helper = new LogDbHelper(getContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(LogDbHelper.DB_TABLE_NAME, projection, selection,
                selectionArgs, null, null, DEFAULT_SORT_ORDER);
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0; // should not update data
    }

    public class LogDbHelper extends SQLiteOpenHelper {
        private static final String DB_TABLE_NAME = "bluetooth_log";
        public static final String WATER_CC = "water_cc";
        public static final String TIMESTAMP = "timestamp";

        public LogDbHelper(Context context) {
            super(context, "iDrink", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "create table " + DB_TABLE_NAME
                    + " ("
                    + "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                    + ", " + WATER_CC + " INTEGER NOT NULL"
                    + ", " + TIMESTAMP + " TIMESTAMP(8)"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {}
    }
}
