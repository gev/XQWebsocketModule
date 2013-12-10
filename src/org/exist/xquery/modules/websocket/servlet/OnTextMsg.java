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
import org.exist.EXistException;
import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.memtree.MemTreeBuilder;
import org.exist.memtree.SAXAdapter;
import org.exist.security.*;
import org.exist.security.xacml.AccessContext;
import org.exist.source.Source;
import org.exist.source.SourceFactory;
import org.exist.storage.DBBroker;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.modules.websocket.ConnectionInfo;
import org.exist.xquery.modules.websocket.WebsocketModule;
import org.exist.xquery.value.Sequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static org.exist.xquery.modules.websocket.servlet.XQWSServlet.brokerPool;
import static org.exist.xquery.modules.websocket.servlet.XQWSServlet.xqPath;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:11
 */
public class OnTextMsg implements WebSocket.OnTextMessage {
    public ConnectionInfo info;

    public OnTextMsg(Set users, HttpServletRequest request) {
        WebsocketModule.users = users;
        this.info = new ConnectionInfo(request);
    }

    public ConnectionInfo getInfo() {
        return info;
    }

    @Override
    public void onMessage(String s) {
        try {
            MemTreeBuilder builder = new MemTreeBuilder();
            builder.startDocument();
            builder.startElement(new QName("ws-message"), null);
            builder.addAttribute(new QName("from"), info.getUid());

            Document doc = fromString(s);
            domToMemTree(doc.getDocumentElement(), builder);

            builder.endElement();
            builder.endDocument();
            execute(builder.getDocument());
        } catch (XPathException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onOpen(Connection connection) {
        this.info.setConnection(connection);
        WebsocketModule.users.add(this);
    }

    @Override
    public void onClose(int i, String s) {
        WebsocketModule.users.remove(this);
    }

    private static Subject authenticate() throws AuthenticationException {
        org.exist.security.SecurityManager sm = brokerPool.getSecurityManager();
        return sm.authenticate("admin", "");
    }

    public static Sequence execute(Sequence contextSequence) throws XPathException, PermissionDeniedException, AuthenticationException {
        DBBroker broker = null;
        try {
            broker = brokerPool.get(authenticate());
            broker.getConfiguration().setProperty(XQueryContext.PROPERTY_XQUERY_RAISE_ERROR_ON_FAILED_RETRIEVAL, true);
            XQuery xqs = broker.getXQueryService();
            XQueryContext context = new XQueryContext(brokerPool, AccessContext.XMLDB);

            return xqs.execute(
                    xqs.compile(context, getXQSrc(broker, xqPath)),
                    contextSequence
            );
        } catch (EXistException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            brokerPool.release(broker);
        }
        return Sequence.EMPTY_SEQUENCE;
    }

    public static Source getXQSrc(DBBroker broker, String uri) throws PermissionDeniedException, IOException {
        return SourceFactory.getSource(broker, brokerPool.getConfiguration().getExistHome().getCanonicalPath(), uri, false);
    }

    private void domToMemTree(Element e, MemTreeBuilder builder) {
        builder.startElement(new QName(e.getNodeName()), null);

        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            builder.addAttribute(new QName(attr.getNodeName()), attr.getTextContent());
        }

        for (Node ch = e.getFirstChild(); ch != null; ch = ch.getNextSibling())
            switch (ch.getNodeType()) {
                case Node.ELEMENT_NODE:
                    domToMemTree((Element) ch, builder);
                    break;
                case Node.TEXT_NODE:
                    builder.characters(ch.getNodeValue());
                    break;
            }

        builder.endElement();
    }

    private Document fromString(String s) {
        final SAXAdapter adapter = new SAXAdapter();
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final InputSource src = new InputSource(new StringReader(s));
            final SAXParser parser = factory.newSAXParser();
            XMLReader xr = parser.getXMLReader();
            xr.setContentHandler(adapter);
            xr.setProperty(Namespaces.SAX_LEXICAL_HANDLER, adapter);
            xr.parse(src);
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
        } catch (final SAXException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return adapter.getDocument();
    }
}
