package org.exist.xquery.modules.websocket;

import org.exist.dom.QName;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.modules.websocket.servlet.OnTextMsg;
import org.exist.xquery.value.SequenceType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.exist.xquery.modules.websocket.WSFunctions.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author <a href="mailto:wstarcev@gmail.com">Vasilii Startsev</a>
 *         Date: 09.12.13
 *         Time: 18:10
 */
public class WebsocketModule extends AbstractInternalModule {

    public static Set<OnTextMsg> users = new CopyOnWriteArraySet();

    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/websocket";

    public final static String PREFIX = "ws";
    public final static String RELEASED_IN_VERSION = "eXist-2.0";

    public static FunctionDef functionDef(Class<? extends BasicFunction> clazz, String name, String description, SequenceType retType, SequenceType... params) {
        return new FunctionDef(
                new FunctionSignature(
                        new QName(name, NAMESPACE_URI, PREFIX),
                        description,
                        params,
                        retType
                ),
                clazz
        );
    }

    private final static FunctionDef[] functions = {
            close,
            connect,
            get_client,
            list_clients,
            send,
            send_xml,
            send_all
    };

    public WebsocketModule(Map<String, List<? extends Object>> parameters) {
        super(functions, parameters);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "Fenomen WebSocket module";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }
}
