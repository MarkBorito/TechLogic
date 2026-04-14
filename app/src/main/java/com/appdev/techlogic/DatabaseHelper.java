package com.appdev.techlogic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "techlogic_db";
    private static final int DB_VERSION = 3; // Incremented version for migration

    // Cards table (Existing)
    private static final String TABLE_CARDS = "cards";
    private static final String COLUMN_CARD_ID = "id";
    private static final String COLUMN_CARD_TITLE = "title";

    // Gates table (New)
    private static final String TABLE_GATES = "gates";
    private static final String COLUMN_GATE_ID = "gate_id";
    private static final String COLUMN_GATE_CARD_TITLE = "card_title";
    private static final String COLUMN_GATE_RES_ID = "res_id";
    private static final String COLUMN_GATE_X = "x";
    private static final String COLUMN_GATE_Y = "y";
    private static final String COLUMN_GATE_SCALE = "scale";

    // Connections table (New)
    private static final String TABLE_CONNECTIONS = "connections";
    private static final String COLUMN_CONN_ID = "conn_id";
    private static final String COLUMN_CONN_CARD_TITLE = "card_title";
    private static final String COLUMN_START_GATE_INDEX = "start_gate_index";
    private static final String COLUMN_END_GATE_INDEX = "end_gate_index";

    // Texts table (New)
    private static final String TABLE_TEXTS = "texts";
    private static final String COLUMN_TEXT_ID = "text_id";
    private static final String COLUMN_TEXT_CARD_TITLE = "card_title";
    private static final String COLUMN_TEXT_CONTENT = "content";
    private static final String COLUMN_TEXT_X = "x";
    private static final String COLUMN_TEXT_Y = "y";
    private static final String COLUMN_TEXT_SCALE = "scale";
    private static final String COLUMN_CARD_IMAGE = "image";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CARDS_TABLE = "CREATE TABLE " + TABLE_CARDS + "("
                + COLUMN_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CARD_TITLE + " TEXT UNIQUE,"
                + COLUMN_CARD_IMAGE + " BLOB)";
        db.execSQL(CREATE_CARDS_TABLE);

        String CREATE_GATES_TABLE = "CREATE TABLE " + TABLE_GATES + "("
                + COLUMN_GATE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_GATE_CARD_TITLE + " TEXT,"
                + COLUMN_GATE_RES_ID + " INTEGER,"
                + COLUMN_GATE_X + " REAL,"
                + COLUMN_GATE_Y + " REAL,"
                + COLUMN_GATE_SCALE + " REAL)";
        db.execSQL(CREATE_GATES_TABLE);

        String CREATE_CONNECTIONS_TABLE = "CREATE TABLE " + TABLE_CONNECTIONS + "("
                + COLUMN_CONN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CONN_CARD_TITLE + " TEXT,"
                + COLUMN_START_GATE_INDEX + " INTEGER,"
                + COLUMN_END_GATE_INDEX + " INTEGER)";
        db.execSQL(CREATE_CONNECTIONS_TABLE);

        String CREATE_TEXTS_TABLE = "CREATE TABLE " + TABLE_TEXTS + "("
                + COLUMN_TEXT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TEXT_CARD_TITLE + " TEXT,"
                + COLUMN_TEXT_CONTENT + " TEXT,"
                + COLUMN_TEXT_X + " REAL,"
                + COLUMN_TEXT_Y + " REAL,"
                + COLUMN_TEXT_SCALE + " REAL)";
        db.execSQL(CREATE_TEXTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GATES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONNECTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXTS);
            String CREATE_GATES_TABLE = "CREATE TABLE " + TABLE_GATES + "("
                    + COLUMN_GATE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_GATE_CARD_TITLE + " TEXT,"
                    + COLUMN_GATE_RES_ID + " INTEGER,"
                    + COLUMN_GATE_X + " REAL,"
                    + COLUMN_GATE_Y + " REAL)";
            db.execSQL(CREATE_GATES_TABLE);

            String CREATE_CONNECTIONS_TABLE = "CREATE TABLE " + TABLE_CONNECTIONS + "("
                    + COLUMN_CONN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_CONN_CARD_TITLE + " TEXT,"
                    + COLUMN_START_GATE_INDEX + " INTEGER,"
                    + COLUMN_END_GATE_INDEX + " INTEGER)";
            db.execSQL(CREATE_CONNECTIONS_TABLE);
            String CREATE_TEXT_TABLE = "CREATE TABLE " + TABLE_TEXTS + "("
                    + COLUMN_TEXT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TEXT_CARD_TITLE + " TEXT,"
                    + COLUMN_START_GATE_INDEX + " INTEGER,"
                    + COLUMN_END_GATE_INDEX + " INTEGER)";
            db.execSQL(CREATE_TEXT_TABLE);
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_GATES + " ADD COLUMN " + COLUMN_GATE_SCALE + " REAL DEFAULT 1.0");
        }
    }

    // --- Card Methods ---

    public long insertCard(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CARD_TITLE, title);
        long id = db.insertWithOnConflict(TABLE_CARDS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return id;
    }

    public List<CardItem> getAllCards() {
        List<CardItem> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CARDS, new String[]{COLUMN_CARD_ID, COLUMN_CARD_TITLE, COLUMN_CARD_IMAGE},
                null, null, null, null, COLUMN_CARD_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_TITLE));

                // 1. GET THE BLOB FROM DATABASE
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_CARD_IMAGE));

                // 2. PASS IT TO THE CONSTRUCTOR
                // (Ensure your CardItem constructor accepts: String, boolean, byte[])
                cards.add(new CardItem(title, false, imageBytes));

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return cards;
    }

    public void deleteCard(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CARDS, COLUMN_CARD_TITLE + "=?", new String[]{title});
        db.delete(TABLE_GATES, COLUMN_GATE_CARD_TITLE + "=?", new String[]{title});
        db.delete(TABLE_CONNECTIONS, COLUMN_CONN_CARD_TITLE + "=?", new String[]{title});
        db.close();
    }

    public void updateCardTitle(String oldTitle, String newTitle) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cardValues = new ContentValues();
            cardValues.put(COLUMN_CARD_TITLE, newTitle);
            db.update(TABLE_CARDS, cardValues, COLUMN_CARD_TITLE + "=?", new String[]{oldTitle});

            ContentValues gateValues = new ContentValues();
            gateValues.put(COLUMN_GATE_CARD_TITLE, newTitle);
            db.update(TABLE_GATES, gateValues, COLUMN_GATE_CARD_TITLE + "=?", new String[]{oldTitle});

            ContentValues connValues = new ContentValues();
            connValues.put(COLUMN_CONN_CARD_TITLE, newTitle);
            db.update(TABLE_CONNECTIONS, connValues, COLUMN_CONN_CARD_TITLE + "=?", new String[]{oldTitle});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // --- Diagram Data Methods ---

    public void saveDiagram(String cardTitle, List<DiagramView.GateInstance> gates, List<DiagramView.Connection> connections, List<DiagramView.TextInstance> texts, Bitmap thumbnail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear existing data for this card
            db.delete(TABLE_GATES, COLUMN_GATE_CARD_TITLE + "=?", new String[]{cardTitle});
            db.delete(TABLE_CONNECTIONS, COLUMN_CONN_CARD_TITLE + "=?", new String[]{cardTitle});
            db.delete(TABLE_TEXTS, COLUMN_TEXT_CARD_TITLE + "=?", new String[]{cardTitle}); // Clear old text


            // Save Gates
            for (DiagramView.GateInstance gate : gates) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_GATE_CARD_TITLE, cardTitle);
                values.put(COLUMN_GATE_RES_ID, gate.resId);
                values.put(COLUMN_GATE_X, gate.x);
                values.put(COLUMN_GATE_Y, gate.y);
                values.put(COLUMN_GATE_SCALE, gate.scale);
                db.insert(TABLE_GATES, null, values);
            }

            // Save Connections
            for (DiagramView.Connection conn : connections) {
                int startIndex = gates.indexOf(conn.start);
                int endIndex = gates.indexOf(conn.end);
                
                if (startIndex != -1 && endIndex != -1) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_CONN_CARD_TITLE, cardTitle);
                    values.put(COLUMN_START_GATE_INDEX, startIndex);
                    values.put(COLUMN_END_GATE_INDEX, endIndex);
                    db.insert(TABLE_CONNECTIONS, null, values);
                }
            }

            // Save Texts
            for (DiagramView.TextInstance textItem : texts) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_TEXT_CARD_TITLE, cardTitle);
                values.put(COLUMN_TEXT_CONTENT, textItem.text);
                values.put(COLUMN_TEXT_X, textItem.x);
                values.put(COLUMN_TEXT_Y, textItem.y);
                values.put(COLUMN_TEXT_SCALE, textItem.scale);
                db.insert(TABLE_TEXTS, null, values);
            }
            if (thumbnail != null) {
                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                ContentValues values = new ContentValues();
                values.put(COLUMN_CARD_IMAGE, byteArray);
                db.update(TABLE_CARDS, values, COLUMN_CARD_TITLE + " = ?", new String[]{cardTitle});
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void loadDiagram(String cardTitle, DiagramView diagramView) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        List<DiagramView.GateInstance> gates = new ArrayList<>();
        Cursor gateCursor = db.query(TABLE_GATES, null, COLUMN_GATE_CARD_TITLE + "=?", 
                new String[]{cardTitle}, null, null, COLUMN_GATE_ID + " ASC");
        
        if (gateCursor.moveToFirst()) {
            do {
                int resId = gateCursor.getInt(gateCursor.getColumnIndexOrThrow(COLUMN_GATE_RES_ID));
                float x = gateCursor.getFloat(gateCursor.getColumnIndexOrThrow(COLUMN_GATE_X));
                float y = gateCursor.getFloat(gateCursor.getColumnIndexOrThrow(COLUMN_GATE_Y));
                float scale = gateCursor.getFloat(gateCursor.getColumnIndexOrThrow(COLUMN_GATE_SCALE));


                // Use a modified GateInstance constructor or setter that accepts resId
                gates.add(diagramView.createGateFromLoad(resId, x, y, scale));
            } while (gateCursor.moveToNext());
        }
        gateCursor.close();

        List<DiagramView.Connection> connections = new ArrayList<>();
        Cursor connCursor = db.query(TABLE_CONNECTIONS, null, COLUMN_CONN_CARD_TITLE + "=?",
                new String[]{cardTitle}, null, null, null);
        
        if (connCursor.moveToFirst()) {
            do {
                int startIndex = connCursor.getInt(connCursor.getColumnIndexOrThrow(COLUMN_START_GATE_INDEX));
                int endIndex = connCursor.getInt(connCursor.getColumnIndexOrThrow(COLUMN_END_GATE_INDEX));
                
                if (startIndex < gates.size() && endIndex < gates.size()) {
                    connections.add(new DiagramView.Connection(gates.get(startIndex), gates.get(endIndex)));
                }
            } while (connCursor.moveToNext());
        }
        connCursor.close();

        List<DiagramView.TextInstance> texts = new ArrayList<>();
        Cursor textCursor = db.query(TABLE_TEXTS, null, COLUMN_TEXT_CARD_TITLE + "=?",
                new String[]{cardTitle}, null, null, null);

        if (textCursor.moveToFirst()) {
            do {
                String content = textCursor.getString(textCursor.getColumnIndexOrThrow(COLUMN_TEXT_CONTENT));
                float x = textCursor.getFloat(textCursor.getColumnIndexOrThrow(COLUMN_TEXT_X));
                float y = textCursor.getFloat(textCursor.getColumnIndexOrThrow(COLUMN_TEXT_Y));
                float scale = textCursor.getFloat(textCursor.getColumnIndexOrThrow(COLUMN_TEXT_SCALE));

                // Create the text instance and set scale
                DiagramView.TextInstance textInstance = new DiagramView.TextInstance(content, x, y);
                textInstance.scale = scale;
                texts.add(textInstance);
            } while (textCursor.moveToNext());
        }
        textCursor.close();
        db.close();

        diagramView.setLoadedData(gates, connections, texts);
    }
}