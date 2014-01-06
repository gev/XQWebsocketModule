/*
 * XQWebsocketModule
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software Foundation
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

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
    static public String maxIdleTime = "";
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
        maxIdleTime = getServletConfig().getInitParameter("max-idle-time");
    }
}
