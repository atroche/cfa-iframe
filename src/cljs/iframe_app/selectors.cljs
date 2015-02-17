(ns iframe-app.selectors
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [iframe-app.utils :refer [active-conditions form->form-kw string->int]]
    [cljs.core.async :refer [put! chan <!]]
    [dommy.core :as dommy]))

(declare render-state)
(declare init-state)
(declare will-mount)

(defcomponent value-selector [{:keys [selections conditions]} owner]
  (render-state [_ _]
    (html
      [:ul
       (let [conditions-to-use (active-conditions selections conditions)
             selected-field (:master-field selections)
             field-values (:possible-values selected-field)]
         (for [{:keys [name value] :as field-value} field-values]
           [:li {:class (if (= field-value (:field-value selections))
                          "selected")}
            [:a.value {:value      value
                       :data-value value
                       :class      (str "field-value "
                                        (let [value-is-in-condition (some (fn [condition]
                                                                            (and (= (:master-field condition) selected-field)
                                                                                 (= (:field-value condition) field-value)))
                                                                          conditions-to-use)]
                                          (if value-is-in-condition
                                            "assigned")))
                       :on-click   (fn [e]
                                     (let [{:keys [selector-channel]} (om/get-shared owner)]
                                       (put! selector-channel
                                             {:selection-to-update :field-value
                                              :new-value           field-value})))}
             name]]))])))

(defn fields-without-field [fields field]
  (remove (partial = field)
          fields))

(defn active-ticket-fields [selections ticket-forms]
  (let [form-id (get-in selections [:ticket-form :id])]
    (->> ticket-forms
         (filter #(= (:id %) form-id))
         first
         :ticket-fields
         (filter #(or (= :agent (:user-type selections))
                      (:show-to-end-user %))))))

(defcomponent slave-fields-selector [{:keys [master-field field-value slave-fields] :as selections} owner]
  (render-state [_ _]
    (html
      [:ul
       (if field-value
         (let [ticket-forms (om/get-shared owner :ticket-forms)
               ticket-fields (active-ticket-fields selections ticket-forms)
               available-fields (fields-without-field ticket-fields
                                                      master-field)]
           (for [{:keys [name id] :as ticket-field} available-fields]
             [:li
              [:a
               (let [field-is-selected (slave-fields ticket-field)]
                 {:on-click (fn [e]
                              (let [{:keys [selector-channel]} (om/get-shared owner)
                                    updated-slave-fields (if field-is-selected
                                                           (disj slave-fields ticket-field)
                                                           (conj slave-fields ticket-field))]
                                (put! selector-channel
                                      {:selection-to-update :slave-fields
                                       :new-value           updated-slave-fields})))
                  :class    (str "selectedField slave-field " (if field-is-selected " assigned"))
                  :data-id  id
                  :style    {:font-weight (if field-is-selected "bold")}
                  :value    id})
               name]])))])))


(defcomponent master-field [selections owner {:keys [field]}]
  (render-state [_ _]
    (html
      [:li {:class (if (= field (:master-field selections))
                     "selected")}
       [:a.field.master-field
        {:value    (:id field)
         :data-id  (:id field)
         :on-click (fn [_]
                     (let [{:keys [selector-channel]} (om/get-shared owner)]
                       (put! selector-channel
                             {:selection-to-update :master-field
                              :new-value           field})))}
        (:name field)]])))

(defcomponent master-field-list [selections _ {:keys [fields label list-class]}]
  (render-state [_ _]
    (html
      [:div.field-list
       [:div.separator (str label " (" (count fields) ")")]
       [:ul {:class list-class}
        (for [ticket-field fields]
          (om/build master-field
                    selections
                    {:opts {:field ticket-field}}))]])))


(defcomponent master-field-selector [app-state owner]
  (render-state [_ _]
    (html
      (let [conditions (active-conditions (:selections app-state) (:conditions app-state))
            fields-in-conditions (set (map :master-field conditions))
            ticket-forms (om/get-shared owner :ticket-forms)
            ticket-fields (active-ticket-fields (:selections app-state) ticket-forms)
            fields-not-in-conditions (difference (set ticket-fields)
                                                 fields-in-conditions)
            selections (:selections app-state)]
        [:td.fields
         (om/build master-field-list selections {:opts {:fields     fields-not-in-conditions
                                                        :list-class "available-fields"
                                                        :label      "Available"}})
         (if (not (empty? fields-in-conditions))
           (om/build master-field-list selections {:opts {:fields     fields-in-conditions
                                                          :list-class "fields-in-existing-conditions"
                                                          :label      "Existing conditions"}}))]))))



(defcomponent user-type-selector [selections owner]
  (render-state [_ _]
    (html
      [:select {:name      "user-type"
                :on-change (fn [e]
                             (let [selector-channel (om/get-shared owner :selector-channel)
                                   new-user-type (keyword (dommy/value (.-target e)))]
                               (put! selector-channel
                                     {:selection-to-update :user-type
                                      :new-value           new-user-type})))}
       [:option {:value    "agent"
                 :selected (= :agent (:user-type selections))}
        "Agent"]
       [:option {:value    "end-user"
                 :selected (= :end-user (:user-type selections))}
        "End User"]])))

(defcomponent ticket-form-selector [selections owner]
  (render-state [_ _]
    (html
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
            (:name ticket-form)])]))))
