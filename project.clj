(defproject ixinfestor "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]

                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]

                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [com.cemerick/friend "0.2.1"]

                 [clj-time "0.9.0"]
                 [korma "0.4.2"]
                 [net.sourceforge.htmlcleaner/htmlcleaner "2.12"]
                 [image-resizer "0.1.6"]
                 ]

  :source-paths ["src" "src-cljs"]

  :plugins [;;[lein-cljsbuild "1.0.6"]
            [lein-ancient "0.6.7"]]

  :aot [ixinfestor.core
        ixinfestor.core-web
        ixinfestor.core-web-bootstrap
        ixinfestor.core-handler]
  
  ;;:omit-source true
  :jar-exclusions [#"(?:^|ixinfestor/)*.clj\z"]

  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "0.0-3308"]
                             [prismatic/dommy "1.1.0"]
                             [cljs-ajax "0.3.13"]
                             [hipo "0.4.0"]
                             ]
              :plugins [[lein-cljsbuild "1.0.6"]
                        [com.cemerick/clojurescript.test "0.3.3"]]}
             }

  ;;:hooks [leiningen.cljsbuild]
  :cljsbuild {
              :builds{
                      :dev {:source-paths ["src-cljs"]
                            :compiler {:output-to  "target/main.js"
                                       :output-dir "target"
                                       ;;:source-map "target/main.js.map"
                                       :pretty-print true
                                       :optimizations :whitespace}}

                      :prod {:source-paths ["src-cljs"]
                             :compiler {:output-to  "target/main.js"
                                        :output-dir "target"
                                        ;;:source-map "target/main.js.map"
                                        :pretty-print false
                                        :optimizations :advanced ;; мощная оптимизация, но начинаются проблемы с внешними либами
                                        ;;:externs ["externs/jquery-1.9.js" "externs/tarsonis.js"]
                                        }}
                      }
              }
  )
