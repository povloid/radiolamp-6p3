(defproject radiolamp-6P3 "0.4.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]

                 [ring "1.5.0"]
                 [ring/ring-json "0.4.0"]

                 ;;[ring-transit "0.1.3"]
                 [prismatic/plumbing "0.5.3"] ;; Просит транзит
                 [com.cognitect/transit-clj "0.8.297"]

                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [com.cemerick/friend "0.2.3"]

                 [clj-time "0.13.0"]

                 [org.postgresql/postgresql "9.4.1212"]
                 ;;[org.postgresql/postgresql "9.4-1206-jdbc41"]
                 [korma "0.4.2"]
                 

                 [net.sourceforge.htmlcleaner/htmlcleaner "2.18"]

                 [com.draines/postal "2.0.2"]
                 [image-resizer "0.1.9"]
                 ]

  :aot :all
  :omit-source true

  )
