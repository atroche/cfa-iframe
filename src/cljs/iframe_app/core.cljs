(ns iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
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


(defn get-fields-by-ids [fields ids]
  (filter #(ids (:id %)) fields))


(defcomponent conditions-manager [conditions owner]
  (render-state [_ state]

    (html
      [:ul.unstyled.global
       (for [condition conditions]
         (let [master-field (first (filter #(= (:id %) (:master-field-id condition))
                                           dummy-ticket-fields))]
           [:li.rule {:data-id (:master-field-id condition)}
            [:div.ruleTitle
             [:i.icon-arrow-right]
             [:a.field
              (:name master-field)]]
            [:ul.unstyled
             (let [value-name (->> master-field
                                   :possible-values
                                   (filter #(= (:value condition) (:value %)))
                                   first
                                   :name)]
               [:li.value.selectedRule.hardSelect
                [:a.ruleItem {:value (:value condition)} [:i.icon-arrow-right] value-name]
                [:div.pull-right [:a.deleteRule {:value (:value condition)} "×"]]
                [:p
                 [:i.icon-arrow-right]
                 " "
                 (let [slave-fields (get-fields-by-ids dummy-ticket-fields (:slave-fields condition))
                       slave-field-names (->> slave-fields
                                              (map :name)
                                              (clojure.string/join ", "))]
                   [:em slave-field-names])]])]]))])))






(defcomponent master-field-picker [ticket-fields owner]
  (render-state [_ {:keys [selected-field-id]}]
    (html
      [:ul.available
       (for [{:keys [name id] :as ticket-field} ticket-fields]
         (let [is-selected (= id selected-field-id)]
           [:li {:class (if is-selected
                          "active")}
            [:a.field
             {:value    id
              :on-click (fn [e]
                          (let [{:keys [pick-channel]} (om/get-shared owner)]
                            (om/set-state! owner :selected-field-id id)
                            (put! pick-channel
                                  {:updating :selected-field-id
                                   :value    id})))}
             name]]))])))


(defcomponent value-picker [values owner]
  (render-state [_ {:keys [selected-value]}]
    (html
      [:ul
       (for [{:keys [name value]} values]
         [:li {:class (if (= value selected-value)
                        "active")}
          [:a.value {:value    value
                     :on-click (fn [e]
                                 (let [{:keys [pick-channel]} (om/get-shared owner)]
                                   (om/set-state! owner :selected-value value)
                                   (put! pick-channel
                                         {:updating :selected-value
                                          :value    value})))}
           name]])])))


(defcomponent slave-fields-picker [ticket-fields owner {:keys [slave-fields-picker-chan]}]
  (init-state [_]
    {:selected-slave-fields #{}})
  (will-mount [_]
    (go-loop []
      (let [{:keys [msg value]} (<! slave-fields-picker-chan)]
        (case msg
          :selected-master-field-id (om/set-state! owner
                                                   :selected-master-field-id
                                                   value)
          :wipe-selected-slave-fields (om/set-state! owner :selected-slave-fields #{}))
        (recur))))
  (render-state [_ {:keys [selected-master-field-id
                           selected-slave-fields]}]
    (html
      [:ul
       (for [{:keys [name id]} (filter #(not= (:id %) selected-master-field-id)
                                       ticket-fields)]
         [:li
          [:a {:on-click (fn [e]
                           (let [updated-slave-fields
                                 (if (selected-slave-fields id)
                                   (disj selected-slave-fields id)
                                   (conj selected-slave-fields id))]
                             (om/set-state! owner
                                            :selected-slave-fields
                                            updated-slave-fields)

                             (let [{:keys [pick-channel]} (om/get-shared owner)]
                               (put! pick-channel
                                     {:updating :selected-slave-fields
                                      :value    updated-slave-fields}))))
               :class    (if (selected-slave-fields id)
                           "selectedField assigned"
                           "selectedField")
               :style    {:font-weight (if (selected-slave-fields id)
                                         "bold")}
               :value    id}
           name]])])))

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
        ; handle updates from the three picker components
        (let [{:keys [updating value]} (<! pick-channel)
              slave-fields-picker-chan (om/get-state owner :slave-fields-picker-chan)] ; grab new values from channel
          ; set them on local state:
          (om/set-state! owner updating value)
          (case updating
            :selected-field-id (put! slave-fields-picker-chan
                                     {:msg   :selected-master-field-id
                                      :value value})
            :selected-value (put! slave-fields-picker-chan
                                  {:msg :wipe-selected-slave-fields})
            nil))

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
    (html
      [:div.cfa_navbar
       [:div.pane.left
        [:aside.sidebar
         [:div.all_rules
          [:h4.rules_summary_title
           "Conditions in this form"]
          (om/build conditions-manager (:conditions app-state))]]]
       [:div.pane.right.section
        [:section.main
         [:ul.table-header.clearfix
          [:li "Fields"]
          [:li "Values"]
          [:li "Fields to show"]]

         [:div.table-wrapper
          [:table.table
           [:tbody
            [:tr
             [:td.fields
              [:div.separator "Available"]
              (om/build master-field-picker
                        (:ticket-fields app-state)
                        {:opts selected-field-id})]

             [:td.key
              [:div.values
               [:div.separator "Available"]
               (let [selected-field (first (filter #(= selected-field-id (:id %))
                                                   (:ticket-fields app-state)))
                     possible-values (:possible-values selected-field)]
                 (om/build value-picker
                           possible-values))]]
             [:td.selected
              [:div.values
               [:div.separator "Available"]
               (om/build slave-fields-picker
                         (if selected-value
                           (:ticket-fields app-state)
                           [])
                         {:opts {:slave-fields-picker-chan slave-fields-picker-chan}})]]]]]]]]])))







(defn main []
  (om/root
    app
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:pick-channel (chan)}}))

;
;(let [app (.init js/ZAFClient)]
;  (.postMessage app "loaded")
;  (.on app "iframe.fetchedTicketFields" (fn [data] (println data)))
