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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.exist.storage.serializers.Serializer;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xquery.*;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.modules.websocket.servlet.OnTextMsg;
import org.exist.xquery.value.*;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:10
 */
public class WSFunctions extends BasicFunction {

    public static final FunctionReturnSequenceType JAVA_O = new FunctionReturnSequenceType(
            Type.JAVA_OBJECT, Cardinality.ZERO_OR_ONE, "Sequence of nodes");

    public static final FunctionReturnSequenceType XML_SEQUENCE = new FunctionReturnSequenceType(
            Type.NODE, Cardinality.ZERO_OR_MORE, "Sequence of nodes");

    public static final FunctionReturnSequenceType EMPTY = new FunctionReturnSequenceType(
            Type.EMPTY, Cardinality.EMPTY, "Sequence of nodes");

    public static final FunctionParameterSequenceType MESSAGE = new FunctionParameterSequenceType(
            "message", Type.STRING, Cardinality.ONE,
            ""
    );

    public static final FunctionParameterSequenceType XML_MESSAGE = new FunctionParameterSequenceType(
            "xml-message", Type.NODE, Cardinality.ONE,
            ""
    );

    public static final FunctionParameterSequenceType UID = new FunctionParameterSequenceType(
            "uid", Type.STRING, Cardinality.ONE,
            ""
    );

    public static final FunctionParameterSequenceType MAP = new FunctionParameterSequenceType(
            "uid", Type.MAP, Cardinality.ONE,
            ""
    );

    public static final FunctionParameterSequenceType USERS = new FunctionParameterSequenceType(
            "users", Type.JAVA_OBJECT, Cardinality.ZERO_OR_MORE,
            ""
    );

    static FunctionDef functionDef(String name, String description, SequenceType retType, SequenceType... args) {
        return WebsocketModule.functionDef(
                WSFunctions.class,
                name, description,
                retType, args
        );
    }

    boolean isCalledAs(FunctionDef def) {
        return getSignature().equals(def.getSignature());
    }

    public final static FunctionDef send_all = functionDef("send-all", "send message to all clients",
            EMPTY, MESSAGE
    );

    public final static FunctionDef send = functionDef("send", "send message to sequence of clients or connections",
            EMPTY, USERS, MESSAGE
    );

    public final static FunctionDef send_xml = functionDef("send", "send message to sequence of clients or connections",
            EMPTY, USERS, XML_MESSAGE
    );

    public final static FunctionDef list_clients = functionDef("list-clients", "return clients in xml format",
            XML_SEQUENCE
    );

    public final static FunctionDef get_client = functionDef("get-client", "get client by uid",
            JAVA_O, UID
    );

    public final static FunctionDef connect = functionDef("connect", "connect to server. map { 'url', 'onclose' := function(){}, 'onmessage' := function(){}, 'onclose' := function(){} }",
            JAVA_O, MAP
    );

    public final static FunctionDef close = functionDef("close", "close connection",
            JAVA_O, USERS
    );

    public WSFunctions(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {

        if (isCalledAs(close)) {
            WebSocket.Connection connection = args[0].itemAt(0).toJavaObject(WebSocket.Connection.class);
            connection.close();
        }

        if (isCalledAs(connect)) {
            final MapType map = (MapType) args[0];

            try {
                WebSocketClientFactory factory = new WebSocketClientFactory();
                factory.start();

                WebSocketClient client = factory.newWebSocketClient();

                Sequence mUrl = map.get(new StringValue("url"));
                WebSocket.Connection connection = client.open(new URI(mUrl.getStringValue()), new TextMessageWorker(
                        context,
                        context.getSubject(),
                        (FunctionReference) map.get(new StringValue("onopen")),
                        (FunctionReference) map.get(new StringValue("onclose")),
                        (FunctionReference) map.get(new StringValue("onmessage"))
                )).get(5, TimeUnit.SECONDS);
                return new JavaObjectValue(connection);
            } catch (InterruptedException e) {
                throw new XPathException(e.getMessage());
            } catch (ExecutionException e) {
                throw new XPathException(e.getMessage());
            } catch (TimeoutException e) {
                throw new XPathException(e.getMessage());
            } catch (URISyntaxException e) {
                throw new XPathException(e.getMessage());
            } catch (IOException e) {
                throw new XPathException(e.getMessage());
            } catch (Exception e) {
                throw new XPathException(e.getMessage());
            }
        }

        if (isCalledAs(send_all)) {
            for (OnTextMsg client : WebsocketModule.users) try {
                client.getInfo().connection.sendMessage(args[0].getStringValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (isCalledAs(send)) {
            SequenceIterator users = args[0].iterate();
            while (users.hasNext()) try {
                Object obj = users.nextItem().toJavaObject(Object.class);
                if (obj instanceof OnTextMsg)
                    ((OnTextMsg) obj).getInfo().getConnection().sendMessage(args[1].getStringValue());
                else if (obj instanceof WebSocket.Connection)
                    ((WebSocket.Connection) obj).sendMessage(args[1].getStringValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (isCalledAs(send_xml)) {
            SequenceIterator users = args[0].iterate();
            SequenceIterator siNode = args[1].iterate();

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            final SAXSerializer sax = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
            Properties outputProperties = new Properties();
            outputProperties.setProperty(Serializer.GENERATE_DOC_EVENTS, "false");
            try
            {
                final String encoding = outputProperties.getProperty(OutputKeys.ENCODING, "UTF-8");
                final Writer writer = new OutputStreamWriter(os, encoding);
                sax.setOutput(writer, outputProperties);
                final Serializer serializer = context.getBroker().getSerializer();
                serializer.reset();
                serializer.setProperties(outputProperties);
                serializer.setSAXHandlers(sax, sax);

                sax.startDocument();

                while(siNode.hasNext())
                {
                    final NodeValue next = (NodeValue)siNode.nextItem();
                    serializer.toSAX(next);
                }

                sax.endDocument();
                writer.close();
            } catch(final SAXException e) {
                throw new XPathException(this, "A problem occurred while serializing the node set: " + e.getMessage(), e);
            } catch (final IOException e) {
                throw new XPathException(this, "A problem occurred while serializing the node set: " + e.getMessage(), e);
            } finally {
                SerializerPool.getInstance().returnObject(sax);
            }

            try {
                String text = new String(
                        os.toByteArray(),
                        outputProperties.getProperty(OutputKeys.ENCODING, "UTF-8")
                );
                while (users.hasNext()) try {
                    Object obj = users.nextItem().toJavaObject(Object.class);
                    if (obj instanceof OnTextMsg)
                        ((OnTextMsg) obj).getInfo().getConnection().sendMessage(text);
                    else if (obj instanceof WebSocket.Connection)
                        ((WebSocket.Connection) obj).sendMessage(text);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                throw new XPathException(this, "A problem occurred while serializing the node set: " + e.getMessage(), e);
            }
        }

        if (isCalledAs(list_clients)) {
            // return NPE
            ValueSequence res = new ValueSequence();

            for (OnTextMsg client : WebsocketModule.users)
                res.addAll(client.getInfo().toNode());

            return res;
        }

        if (isCalledAs(get_client)) {
            for (OnTextMsg client : WebsocketModule.users)
                if (client.getInfo().getUid().equals(args[0].getStringValue()))
                    return new JavaObjectValue(client);
        }

        return Sequence.EMPTY_SEQUENCE;
    }
}
