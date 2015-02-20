(ns ^:figwheel-load iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [om.core :as om :include-macros true]

    [iframe-app.components.app :refer [app]]
    [iframe-app.initialize-state :refer [init-state!]]
    [iframe-app.fetch-remote-data :refer [fetch-ticket-forms]]

    [dommy.core :refer-macros [sel1]]
    [cljs.core.async :refer [chan <!]]))


(defn attach-app-to-dom! [state ticket-forms]
  (om/root
    app
    state
    {:target (sel1 "#app")
     :shared {:selector-channel (chan)
              :ticket-forms     ticket-forms}}))


(defn main
  "Runs on page load, and when JS gets refreshed during development"
  []
  (go
    (let [ticket-forms (<! (fetch-ticket-forms))
          state (init-state! ticket-forms)]
      (attach-app-to-dom! state ticket-forms))))
