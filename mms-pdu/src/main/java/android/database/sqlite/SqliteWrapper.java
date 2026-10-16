/*
 * SPDX-FileCopyrightText: 2008 Esmertec AG.
 * SPDX-FileCopyrightText: 2008 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.database.sqlite;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;

/**
 * @hide
 */

public final class SqliteWrapper {
    private static final String SQLITE_EXCEPTION_DETAIL_MESSAGE
            = "unable to open database file";

    private SqliteWrapper() {
        // Forbidden being instantiated.
    }

    // FIXME: need to optimize this method.
    private static boolean isLowMemory(SQLiteException e) {
        return e.getMessage().equals(SQLITE_EXCEPTION_DETAIL_MESSAGE);
    }

    public static void checkSQLiteException(Context context, SQLiteException e) {
        if (isLowMemory(e)) {
            Toast.makeText(context, "Low Memory",
                    Toast.LENGTH_SHORT).show();
        } else {
            throw e;
        }
    }

    @Nullable
    public static Cursor query(Context context, ContentResolver resolver, Uri uri,
                               String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (SQLiteException e) {
            Timber.e(e, "Catch a SQLiteException when query: ");
            checkSQLiteException(context, e);
            return null;
        }
    }

    public static int update(Context context, ContentResolver resolver, Uri uri,
                             ContentValues values, String where, String[] selectionArgs) {
        try {
            return resolver.update(uri, values, where, selectionArgs);
        } catch (SQLiteException e) {
            Timber.e(e, "Catch a SQLiteException when update: ");
            checkSQLiteException(context, e);
            return -1;
        }
    }

    public static int delete(Context context, ContentResolver resolver, Uri uri,
                             String where, String[] selectionArgs) {
        try {
            return resolver.delete(uri, where, selectionArgs);
        } catch (SQLiteException e) {
            Timber.e(e, "Catch a SQLiteException when delete: ");
            checkSQLiteException(context, e);
            return -1;
        }
    }

    public static Uri insert(Context context, ContentResolver resolver,
                             Uri uri, ContentValues values) {
        try {
            return resolver.insert(uri, values);
        } catch (SQLiteException e) {
            Timber.e(e, "Catch a SQLiteException when insert: ");
            checkSQLiteException(context, e);
            return null;
        }
    }
}
