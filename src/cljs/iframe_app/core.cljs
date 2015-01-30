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

   {:name            "custom field"
    :id              4321
    :type            :tagger
    :possible-values [{:name "asdfasd" :value "asdfasd"}
                      {:name "12345" :value "12345"}]}
   {:name            "a third field"
    :id              9999
    :type            :tagger
    :possible-values [{:name "value number one" :value "v1"}
                      {:name "value number two" :value "v2"}]}])

(defonce app-state (atom {:ticket-fields dummy-ticket-fields
                          :conditions    #{}}))

(declare render-state)
(declare init-state)
(declare will-mount)

(defcomponent conditions-manager [conditions owner]
  (render-state [_ state]
    (dom/div
      (dom/h2
        "conditions")
      (pr-str conditions))))



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
  (render-state [_ {:keys [selected-value]}]
    (dom/div
      (dom/h2
        "possible values")
      (dom/ul
        (for [{:keys [name value]} values]
          (dom/li
            {:on-click (fn [e]
                         (let [{:keys [pick-channel]} (om/get-shared owner)]
                           (om/set-state! owner :selected-value value)
                           (put! pick-channel
                                 {:selected-value value})))
             :style    {:font-weight (if (= value selected-value)
                                       "bold")}}
            name))))))

(defcomponent slave-fields-picker [ticket-fields owner {:keys [slave-fields-picker-chan]}]
  (init-state [_]
    {:selected-slave-fields #{}})
  (will-mount [_]
    (go-loop []
      (let [{:keys [selected-master-field-id
                    wipe-selected-slave-fields]} (<! slave-fields-picker-chan)]
        (when selected-master-field-id
          (om/set-state! owner :selected-master-field-id selected-master-field-id))
        (when wipe-selected-slave-fields
          (println "wtf")
          (om/set-state! owner :selected-slave-fields #{}))
        (recur))))
  (render-state [_ {:keys [selected-master-field-id
                           selected-slave-fields]}]
    (dom/div
      (dom/h2
        "fields to show")
      (dom/ul
        (for [{:keys [name id]} (filter #(not= (:id %) selected-master-field-id)
                                        ticket-fields)]
          (dom/li
            {:on-click (fn [e]
                         (let [updated-slave-fields
                               (if (selected-slave-fields id)
                                 (disj selected-slave-fields id)
                                 (conj selected-slave-fields id))]
                           (om/set-state! owner
                                          :selected-slave-fields
                                          updated-slave-fields)

                           (let [{:keys [pick-channel]} (om/get-shared owner)]
                             (println updated-slave-fields)
                             (put! pick-channel
                                   {:selected-slave-fields updated-slave-fields}))))
             :style    {:font-weight (if (selected-slave-fields id)
                                       "bold")}}
            name))))))

(defn remove-condition [selected-field selected-value conditions]
  (set (remove (fn [{:keys [master-field-id value]}]
                 (and (= master-field-id selected-field)
                      (= value selected-value)))
               conditions)))

(defcomponent app [app-state owner]
  (init-state [_]
    {:slave-fields-picker-chan (chan)})
  (will-mount [_]
    (let [{:keys [pick-channel]} (om/get-shared owner)]
      (go-loop []
        (let [{:keys [selected-field-id selected-value
                      selected-slave-fields]} (<! pick-channel)]
          (when selected-field-id
            (put! (om/get-state owner :slave-fields-picker-chan)
                  {:selected-master-field-id selected-field-id})
            (om/set-state! owner :selected-field-id selected-field-id))
          (when selected-value
            (om/set-state! owner :selected-value selected-value)
            (om/set-state! owner :selected-slave-fields #{})
            (put! (om/get-state owner :slave-fields-picker-chan)
                  {:wipe-selected-slave-fields true}))
          (when selected-slave-fields
            (om/set-state! owner :selected-slave-fields selected-slave-fields)))
        (let [{:keys [selected-field-id
                      selected-value
                      selected-slave-fields] :as state} (om/get-state owner)]
          (when (and selected-field-id selected-value)
            ; when a conditions values have just changed
            (when (not (empty? selected-slave-fields))
              (om/transact! app-state
                            :conditions
                            (fn [conditions]
                              (let [conditions (remove-condition selected-field-id
                                                                 selected-value
                                                                 conditions)]
                                (conj conditions {:master-field-id selected-field-id
                                                  :value           selected-value
                                                  :slave-fields    selected-slave-fields})))))
            (when (empty? selected-slave-fields)
              ; when all values have been toggled off for a condition
              (om/transact! app-state
                            :conditions
                            (partial remove-condition
                                     selected-field-id
                                     selected-value)))))
        (recur))))
  (render-state [_ {:keys [selected-field-id slave-fields-picker-chan
                           selected-value]}]
    (dom/div
      (om/build master-field-picker (:ticket-fields app-state) {:opts selected-field-id})
      (let [selected-field (first (filter #(= selected-field-id (:id %))
                                          (:ticket-fields app-state)))
            possible-values (:possible-values selected-field)]
        (om/build value-picker
                  possible-values))
      (om/build slave-fields-picker
                (if selected-value
                  (:ticket-fields app-state)
                  [])
                {:opts {:slave-fields-picker-chan slave-fields-picker-chan}})
      (om/build conditions-manager (:conditions app-state)))))

(defn main []
  (om/root
    app
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:pick-channel (chan)}}))


;(let [app (.init js/ZAFClient)]
;  (.postMessage app "loaded")
;  (.on app "iframe.fetchedTicketFields" (fn [data] (println data)))