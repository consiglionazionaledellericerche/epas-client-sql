#!/bin/bash

echo "=== " `date +'%Y/%m/%d %H:%M:%S'` "==="
# Posiziona sulla directory dove si trova questa procedura
cd `dirname $0`
# Scarica i dati
echo "===" eseguo il download delle timbrature
# -Djava.security.egd=file:///dev/urandom sistema il problema del connection reset
java -Djava.security.egd=file:///dev/urandom -jar epas-client-sql-1.0-all.jar -test

exit $?
