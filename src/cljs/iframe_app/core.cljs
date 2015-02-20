(ns ^:figwheel-load iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]

    [iframe-app.components.app :refer [app]]
    [iframe-app.utils :refer [active-conditions form->form-kw blank-conditions]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [iframe-app.persistence :refer [get-persisted-conditions]]
    [cljs.core.async :refer [put! chan <!]]))


(defonce app-state
         (atom {:selections {:master-field nil
                             :field-value  nil
                             :slave-fields #{}
                             :user-type    :agent
                             :ticket-form  nil}}))


(defn main []
  (go
    (let [ticket-forms-chan (chan)
          _ (fetch-ticket-forms ticket-forms-chan)
          ticket-forms (<! ticket-forms-chan)]
      (swap! app-state assoc-in [:selections :ticket-form] (first ticket-forms))

      (let [initial-conditions (if-let [persisted-conditions (get-persisted-conditions)]
                                 persisted-conditions
                                 (blank-conditions ticket-forms))]
        (swap! app-state assoc-in [:conditions] initial-conditions))




      ;(swap! app-state assoc-in [:conditions] (get-persisted-conditions))

      (om/root
        app
        app-state
        {:target (. js/document (getElementById "app"))
         :shared {:selector-channel (chan)
                  :ticket-forms     ticket-forms}}))))
