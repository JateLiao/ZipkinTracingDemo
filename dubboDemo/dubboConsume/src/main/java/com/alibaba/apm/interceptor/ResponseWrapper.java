package com.alibaba.apm.interceptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/6/3 19:30
 */
public class ResponseWrapper extends javax.servlet.http.HttpServletResponseWrapper {
    private ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private HttpServletResponse response;
    private PrintWriter pwrite;
    
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new MyServletOutputStream(bytes); // 将数据写到 byte
    }
    
    /**
     * 重写父类的 getWriter() 方法，将响应数据缓存在 PrintWriter 中
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        try { pwrite = new PrintWriter(new OutputStreamWriter(bytes, "utf-8")); } catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        return pwrite;
    }
    
    /**
     * 获取缓存在 PrintWriter 中的响应数据
     * @return
     */
    public byte[] getBytes() {
        if (null != pwrite) {
            pwrite.close();
            return bytes.toByteArray();
        }
        if (null != bytes) { try { bytes.flush(); } catch (IOException e) { e.printStackTrace(); } }
        return bytes.toByteArray();
    }
    
    class MyServletOutputStream extends ServletOutputStream {
        private ByteArrayOutputStream ostream;
        
        public MyServletOutputStream(ByteArrayOutputStream ostream) {
            this.ostream = ostream;
        }
        
        @Override
        public void write(int b) throws IOException {
            ostream.write(b);
            // 将数据写到 stream　中
        }
    
        @Override
        public boolean isReady() {
            return false;
        }
    
        @Override
        public void setWriteListener(WriteListener listener) {
        
        }
    }
    
    
    //private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    //private HttpServletResponse response;
    //
    ///**
    // * Constructs a response adaptor wrapping the given response.
    // *
    // * @param response The response to be wrapped
    // *
    // * @throws IllegalArgumentException
    // *             if the response is null
    // */
    //public ResponseWrapper(HttpServletResponse response) {
    //    super(response);
    //    this.response = response;
    //}
    //
    //public byte[] getBody() {
    //    return byteArrayOutputStream.toByteArray();
    //}
    //
    //@Override
    //public ServletOutputStream getOutputStream() {
    //    return new ServletOutputStreamWrapper(this.byteArrayOutputStream , this.response);
    //}
    //
    //@Override
    //public PrintWriter getWriter() throws IOException {
    //    return new PrintWriter(new OutputStreamWriter(this.byteArrayOutputStream , this.response.getCharacterEncoding()));
    //}
    //
    //
    ////@Data
    ////@AllArgsConstructor
    //private static class ServletOutputStreamWrapper extends ServletOutputStream {
    //
    //    private ByteArrayOutputStream outputStream;
    //    private HttpServletResponse response;
    //
    //    public ServletOutputStreamWrapper() {}
    //    public ServletOutputStreamWrapper(ByteArrayOutputStream outputStream, HttpServletResponse response) {
    //        this.outputStream = outputStream;
    //        this.response = response;
    //    }
    //
    //    @Override
    //    public boolean isReady() {
    //        return true;
    //    }
    //
    //    @Override
    //    public void setWriteListener(WriteListener listener) {
    //
    //    }
    //
    //    @Override
    //    public void write(int b) throws IOException {
    //        this.outputStream.write(b);
    //    }
    //
    //    @Override
    //    public void flush() throws IOException {
    //        if (! this.response.isCommitted()) {
    //            byte[] body = this.outputStream.toByteArray();
    //            ServletOutputStream outputStream = this.response.getOutputStream();
    //            outputStream.write(body);
    //            outputStream.flush();
    //        }
    //    }
    //}
}
