package android.app.printerapp.devices.database;

import android.app.printerapp.Log;
import android.app.printerapp.devices.database.DeviceInfo.FeedEntry;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;


/**
 * This class will handle Database interaction on a static way
 * Also will contain the SharedPreferences to handle Favorites
 *
 * @author alberto-baeza
 */
public class DatabaseController {

	public static final String TAG_NETWORK = "Network";
	public static final String TAG_REFERENCES = "References";
	public static final String TAG_FAVORITES = "Favorites";
	public static final String TAG_KEYS = "Keys";
	public static final String TAG_SLICING = "Slicing";
	public static final String TAG_PROFILE = "ProfilePreferences";
	public static final String TAG_RESTORE = "Restore";

	static DatabaseHelper mDbHelper;
	static SQLiteDatabase mDb;
	static Context mContext;

	public DatabaseController(Context context) {

		mContext = context;
		mDbHelper = new DatabaseHelper(mContext);

	}


	//Retrieve the cursor with the elements from the database
	public static Cursor retrieveDeviceList() {

		// Gets the data repository in read mode
		mDb = mDbHelper.getReadableDatabase();

		String selectQuery = "SELECT * FROM " + FeedEntry.TABLE_NAME;

		Cursor c = mDb.rawQuery(selectQuery, null);

		return c;
	}

	//Close database statically
	public static void closeDb() {


		if (mDb.isOpen()) mDb.close();

	}


	public static int count() {

		Cursor c = retrieveDeviceList();
		int count = c.getCount();
		closeDb();
		return count;
	}


	/*****************************************************************************************
	 * 					SHARED PREFERENCES HANDLER
	 *****************************************************************************************/

	/**
	 * Check if a file is favorite
	 *
	 * @return
	 */
	public static boolean isPreference(String where, String key) {

		SharedPreferences prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE);

		if (prefs.contains(key)) return true;
		return false;

	}

	/**
	 * Get the list of favorites to add to the file list
	 *
	 * @return
	 */
	public static Map<String, ?> getPreferences(String where) {
		SharedPreferences prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE);
		return prefs.getAll();
	}

	/**
	 * Get a single item from the list
	 *
	 * @param where
	 * @param key
	 * @return
	 */
	public static String getPreference(String where, String key) {

		SharedPreferences prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE);
		return prefs.getString(key, null);
	}

	/**
	 * Set/remove as favorite using SharedPreferences, can't repeat names
	 * The type of operation is switched by a boolean
	 */
	public static void handlePreference(String where, String key, String value, boolean add) {


		SharedPreferences prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();

		if (!add) {
			Log.i("OUT", "Removing " + key);
			editor.remove(key);
		} else {
			Log.i("OUT", "Putting favorite " + key);
			editor.putString(key, value);
		}

		editor.commit();

	}
}
