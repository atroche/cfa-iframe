(ns iframe-app.components.selections.field-value
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


(defcomponent field-value-selector [{:keys [selections conditions]} owner]
  (render-state [_ _]
    (html
      [:td.key
       (let [{:keys [selector-channel]} (om/get-shared owner)
             conditions-to-use (active-conditions selections conditions)
             selected-field (:master-field selections)
             text-field (= (:type selected-field) "text")
             field-values (if text-field
                            (map :field-value conditions-to-use)
                            (:possible-values selected-field))]
         [:span
          (if text-field
            [:input {:type      "text"
                     :value     (:value (:field-value selections))
                     :on-change (fn [e]
                                  (let [new-value (.-value (.-target e))]
                                    (put! selector-channel
                                          {:selection-to-update :field-value
                                           :new-value           (if (not-empty new-value)
                                                                  {:value new-value
                                                                   :name  new-value})})))}])
          [:div.separator "Available"]
          (for [{:keys [name value] :as field-value} field-values]
            [:ul
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
                                       (put! selector-channel
                                             {:selection-to-update :field-value
                                              :new-value           field-value}))}
               name]]])])])))