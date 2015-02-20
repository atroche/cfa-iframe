(ns iframe-app.initialize-state
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]

    [iframe-app.components.app :refer [app]]
    [iframe-app.utils :refer [active-conditions form->form-kw blank-conditions]]
    [iframe-app.fetch-remote-data :refer [fetch-ticket-forms]]
    [iframe-app.persistence :refer [get-persisted-conditions]]
    [cljs.core.async :refer [put! chan <!]]))


(defonce app-state
         (atom {:selections {:master-field nil
                             :field-value  nil
                             :slave-fields #{}
                             :user-type    :agent
                             :ticket-form  nil}}))

(defn select-default-ticket-form! [ticket-forms]
  (swap! app-state assoc-in [:selections :ticket-form] (first ticket-forms)))

(defn load-or-initialize-conditions! [ticket-forms]
  (let [initial-conditions (if-let [persisted-conditions (get-persisted-conditions)]
                             persisted-conditions
                             (blank-conditions ticket-forms))]
    (swap! app-state assoc-in [:conditions] initial-conditions)))


(defn init-state! [ticket-forms]
  (select-default-ticket-form! ticket-forms)
  (load-or-initialize-conditions! ticket-forms)
  app-state)