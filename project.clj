(defproject muchjs "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [fipp "0.6.6"]
                 [aprint "0.1.3"]
                 [org.clojure/tools.reader "1.0.0-beta3"]
                 [regexpforobj "1.0.0-alpha3-SNAPSHOT"]
                 ]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]]

  :source-paths ["src"]

  :clean-targets ["server.js"
                  "target"]

  :figwheel {
    :server-port 5377          ;; default is 3449
  }

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :figwheel true
              :compiler {
                :main muchjs.core
                :output-to "target/server_dev/muchjs.js"
                :output-dir "target/server_dev"
                :target :nodejs
                :optimizations :none
                :source-map true}}
             {:id "prod"
              :source-paths ["src"]
              :compiler {
                :output-to "server.js"
                :output-dir "target/server_prod"
                :target :nodejs
                :optimizations :simple}}]})
