(ns iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.condition-selector :refer [slave-fields-picker value-picker
                                           master-field-picker field-list]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [cljs.core.async :refer [put! chan <!]]))

; TODO: instead of storing stuff as IDs, store the whole data structure (e.g. under conditions)
; TODO: store selected fields under app state, share them between most subcomponents and quit doing channels so much


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


(defonce app-state (atom {:selections {:master-field nil
                                       :field-value  nil
                                       :slave-fields #{}}
                          :conditions #{}}))


(declare render-state)
(declare init-state)
(declare will-mount)



(defcomponent conditions-manager [conditions owner]
  (render-state [_ state]
    (html
      [:ul.unstyled.global
       (for [{:keys [master-field field-value slave-fields]} conditions]
         [:li.rule {:data-id (:id master-field)}
          [:div.ruleTitle
           [:i.icon-arrow-right]
           [:a.field
            (:name master-field)]]
          [:ul.unstyled
           (let [{:keys [name value]} field-value]

             [:li.value.selectedRule.hardSelect
              [:a.ruleItem {:value value}
               [:i.icon-arrow-right] name]
              [:div.pull-right
               [:a.deleteRule {:value value} "×"]]
              [:p
               [:i.icon-arrow-right]
               " "
               (let [slave-field-names (->> slave-fields
                                            (map :name)
                                            (clojure.string/join ", "))]
                 [:em slave-field-names])]])]])])))




(defn remove-condition [selected-master-field selected-field-value conditions]
  (set (remove (fn [{:keys [master-field field-value]}]
                 (and (= master-field selected-master-field)
                      (= field-value selected-field-value)))
               conditions)))

(defn update-conditions [conditions {:keys [master-field field-value slave-fields]}]
  (let [cleaned-conditions (remove-condition master-field
                                             field-value
                                             conditions)

        slave-fields-selected? (not (empty? slave-fields))
        new-condition {:master-field master-field
                       :field-value  field-value
                       :slave-fields slave-fields}]
    (if slave-fields-selected?
      (conj cleaned-conditions new-condition)
      cleaned-conditions)))

(defn master-field-and-values-selected? [{:keys [master-field field-value]}]
  (every? (comp not nil?) [master-field field-value]))



:slave-fields (let [{:keys [master-field field-value] :as selections} (:selections @app-state)
                    conditions (:conditions @app-state)
                    master-and-value-selected? (every? (comp not nil?) [master-field field-value])]
                (when master-and-value-selected?
                  (let [updated-conditions (update-conditions conditions selections)]
                    (om/update! app-state [:conditions] updated-conditions))))

(defn reset-irrelevant-selections
  "When someone selects a master field (e.g.) we want to deselect the value
   and slave fields that were selected (because they only applied to that
   field. Likewise when someone selects a new value."
  [selections selection-to-update]
  (case selection-to-update
    :master-field (assoc selections :field-value nil
                                    :slave-fields #{})
    :field-value (assoc selections :slave-fields #{})
    selections))

(defcomponent app [app-state owner]
  (init-state [_]
    {:selections (:selections app-state)})
  (will-mount [_]
    (go-loop []
      ; handle updates from the three picker components
      (let [pick-channel (om/get-shared owner :pick-channel)
            {:keys [selection-to-update new-value]} (<! pick-channel)
            new-selections (-> (om/get-state owner :selections)
                               (assoc selection-to-update new-value)
                               (reset-irrelevant-selections selection-to-update))]
        (om/update! app-state :selections new-selections)
        (om/set-state! owner :selections new-selections)

        (when (= selection-to-update :slave-fields)
          (let [updated-conditions (update-conditions (:conditions app-state) new-selections)]
            (om/update! app-state [:conditions] updated-conditions))))

      (recur)))
  (render-state [_ {:keys [selected-field-id slave-fields-picker-chan
                           selected-value]}]
    (html
      [:section.ember-view.apps.app-554.apps_nav_bar.app_pane.main_panes
       [:header
        [:h3 "Conditional Fields"]]



       [:div.cfa_navbar
        {:data-main 1}
        [:div.pane.left
         [:h4 "Conditions for:"]
         [:select {:style {:width "90%"}}
          [:option "Agent"]
          [:option "what"]]

         [:h4 "Ticket Form:"]
         [:select {:style {:width "90%"}}
          [:option "Default Ticket Form"]
          [:option "what"]]

         [:aside.sidebar
          [:div.all_rules
           [:h4.rules_summary_title
            (str "Conditions in this form (" (count (:conditions app-state)) ")")]
           (om/build conditions-manager (:conditions app-state))]]]
        [:div.pane.right.section
         [:section.main
          [:div.intro
           [:h3 "Manage conditional fields"]
           [:p
            "Configure your conditions to build your conditional fields. Select a field, a value for that field, and the appropriate field to show. "
            [:a
             {:target "_blank",
              :href
                      "https://support.zendesk.com/entries/26674953-Using-the-Conditional-Fields-app-Enterprise-Only-"}
             "Learn more."]]]

          [:ul.table-header.clearfix
           [:li "Fields"]
           [:li "Values"]
           [:li "Fields to show"]]

          [:div.table-wrapper
           [:table.table
            [:tbody
             [:tr
              (om/build master-field-picker
                        app-state)

              [:td.key
               [:div.values
                [:div.separator "Available"]
                (om/build value-picker
                          (:selections app-state))
                ]]
              [:td.selected
               [:div.values
                [:div.separator "Available"]
                (om/build slave-fields-picker (:selections app-state))]]]]]]]
         ]

        [:footer
         [:div.pane
          [:div
           (prn-str (clj->js (:selections app-state)))]
          [:button.delete.text-error.deleteAll
           {:style {:display :none}}
           "Delete all conditional rules for this form"]
          [:div.action-buttons.pull-right
           [:button.btn.cancel {:disabled "disabled"} "Cancel changes"]
           [:button.btn.btn-primary.save {:disabled "disabled"} "Save"]]]]]])))





(defn main []
  (om/root
    app
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:pick-channel  (chan)
              :ticket-fields dummy-ticket-fields}}))








