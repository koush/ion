package com.example.ion.test;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.test.AndroidTestCase;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by koush on 5/22/13.
 */
public class ContentTests extends AndroidTestCase {
    public void testContact() throws Exception {
        Cursor c = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        String id = null;
        while (c.moveToNext()) {
            id = c.getString(c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
            if (id != null)
                break;
        }
        Ion.with(getContext()).load(id).write(new FileOutputStream("/sdcard/test.png")).get();
        assertNotNull(id);
        assertNotNull(c);
    }
}
