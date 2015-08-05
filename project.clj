(defproject ixinfestor "0.1.2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]

                 [ring "1.4.0"]
                 [ring/ring-json "0.3.1"]

                 ;;[ring-transit "0.1.3"]
                 [prismatic/plumbing "0.4.4"]
                 [com.cognitect/transit-clj "0.8.275"]


                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [com.cemerick/friend "0.2.1"]

                 [clj-time "0.10.0"]

                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [korma "0.4.2"]

                 [net.sourceforge.htmlcleaner/htmlcleaner "2.13"]

                 [com.draines/postal "1.11.3"]
                 [image-resizer "0.1.7"]


                 ;; CLOJURESCRIPT LIBRARIES
                 [org.clojure/clojurescript "1.7.28"]
                 [prismatic/dommy "1.1.0"]

                 [cljs-ajax "0.3.14"]
                 [com.cognitect/transit-cljs "0.8.220"]

                 [hipo "0.4.0"]

                 [org.omcljs/om "0.8.8"]
                 [sablono "0.3.4"]
                 [secretary "1.2.3"]


                 ]

  :source-paths ["src" "src-cljs"]

  :plugins [;;[lein-cljsbuild "1.0.6"]
            [lein-ancient "0.6.7"]]

  :aot [ixinfestor.core
        ixinfestor.core-web
        ixinfestor.core-web-bootstrap
        ixinfestor.core-handler
        ixinfestor.transit]

  ;;:omit-source true
  :jar-exclusions [#"(?:^|ixinfestor/)*.clj\z"]

  )
