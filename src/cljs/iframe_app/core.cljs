(ns ^:figwheel-load iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.selectors :refer [slave-fields-selector value-selector
                                  master-field-selector user-type-selector
                                  ticket-form-selector]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [ankha.core :as ankha]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [cljs.core.async :refer [put! chan <!]]))


(defonce app-state (atom {:selections {:master-field nil
                                       :field-value  nil
                                       :slave-fields #{}
                                       :user-type    :agent
                                       :ticket-form  nil}}))

(declare render-state)
(declare init-state)
(declare will-mount)


(defcomponent condition-detail [condition owner]
  (render-state [_ state]
    (let [{:keys [master-field field-value slave-fields]} condition]
      (html (let [{:keys [name value]} field-value]
              [:li.value.selectedRule.hardSelect
               [:a.ruleItem
                {:value value
                 :on-click (fn [_]
                             (let [{:keys [selector-channel]} (om/get-shared owner)]
                               (put! selector-channel
                                     {:selection-to-update :master-field
                                      :new-value           master-field})
                               (put! selector-channel
                                     {:selection-to-update :field-value
                                      :new-value           field-value})))}
                [:i.icon-arrow-right] name]
               [:div.pull-right
                [:a.deleteRule {:value value} "×"]]
               [:p
                [:i.icon-arrow-right]
                " "
                (let [slave-field-names (->> slave-fields
                                             (map :name)
                                             (clojure.string/join ", "))]
                  [:em slave-field-names])]])))))


;; Conditions grouped by a common master field
(defcomponent condition-group [{:keys [master-field conditions]} owner]
  (render-state [_ state]
    (html [:li.rule {:data-id (:id master-field)}
             [:div.ruleTitle
              [:i.icon-arrow-right]
              [:a.field
               {:on-click (fn [_]
                            (let [{:keys [selector-channel]} (om/get-shared owner)]
                              (put! selector-channel
                                    {:selection-to-update :master-field
                                     :new-value           master-field})))}
               (:name master-field)]]
             [:ul.unstyled
              (om/build-all condition-detail conditions)]])))



(defcomponent conditions-manager [conditions owner]
  (render-state [_ state]
    (html
      [:div.all_rules
       [:h4.rules_summary_title
        (str "Conditions in this form (" (count conditions) ")")]
       [:ul.unstyled.global
        (let [grouped-conditions (group-by :master-field conditions)]
          (for [[master-field conditions] grouped-conditions]
            (om/build condition-group {:master-field master-field
                                       :conditions conditions})))]])))


(defn remove-condition
  "Takes a list of conditions, and returns all the ones that don't include
   the selected master field / value. Used when “updating” a condition to
   have new slave fields (actually removing old one and adding new one.)"
  [selected-master-field selected-field-value conditions]
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

(defn selected-condition [selections conditions]
  (first (filter (fn [{:keys [master-field field-value]}]
                   (and (= (:master-field selections) master-field)
                        (= (:field-value selections) field-value)))
                 conditions)))

(defn reset-irrelevant-selections
  "When someone selects (e.g.) a master field we want to deselect the value
   and slave fields that were selected (because they only applied to that
   field. Likewise when someone selects a new value."
  [selections selection-to-update conditions]
  (case selection-to-update
    :master-field (assoc selections :field-value nil
                                    :slave-fields #{})
    :field-value (let [slave-fields-of-selected-condition (-> selections
                                                              (selected-condition conditions)
                                                              :slave-fields)]
                   (assoc selections :slave-fields (or slave-fields-of-selected-condition #{})))
    :user-type (assoc selections :master-field nil
                                 :field-value nil
                                 :slave-fields #{})
    :ticket-form (assoc selections :master-field nil
                                   :field-value nil
                                   :slave-fields #{})
    selections))



(defcomponent app [{:keys [conditions selections] :as app-state} owner]
  (will-mount [_]
    (go-loop []
      ; handle updates from the selectors
      (let [selector-channel (om/get-shared owner :selector-channel)
            {:keys [selection-to-update new-value]} (<! selector-channel)
            {:keys [conditions selections]} @app-state
            conditions (active-conditions selections conditions)
            new-selections (-> selections
                               (assoc selection-to-update new-value)
                               (reset-irrelevant-selections selection-to-update conditions))]
        (om/update! app-state :selections new-selections)

        (when (= selection-to-update :slave-fields)
          (let [updated-conditions (update-conditions conditions new-selections)
                {:keys [user-type ticket-form]} selections]
            (om/update! app-state [:conditions user-type (form->form-kw ticket-form)] updated-conditions))))
      (recur)))
  (render-state [_ _]
    (html
      [:section.ember-view.apps.app-554.apps_nav_bar.app_pane.main_panes
       [:header
        [:h3 "Conditional Fields"]]
       [:div.cfa_navbar
        {:data-main 1}
        [:div.pane.left
         [:h4 "Conditions for:"]
         (om/build user-type-selector (:selections app-state))


         [:h4 "Ticket Form:"]
         (om/build ticket-form-selector (:selections app-state))

         [:aside.sidebar
          (om/build conditions-manager (active-conditions selections conditions))]]
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
           [:li (str "Fields to show (" (count (:slave-fields (:selections app-state))) ")")]]

          [:div.table-wrapper
           [:table.table
            [:tbody
             [:tr
              (om/build master-field-selector
                        app-state)


              (om/build value-selector
                        app-state)

              [:td.selected
               [:div.values
                [:div.separator "Available"]
                (om/build slave-fields-selector (:selections app-state))]]]]]

           (om/build ankha/inspector
                     app-state)]]]

        [:footer
         [:div.pane
          [:button.delete.text-error.deleteAll
           {:style {:display :none}}
           "Delete all conditional rules for this form"]
          [:div.action-buttons.pull-right
           [:button.btn.cancel {:disabled "disabled"} "Cancel changes"]
           [:button.btn.btn-primary.save {:disabled "disabled"} "Save"]]]]]])))


(defn main []
  (go
    (let [ticket-forms-chan (chan)]
      (fetch-ticket-forms ticket-forms-chan)

      (let [ticket-forms (<! ticket-forms-chan)]
        (swap! app-state assoc-in [:selections :ticket-form] (first ticket-forms))

        (om/root
          app
          app-state
          {:target (. js/document (getElementById "app"))
           :shared {:selector-channel (chan)
                    :ticket-forms     ticket-forms}})))))
