(ns iframe-app.dev
  (:require [iframe-app.core :as core]
            [figwheel.client :as figwheel :include-macros true]
            [weasel.repl :as weasel]
            [cljs.core.async :refer [put!]]))

(enable-console-print!)
;
(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback (fn []
                     (core/main)))

(weasel/connect "ws://localhost:9001" :verbose true :print #{:repl :console})

(core/main)
