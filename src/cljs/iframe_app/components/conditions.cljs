(ns iframe-app.components.conditions
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [ankha.core :as ankha]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-remote-data :refer [fetch-ticket-forms]]
    [cljs.core.async :refer [put! chan <!]]))


(defcomponent condition-detail [condition owner]
  (render-state [_ state]
    (let [{:keys [master-field field-value slave-fields]} condition]
      (html
        (let [{:keys [name value]} field-value]
          [:li.value.selectedRule.hardSelect
           [:a.ruleItem
            {:value    value
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
    (html
      [:li.rule {:data-id (:id master-field)}
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
                                       :conditions   conditions})))]])))
