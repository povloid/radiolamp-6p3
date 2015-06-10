(defproject ixinfestor "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-beta3"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [clj-time "0.6.0"]
                 [korma "0.4.2"]
                 
                 [net.sourceforge.htmlcleaner/htmlcleaner "2.12"]

                 ;; Нужно только на время разработки
                 ;;[hiccup-bridge "1.0.1"]
                 ]

  :source-paths ["src" "src-cljs"]
  
  :profiles
  {:dev
   {:dependencies [
                   [org.clojure/clojurescript "0.0-3269"]
                   [prismatic/dommy "1.1.0"]
                   [cljs-ajax "0.3.11"]
                   [hipo "0.4.0"]
                   ]
    :plugins [[lein-cljsbuild "1.0.6"]
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
