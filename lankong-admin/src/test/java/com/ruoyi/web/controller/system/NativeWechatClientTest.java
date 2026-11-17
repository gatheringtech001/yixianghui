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
        assertEquals(NativeWechatClient.subject("wx_test","synthetic_openid"),client(c).exchange("wx_test","secret","code"));assertTrue(c.closed);
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
