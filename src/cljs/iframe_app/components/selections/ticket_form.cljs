(ns iframe-app.components.selections.ticket-form
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [iframe-app.utils :refer [active-conditions active-ticket-fields form->form-kw string->int]]
    [cljs.core.async :refer [put! chan <!]]
    [dommy.core :as dommy]))


(defcomponent ticket-form-selector [selections owner]
  (render-state [_ _]
    (html
      [:div
       [:h4 "Ticket Form:"]
       (let [{:keys [ticket-forms selector-channel]} (om/get-shared owner)]
         [:select {:name      "ticket-form"
                   :on-change (fn [e]

                                (let [form-id (string->int (dommy/value (.-target e)))
                                      selected-form (first (filter #(= form-id (:id %)) ticket-forms))]
                                  (put! selector-channel
                                        {:selection-to-update :ticket-form
                                         :new-value           selected-form})))}
          (for [ticket-form ticket-forms]
            [:option {:value    (:id ticket-form)
                      :selected (= (ticket-form (:ticket-form selections)))}
             (:name ticket-form)])])])))
