#!/bin/sh

cd `dirname $0`

java -X${java.mem.mx} -Xms1500M \
     -Dorg.protege.owl.server.configuration=server-configuration.json \
     -cp "bundles/*" \
     org.protege.editor.owl.server.http.HTTPServer
