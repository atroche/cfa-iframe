(ns iframe-app.dev
  (:require [iframe-app.core :as core]
            [figwheel.client :refer [start]]
            [cljs.core.async :refer [put!]]))

(enable-console-print!)

(start {:websocket-url       "wss://figwheel.zd-dev.com/figwheel-ws"
        :load-warninged-code true
        :build-id            "app"
        :on-jsload           (fn []
                               (core/main))})

(core/main)
