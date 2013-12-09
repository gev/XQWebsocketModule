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
import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xquery.*;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.StringValue;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:33
 */
public class TextMessageWorker implements WebSocket.OnTextMessage {
    FunctionReference onOpen = null;
    FunctionReference onClose = null;
    FunctionReference onMessage = null;

    XQueryContext context;

    DBBroker broker = null;
    BrokerPool pool = null;

    Subject subject = null;

    public TextMessageWorker(XQueryContext context, final Subject subject, FunctionReference openFun, FunctionReference closeFun, FunctionReference msgFun) throws EXistException {
        onOpen = openFun;
        onClose = closeFun;
        onMessage = msgFun;
        this.context = context;
        pool = BrokerPool.getInstance();
        this.subject = subject;
    }

    @Override
    public void onMessage(final String message) {
        try {
            final XQueryContext cntx = context;
            pool = BrokerPool.getInstance();
            broker = pool.get(subject);
            broker.getConfiguration().setProperty(XQueryContext.PROPERTY_XQUERY_RAISE_ERROR_ON_FAILED_RETRIEVAL, true);
            XQuery service = broker.getXQueryService();
            onMessage.setContext(cntx);
            onMessage.setArguments(new ArrayList<Expression>() {{
                add(new LiteralValue(context, new StringValue(message)));
            }});
            service.execute(onMessage.getCall(), null);
        } catch (XPathException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (EXistException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.release(broker);
        }
    }

    @Override
    public void onOpen(Connection connection) {
        try {
            final XQueryContext cntx = context;
            pool = BrokerPool.getInstance();
            broker = pool.get(subject);
            broker.getConfiguration().setProperty(XQueryContext.PROPERTY_XQUERY_RAISE_ERROR_ON_FAILED_RETRIEVAL, true);
            XQuery service = broker.getXQueryService();
            onOpen.setContext(cntx);
            service.execute(onOpen.getCall(), null);
        } catch (XPathException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (EXistException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.release(broker);
        }
    }

    @Override
    public void onClose(final int closeCode, final String message) {
        try {
            final XQueryContext cntx = context;
            pool = BrokerPool.getInstance();
            broker = pool.get(subject);
            broker.getConfiguration().setProperty(XQueryContext.PROPERTY_XQUERY_RAISE_ERROR_ON_FAILED_RETRIEVAL, true);
            XQuery service = broker.getXQueryService();
            onClose.setContext(cntx);
            onClose.setArguments(new ArrayList<Expression>() {{
                MapType map = new MapType(cntx);
                map.add(new StringValue("message"), new StringValue(message));
                map.add(new StringValue("close-code"), new IntegerValue(closeCode));
                add(new LiteralValue(cntx, map));
            }});
            service.execute(onClose.getCall(), null);
        } catch (XPathException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (EXistException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.release(broker);
        }
    }
}
