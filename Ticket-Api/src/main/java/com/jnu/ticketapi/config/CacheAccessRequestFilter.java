package com.jnu.ticketapi.config;


import com.amazonaws.util.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CacheAccessRequestFilter extends HttpServletRequestWrapper {
    private ByteArrayOutputStream contents = new ByteArrayOutputStream();

    public CacheAccessRequestFilter(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        IOUtils.copy(super.getInputStream(), contents);
        return new ServletInputStream() {
            private ByteArrayInputStream buffer = new ByteArrayInputStream(contents.toByteArray());

            @Override
            public int read() {
                return buffer.read();
            }

            @Override
            public boolean isFinished() {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {}
        };
    }

    public byte[] getContents() throws IOException {
        return this.getInputStream().readAllBytes();
    }
}
