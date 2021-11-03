package com.iot.connect.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.iot.connect.R;

public class RFIDHelper {
    private static final String TAG = "RFIDHelper";
    private static final double stepValue = 2.00;
    private static final String TABLE_NAME = "HFCard";
    private static final String ID = "_id";
    private static final String CARD_ID = "card_id";
    private static final String SUM = "sum";

    Context mContext;
    DatabaseHelper mDatabaseHelper;
    SQLiteDatabase mDatabase;

    public RFIDHelper(Context context) {
        mContext = context;
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mDatabase = mDatabaseHelper.getReadableDatabase();
    }

    private void updateCardUI(String CardId) {
        String searchResult = searchHFCard(CARD_ID,CardId);
        if (searchResult == null || searchResult.length() <= 0) { //如果数据库中没有记录


        } else if (searchResult.equals("-1")) {  //返回值为-1，数据库中搜索不止一个记录，错误


        } else {  //返回金额，更新UI
            double newSum = Double.valueOf(searchResult) - stepValue;
            if (newSum < 0) {

            }else {
                if (Double.toString(newSum).equals(updateHFCard(CARD_ID, CardId, SUM, Double.toString(newSum)))) {

                }

            }

        }
    }

    public void rechargeCard(String currentId) {

    }

    /**
     * 查询一条记录
     * @param key
     * @param selectionArgs
     * @return 返回金额数值对应的字符串
     */
    private String searchHFCard(String key,String selectionArgs) {
        Cursor cursor = mDatabase.query(TABLE_NAME, new String[]{SUM}, key + "=?", new String[] {selectionArgs}, null, null,null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        //double[] sumList = new double[cursor.getCount()];
        if (cursor.getCount() == 1) {
            double sum = cursor.getDouble(0);
            cursor.close();
            return Double.toString(sum);
        }else if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            for (int i = 0;i <cursor.getCount();i++) {
                Log.v(TAG, "Current cursor = " + Double.toString(cursor.getDouble(0)));
                cursor.moveToNext();
            }
            cursor.close();
            return "-1";
        }
    }

    /**
     * 更新一条记录
     * @param key
     * @param data
     * @return 返回充值后的金额金额字符串，错误返回null
     */
    private String updateHFCard(String key, String data,String Column, String value) {
        ContentValues values = new ContentValues();
        values.put(Column, value);
        int result =  mDatabase.update(TABLE_NAME, values, key + "=?",new String[]{data});
        if (result != 0) {
            return value;
        }
        return null;
    }

    /**
     * 插入一条记录
     * @param key   需要插入的列名称
     * @param data  对应列赋值
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    private long insertHFCard(String key,String data) {
        ContentValues values = new ContentValues();
        values.put(key,data);
        return mDatabase.insert(TABLE_NAME,null,values);
    }

    /**
     * 删除一条记录
     * @param key
     * @param data
     * @return 返回所删除的行数，否则返回0。
     */
    private int deleteHFCard(String key, String data) {
        return mDatabase.delete(TABLE_NAME,key + "=?", new String[] {data});
    }
}
