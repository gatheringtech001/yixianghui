package com.ruoyi.web.internal.talent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TalentCenterHmacFilter extends OncePerRequestFilter
{
    public static final String SERVICE_ID_ATTRIBUTE = "talentCenterServiceId";
    private static final String PREFIX = "/internal/talent-center/";
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private final TalentCenterHmacVerifier verifier;

    public TalentCenterHmacFilter(TalentCenterHmacVerifier verifier)
    {
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        try
        {
            byte[] body = readBody(request);
            CachedRequest wrapped = new CachedRequest(request, body);
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String serviceId = request.getHeader("X-Service-Id");
            verifier.verify(serviceId, request.getHeader("X-Timestamp"), request.getHeader("X-Nonce"),
                    request.getHeader("X-Signature"), request.getMethod(), path, body);
            wrapped.setAttribute(SERVICE_ID_ATTRIBUTE, serviceId);
            chain.doFilter(wrapped, response);
        }
        catch (TalentCenterApiException e)
        {
            response.setStatus(e.getHttpStatus());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(AjaxResult.error(e.getHttpStatus(), e.getMessage())));
        }
    }

    private byte[] readBody(HttpServletRequest request) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int count;
        ServletInputStream input = request.getInputStream();
        while ((count = input.read(buffer)) != -1)
        {
            total += count;
            if (total > MAX_BODY_BYTES)
            {
                throw new TalentCenterApiException(413, "请求体过大");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static class CachedRequest extends HttpServletRequestWrapper
    {
        private final byte[] body;

        CachedRequest(HttpServletRequest request, byte[] body)
        {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream()
        {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream()
            {
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
                @Override public int read() { return input.read(); }
            };
        }

        @Override
        public BufferedReader getReader()
        {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
