#!/bin/bash
mv -f  /app/jar/obs.jar /app/jar/obs.jar.bak
pid=`ps -ef | grep obs | grep 'java' | awk '{printf $2}'`
if [ -z $pid ];

 then
 
   echo "obs未启动"
 else 
 
   kill -9 $pid && echo "obs已关闭"
fi
