(ns iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :refer [put! chan <!]]))


;
;(def dummy-ticket-fields
;  [{:name            "Priority"
;    :id              1234
;    :type            :system
;    :possible-values [{:name "Low" :value "low"}
;                      {:name "Normal" :value "normal"}
;                      {:name "High" :value "high"}
;                      {:name "Urgent" :value "urgent"}]}
;
;   {:name            "custom field"
;    :id              4321
;    :type            :tagger
;    :possible-values [{:name "asdfasd" :value "asdfasd"}
;                      {:name "12345" :value "12345"}]}
;   {:name            "a third field"
;    :id              9999
;    :type            :tagger
;    :possible-values [{:name "value number one" :value "v1"}
;                      {:name "value number two" :value "v2"}]}])
;
;(defonce app-state (atom {:ticket-fields dummy-ticket-fields
;                          :conditions    #{}}))
;
;(declare render-state)
;(declare init-state)
;(declare will-mount)
;
;(defcomponent conditions-manager [conditions owner]
;  (render-state [_ state]
;    (dom/ul
;      {:class "unstyled global"}
;      (for [condition conditions]
;        (dom/li
;          {:class "rule"}
;          (dom/div
;            {:class "ruleTitle"}
;            (dom/a
;              {:class "field"}
;              (:name (first (filter #(= (:id %) (:master-field-id condition))
;                                    dummy-ticket-fields))))))))))
;
;
;
;(defcomponent master-field-picker [ticket-fields owner]
;  (render-state [_ {:keys [selected-field-id]}]
;    (dom/ul
;      {:class "available"}
;      (for [{:keys [name id] :as ticket-field} ticket-fields]
;        (dom/a
;          {:class "field"}
;          (dom/li {:on-click (fn [e]
;                               (let [{:keys [pick-channel]} (om/get-shared owner)]
;                                 (om/set-state! owner :selected-field-id id)
;                                 (put! pick-channel
;                                       {:updating :selected-field-id
;                                        :value id})))
;                   :style    {:font-weight (if (= id selected-field-id)
;                                             "bold")}}
;                  name))))))
;
;(defcomponent value-picker [values owner]
;  (render-state [_ {:keys [selected-value]}]
;    (dom/ul
;      (for [{:keys [name value]} values]
;        (dom/li
;          {:on-click (fn [e]
;                       (let [{:keys [pick-channel]} (om/get-shared owner)]
;                         (om/set-state! owner :selected-value value)
;                         (put! pick-channel
;                               {:updating :selected-value
;                                :value :value})))
;           :style    {:font-weight (if (= value selected-value)
;                                     "bold")}}
;          name)))))
;
;(defcomponent slave-fields-picker [ticket-fields owner {:keys [slave-fields-picker-chan]}]
;  (init-state [_]
;    {:selected-slave-fields #{}})
;  (will-mount [_]
;    (go-loop []
;      (let [{:keys [msg value]} (<! slave-fields-picker-chan)]
;        (case msg
;          :selected-master-field-id (om/set-state! owner
;                                                   :selected-master-field-id
;                                                   value)
;          :wipe-selected-slave-fields (om/set-state! owner :selected-slave-fields #{}))
;        (recur))))
;  (render-state [_ {:keys [selected-master-field-id
;                           selected-slave-fields]}]
;    (dom/ul
;      (for [{:keys [name id]} (filter #(not= (:id %) selected-master-field-id)
;                                      ticket-fields)]
;        (dom/li
;          {:on-click (fn [e]
;                       (let [updated-slave-fields
;                             (if (selected-slave-fields id)
;                               (disj selected-slave-fields id)
;                               (conj selected-slave-fields id))]
;                         (om/set-state! owner
;                                        :selected-slave-fields
;                                        updated-slave-fields)
;
;                         (let [{:keys [pick-channel]} (om/get-shared owner)]
;                           (println updated-slave-fields)
;                           (put! pick-channel
;                                 {:updating :selected-slave-fields
;                                  :value updated-slave-fields}))))
;           :style    {:font-weight (if (selected-slave-fields id)
;                                     "bold")}}
;          name)))))
;
;(defn remove-condition [selected-field selected-value conditions]
;  (set (remove (fn [{:keys [master-field-id value]}]
;                 (and (= master-field-id selected-field)
;                      (= value selected-value)))
;               conditions)))
;
;(defcomponent app [app-state owner]
;  (init-state [_]
;    {:slave-fields-picker-chan (chan)})
;  (will-mount [_]
;    (let [{:keys [pick-channel]} (om/get-shared owner)]
;      (go-loop []
;        ; handle updates from the three picker components
;        (let [{:keys [updating value]} (<! pick-channel)
;              slave-fields-picker-chan (om/get-state owner :slave-fields-picker-chan)] ; grab new values from channel
;          ; set them on local state:
;          (om/set-state! owner updating value)
;          (case updating
;            :selected-field-id (put! slave-fields-picker-chan
;                                     {:msg :selected-master-field-id
;                                      :value value})
;            :selected-value (put! slave-fields-picker-chan
;                                  {:msg :wipe-selected-slave-fields})
;            nil))
;
;        ;(let [{:keys [selected-field-id
;        ;              selected-value
;        ;              selected-slave-fields] :as state} (om/get-state owner)]
;        ;  (when (and selected-field-id selected-value)
;        ;    ; when a conditions values have just changed
;        ;    (when (not (empty? selected-slave-fields))
;        ;      (om/transact! app-state
;        ;                    :conditions
;        ;                    (fn [conditions]
;        ;                      (let [conditions (remove-condition selected-field-id
;        ;                                                         selected-value
;        ;                                                         conditions)]
;        ;                        (conj conditions {:master-field-id selected-field-id
;        ;                                          :value           selected-value
;        ;                                          :slave-fields    selected-slave-fields})))))
;        ;    (when (empty? selected-slave-fields)
;        ;      ; when all values have been toggled off for a condition
;        ;      (om/transact! app-state
;        ;                    :conditions
;        ;                    (partial remove-condition
;        ;                             selected-field-id
;        ;                             selected-value)))))
;        (recur))))
;  (render-state [_ {:keys [selected-field-id slave-fields-picker-chan
;                           selected-value]}]
;    (dom/div
;      {:class "cfa_navbar"}
;      (dom/div
;        {:class "pane left"}
;        (dom/aside
;          {:class "sidebar"}
;          (dom/div
;            {:class "all_rules"}
;            (dom/h4
;              {:class "rules_summary_title"}
;              "Conditions in this form")
;            (om/build conditions-manager (:conditions app-state)))))
;      (dom/div
;        {:class "pane right section"}
;        (dom/section
;          {:class "main"}
;
;          (dom/ul
;            {:class "table-header clearfix"}
;            (dom/li
;              "Fields")
;            (dom/li "Values")
;            (dom/li "Fields to show"))
;
;          (dom/div
;            {:class "table-wrapper"}
;            (dom/table
;              {:class "table"}
;              (dom/tbody
;                (dom/tr
;                  (dom/td
;                    {:class "fields"}
;                    (dom/div
;                      {:class "separator"}
;                      "Available")
;                    (om/build master-field-picker
;                              (:ticket-fields app-state)
;                              {:opts selected-field-id}))
;                  (dom/td
;                    (dom/div
;                      {:class "values"}
;                      (dom/div
;                        {:class "separator"}
;                        "Available")
;                      (let [selected-field (first (filter #(= selected-field-id (:id %))
;                                                          (:ticket-fields app-state)))
;                            possible-values (:possible-values selected-field)]
;                        (om/build value-picker
;                                  possible-values))))
;                  (dom/td
;                    {:class "selected"}
;                    (dom/div
;                      {:class "values"}
;                      (dom/div
;                        {:class "separator"}
;                        "Available")
;                      (om/build slave-fields-picker
;                                (if selected-value
;                                  (:ticket-fields app-state)
;                                  [])
;                                {:opts {:slave-fields-picker-chan slave-fields-picker-chan}})
;                      ))
;                  ))))
;          )))
;    ))
;
;
;
;
;
;
;
;(defn main []
;  (om/root
;    app
;    app-state
;    {:target (. js/document (getElementById "app"))
;     :shared {:pick-channel (chan)}}))

;
;(let [app (.init js/ZAFClient)]
;  (.postMessage app "loaded")
;  (.on app "iframe.fetchedTicketFields" (fn [data] (println data)))
