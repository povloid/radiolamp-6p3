(defproject ixinfestor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]
                 [clj-time "0.6.0"]
                 [korma "0.3.3"]

                 ;; Нужно только на время разработки
                 [hiccup-bridge "1.0.1"]
                 ]

  :source-paths ["src" "src-cljs"]
  
  :profiles
  {:dev
   {:dependencies [
                   [org.clojure/clojurescript "0.0-3169"]
                   [prismatic/dommy "1.0.0"]
                   [cljs-ajax "0.3.10"]
                   [hipo "0.3.0"]
                   ]
    :plugins [[lein-cljsbuild "1.0.5"]
              [com.cemerick/clojurescript.test "0.3.3"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src-cljs" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :optimizations :whitespace
                      :pretty-print true}}}

   :aliases {"clean-test" ["do" "clean," "cljsbuild" "test"]
             "clean-install" ["do" "clean," "install"]}
   }

  )
