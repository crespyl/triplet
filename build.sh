#!/usr/bin/env bash

[ -z "$1" ] && echo "Specify build action [dev|release|licenses]" && exit

package() {
    npx electron-packager . "$1" \
        --out target \
        --overwrite \
        --asar \
        --prune=true \
        --ignore="src|node_modules|.git|.dir-locals.el|target|shadow-cljs.edn|build.sh|(.shadow-cljs/builds/.+/dev)|NODE_DEPS" \
        --app-copyright "Peter Jacobs <peter@crespyl.net>" \
        --extraResource=NODE_DEPS \
        --platform linux,win32
}

licensetool() {
        FILE=mktemp
        npx license-checker --json > $FILE
        ./licensetool.rb $FILE NODE_DEPS
        rm $FILE
        #todo find some way to do the same for cljs deps
}

case $1 in
        dev)
                npm run build
                package Triplet-Dev
                ;;
        release)
                npm run release
                package Triplet
                ;;
        licenses)
                licensetool
                ;;
esac
