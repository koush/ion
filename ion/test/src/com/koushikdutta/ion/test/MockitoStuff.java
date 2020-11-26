package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.mock.MockLoader;
import com.koushikdutta.ion.mock.MockRequestHandler;

import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Created by koush on 3/6/15.
 */
public class MockitoStuff extends AndroidTestCase {
    public void testMock() throws Exception {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().toString());

        MockRequestHandler handler = Mockito.mock(MockRequestHandler.class);
        when(handler.request("http://www.myfakedomain.com")).thenReturn("hello world");
        when(handler.request("http://www.myfakedomain.com/user")).thenReturn("koush");

        MockLoader.install(Ion.getDefault(getContext()), handler);

        String result = Ion.with(getContext())
                .load("http://www.myfakedomain.com")
                .asString()
                .get();
        assertEquals(result, "hello world");

        result = Ion.with(getContext())
                .load("http://www.myfakedomain.com/user")
                .asString()
                .get();
        assertEquals(result, "koush");
    }

}
