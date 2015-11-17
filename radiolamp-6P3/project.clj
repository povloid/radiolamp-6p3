(defproject radiolamp-6P3 "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]

                 [ring "1.4.0"]
                 [ring/ring-json "0.4.0"]

                 ;;[ring-transit "0.1.3"]
                 [prismatic/plumbing "0.5.2"] ;; Просит транзит
                 [com.cognitect/transit-clj "0.8.285"]

                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [com.cemerick/friend "0.2.1"]

                 [clj-time "0.11.0"]

                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [korma "0.4.2"]

                 [net.sourceforge.htmlcleaner/htmlcleaner "2.15"]

                 [com.draines/postal "1.11.4"]
                 [image-resizer "0.1.8"]                              
                 ]

  :aot :all
  :omit-source true

  )
