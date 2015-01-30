(ns iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :refer [put! chan <!]]))

(def dummy-ticket-fields
  [{:name            "Priority"
    :id              1234
    :type            :system
    :possible-values [{:name "Low" :value "low"}
                      {:name "Normal" :value "normal"}
                      {:name "High" :value "high"}
                      {:name "Urgent" :value "urgent"}]}

   {:name            "haha"
    :id              4321
    :type            :tagger
    :possible-values [{:name "asdfasd" :value "asdfasd"}
                      {:name "12345" :value "12345"}]}])

(defonce app-state (atom {:ticket-fields dummy-ticket-fields}))



(declare render-state)
(declare init-state)
(declare will-mount)

(defcomponent master-field-picker [ticket-fields owner]
  (render-state [_ {:keys [selected-field-id]}]
    (dom/div
      (dom/h2
        "available fields")
      (dom/ul
        (for [{:keys [name id] :as ticket-field} ticket-fields]
          (dom/li {:on-click (fn [e]
                               (let [{:keys [pick-channel]} (om/get-shared owner)]
                                 (om/set-state! owner :selected-field-id id)
                                 (put! pick-channel
                                       {:selected-field-id id})))
                   :style    {:font-weight (if (= id selected-field-id)
                                             "bold")}}
                  name))))))

(defcomponent value-picker [values owner]

  (render-state [_ state]
    (dom/div
      (dom/h2
        "possible values")
      (dom/ul
        (for [value values]
          (dom/li (:name value)))))))

(defcomponent slave-fields-picker [ticket-fields owner {:keys [slave-fields-picker-chan]}]
  (will-mount [_]
    (go-loop []
      (let [{:keys [selected-master-field-id]} (<! slave-fields-picker-chan)]
        (om/set-state! owner :selected-master-field-id selected-master-field-id)
        (recur))))
  (render-state [_ {:keys [selected-master-field-id]}]
    (dom/div
      (dom/h2
        "fields to show")
      (dom/ul
        (for [ticket-field ticket-fields]
          (if-not (= (:id ticket-field) selected-master-field-id)
            (dom/li (:name ticket-field))))))))

(defcomponent app [app-state owner]
  (init-state [_]
    {:slave-fields-picker-chan (chan)})
  (will-mount [_]
    (let [{:keys [pick-channel]} (om/get-shared owner)]
      (go-loop []
        (when-let [{:keys [selected-field-id]} (<! pick-channel)]
          (put! (om/get-state owner :slave-fields-picker-chan)
                {:selected-master-field-id selected-field-id})
          (om/set-state! owner :selected-field-id selected-field-id))
        (recur))))
  (render-state [_ {:keys [selected-field-id slave-fields-picker-chan]}]
    (dom/div
      (om/build master-field-picker (:ticket-fields app-state) {:opts selected-field-id})
      (let [selected-field (first (filter #(= selected-field-id (:id %))
                                          (:ticket-fields app-state)))
            possible-values (:possible-values selected-field)]
        (om/build value-picker
                  possible-values))
      (om/build slave-fields-picker
                (:ticket-fields app-state)
                {:opts {:slave-fields-picker-chan slave-fields-picker-chan}}))))

(defn main []
  (om/root
    app
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:pick-channel (chan)}}))


;(let [app (.init js/ZAFClient)]
;  (.postMessage app "loaded")
;  (.on app "iframe.fetchedTicketFields" (fn [data] (println data)))