(ns iframe-app.condition-selector
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [cljs.core.async :refer [put! chan <!]]))

(declare render-state)
(declare init-state)
(declare will-mount)

(defcomponent value-picker [selections owner]
  (render-state [_ _]
    (html
      [:ul
       (let [selected-field (first (filter (partial = (:master-field selections))
                                           (om/get-shared owner :ticket-fields)))
             field-values (:possible-values selected-field)]
         (for [{:keys [name value] :as field-value} field-values]
           [:li {:class (if (= field-value (:field-value selections))
                          "active")}
            [:a.value {:value    value
                       :on-click (fn [e]
                                   (let [{:keys [pick-channel]} (om/get-shared owner)]
                                     (put! pick-channel
                                           {:selection-to-update :field-value
                                            :new-value           field-value})))}
             name]]))])))


(defcomponent slave-fields-picker [{:keys [master-field field-value slave-fields]} owner]
  (render-state [_ _]
    (html
      [:ul
       (if field-value
         (for [{:keys [name id] :as ticket-field} (remove (partial = master-field)
                                                          (om/get-shared owner :ticket-fields))]
           [:li
            [:a
             (let [field-is-selected (slave-fields ticket-field)]
               {:on-click (fn [e]
                            (let [{:keys [pick-channel]} (om/get-shared owner)
                                  updated-slave-fields (if field-is-selected
                                                         (disj slave-fields ticket-field)
                                                         (conj slave-fields ticket-field))]
                              (put! pick-channel
                                    {:selection-to-update :slave-fields
                                     :new-value           updated-slave-fields})))
                :class    (str "selectedField" (if field-is-selected " assigned"))
                :style    {:font-weight (if field-is-selected "bold")}
                :value    id})
             name]]))])))


(defcomponent field-list [selections owner {:keys [fields highlighted-by-default label]}]
  (render-state [_ _]
    (html
      [:div.field-list
       [:div.separator (str label " (" (count fields) ")")]
       [:ul.available
        (for [{:keys [name id] :as ticket-field} fields]
          [:li {:class (if (= ticket-field (:master-field selections))
                         "active")}
           [:a.field
            {:class    (if highlighted-by-default "assigned")
             :value    id
             :on-click (fn [e]
                         (let [{:keys [pick-channel]} (om/get-shared owner)]
                           (put! pick-channel
                                 {:selection-to-update :master-field
                                  :new-value           ticket-field})))}
            name]])]])))


(defcomponent master-field-picker [app-state owner]
  (render-state [_ _]
    (html

      (let [fields-in-conditions (->> @app-state :conditions (map :master-field) set)
            fields-not-in-conditions (difference (set (om/get-shared owner :ticket-fields))
                                                 fields-in-conditions)
            selections (:selections app-state)]
        [:td.fields
         (om/build field-list selections {:opts {:fields                 fields-not-in-conditions
                                                 :highlighted-by-default false
                                                 :label                  "Available"}})
         (if (not (empty? fields-in-conditions))
           (om/build field-list selections {:opts {:fields                 fields-in-conditions
                                                   :highlighted-by-default true
                                                   :label                  "Existing conditions"}}))]))))
