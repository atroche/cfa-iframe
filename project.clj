(defproject iframe-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :test-paths ["spec/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2740"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.2"]
                 [org.clojure/test.check "0.7.0"]
                 [compojure "1.3.1"]
                 [enlive "1.1.5"]
                 [com.cemerick/clojurescript.test "0.3.3"]
                 [org.omcljs/om "0.8.7"]
                 [environ "1.0.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/om-tools "0.3.9" :exclusions [potemkin]]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.3"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "iframe-app.jar"

  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test"]}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "spec/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        ;:source-map    "resources/public/js/out.js.map"
                                        :externs       ["react/externs/react.js"]
                                        :optimizations :whitespace
                                        ;:pretty-print  true
                                        :closure-warnings {:non-standard-jsdoc :off}}}
                       :test {:source-paths ["src/cljs" "spec/cljs"]
                              :notify-command ["slimerjs" "resources/runner.js"
                                               "resources/public/js/app-test.js"]
                              :compiler {:output-to     "resources/public/js/app-test.js"
                                         :output-dir    "resources/public/js/out-test"
                                         ;:source-map    "resources/public/js/out-test.js.map"
                                         :externs       ["react/externs/react.js"]
                                         :optimizations :whitespace
                                         ;:pretty-print  true
                                         :closure-warnings {:non-standard-jsdoc :off}}}}
              :test-commands {"slimer" ["slimerjs" "resources/runner.js"
                                        "resources/public/js/app-test.js"]}}

  :profiles {:dev {:source-paths ["env/dev/clj"]

                   :dependencies [[figwheel "0.1.6-SNAPSHOT"]
                                  [com.cemerick/piggieback "0.1.3"]
                                  [weasel "0.4.2"]
                                  [leiningen "2.5.0"]]

                   :repl-options {:init-ns iframe-app.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.1.6-SNAPSHOT"]]

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
