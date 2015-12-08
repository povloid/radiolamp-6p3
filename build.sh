#!/bin/sh
#!/bin/sh
cd radiolamp-6P3
lein clean
lein install
cd ../radiolamp-6P3S
lein clean
lein install
cd ..
