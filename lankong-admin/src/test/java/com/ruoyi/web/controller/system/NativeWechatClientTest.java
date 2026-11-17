package com.ruoyi.web.controller.system;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeWechatClientTest {
    static class Connection extends HttpURLConnection {
        String body; int status=200;boolean closed;
        Connection(String body) throws Exception {super(new URL("https://api.weixin.qq.com/"));this.body=body;}
        public int getResponseCode(){return status;}
        public InputStream getInputStream(){return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));}
        public void disconnect(){closed=true;}
        public boolean usingProxy(){return false;}
        public void connect(){}
    }
    NativeWechatClient client(Connection connection) {
        return new NativeWechatClient(){@Override protected HttpURLConnection connect(String url){return connection;}};
    }
    @Test void validIdentityIsHashedAndConnectionIsClosed() throws Exception {
        Connection c=new Connection("{\"openid\":\"synthetic_openid\",\"access_token\":\"DO_NOT_EXPOSE\"}");
        assertEquals("31ecf49407cd34e9cabc9a62eb85d08fe27cd7e20eb64eb50799ff6d411461df",client(c).exchange("wx1234567890abcdef","secret","code"));assertTrue(c.closed);
    }
    @Test void subjectMatchesGoldenValuesFromLegacyNodeStoreDigest() {
        // Generated using the original store.mjs digest(), independently of this Java implementation.
        assertEquals("31ecf49407cd34e9cabc9a62eb85d08fe27cd7e20eb64eb50799ff6d411461df",NativeWechatClient.subject("wx1234567890abcdef","synthetic_openid"));
        assertEquals("525713249f24c2c18c06ea740357fbdd5f711bb23e147d10f8dab52a128f7502",NativeWechatClient.subject("wx_test","test_openid_-"));
        assertEquals("17e828e70938db62e4faa2f6962540df7d8a00a59a43ce7eb65b4c60f2b665ca",NativeWechatClient.subject("wxotheraccount","synthetic_openid"));
    }
    @Test void wechatErrorRetainsOnlyNumericCode() throws Exception {
        Connection c=new Connection("{\"errcode\":40029,\"errmsg\":\"SECRET_URL_SHOULD_NOT_APPEAR\"}");
        NativeWechatClient.AuthorizationException e=assertThrows(NativeWechatClient.AuthorizationException.class,()->client(c).exchange("wx_test","secret","code"));
        assertEquals(40029,e.getErrorCode());assertFalse(e.toString().contains("SECRET_URL"));assertTrue(c.closed);
    }
    @Test void badStatusMissingIdentityAndOversizedBodyCannotAuthenticate() throws Exception {
        Connection c=new Connection("{}");c.status=503;
        assertThrows(IOException.class,()->client(c).exchange("wx_test","secret","code"));assertTrue(c.closed);
        Connection empty=new Connection("{}");assertThrows(IOException.class,()->client(empty).exchange("wx_test","secret","code"));
        Connection huge=new Connection(new String(new char[33000]).replace('\0','a'));assertThrows(IOException.class,()->client(huge).exchange("wx_test","secret","code"));assertTrue(huge.closed);
    }
}
