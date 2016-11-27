#!/bin/bash -e

JS_LIB=./static/lib/js
mkdir -p $JS_LIB
wget -O $JS_LIB/ipaddr.min.js https://raw.githubusercontent.com/whitequark/ipaddr.js/master/ipaddr.min.js

