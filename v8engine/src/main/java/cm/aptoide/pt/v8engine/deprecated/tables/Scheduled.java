/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 25/08/2016.
 */

package cm.aptoide.pt.v8engine.deprecated.tables;

import android.database.Cursor;

import cm.aptoide.pt.utils.IdUtils;
import io.realm.RealmObject;

/**
 * Created by sithengineer on 24/08/16.
 */
public class Scheduled extends BaseTable {

	public final static String COLUMN_ID = "_id";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_NAME = "name";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_VERSION_NAME = "version_name";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_MD5 = "md5";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_ICON = "icon";

	private static final String NAME = "scheduled";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_PACKAGE_NAME = "package_name";

	// @ColumnDefinition(type = SQLType.TEXT)
	public final static String COLUMN_REPO = "repo_name";

	@Override
	public String getTableName() {
		return NAME;
	}

	@Override
	public RealmObject convert(Cursor cursor) {
		cm.aptoide.pt.database.realm.Scheduled realmObject = new cm.aptoide.pt.database.realm.Scheduled();
		realmObject.setAppId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
		realmObject.setPackageName(cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE_NAME)));
		realmObject.setMd5(cursor.getString(cursor.getColumnIndex(COLUMN_MD5)));
		realmObject.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
		realmObject.setVersionName(cursor.getString(cursor.getColumnIndex(COLUMN_VERSION_NAME)));
		realmObject.setStoreName(cursor.getString(cursor.getColumnIndex(COLUMN_REPO)));
		realmObject.setIcon(cursor.getString(cursor.getColumnIndex(COLUMN_ICON)));
		return realmObject;
	}
}
