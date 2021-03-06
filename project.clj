(defproject iframe-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :test-paths ["spec/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2843"]
                 [org.clojure/test.check "0.7.0"]
                 [com.cemerick/clojurescript.test "0.3.3"]
                 [org.omcljs/om "0.8.7"]
                 [environ "1.0.0"]
                 [sablono "0.3.1"]
                 [camel-snake-kebab "0.3.0"]
                 [ankha "0.1.5.1-21e6ac"]
                 [prismatic/dommy "1.0.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/om-tools "0.3.10" :exclusions [potemkin]]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [com.cemerick/clojurescript.test "0.3.3"]

            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "iframe-app.jar"

  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test"]}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        :preamble      ["react/react.min.js"]
                                        :externs       ["react/externs/react.js"]
                                        :optimizations :none
                                        :pretty-print  true
                                        :closure-warnings {:non-standard-jsdoc :off}}}
                       :test {:source-paths ["src/cljs" "spec/cljs"]
                              :notify-command ["slimerjs" "resources/runner.js"
                                               "resources/public/js/app-test.js"]
                              :compiler {:output-to     "resources/public/js/app-test.js"
                                         :output-dir    "resources/public/js/out-test"
                                         ;:source-map    "resources/public/js/out-test.js.map"
                                         :externs       ["react/externs/react.js"]
                                         :optimizations :whitespace
                                         :pretty-print  true
                                         :closure-warnings {:non-standard-jsdoc :off}}}}
              :test-commands {"slimer" ["slimerjs" "resources/runner.js"
                                        "resources/public/js/app-test.js"]}}

  :profiles {:dev {:source-paths ["env/dev/clj"]

                   :dependencies [
                                  [figwheel "0.2.5-SNAPSHOT"]
                                  [com.cemerick/piggieback "0.1.6-SNAPSHOT"]
                                  [leiningen "2.5.0"]]

                   :repl-options {:init-ns iframe-app.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.2.5-SNAPSHOT"]
                             [lein-checkouts "1.1.0"]]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]}

                   :env {:is-dev true}

                   :cljsbuild {:builds
                               {:app
                                {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:source-paths ["env/prod/clj"]
                       :hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
