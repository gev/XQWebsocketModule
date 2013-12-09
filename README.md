XQWebsocketModule
=================

Usage
=====

add

    <servlet>
        <servlet-name>XQWebSocket</servlet-name>
        <servlet-class>org.exist.xquery.modules.websocket.WebsocketModule</servlet-class>

        <init-param>
             <param-name>default-xq-path</param-name>
             <param-value>xmldb:exist:///db/apps/yourapp/ws-controller.xq</param-value>
        </init-param>
    </servlet>
    
into your web.xml
