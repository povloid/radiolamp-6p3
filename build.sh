#!/bin/sh

cd radiolamp-6p3
lein clean
lein install

cd ../radiolamp-6p3s
lein clean
lein install

cd ../radiolamp-6p3s-complex-dataform
lein clean
lein install

cd ../radiolamp-6p3s-d3js
lein clean
lein install

cd ../radiolamp-6p3s-yandex
lein clean
lein install

cd ..
