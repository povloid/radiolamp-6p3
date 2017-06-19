#!/bin/sh


cd radiolamp-6p3
lein clean
lein install
cd -

cd radiolamp-6p3-complex-webusers
lein clean
lein install
cd -

cd radiolamp-6p3-complex-dataform
lein clean
lein install
cd -

cd radiolamp-6p3-complex-areal
lein clean
lein install
cd -

cd radiolamp-6p3-complex-page
lein clean
lein install
cd -

cd radiolamp-6p3-complex-scontent
lein clean
lein install
cd -




cd radiolamp-6p3s
lein clean
lein install
cd -

cd radiolamp-6p3s-complex-webusers
lein clean
lein install
cd -

cd radiolamp-6p3s-complex-dataform
lein clean
lein install
cd -

cd radiolamp-6p3s-complex-areal
lein clean
lein install
cd -

cd radiolamp-6p3s-complex-page
lein clean
lein install
cd -

cd radiolamp-6p3s-complex-scontent
lein clean
lein install
cd -

cd radiolamp-6p3s-d3js
lein clean
lein install
cd -

cd radiolamp-6p3s-yandex
lein clean
lein install
cd -
