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

package org.exist.xquery.modules.websocket;

import org.eclipse.jetty.websocket.WebSocket;
import org.exist.dom.QName;
import org.exist.memtree.MemTreeBuilder;
import org.exist.xquery.value.NodeValue;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:13
 */
public class ConnectionInfo {
    WebSocket.Connection connection;
    NodeValue xmlInfo;
    String uid = "";

    public ConnectionInfo(HttpServletRequest r) {
        this.uid = UUID.randomUUID().toString();
        this.xmlInfo = toNode(r);
    }

    public void setConnection(WebSocket.Connection c) {
        this.connection = c;
    }

    public NodeValue toNode(HttpServletRequest request) {
        MemTreeBuilder builder = new MemTreeBuilder();
        builder.startDocument();
        builder.startElement(new QName("ws-connection"), null);
        builder.addAttribute(new QName("uid"), uid);
        builder.addAttribute(new QName("query"), request.getQueryString());
        builder.addAttribute(new QName("path"), request.getRequestURI());
        builder.startElement(new QName("headers"), null);

        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            builder.startElement(new QName("h"), null);
            builder.addAttribute(new QName("name"), header);
            builder.addAttribute(new QName("value"), request.getHeader(header));
            builder.endElement();
        }

        builder.endElement();

        builder.startElement(new QName("connection"), null);

        builder.startElement(new QName("remote-addr"), null);
        builder.characters(request.getRemoteAddr());
        builder.endElement();

        builder.startElement(new QName("remote-host"), null);
        builder.characters(request.getRemoteHost());
        builder.endElement();

        builder.startElement(new QName("remote-port"), null);
        builder.characters(String.valueOf(request.getRemotePort()));
        builder.endElement();

        builder.endElement();

        builder.endElement();

        return builder.getDocument();
    }

    public String getUid() {
        return uid;
    }

    public NodeValue toNode() {
        return xmlInfo;
    }

    public WebSocket.Connection getConnection() {
        return connection;
    }
}
