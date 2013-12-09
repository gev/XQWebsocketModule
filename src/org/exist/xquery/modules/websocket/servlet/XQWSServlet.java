package org.exist.xquery.modules.websocket.servlet;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.exist.EXistException;
import org.exist.storage.BrokerPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:11
 */
public class XQWSServlet extends WebSocketServlet {
    static public String xqPath = "";
    static public BrokerPool brokerPool = null;

    public final Set users = new CopyOnWriteArraySet();

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new OnTextMsg(users, request);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            brokerPool = BrokerPool.getInstance();
        } catch (EXistException e) {
            e.printStackTrace();
        }
        xqPath = getServletConfig().getInitParameter("default-xq-path");
    }
}
