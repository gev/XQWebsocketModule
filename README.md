XQWebsocketModule
=================

Usage
=====

add

    <servlet>
        <servlet-name>XQWebSocket</servlet-name>
        <servlet-class>org.exist.xquery.modules.websocket.servlet.XQWSServlet</servlet-class>

        <init-param>
             <param-name>default-xq-path</param-name>
             <param-value>xmldb:exist:///db/apps/yourapp/ws-controller.xq</param-value>
        </init-param>
    </servlet>
    
into your web.xml

Functions
=========

    ws:connect($map as map) as object

Establish WebSocket conection.

$map keys:

    "url" as xs:string, "onopen" as function, "onclose" as function, onmessage as function

    "onopen" := function() { () }

    "onclose" := function($map as map) { $map("message") as xs:string, $map("close-code") as xs:integer }

    "onmessage" := function($message as xs:string) { () }
    
return WebSocket connection object


    ws:close($connection as object)

Close connection


    ws:get-client($uid as xs:string) as object

Get client from servlet connections


    ws:list-clients() as node()*

List all servlet connections in xml format:

    <ws-connection uid="" query="" path="">
        <headers>
            <h name="" value=""/>
        </headers>
        <connection>
            <remote-addr>{IP}</remote-addr>
            <remote-host>{HOST}</remote-host>
            <remote-port>{PORT}</remote-port>
        </connection>
    </ws-connection>

.

    ws:send($users as object*, $message as xs:string)

Send a message to sequense of clients or connections


    ws:send-all($message as xs:string)

Send a message to all clients


Example
=======
on client side

    xquery version "3.0";
    
    let $cache := cache:cache("websocket")
    let $connection := cache:get($cache, "connection")
    let $connection := if (empty($connection)) then 
        let $c := ws:connect(map {
            "url" := "ws://localhost:8080/exist/ws/test",
            "onopen" := function() { util:log-system-out("is open") },
            "onclose" := function($map) { 
                util:log-system-out("is close with code: " || xs:string($map("close-code")) || " and message: " || $map("message")),
                cache:clear($cache)
            },
            "onmessage" := function($msg) { util:log-system-out($msg) }
        })
        let $put := cache:put(cache:cache("websocket"), "connection", $c)
        return $c
    else $connection
    return ws:send($connection, util:serialize(
        element hello { },
        ()
    ))
    
on server side (ws-controller.xq)

    xquery version "3.0";
    
    let $req := *
    return
    ws:send( ws:get-client($req/@from),
        util:serialize(
            $req, 
            ()
        )
    )

