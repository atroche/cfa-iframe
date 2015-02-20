(ns iframe-app.components.selections.master-field
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

