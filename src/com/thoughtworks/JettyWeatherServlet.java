package com.thoughtworks;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JettyWeatherServlet extends HttpServlet {

    private MessageSender messageSender = null;
    private static final Integer TIMEOUT = 5 * 1000;

    @Override
    public void destroy() {
        messageSender.stop();
        messageSender = null;
    }

    @Override
    public void init() throws ServletException {
        messageSender = new MessageSender();
        Thread messageSenderThread =
                new Thread(messageSender, "MessageSender[" + getServletContext().getContextPath() + "]");
        messageSenderThread.setDaemon(true);
        messageSenderThread.start();

    }

    public void begin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("org.apache.tomcat.comet", Boolean.TRUE);
        request.setAttribute("org.apache.tomcat.comet.timeout", TIMEOUT);
        log("Begin for session: " + request.getSession(true).getId());
        Weatherman weatherman = new Weatherman(95118, 32408);
        weatherman.start();
        messageSender.setConnection(response);
    }

    public void end(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (request) {
            request.removeAttribute("org.apache.tomcat.comet");

            Continuation continuation = ContinuationSupport.getContinuation(request, request);
            if (continuation.isPending()) {
                continuation.resume();
            }
        }
        log("End for session: " + request.getSession(true).getId());
    }

    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log("Error for session: " + request.getSession(true).getId());
        end(request, response);
    }

    public boolean read(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (request) {
            // TODO: wrap response so we can reset timeout on writes.

            Continuation continuation = ContinuationSupport.getContinuation(request, request);

            if (!continuation.isPending()) {
                begin(request, response);
            }

            Integer timeout = (Integer) request.getAttribute("org.apache.tomcat.comet.timeout");
            boolean resumed = continuation.suspend(timeout == null ? 10000 : timeout.intValue());

            if (!resumed) {
                error(request, response);
            }
        }
    }

    public void setTimeout(HttpServletRequest request, HttpServletResponse response, int timeout) throws IOException, ServletException,
            UnsupportedOperationException {
        request.setAttribute("org.apache.tomcat.comet.timeout", new Integer(timeout));
    }
}


